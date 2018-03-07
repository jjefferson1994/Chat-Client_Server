import java.net.*;
import java.util.List;
import java.util.Vector;
import java.awt.Color;
import java.awt.Font;
//import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
	
public class PrivateChatClient implements ActionListener{

		//instance variables
		String serverAddress;
		String chatName;
		String password;
		ServerSocket ss;
		JList<String> whosInList = new JList<String>();
		JList<String> whosNOTInList = new JList<String>();
		JFrame whosInWindow = new JFrame("Who's In");
		JFrame whosNotInWindow = new JFrame("Who's NOT in");
		JButton sendPrivateButton = new JButton("Send Private To");
		JButton saveMessageButton = new JButton("Save Message For");
		JButton clearwhosInButton = new JButton("CLEAR SELECTIONS");
		JButton clearwhosNotInButton = new JButton("CLEAR SELECTIONS");
		JFrame       chatWindow       = new JFrame();
		JLabel       errorLabelField  = new JLabel("error messages here =>");
		JLabel		leavenotice	= new JLabel("Close the Window to LEAVE the Chat Room");
		JButton      sendToAllButton  = new JButton("Send To All");     
		JRadioButton horizontalRButton= new JRadioButton("Horizontal Split",true);     
		JRadioButton verticalRButton  = new JRadioButton("Vertical Split");     
		ButtonGroup  splitButtonGroup = new ButtonGroup();      
		JTextArea    inChatArea       = new JTextArea("(enter chat here)");
		JTextArea    outChatArea      = new JTextArea();

		JScrollPane  inChatScrollPane = new JScrollPane(inChatArea);
		JScrollPane  outChatScrollPane= new JScrollPane(outChatArea);
		JScrollPane	whosInPane = new JScrollPane(whosInList);
		JScrollPane whosNotInPane = new JScrollPane(whosNOTInList);
		JSplitPane   chatPane         = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		                                   inChatScrollPane, outChatScrollPane);
		JPanel       bottomPanel      = new JPanel();
		String newLine = System.lineSeparator();
		
		public PrivateChatClient(String serverAddress, String chatName, String password) throws Exception {//constructor
			
			//BUILD the GUI
			whosInWindow.getContentPane().add(whosInList, "Center");
			whosNotInWindow.getContentPane().add(whosNOTInList, "Center");
			
			//size the windows
			whosInWindow.setSize(200,400);
			whosNotInWindow.setSize(200,400);
			
			//location of windows
			
			whosInWindow.setLocation(600,0);
			whosNotInWindow.setLocation(800,0);//x,y
			
			//show the windows
			whosInWindow.setVisible(true);
			whosNotInWindow.setVisible(true);
			
			whosInWindow.getContentPane().add(whosInPane,"Center");
			whosNotInWindow.getContentPane().add(whosNotInPane,"Center");
			whosInWindow.revalidate();
			whosInWindow.repaint();
			whosNotInWindow.revalidate();
			whosNotInWindow.repaint();
			
			//disable the red X
			whosInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			whosNotInWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			
			whosInWindow.getContentPane().add(clearwhosInButton,  "North");
			whosInWindow.getContentPane().add(sendPrivateButton, "South");
			whosNotInWindow.getContentPane().add(clearwhosNotInButton, "North");
			whosNotInWindow.getContentPane().add(saveMessageButton,"South");
			
			//inChatScrollPane(whosInList);//add the pane to this too
			
			chatWindow.setSize(600, 800);
			//chatWindow.setLocation();
			chatWindow.setVisible(true);
			chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			chatWindow.getContentPane().add(bottomPanel, "South");
			chatWindow.getContentPane().add(errorLabelField, "North");
			chatWindow.getContentPane().add(chatPane, "Center");
			
			clearwhosInButton.addActionListener(this);
			clearwhosNotInButton.addActionListener(this);
			clearwhosInButton.setBackground(Color.yellow);
			clearwhosNotInButton.setBackground(Color.yellow);
			
			sendPrivateButton.addActionListener(this);
			sendPrivateButton.setBackground(Color.green);
			
			saveMessageButton.addActionListener(this);
			saveMessageButton.setBackground(Color.cyan);

			
			bottomPanel.add(sendToAllButton); // Add GUI objects in
			sendToAllButton.setBackground(Color.green);
			bottomPanel.add(horizontalRButton);// left-to-right
			bottomPanel.add(verticalRButton);  // sequence
			bottomPanel.add(leavenotice);
			leavenotice.setForeground(Color.blue);
			errorLabelField.setForeground(Color.red);
			
		    splitButtonGroup.add(horizontalRButton);//one button at a time
		    splitButtonGroup.add(verticalRButton);

		    sendToAllButton.addActionListener(this);
		    horizontalRButton.addActionListener(this);
		    verticalRButton.addActionListener(this);
			
			chatWindow.setTitle(chatName + "'s Chat Room. "
				      + " Move separator bar to give more space to in vs out."); 
			inChatArea.setEditable(true);
			outChatArea.setEditable(false); //stops cursor from entering outchat
			
			chatPane.setDividerLocation(200);//split pane separator location 
			
			   inChatArea.setFont (new Font("default",Font.BOLD,20));
			   outChatArea.setFont(new Font("default",Font.BOLD,20));
			   chatWindow.setFont(new Font("default", Font.BOLD, 20));
			   inChatArea.setLineWrap(true);
			   outChatArea.setLineWrap(true);
			   inChatArea.setWrapStyleWord(true);
			   outChatArea.setWrapStyleWord(true);
			
			// scroll the outChatArea to the bottom
			   outChatArea.setCaretPosition(outChatArea.getDocument().getLength()); 
			   
			//assignment to local reference
			this.serverAddress = serverAddress.trim();
			this.chatName = chatName.trim();
			this.password = password.trim();
			
			//Socket Communication
			ss = new ServerSocket(4567);
			 
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
		    System.out.println("Connecting to ChatServer at " + serverAddress + " on port 4444.");
		    Socket s = new Socket(this.serverAddress, 4444);
		    System.out.println("Connected to ChatServer!");
		    
		    //Checking chatName availability by sending name to Server
		    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		    oos.writeObject('\u007F' + "JOIN" + " " + chatName + " " + password);
		    
		    //check if the JOIN was successful
		    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
		    String serverReply = (String)ois.readObject();
		    
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
				PrivateChatClient cc = new PrivateChatClient(args[0],args[1], args[2]);
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
			       ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
			       Object something = ois.readObject();//wait for server to send.
			       if(something instanceof String){
			    	   String chatMessage = (String)something;
			    	   outChatArea.append(newLine + chatMessage);
				       outChatArea.setCaretPosition(outChatArea.getDocument().getLength());
			    	   System.out.println("Received:" + something);
			       }
			       else if(something instanceof String[]){
			    	   String[] clientList = (String[])something;
			    	   System.out.println("Currently in the Chat Room:");
			    	   for (String chatName : clientList){
			    		   System.out.println(chatName);
			    	   }
			    	   whosInList.setListData(clientList);
			       }
			       else if(something instanceof Vector){
			    	   Vector nclientList = (Vector)something;
			    	   System.out.println("Currently Not in the Chat Room:");
			    	   System.out.println(nclientList);
			    	   whosNOTInList.setListData(nclientList);
			       }
			       else System.out.println("Unexpected Object type received from server:" + something);
			       
			       System.out.println("Received: " + something);	
				}
				catch(IOException ioe) 
				{
					System.out.println("Receive error" + ioe);
				}
				catch(ClassNotFoundException cnfe)
				{
					
				}
			}
		}

		//@Override

public void actionPerformed(ActionEvent ae)
		  {
		  errorLabelField.setText("");
		  if (ae.getSource() == clearwhosInButton)
		     {
		     // do whatever when this button is pushed.
		     System.out.println("clearWhosInButton was pushed."); 
		     whosInList.clearSelection();
		     
		     
		     }

		  if (ae.getSource() == clearwhosNotInButton)
		     {
		     // do whatever when this button is pushed.
		     System.out.println("clearWhosNotButton was pushed."); 
		     whosNOTInList.clearSelection();
		     }

		  if (ae.getSource() == sendPrivateButton)
		     {
		     // do whatever when this button is pushed.
		     System.out.println("sendPrivateButton was pushed."); 
		     List<String> privateMessageList = whosInList.getSelectedValuesList();
		     if (privateMessageList.isEmpty())
		     {
		     System.out.println("No private message recipients were selected.");
		     return; // give the button it's thread back!
		     }
		     String[] privateMessageRecipients = privateMessageList.toArray(new String[0]);
		     System.out.println("Recipients of this private message will be:");   
		     for (String recipient : privateMessageRecipients)
		          System.out.println(recipient);
		     }

		  if (ae.getSource() == saveMessageButton)
		     {
		     // do whatever when this button is pushed.
		     System.out.println("saveMessageButton was pushed."); 
		     List<String> saveMessageList = whosNOTInList.getSelectedValuesList();
		     if (saveMessageList.isEmpty())
		     {
		     System.out.println("No save-message recipients were selected.");
		     return;
		     }
		  Vector<String> saveMessageRecipients 
		     = new Vector<String>(saveMessageList);
		  System.out.println("Recipients of this saved message will be:");
		  System.out.println(saveMessageRecipients);
		     }
		  
		  if(ae.getSource() == horizontalRButton){
			  System.out.println("Horizontal Button was pushed.");
			  chatPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT); 
		  }
		  
		  if(ae.getSource() == verticalRButton){
			  System.out.println("Vertical Button was pushed.");
			  chatPane.setOrientation(JSplitPane.VERTICAL_SPLIT); 
		  }
		  
		  if(ae.getSource() == sendToAllButton){
			  System.out.println("Send To All Button was pushed.");
			     String chatToSend = inChatArea.getText().trim();
			     if (chatToSend.length() == 0)//watch for blank-only entry 
				{
				errorLabelField.setText("NO MESSAGE WAS ENTERED"); 
				return; 
				}
			    inChatArea.setText(""); // clear input area on GUI
			    System.out.println("Sending " + chatToSend);
			    try{
			    Socket s = new Socket(serverAddress, 4444);
			    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			    oos.writeObject('\u007F' + chatName + " " + chatToSend);
			    }
			    catch(Exception e){
			    	System.out.println("Error sending...");
			    }
		  }
		  }

	
}
