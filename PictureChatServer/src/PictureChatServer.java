/* This is the ChatServer for Lab 4 Spring 2015
 * NOTE: The server could easily be made more "nimble"
 * (and safer from a hacker that connected but did not
 * send) by using multithreading, while still not
 * keeping the Socket(s) to each client up. This would
 * require making a new Thread for each incoming message
 * or maintaining a Thread pool (using wait()/notify()).
 * 
 * After each message was received and "processed",
 * that client thread would be terminated (or returned
 * to the pool) and the receive Socket closed.
 * 
 * We'll see how the initial single-threaded version
 * of the server works when 80 students chat! 1-19-15
 */

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
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;


public class PictureChatServer
{
	ServerSocket ss;

	ConcurrentHashMap<String,InetAddress> whosIn = 
			new ConcurrentHashMap<String,InetAddress>();

	Vector<String> whosNotIn = 
			new Vector<String>();

	ConcurrentHashMap<String,Integer> clientPorts = 
			new ConcurrentHashMap<String,Integer>();

	ConcurrentHashMap<String,String> passwords = 
			new ConcurrentHashMap<String,String>();

	ConcurrentHashMap<String,Vector<Object>> savedMessages = 
			new ConcurrentHashMap<String,Vector<Object>>();

	//****************************************************************
	public static void main(String[] args)
	{
		System.out.println("Instructor solution for Lab 5 MessageChatServer - Spring 2015");	
		if (args.length != 0)
			System.out.println("ChatServer does not accept comand line parameters.");
		try { 
			PictureChatServer cs = new PictureChatServer();
			cs.receive();// main thread becomes receive thread.
		}
		catch (Exception e)
		{
			System.out.println(e);
		}	
	}


	//*********************************************************
	public PictureChatServer()throws IOException//CONSTRUCTOR
	{
		ss = new ServerSocket(7777); // Grab port 7777!

		// Retrieve the passwords collection from disk.
		try {
			FileInputStream fis = new FileInputStream("passwords.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			passwords = (ConcurrentHashMap<String,String>) ois.readObject();
			ois.close();
			System.out.println("Contents of the passwords collection is:");
			System.out.println(passwords);
		}
		catch (ClassNotFoundException cnfe) {} // not going to happen!
		catch(FileNotFoundException fnfe)
		{
			System.out.println("passwords.ser is not found, so an empty collection will be used.");
		}

		// Fill the whosNotIn collection from passwords.
		Set<String> previousClients = passwords.keySet();
		for (String chatName : previousClients)
			whosNotIn.add(chatName); 

		// Retrieve the saved messages collection from disk.
		try {
			FileInputStream fis = new FileInputStream("SavedMessages.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			savedMessages = (ConcurrentHashMap<String,Vector<Object>>) ois.readObject();
			ois.close();
			System.out.println("Contents of the SavedMessages collection is:");
			System.out.println(savedMessages);
		}
		catch (ClassNotFoundException cnfe) {} // not going to happen!
		catch(FileNotFoundException fnfe)
		{
			System.out.println("SavedMessages.ser is not found, so an empty collection will be used.");
			// If a stored savedMessages is not found, build
			// an "empty" collections from passwords.
			for (String chatName : previousClients)
				savedMessages.put(chatName, new Vector<Object>()); 
		}

		System.out.println("MessageChatServer is up at "
				+ InetAddress.getLocalHost().getHostAddress()
				+ " on port " + ss.getLocalPort());
	}


	//*********************************************************
	private void receive()
	{
		//"Capture" the receive thread
		while(true) // "do forever"
		{
			try {
				Socket s = ss.accept();//WAIT for a client to connect.
				InetAddress clientAddress = s.getInetAddress();
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				Object messageFromClient = ois.readObject();
				System.out.println("Received from client at " + clientAddress
						+ " : " + messageFromClient);
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
					else // should be a chat message to everyone  
					{
						chatProcessing(message);
						continue;//Go back to loop top to receive next client.
					}
				}

				if (messageFromClient instanceof Object[])//A Private Message
				{
					Object[] privateMessageArray = (Object[]) messageFromClient;
					sendPrivateMessage(privateMessageArray);
					continue;
				}

				if (messageFromClient instanceof Vector)//A Save Message
				{
					Vector<Object> saveMessageVector = (Vector<Object>) messageFromClient;
					saveThisMessage(saveMessageVector);
					continue;
				}
				if(messageFromClient instanceof ImageIcon){
					sendToAll(messageFromClient);
					continue;
				}
				System.out.println("Unexpected object received: " + messageFromClient);

			} 
			catch(ClassNotFoundException cnfe) {}
			catch (IOException ioe)
			{        // Don't try to close() after IOException 
				continue;// Ignore this "bad client".
			}        // Go back and wait for next client. 
		}
	}


	//************************************************************
	private void saveThisMessage(Vector<Object> saveMessageVector)
	{
		System.out.println("In saveThisMessage(). Parameter is :");
		System.out.println(saveMessageVector);
		String senderChatName = (String)saveMessageVector.remove(0);
		String messageToBeSaved = (String)saveMessageVector.remove(0);
		// This leaves just the recipients in the Vector.
		String savedForList = "";
		for (Object recipientChatName : saveMessageVector)
		{
			Vector<Object> recipientVector = savedMessages.get(recipientChatName);
			if (recipientVector == null)//this client is not yet in the savedMessages collection.
			{
				recipientVector = new Vector<Object>(); 
				savedMessages.put((String) recipientChatName, recipientVector); 
			}
			recipientVector.add("Saved message from " + senderChatName
					+ ": " + messageToBeSaved
					+ " on " + new Date());
			savedForList += recipientChatName + " ";
		}
		// OK. The saveMessage is stored in every recipient's
		// saved-messages Vector. So now save the savedMessages
		// collection on the disk.
		try {
			FileOutputStream fos = new FileOutputStream("SavedMessages.ser");
			ObjectOutputStream foos = new ObjectOutputStream(fos);
			foos.writeObject(savedMessages);
			foos.close();
			// and send the saving client a confirmation message.
			InetAddress saverAddress = whosIn.get(senderChatName);
			int clientPort = clientPorts.get(senderChatName);
			Socket s = new Socket(saverAddress, clientPort);
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject("Your message '" + messageToBeSaved
					+ "' has been saved for " + savedForList);
		}
		catch (Exception e)
		{
			System.out.println("Unable to save the savedMessages collection on disk after adding messages.");
			System.out.println(e);// Show the system's error msg
		}
	}


	//***********************************************************
	private void sendSavedMessages(String chatName)
	{
		// joinProcessing() calls here.
		Vector<Object> messageList = savedMessages.get(chatName);
		if (messageList == null) // client is a new join!
		{//So add them.
			savedMessages.put(chatName, new Vector<Object>()); 
			return; 
		}
		if (messageList.isEmpty()) return; // no messages!
		// There are savedMessages. Send them.
		InetAddress clientAddress = whosIn.get(chatName);
		int clientPort = clientPorts.get(chatName);
		synchronized(savedMessages)//get lock on list
		{//Not necessary with this single-threaded server...
			for (Object message : messageList)
			{
				try {
					Socket s = new Socket(clientAddress,clientPort);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(message);
				}
				catch(Exception e)
				{
					System.out.println("Cannot send saved message to " + chatName);
					System.out.println(e);
					return; // stop trying 
				}
			}
			messageList.clear();//remove all (sent) entries.
			// Now save updated collection
			try {
				FileOutputStream fos = new FileOutputStream("SavedMessages.ser");
				ObjectOutputStream foos = new ObjectOutputStream(fos);
				foos.writeObject(savedMessages);
				foos.close();
			}
			catch(Exception e)
			{
				System.out.println("Unable to save savesMessages collection on disk in joinProcessing()");
				System.out.println(e);
			}
		}
	}


	//***********************************************************
	private void sendPrivateMessage(Object[] privateMessageArray)
	{
		int chatOrPic = 0;
		System.out.println("In sendPrivateMessage(). Parameter is :");
		for (Object msgOrClient : privateMessageArray)	
			System.out.println(msgOrClient);
		// Send the message to all recipients.
		String senderChatName = (String)privateMessageArray[0];
		if(privateMessageArray[1] instanceof String){
			chatOrPic = 0;
		}
		if(privateMessageArray[1] instanceof ImageIcon){
			chatOrPic = 1;
		}
		String sendToList = "";
		for (int i = 2; i < privateMessageArray.length; i++)
			sendToList += privateMessageArray[i] + " ";
		String sentToList = "";
		System.out.println("Sending private message from " + senderChatName);

		if(chatOrPic == 0){
			String privateMessage = (String)privateMessageArray[1];
			for (int i = 2; i < privateMessageArray.length; i++)
			{
				String recipientChatName = (String)privateMessageArray[i];
				InetAddress thisRecipientsAddress = whosIn.get(recipientChatName);
				if (thisRecipientsAddress == null) continue; // this recipient left the chat room
				int thisRecipientPort = clientPorts.get(recipientChatName);
				try {
					Socket s = new Socket(thisRecipientsAddress,thisRecipientPort);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject("Private message from " + senderChatName
							+ " to " + sendToList 
							+ ": "   + privateMessage);
					sentToList += recipientChatName + " ";
				}
				catch(Exception e)
				{
					System.out.println("Error sending a private message to " + recipientChatName);
				}
				// with the catch inside the loop, sending will
				// be attempted to ALL recipients.
			}
			// Send a confirmation message to the sender.
			InetAddress senderAddress = whosIn.get(senderChatName);
			if (senderAddress == null) return;//sender left chat room
			int senderPort = clientPorts.get(senderChatName);
			System.out.println("Sending confirmation of private message send to " + senderChatName);
			String confirmationMessage = "Your private message: '" 
					+ privateMessage
					+ "' was sent to "
					+ sentToList;
			try {
				Socket s = new Socket(senderAddress,senderPort);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				if (sentToList.length() == 0)
					confirmationMessage = "Sorry, your private message: '"
							+ privateMessage
							+ "' could not be sent to any of the designated recipients.";
				oos.writeObject(confirmationMessage); 
			}
			catch(Exception e)
			{
				System.out.println("Error sending confirmation of private message to " + senderChatName);
			}
		}
		else if(chatOrPic == 1){
			ImageIcon privateImage = (ImageIcon)privateMessageArray[1];
			for (int i = 2; i < privateMessageArray.length; i++)
			{
				String recipientChatName = (String)privateMessageArray[i];
				InetAddress thisRecipientsAddress = whosIn.get(recipientChatName);
				if (thisRecipientsAddress == null) continue; // this recipient left the chat room
				int thisRecipientPort = clientPorts.get(recipientChatName);
				try {
					Socket s = new Socket(thisRecipientsAddress,thisRecipientPort);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					String copyDescript = privateImage.getDescription();
					privateImage.setDescription("Private message from " + senderChatName
							+ " to " + sendToList 
							+ ": "   + privateImage);
					oos.writeObject(privateImage);
					sentToList += recipientChatName + " ";
				}
				catch(Exception e)
				{
					System.out.println("Error sending a private message to " + recipientChatName);
				}
				// with the catch inside the loop, sending will
				// be attempted to ALL recipients.
			}
			// Send a confirmation message to the sender.
			InetAddress senderAddress = whosIn.get(senderChatName);
			if (senderAddress == null) return;//sender left chat room
			int senderPort = clientPorts.get(senderChatName);
			System.out.println("Sending confirmation of private message send to " + senderChatName);
			String confirmationMessage = "Your private message: '" 
					+ privateImage
					+ "' was sent to "
					+ sentToList;
			try {
				Socket s = new Socket(senderAddress,senderPort);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				if (sentToList.length() == 0)
					confirmationMessage = "Sorry, your private message: '"
							+ privateImage
							+ "' could not be sent to any of the designated recipients.";
				oos.writeObject(confirmationMessage); 
			}
			catch(Exception e)
			{
				System.out.println("Error sending confirmation of private message to " + senderChatName);
			}
		}
	}


	//***************************************************************
	private void joinProcessing(Socket s, String chatNamePasswordPort)
	{
		try {
			InetAddress clientAddress = s.getInetAddress();
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			int blankOffset = chatNamePasswordPort.indexOf(" ");
			if (blankOffset < 0)//should be name-blank-password-blank-callBackPort
			{
				oos.writeObject("Invalid Format");
				oos.close();//join Socket
				return;
			}  
			String chatName = chatNamePasswordPort.substring(0,blankOffset).toUpperCase();
			String passwordAndPort = chatNamePasswordPort.substring(blankOffset).trim();
			blankOffset = passwordAndPort.indexOf(" ");
			if (blankOffset < 0)//should be name-blank-password-blank-callBackPort
			{
				oos.writeObject("Invalid Format");
				oos.close();//join Socket
				return;
			}  
			String password = passwordAndPort.substring(0,blankOffset);
			String portNumber = passwordAndPort.substring(blankOffset).trim();
			int clientPort = Integer.parseInt(portNumber);     
			if (!passwords.containsKey(chatName))//never been in!
			{//add & join Bubba as *** brand new client ***!
				passwords.put(chatName,password);//add Bubba to pw collection
				whosIn.put(chatName, clientAddress);//add Bubba to who's in now
				clientPorts.put(chatName, clientPort);
				//(Don't need to remove from whosNotIn because he wasn't in passwords.)
				// Send join confirmation to Bubba
				oos.writeObject("Welcome " + chatName + " !");
				oos.close(); // join Socket
				// Save the updated passwords collection on disk.
				FileOutputStream pwfos = new FileOutputStream("passwords.ser");
				ObjectOutputStream pwoos = new ObjectOutputStream(pwfos);
				pwoos.writeObject(passwords);
				pwoos.close();
				// Tell everyone that Bubba just joined the chat room.
				sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
				// Send whosIn list to everyone
				String[] chatNameList = whosIn.keySet().toArray(new String[0]); 
				Arrays.sort(chatNameList);
				sendToAll(chatNameList);
				//Do need to re-send whosNotIn even though
				//that hasn't changed because that's the only
				//way the new guy will get it!
				sendToAll(whosNotIn);
				// Don't need to sendSavedMessages(chatName)
				// because a never-been-in client couldn't have messages.
				System.out.println("Never-been-in-before client " + chatName + " has joined!");
				return; 
			}

			// At this point, Bubba HAS been in the chat room before.
			// Check his password.
			String storedPassword = passwords.get(chatName);
			if (password.equals(storedPassword))//case-sensitive
			{ // *** rejoin Bubba ***
				// Send join confirmation to Bubba
				oos.writeObject("Welcome " + chatName + " !");
				oos.close(); // join Socket
				// See if Bubba is rejoining from a new location:
				if (whosIn.containsKey(chatName))//Bubba is already in!
				{
					//We do need to update where he is now.
					whosIn.replace(chatName, clientAddress);
					clientPorts.replace(chatName, clientPort);
					//but no need to "notify" everyone - Bubba never left!
					System.out.println(chatName + " has rejoined from a new location!");
				}
				else // Bubba has been in the chat room before, 
					// but was NOT already in the chat room.
				{
					whosIn.put(chatName, clientAddress);//add Bubba to who's in now
					whosNotIn.remove(chatName);
					clientPorts.put(chatName, clientPort);
					// Tell everyone that Bubba just joined the chat room.
					sendToAll("Welcome to " + chatName + " who has just joined the chat room!");
					// Send whosIn & whosNotIn list to everyone
					String[] chatNameList = whosIn.keySet().toArray(new String[0]); 
					Arrays.sort(chatNameList);
					sendToAll(chatNameList);
					sendToAll(whosNotIn);
					System.out.println(chatName + " has joined!");
					sendSavedMessages(chatName);
				}
				return; 
			}

			// If we are still executing at this point, it is because
			// we did not rejoin Bubba because his password is bad!
			// Send join failure error message to Bubba:
			oos.writeObject("JOIN FAILURE: Submitted password " + password + " does not match stored password for " + chatName);
			oos.close(); // We're done!
		}
		catch(IOException ioe)
		{
			System.out.println("Join response failure. " + ioe);
		}
	}


	//********************************************************
	private void sendToAll(Object message)
	{
		// Send this message to the client at every InetAddress
		// in the whosIn collection. (We don't really care what
		// their chatName is, but we need it if we are going
		// to remove them from whosIn on a send failure.)	
		// Ask the keyed collection for a list of it's keys:
		String[] clientList = whosIn.keySet().toArray(new String[0]);
		String leaveList = "";//who failed a send
		for (String chatName : clientList)
		{
			InetAddress clientAddress = whosIn.get(chatName);
			if (clientAddress == null) System.out.println(chatName + " is not in whosIn!");
			Integer clientPort = clientPorts.get(chatName);
			if (clientPort == null) System.out.println(chatName + " is not in clientPorts!");
			try {
				Socket s = new Socket(clientAddress,clientPort);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(message);
				oos.close();
			}
			catch(IOException ioe)//must have gone down without leaving...
			{
				System.out.println("Send failure in sendToAll() to " + chatName 
						+ " at address " + clientAddress
						+ " on port " + clientPort);
				whosIn.remove(chatName);//"leave" this client
				whosNotIn.add(chatName);
				clientPorts.remove(chatName);
				leaveList += chatName + " ";
				System.out.println(chatName + " has left.");
			}
		}
		// If a send fails, it means that client has "left".
		// This notifies everyone of who it was that left,
		// and also updates everyone's whosIn & whosNot list.
		if (leaveList.length() > 0) //recursive calls below!
		{
			sendToAll("Goodbye to " + leaveList + " who has left the chat room.");
			sendToAll(whosNotIn); // Vector
			sendToAll(whosIn.keySet().toArray(new String[0]));//String[] array
		}
	}


	//*******************************************************
	private void chatProcessing(String messageFromClient)
	{
		int firstBlankOffset = messageFromClient.indexOf(" ");
		if (firstBlankOffset < 0) // does not contain a blank!
		{
			System.out.println("Invalid message from a PrivateChatClient: "  
					+ messageFromClient
					+ " does not contain a blank and thus the sending client cannot be identified."); 
			return;
		}
		String chatName = messageFromClient.substring(0,firstBlankOffset).toUpperCase();
		String chatMessage = messageFromClient.substring(firstBlankOffset).trim();
		if (!whosIn.containsKey(chatName)) // oops!
		{
			System.out.println("Sending chatName: " + chatName
					+ " received from a PrivateChatClient is not in the chat room.");
			return;
		}
		sendToAll(chatName + " says: " + chatMessage);	  
	}
}