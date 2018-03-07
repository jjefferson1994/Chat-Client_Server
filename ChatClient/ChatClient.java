import java.net.*;
import java.io.*;


public class ChatClient { 

	//instance variables
	String serverAddress;
	String chatName;
	String password;
	ServerSocket ss;
	
	public ChatClient(String serverAddress, String chatName, String password) throws Exception {//constructor

		//assignment to local reference
		this.serverAddress = serverAddress.trim();
		this.chatName = chatName.trim();
		this.password = password.trim();
		
		//Socket Communication
		ss = new ServerSocket(3456);
		 
		//checking for name params
	    if ((serverAddress == null)              // zero pointer
	    	     || (serverAddress.trim().length() == 0) // zero-length pointer
	    	     ||  serverAddress.trim().contains(" ")) // embedded blank(s) in the String
	    	     throw new IllegalArgumentException("serverAddress is null, zero length, or contains blank(s)");
		
	    //cannot set names to JOIN or LEAVE
	    if (chatName.trim().equalsIgnoreCase("JOIN") 
	    	     || chatName.trim().equalsIgnoreCase("LEAVE"))
	    	       throw new IllegalArgumentException("Chat name cannot be 'Join' or 'Leave'.");
	    
	    //Connect to server
	    System.out.println("Connecting to ChatServer at " + serverAddress + " on port 3333.");
	    Socket s = new Socket(this.serverAddress, 3333);
	    System.out.println("Connected to ChatServer!");
	    
	    //Checking chatName availability by sending name to Server
	    DataOutputStream dos = new DataOutputStream(s.getOutputStream());
	    dos.writeUTF("JOIN" + " " + chatName + " " + password);
	    
	    //check if the JOIN was successful
	    DataInputStream dis = new DataInputStream(s.getInputStream());
	    String serverReply = dis.readUTF();
	    
	    if(serverReply.startsWith("Welcome"))
	    {
	    	System.out.println(serverReply);
	    	return; //success
	    }
	    else throw new IllegalArgumentException(serverReply);
	    

	}
	
	public static void main(String[] args) { // method
		
		System.out.println("J Jefferrson jajeffe2@ncsu.edu");
		
		//command line parameters
		if (args.length != 3){
			System.out.println("Error with Command line parameters. Expecting: the ChatServer network address, the user's chat name, and the user's password.");
			return;
		}
		
		//passes the parameters to the client
		try {
			ChatClient cc = new ChatClient(args[0],args[1], args[2]);
		    cc.receive(); //calls receive method from main
		} 
		catch (Exception e) {
			System.out.println(e);
			return;
		}
	}
	
	private void receive() throws IOException 
	{
		while(true)
		{
			try
			{
		       Socket s = ss.accept();//wait for server to connect
		       DataInputStream dis = new DataInputStream(s.getInputStream());
		       String chatMessage = dis.readUTF();//wait for server to send.
		       System.out.println("Received: " + chatMessage);	
			}
			catch(IOException ioe)
			{
				System.out.println("Receive error" + ioe);
			}
		}
	}
	

	
}
