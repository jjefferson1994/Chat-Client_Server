import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/* 
 * FOR SIMPLICITY we will branch the main() thread into
 * the Client and the Server as the receive() thread.
 * ALSO FOR SIMPLICITY we will implement a single-threaded
 * Server. In "reality", we would use at least a separate
 * send thread since we are not using a stay-connected
 * design and having to reestablish a connection to
 * each client for each message. But not using a 2nd  
 * thread avoids having to get into making Threads and
 * being Runnable and even having a run() method.
 * On the Server side being single-threaded also avoids
 * having to use wait()/notify() to have the receive()
 * thread wake up the send thread.
 * Note that in this part 1 of lab3 we are using the
 * client constructor thread to connect to the server, 
 * and we don't even need a receive() thread. When we
 * do need a receive() thread in lab 4, we can branch
 * the main() thread into receive() AFTER it has returned
 * from the constructor!
 * We should probably put the ChatClient in a jar so 
 * the user can at least run it by clicking on it!
 * 
 * We will no need to send the chat name with the message
 * and use "Join" and "Leave" instead for those events
 * (and disallow those words as chat names!). We can
 * "force" a leave message from clients by making the
 * chat window DO_NOTHING_ON_CLOSE. (More simple than
 * making the client a WindowListener.)  
 */

public class PictureChatClient implements ActionListener, ListSelectionListener
{
	// Instance Variables:
	ServerSocket ss;	
	//File pointer for pictures
	File localDirectory = new File(System.getProperty("user.dir"));
	String serverAddress;
	int    serverPort = 7777; // (this is lab 5)
	int    clientPort;
	String chatName;
	String password;
	String newLine = System.lineSeparator();

	// GUI Objects
	// Chat Window
	JFrame       chatWindow       = new JFrame();
	JLabel       errorLabelField  = new JLabel(" Move separator bar to give more space to in or out.");
	JButton      sendToAllButton  = new JButton("Send To All");     
	JRadioButton horizontalRButton= new JRadioButton("Horizontal Split",true);//initially selected     
	JRadioButton verticalRButton  = new JRadioButton("Vertical Split");     
	ButtonGroup  splitButtonGroup = new ButtonGroup();      
	JTextArea    inChatArea       = new JTextArea("(enter chat here)");
	JTextArea    outChatArea      = new JTextArea();
	JTextArea	 outChatArea2 = new JTextArea();
	JScrollPane  inChatScrollPane = new JScrollPane(inChatArea);
	JScrollPane  outChatScrollPane= new JScrollPane(outChatArea);
	JSplitPane   chatPane         = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			inChatScrollPane, outChatScrollPane);
	JPanel       bottomPanel      = new JPanel();


	// Who Windows
	JFrame whosInWindow        = new JFrame("Who's In");
	JFrame whosNotInWindow     = new JFrame("Who's NOT In");
	JFrame myPicturesListWindow = new JFrame("Pictures to send" + localDirectory);
	JFrame sendPictureWindow = new JFrame(); 
	JList<String>whosInList    = new JList<String>();
	JList<String>whosNotInList = new JList<String>();
	JList<ImageIcon>myPicturesList = new JList<ImageIcon>();
	JList<ImageIcon>sendPictureList = new JList<ImageIcon>();
	JScrollPane inScrollPane   = new JScrollPane(whosInList);
	JScrollPane notInScrollPane= new JScrollPane(whosNotInList);
	JScrollPane myPicturesScrollPane = new JScrollPane(myPicturesList);
	//JScrollPane sendPicturesScrollPane = new JScrollPane(sendPictureList);
	JButton sendPrivateButton  = new JButton("Send Private To");
	JButton saveMessageButton  = new JButton("Save Message For");
	JButton clearWhosInButton  = new JButton("CLEAR SELECTIONS");
	JButton clearWhosNotButton = new JButton("CLEAR SELECTIONS");
	JButton previewPicturesButton = new JButton("Preview Pictures to Send");

	JLabel myPictureWindowLabel = new JLabel("Select a picture.");
	JButton clearPicSelectButton = new JButton("Clear Selection");


	//***********************************************************
	public static void main(String[] args)// Command Line Loader
	{                                   // calls here!
		System.out.println("Instructor solution for Lab 5 MessageChatClient - Spring 2015");	

		if (args.length != 3)
		{
			System.out.println("Restart. Provide host address, chat name, and password as 3 command line parameters."); 
			return; 
		}
		try {
			PictureChatClient cc = new PictureChatClient(args[0],args[1],args[2]);	
			cc.receive();//main thread becomes receive thread.
		}
		catch(Exception e)
		{
			System.out.println(e);//Print the Exception object 
		}                     //as our error message!
	}


	//**********************************************************
	public PictureChatClient(String serverAddress,// CONSTRUCTOR
			String chatName,     //(Our loading 
			String password)     // program calls 
					throws Exception      // us here!)
	{
		// Save the constructor parameters
		this.serverAddress = serverAddress.trim();
		this.chatName      = chatName.trim().toUpperCase();
		this.password      = password.trim();

		// Build the GUIs
		chatWindow.getContentPane().add(errorLabelField,"North");
		chatWindow.getContentPane().add(chatPane,"Center");
		bottomPanel.add(sendToAllButton);  // Add GUI objects in
		bottomPanel.add(horizontalRButton);// left-to-right
		bottomPanel.add(verticalRButton);  // sequence.
		bottomPanel.add(previewPicturesButton);
		chatWindow.getContentPane().add(bottomPanel,"South");

		whosInWindow.getContentPane().add(clearWhosInButton,"North");
		whosInWindow.getContentPane().add(inScrollPane,"Center");
		whosInWindow.getContentPane().add(sendPrivateButton,"South");
		whosNotInWindow.getContentPane().add(clearWhosNotButton,"North");
		whosNotInWindow.getContentPane().add(notInScrollPane,"Center");
		whosNotInWindow.getContentPane().add(saveMessageButton,"South");

		myPicturesListWindow.getContentPane().add(clearPicSelectButton, "North");
		myPicturesListWindow.getContentPane().add(myPicturesScrollPane, "Center");
		myPicturesListWindow.getContentPane().add(myPictureWindowLabel, "South");
		myPicturesList.setSelectionMode(0); //select single thing at a time


		// Set attributes of GUI objects
		chatWindow.setTitle(chatName + "'s Chat Room. "
				+ "Close window to LEAVE the Chat Room"); 
		chatPane.setDividerLocation(200); 
		splitButtonGroup.add(horizontalRButton);
		splitButtonGroup.add(verticalRButton);
		clearWhosInButton.setBackground(Color.YELLOW);
		clearWhosNotButton.setBackground(Color.YELLOW);
		sendPrivateButton.setBackground(Color.green);
		saveMessageButton.setBackground(Color.cyan);
		sendToAllButton.setBackground(Color.green);
		errorLabelField.setForeground(Color.red);

		outChatArea.setEditable(false);
		inChatArea.setFont (new Font("default",Font.BOLD,20));
		outChatArea.setFont(new Font("default",Font.BOLD,20));
		chatWindow.setFont(new Font("default", Font.BOLD, 20));
		inChatArea.setLineWrap(true);
		outChatArea.setLineWrap(true);
		inChatArea.setWrapStyleWord(true);
		outChatArea.setWrapStyleWord(true);

		myPictureWindowLabel.setForeground(Color.red);

		//Sign up for event notification with the
		//GUI objects we want to hear from.
		clearWhosInButton.addActionListener(this);
		clearWhosNotButton.addActionListener(this);
		sendPrivateButton.addActionListener(this);
		saveMessageButton.addActionListener(this);
		sendToAllButton.addActionListener(this);
		horizontalRButton.addActionListener(this);
		verticalRButton.addActionListener(this);
		previewPicturesButton.addActionListener(this);
		myPicturesList.addListSelectionListener(this);
		clearPicSelectButton.addActionListener(this);

		//Show the windows
		chatWindow.setSize(600,400);
		chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chatWindow.setVisible(true);
		whosInWindow.setSize(200,400); // width, height 
		whosInWindow.setLocation(600,0);//x,y   
		whosInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   
		whosInWindow.setVisible(true);//false closes the window   
		whosNotInWindow.setSize(200,400);   
		whosNotInWindow.setLocation(800,0);   
		whosNotInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   
		whosNotInWindow.setVisible(true);// just like hitting the "X"  

		myPicturesListWindow.setSize(400,400);
		myPicturesListWindow.setLocation(600, 400);
		//myPicturesListWindow.setVisible(true); //for testing purposes


		// Edit the constructor parameters.
		if ((serverAddress == null)              // zero pointer
				|| (serverAddress.trim().length() == 0) // zero-length pointer
				||  serverAddress.trim().contains(" ")) // embedded blank(s) in the String
			throw new IllegalArgumentException("serverAddress is null, zero length, or contains blank(s)");

		if ((chatName == null)              // zero pointer
				|| (chatName.trim().length() == 0) // zero-length pointer
				||  chatName.trim().contains(" ")) // embedded blank(s) in the String
			throw new IllegalArgumentException("chatName is null, zero length, or contains blank(s)");

		if (chatName.trim().equalsIgnoreCase("JOIN") 
				|| chatName.trim().charAt(0) == '\u007F')
			throw new IllegalArgumentException("Chat name cannot be JOIN");

		if ((password == null)              // zero pointer
				|| (password.trim().length() == 0) // zero-length pointer
				||  password.trim().contains(" ")) // embedded blank(s) in the String
			throw new IllegalArgumentException("password is null, zero length, or contains blank(s)");

		// Connect to the server.
		System.out.println("Consructor parameters were: "
				+ this.serverAddress + ", "
				+ this.chatName + ", "
				+ this.password);
		System.out.println("Connecting to ChatServer at "
				+ serverAddress + " on port " + serverPort);
		Socket s = new Socket(this.serverAddress, serverPort);
		System.out.println("Connected to the ChatServer!");

		// Create the ServerSocket to guarantee availability
		// of client-side port.
		ss = new ServerSocket(0);//Get some available local port.
		clientPort = ss.getLocalPort();
		System.out.println("Chat Client is up at "
				+ InetAddress.getLocalHost().getHostAddress()
				+ " on port " + clientPort);

		//prints directory of pictures
		System.out.println("Local Directory is" + localDirectory);

		// Join the Chat Room.
		ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		oos.writeObject('\u007F' + "JOIN" + " " + chatName + " " + password + " " + clientPort);
		ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
		String serverReply = (String) ois.readObject();
		System.out.println(serverReply);
		if (!serverReply.startsWith("Welcome"))
			throw new IllegalArgumentException(serverReply);  
	}


	//********************************************************
	private void receive() // main thread calls here
	{                    // to become receive thread.
		while(true)//"capture" receive thread.
		{
			try {  
				Socket s = ss.accept();//wait for server to connect
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
				Object something = ois.readObject();//wait for server to send.
				if (something instanceof String)//pointer to Object on left, a TYPE on right
				{
					//Use casting to move the <em>something</em> pointer
					//into another pointer that is typed as String:
					String chatMessage = (String) something;
					// Now we can use this <em>chatMessage</em> pointer
					// to call String methods on it, and pass this pointer
					// to methods that are expecting a String parameter.  
					System.out.println("Received: " + chatMessage);
					outChatArea.append(newLine + chatMessage);
					// scroll the outChatArea to the bottom
					outChatArea.setCaretPosition(outChatArea.getDocument().getLength()); 
				}
				else if (something instanceof String[])//pointer to Object on left, a TYPE on right
				{
					//Use casting to move the <em>something</em> pointer
					//into another pointer that is typed as String[]:
					String[] clientList = (String[]) something;
					// Now we can use this <em>clientList</em> pointer
					// to treat it as an array, and pass this pointer
					// to methods that are expecting an array-of-Strings
					// parameter.
					System.out.println("Currently in the chat room:");
					for (String chatName : clientList)
						System.out.println(chatName);
					whosInList.setListData(clientList);
				}
				else if (something instanceof Vector)//pointer to Object on left, a TYPE on right
				{
					//Use casting to move the <em>something</em> pointer
					//into another pointer that is typed as Vector:
					Vector notInClientList = (Vector) something;
					//Now we can use this <em>notInclientList</em> pointer
					//to treat it as a Vector, and pass this pointer
					//to methods that are expecting a Vector parameter.
					System.out.println("Currently NOT in the chat room:");
					System.out.println(notInClientList);
					whosNotInList.setListData(notInClientList);
				}
				else if (something instanceof ImageIcon){
					ImageIcon picture = (ImageIcon)something;
					String descript = picture.getDescription();
					Vector<ImageIcon> picturevec = new Vector<ImageIcon>();
					JPanel refreshingPicturePanel = new RefreshingPicturePanel(picture.getImage());
					String firstWord = null;
					if(descript.contains(" ")){
						firstWord = descript.substring(0,descript.indexOf(" "));
					}

					sendPictureWindow.getContentPane().add(refreshingPicturePanel, "Center");
					sendPictureWindow.getContentPane().add(outChatArea2, "South");
					outChatArea2.setEditable(false);
					//outChatArea2.setFont(new Font("default"));
					outChatArea2.setLineWrap(true);
					outChatArea2.setWrapStyleWord(true);
					//sendPictureWindow.setVisible(true);

					sendPictureWindow.setSize(400,400);
					sendPictureWindow.setLocation(0,400);

					picturevec.add(picture);
					sendPictureWindow.setTitle(firstWord);
					sendPictureList.setListData(picturevec);
					outChatArea2.setText(descript);
					sendPictureWindow.setVisible(true);

				}
				else System.out.println("Unexpected Object type received from server: "
						+ something);
			}
			catch(ClassNotFoundException cnfe){}
			catch(IOException ioe)
			{
				System.out.println("Receive error:" + ioe);
			}
		}
	}


	//*******************************************************
	public void actionPerformed(ActionEvent ae)//buttons call here!
	{
		errorLabelField.setText("");// clear 

		if (ae.getSource() == clearWhosInButton)
		{
			// do whatever when this button is pushed.
			System.out.println("clearWhosInButton was pushed.");
			whosInList.clearSelection();
			return;
		}

		if (ae.getSource() == clearWhosNotButton)
		{
			// do whatever when this button is pushed.
			System.out.println("clearWhosNotButton was pushed.");
			whosNotInList.clearSelection();
			return;
		}

		if (ae.getSource() == sendPrivateButton)
		{
			System.out.println("sendPrivateButton was pushed."); 
			List<String> privateMessageList = whosInList.getSelectedValuesList();
			if (privateMessageList.isEmpty())
			{
				System.out.println("No private message recipients were selected.");
				errorLabelField.setText("No private message recipients were selected.");
				return;
			}
			String[] privateMessageRecipients = privateMessageList.toArray(new String[0]);
			String privateChatMessage = inChatArea.getText().trim();
			if(((privateChatMessage.length() == 0) || privateChatMessage.equals("(enter chat here)")) && myPicturesList.isSelectionEmpty())	 
			{
				System.out.println("No private message provided to send.");
				errorLabelField.setText("No private message was entered to send.");
				return;
			}

			if(privateChatMessage.length() > 0 && myPicturesList.isSelectionEmpty()){
				/*		 System.out.println("Recipients of this private message will be:");   
	     for (String recipient : privateMessageRecipients)
	     	  System.out.println(recipient);
				 */
				inChatArea.setText("");//clear as a send indication.
				// SEND ARRAY TO SERVER
				// First we have to make another array that also
				// contains the sender and the message!
				String[] messageAndRecipients = new String[privateMessageRecipients.length+2]; 
				messageAndRecipients[0] = chatName;
				messageAndRecipients[1] = privateChatMessage;
				for (int i = 0; i < privateMessageRecipients.length; i++)
					messageAndRecipients[i+2] = privateMessageRecipients[i];
				// Now send it! (can't throw an Exception from actionPerformed()!)
				try {
					Socket s = new Socket(serverAddress,7777);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(messageAndRecipients);
				}
				catch (Exception e)
				{
					System.out.println("Error sending private message to server.");
					errorLabelField.setText("Error sending private message to chat server.");
				}
				return;
			}
			
			if(privateChatMessage.length() > 0 || !myPicturesList.isSelectionEmpty()){
				inChatArea.setText("");
				ImageIcon privatePicture = myPicturesList.getSelectedValue();
				String copyDescription = privatePicture.getDescription();
				privatePicture.setDescription(chatName + " " + copyDescription + " " + privateChatMessage);

				myPicturesList.clearSelection();
				Object[] pictureAndMessage = new Object[privateMessageRecipients.length + 2];
				pictureAndMessage[0] = chatName;
				pictureAndMessage[1] = privatePicture;
				
				for (int i = 0; i < privateMessageRecipients.length; i++)
					pictureAndMessage[i+2] = privateMessageRecipients[i];
				
				try {
					Socket s = new Socket(serverAddress,7777);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(pictureAndMessage);
				}
				catch (Exception e)
				{
					System.out.println("Error sending private message to server.");
					errorLabelField.setText("Error sending private message to chat server.");
				}
				
				privatePicture.setDescription(copyDescription);
			}
		}

		if (ae.getSource() == saveMessageButton)
		{
			System.out.println("saveMessageButton was pushed."); 
			List<String> saveMessageList = whosNotInList.getSelectedValuesList();
			if (saveMessageList.isEmpty())
			{
				System.out.println("No save-message recipients were selected.");
				errorLabelField.setText("No save-message recipients were selected.");
				return;
			}
			Vector<Object> saveMessageRecipients 
			= new Vector<Object>(saveMessageList);
			/*		 System.out.println("Recipients of this saved message are:");
		 System.out.println(saveMessageRecipients);
			 */
			String saveChatMessage = inChatArea.getText().trim();
			if((saveChatMessage.length() == 0)
					|| saveChatMessage.equals("(enter chat here)"))	 
			{
				System.out.println("No message was entered to save.");
				errorLabelField.setText("No message was entered to save.");
				return;
			}
			inChatArea.setText("");//clear as a send indication.
			// Add sender name to the Vector
			saveMessageRecipients.add(0,chatName);//insert at slot 0
			// Add the save message to the recipients Vector
			saveMessageRecipients.add(1,saveChatMessage);//insert at slot 0
			// Now send it!
			try {
				Socket s = new Socket(serverAddress,7777);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(saveMessageRecipients);
			}
			catch (Exception e)
			{
				System.out.println("Error sending save message to server.");
				errorLabelField.setText("Error sending save message to chat server.");
			}
			return;
		}

		if (ae.getSource() == sendToAllButton)
		{
			// do whatever when this button is pushed.
			System.out.println("sendToAllButton was pushed."); 
			String chatToSend = inChatArea.getText().trim();
			ImageIcon picture = myPicturesList.getSelectedValue();
			if(((chatToSend.length() == 0) || chatToSend.equals("(enter chat here)")) && myPicturesList.isSelectionEmpty())	 
			{
				errorLabelField.setText("NO CHAT OR PICTURE WAS ENTERED"); 
				return; 
			}

			if((chatToSend.length() > 0 && myPicturesList.isSelectionEmpty())){//chat message but no picture
				inChatArea.setText(""); // clear input area on GUI
				System.out.println("Sending " + chatToSend);
				try {
					Socket s = new Socket(serverAddress, 5555);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject('\u007F' + chatName + " " + chatToSend);
				}
				catch(Exception e)
				{
					errorLabelField.setText("ERROR SENDING TO CHAT SERVER");
					System.out.println(e);
				}
				return;
			}

			if((chatToSend.length() > 0 || !myPicturesList.isSelectionEmpty())){//both a chat and a picture message
				inChatArea.setText(""); // clear input area on GUI
				System.out.println("Sending " + chatToSend);

				System.out.println("Sending Picture: " + myPicturesList.getSelectedValue());
				ImageIcon pictureToSend = myPicturesList.getSelectedValue();
				myPicturesList.clearSelection();

				String copyDescription = pictureToSend.getDescription();
				pictureToSend.setDescription(chatName + " " + copyDescription + " " + chatToSend);

				try {
					Socket s = new Socket(serverAddress, 7777);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.writeObject(pictureToSend);
				}
				catch(Exception e)
				{
					errorLabelField.setText("ERROR SENDING TO CHAT SERVER");
					System.out.println(e);
				}
				pictureToSend.setDescription(copyDescription);
				return;
			}
		}

		if (ae.getSource() == horizontalRButton)
		{
			// do whatever when this button is pushed.
			System.out.println("horizontalRButton was pushed."); 
			chatPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			return;
		}

		if (ae.getSource() == verticalRButton)
		{
			// do whatever when this button is pushed.
			System.out.println("verticalRButton was pushed."); 
			chatPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
			return;
		}

		if (ae.getSource() == previewPicturesButton){
			System.out.println("Preview Pictures Button was pushed.");
			myPicturesListWindow.setVisible(true);
			String[] listOfFiles = localDirectory.list(); //where the hell does this go???!?!?!?
			Vector<String> pictureFileNames = new Vector<String>();
			for(String s : listOfFiles){
				if(s.endsWith(".png") || s.endsWith(".gif") || s.endsWith(".jpg")){
					pictureFileNames.add(s);
				}
			}
			if(pictureFileNames.isEmpty()){
				System.out.println("No pictures are found in the local directory.");
				return;
			}
			System.out.println("Local directory pictures are " + pictureFileNames);

			Vector<ImageIcon> imageIcons = new Vector<ImageIcon>();
			for (String pictureFileName : pictureFileNames)
			{
				ImageIcon picture = new ImageIcon(pictureFileName,pictureFileName);// filename,description   	    	 myPictures.add(picture);
				imageIcons.add(picture);
			}
			myPicturesList.setListData(imageIcons);
		}

		if(ae.getSource()== clearPicSelectButton){
			//System.out.println("Clear Picture Selection Button was pushed.");
			myPicturesList.clearSelection();
			//System.out.println("Selection cleared.");
			myPictureWindowLabel.setText("Select a picture.");
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent lse) {
		if(lse.getValueIsAdjusting()) return;
		//System.out.println("Picture was selected.");
		ImageIcon selectedPicture = myPicturesList.getSelectedValue();
		if (selectedPicture == null) return; // selection was removed! 
		String pictureDescription = selectedPicture.getDescription();
		myPictureWindowLabel.setText(pictureDescription);

	}
}