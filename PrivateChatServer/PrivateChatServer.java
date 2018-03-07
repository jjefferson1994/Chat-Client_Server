import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class PrivateChatServer {

		//instance variables
		ServerSocket ss;
		//making list of chatnames using keyed collection
		ConcurrentHashMap<String,InetAddress> whosIn = new ConcurrentHashMap<String,InetAddress>();
		ConcurrentHashMap<String,String> passwords = new ConcurrentHashMap<String,String>();
		Vector<String> whosNotIn = new Vector<String>();
		
		public PrivateChatServer() throws IOException {
			ss = new ServerSocket(4444);
			
			//print address of the server
			System.out.println("PrivateChatServer is up at " + InetAddress.getLocalHost().getHostAddress() + " on port " + ss.getLocalPort());
			//Socket s = ss.accept(); //wait for client connection
			
		}
		
		private void receive() throws IOException
		{
			//capture the receive thread
			while(true) // do forever loop
			{
				try{
				Socket s = ss.accept(); //wait for client connection
				InetAddress clientAddress = s.getInetAddress();
				
			    System.out.println("A client has connected from " + clientAddress);
			    
			    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
			    Object messageFromClient = ois.readObject();
			    if (messageFromClient instanceof String)//JOIN or sendToAll CHAT?
			       {
			       String message = (String) messageFromClient;
			       //Is this a message from a real PrivateChatClient?
			       char firstCharacter = message.charAt(0);
			       if (firstCharacter != '\u007F')//unicode Circle-C
			          {// WRONG NUMBER OR HACKER
			          System.out.println("Unrecognized message received: " + message);
			          ois.close(); // hang up
			          continue;//Go back to loop top to receive next client.
			          }
			       message = message.substring(1).trim();// drop 1st character 
			       //Is this is a JOIN or a chat message?
			       if (message.startsWith("JOIN"))
			          {
			          joinProcessing(s, message.substring(4).trim());//drop "JOIN"
			          continue;//Go back to loop top to receive next client.
			          }         
			        else // must be a chat message for everyone  
			          {
			          chatProcessing(message);
			          continue;//Go back to loop top to receive next client.
			          }
			       }

			    if (messageFromClient instanceof String[])//A Private Message
			       {
			       String[] privateMessageArray = (String[]) messageFromClient;

			       continue;
			       }

			    if (messageFromClient instanceof Vector)//A Save Message
			       {
			       Vector<String> saveMessageVector = (Vector<String>) messageFromClient;
			    	   
			       continue;
			       }
			    System.out.println("Unexpected object received: " + messageFromClient);
			    } 
			catch(IOException e){
				continue;
			}
			catch (ClassNotFoundException cnfe)
			{
			}
			    // All processing routines return to here and 
			    // continue in the receive-from-client loop.
			}
		}

		public static void main(String[] args) { //catch and print Exceptions
			System.out.println("J Jefferson jajeffe2@ncsu.edu");
			
			if(args.length > 0)
			{
				System.out.println("Server does not accept Command Line Parameters");
			}
			
			try
			{
				PrivateChatServer cs = new PrivateChatServer();
				cs.receive(); //call branch to receive
				//InetAddress clientAddress = s.getInetAddress();
			    //System.out.println("A client has connected from " + clientAddress);
			}
			catch (Exception e)
			{
				System.out.println(e);
			}
		}
		
		private void joinProcessing(Socket s, String chatNameAndPassword)
		  {
			try
			{
				//strips to chatname and password
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				int blankOffset = chatNameAndPassword.indexOf(" ");
				if(blankOffset <0){
					oos.writeObject("Invalid Format");
					oos.close();
					return;
				}
				String chatName = chatNameAndPassword.substring(0,blankOffset).toUpperCase();
				String password = chatNameAndPassword.substring(blankOffset).trim();
			
				//ConcurrentHashMap<String,InetAddress> whosIn = new ConcurrentHashMap<String,InetAddress>();
				//InetAddress clientAddress = whosIn.get(chatName); //gets client address
				//whosIn.put(chatName, clientAddress); //add a client and their address

				//checking for chatname occurrence 
				if (!passwords.containsKey(chatName)) //never been in!
				   { // add and join Bubba
					
				   passwords.put(chatName,password); //add Bubba to pw collection
				   whosIn.put(chatName, s.getInetAddress()); //add Bubba to who's in now
				   
				   // Send join confirmation to Bubba
				   oos.writeObject("Welcome " + chatName + " !");
				   oos.close(); // We're done!

				   // Save the updated passwords collection on disk.
				   FileOutputStream fos = new FileOutputStream("passwords.ser");
				   ObjectOutputStream pwoos = new ObjectOutputStream(fos);
				   pwoos.writeObject(passwords);
				   pwoos.close();
				   
				   // Tell everyone that Bubba just joined the chat room.
				   sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
				   return; 
				   }
				
				//if he has joined the chat before
			      String storedPassword = passwords.get(chatName);
			      if (password.equals(storedPassword))//case-sensitive compare
			         { // rejoin Bubba
			         // Send join confirmation to Bubba
			         oos.writeObject("Welcome " + chatName + " !");
			         oos.close(); // We're done!

			         // See if Bubba is rejoining from a new location:
			      if (whosIn.containsKey(chatName)){//Bubba is already in!
			             //We do need to update where he is now.
			        	 whosIn.replace(chatName, s.getInetAddress());
			      		 String[] chatNameList = whosIn.keySet().toArray(new String[0]);
			      		 sendToAll(chatNameList);
			      		 
			      		 whosNotIn.remove(chatName);
			      		 sendToAll(whosNotIn);
			      }
			         	 //but no need to "notify" everyone - Bubba never left!
			         
			      else // Bubba was NOT already in the chat room.
			             {
			        	 whosIn.put(chatName, s.getInetAddress());//add Bubba to who's in now
			        	 String[] chatNameList = whosIn.keySet().toArray(new String[0]);
						 sendToAll(chatNameList);
			        	 // Tell everyone that Bubba just joined the chat room.
			             sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
			             }
			       return; 
			       }
			
			      // If we are still executing at this point, it is because
			      // we did not rejoin Bubba because his password is bad!
			      // Send join failure error message to Bubba:
			      oos.writeUTF("Submitted password " + password + " does not match stored password for " + chatName);
			      oos.close(); // We're done!
			}
			catch (IOException ioe)
			{
				System.out.println("Join Respone Failure:" + ioe);
			}
		  }

		//private void leaveProcessing(String chatName)
		  //{
			
		//  }

		private void chatProcessing(String chatMessageFromClient)
		  {
			int blankOffset = chatMessageFromClient.indexOf(" ");
			if(blankOffset == 0){
				System.out.println("Internal Client Error.");
			}
			String chatName = chatMessageFromClient.substring(0,blankOffset).toUpperCase();
			String message = chatMessageFromClient.substring(blankOffset).trim();
			if(!whosIn.containsKey(chatName)){
				System.out.println("Error Verifying Chat Name.");
				return;
			}
			sendToAll(chatName + " says:" + message);
			
		  }
		
		private void sendToAll(Object message)
		{
			//broadcast message
			//ask keyed collection for list of names
			String leaveList = "";
			String[] clientList = whosIn.keySet().toArray(new String[0]);
			for (String chatName : clientList){
				InetAddress clientAddress = whosIn.get(chatName);
				try{
					Socket s = new Socket(clientAddress,4567);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(message);
					oos.close();
				}
				catch(IOException ioe){//if client goes down without leaving
					whosIn.remove(chatName);
					whosNotIn.add(chatName);
					System.out.println(chatName + "has left.");
					leaveList += chatName + " ";
				}
				// The calling join code will send updated whosIn and whosNotIn lists to everyone.
				// This notifies everyone of who left (different from whosNotIn).
				if (leaveList.length() > 0)
				    sendToAll("Goodbye to " + leaveList
				            + " who has left the chat room.");
			}
		}

	
}
