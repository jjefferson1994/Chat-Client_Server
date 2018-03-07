import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
	//instance variables
	ServerSocket ss;
	//making list of chatnames using keyed collection
	ConcurrentHashMap<String,InetAddress> whosIn = new ConcurrentHashMap<String,InetAddress>();
	ConcurrentHashMap<String,String> passwords = new ConcurrentHashMap<String,String>();
	
	public ChatServer() throws IOException {
		ss = new ServerSocket(3333);
		
		//print address of the server
		System.out.println("ChatServer is up at " + InetAddress.getLocalHost().getHostAddress() + " on port " + ss.getLocalPort());
		//Socket s = ss.accept(); //wait for client connection
		
	}
	
	private void receive() throws IOException
	{
		//capture the receive thread
		while(true) // do forever loop
		{
			Socket s = ss.accept(); //wait for client connection
			InetAddress clientAddress = s.getInetAddress();
			
			//gets list of passwords from disk
			 // Retrieve the passwords collection from disk.
			 try {
			     FileInputStream fis = new FileInputStream("passwords.ser");
			     ObjectInputStream ois = new ObjectInputStream(fis);
			     passwords = (ConcurrentHashMap) ois.readObject();
			     ois.close();
			     System.out.println("Contents of the passwords collection is:");
			     System.out.println(passwords);
			 }
			 catch(FileNotFoundException fnfe)
			     {
			     System.out.println("passwords.ser is not found, so an empty collection will be used.");
			     }
			 catch (ClassNotFoundException cnfe) {} // not going to happen!
			    
			
		    System.out.println("A client has connected from " + clientAddress);
		
		    DataInputStream dis = new DataInputStream(s.getInputStream());
		    String messageFromClient = dis.readUTF();

		    //Is this is a JOIN, LEAVE, or send chat?
		     if (messageFromClient.startsWith("JOIN"))
		              joinProcessing(s, messageFromClient.substring(4).trim());

		     else if (messageFromClient.startsWith("LEAVE"))
		              leaveProcessing(messageFromClient.substring(5).trim());

		     else     chatProcessing(messageFromClient.trim());

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
			ChatServer cs = new ChatServer();
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
			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
			int blankOffset = chatNameAndPassword.indexOf(" ");
			if(blankOffset <0){
				dos.writeUTF("Invalid Format");
				dos.close();
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
			   dos.writeUTF("Welcome " + chatName + " !");
			   dos.close(); // We're done!

			   // Save the updated passwords collection on disk.
			   FileOutputStream fos = new FileOutputStream("passwords.ser");
			   ObjectOutputStream oos = new ObjectOutputStream(fos);
			   oos.writeObject(passwords);
			   oos.close();

			   // Tell everyone that Bubba just joined the chat room.
			   sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
			   return; 
			   }
			
			//if he has joined the chat before
		      String storedPassword = passwords.get(chatName);
		      if (password.equals(storedPassword))//case-sensitive compare
		         { // rejoin Bubba
		         // Send join confirmation to Bubba
		         dos.writeUTF("Welcome " + chatName + " !");
		         dos.close(); // We're done!

		         // See if Bubba is rejoining from a new location:
		      if (whosIn.containsKey(chatName))//Bubba is already in!
		             //We do need to update where he is now.
		        	 whosIn.replace(chatName, s.getInetAddress());
		         	 //but no need to "notify" everyone - Bubba never left!
		      else // Bubba was NOT already in the chat room.
		             {
		        	 whosIn.put(chatName, s.getInetAddress());//add Bubba to who's in now
		             // Tell everyone that Bubba just joined the chat room.
		             sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
		             }
		       return; 
		       }
		      
		      // If we are still executing at this point, it is because
		      // we did not rejoin Bubba because his password is bad!
		      // Send join failure error message to Bubba:
		      dos.writeUTF("Submitted password " + password + " does not match stored password for " + chatName);
		      dos.close(); // We're done!
		}
		catch (IOException ioe)
		{
			System.out.println("Join Respone Failure:" + ioe);
		}
	  }

	private void leaveProcessing(String chatName)
	  {
		
	  }

	private void chatProcessing(String chatMessageFromClient)
	  {
		
	  }
	
	private void sendToAll(String message)
	{
		//broadcast message
		//ask keyed collection for list of names
		String[] clientList = whosIn.keySet().toArray(new String[0]);
		for (String chatName : clientList){
			InetAddress clientAddress = whosIn.get(chatName);
			try{
				Socket s = new Socket(clientAddress,3456);
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeUTF(message);
				dos.close();
			}
			catch(IOException ioe){//if client goes down without leaving
				whosIn.remove(chatName);
			}
		}
	}
}
