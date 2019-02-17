import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.Net;

public class Client extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private ImageIcon icon = new ImageIcon("res/client.png");
	private JPanel contentPane;
	private JTextField textMessage;
	private JTextArea history;
	private JList listOnlineUsers;
	
	private Net net = null;
	private Thread run, listen;
	private String name; 
	private String address;
	
	private int port;
	private boolean running = false;
	
	public Client(String name, String address, int port) {
		this.name = name;
		this.address = address;
		this.port = port;	
		
		net = new Net(port);
		running = net.openConnection(address);
		
		if (!running) {
			System.err.println("Connection failed...");
			console("Connection failed...");
		}
		
		createWindow();
		console("You are trying to connect to: " + address + ", port: " + port + ", user name: " + name);
		String connectionPacket = "/c/" + name + "/e/";
		net.send(connectionPacket.getBytes());
		
		run = new Thread(() -> {
			running = true;
			listen();
		}, "Running");
		run.start();
	}
	
	private void createWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		setResizable(false);
		setIconImage(icon.getImage());
		setTitle("Messenger Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 600);
		setLocationRelativeTo(null);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		GridBagLayout gblContentPane = new GridBagLayout();
		gblContentPane.columnWidths = new int[] {200, 857, 30, 7};
		gblContentPane.rowHeights = new int[] {35, 475, 40};
		
		contentPane.setLayout(gblContentPane);
		
		history = new JTextArea();
		history.setEditable(false);
		history.setFont(new Font("consolas", Font.PLAIN, 14));
		
		JScrollPane scrollHistory = new JScrollPane(history);
		GridBagConstraints scrollConstraints = new GridBagConstraints();
		scrollConstraints.insets = new Insets(0, 0, 5, 5);
		scrollConstraints.fill = GridBagConstraints.BOTH;
		scrollConstraints.gridx = 1;
		scrollConstraints.gridy = 0;
		scrollConstraints.gridwidth = 3;
		scrollConstraints.gridheight = 2;
		scrollConstraints.weightx = 1;
		scrollConstraints.weighty = 1;
		scrollConstraints.insets = new Insets(0, 7, 0, 0);
		contentPane.add(scrollHistory, scrollConstraints);
		
		textMessage = new JTextField();
		textMessage.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					send(textMessage.getText(), true);
				}
			}
		});
		
		listOnlineUsers = new JList();
		listOnlineUsers.setFont(new Font("consolas", Font.PLAIN, 12));

		JScrollPane scrollUsers = new JScrollPane(listOnlineUsers);
		GridBagConstraints gbcOnlineUsers = new GridBagConstraints();
		gbcOnlineUsers.gridheight = 3;
		gbcOnlineUsers.insets = new Insets(0, 0, 5, 5);
		gbcOnlineUsers.fill = GridBagConstraints.BOTH;
		gbcOnlineUsers.gridx = 0;
		gbcOnlineUsers.gridy = 0;
		contentPane.add(scrollUsers, gbcOnlineUsers);
		
		GridBagConstraints gbcTextMessage = new GridBagConstraints();
		gbcTextMessage.insets = new Insets(0, 0, 0, 5);
		gbcTextMessage.fill = GridBagConstraints.HORIZONTAL;
		gbcTextMessage.gridx = 1;
		gbcTextMessage.gridy = 2;
		gbcTextMessage.weightx = 1;
		gbcTextMessage.weighty = 0;
		contentPane.add(textMessage, gbcTextMessage);
		textMessage.setColumns(10);
		
		JButton buttonSend = new JButton("Send");
		buttonSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send(textMessage.getText(), true);
			}
		});
		
		GridBagConstraints gbcButtonSend = new GridBagConstraints();
		gbcButtonSend.insets = new Insets(0, 0, 0, 5);
		gbcButtonSend.gridwidth = 2;
		gbcButtonSend.gridx = 2;
		gbcButtonSend.gridy = 2;
		contentPane.add(buttonSend, gbcButtonSend);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				String disconect = "/d/" + net.getID() + "/e/";
				send(disconect, false);
				running = false;
				net.close();
			}
		});
		
		setVisible(true);
		
		textMessage.requestFocusInWindow();
	}
	
	public void send(String message, boolean text) {
		if (message.equals(""))
			return;
		if (text) {
			message = name + ": " + message;
			message = "/m/" + message + "/e/";
			textMessage.setText("");
		}
		net.send(message.getBytes());
	}
	
	public void listen() {
		listen = new Thread(() -> {
			while (running) {
				String message = net.receive();								
				if (message.startsWith("/c/")) {
					net.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
					console("Successfuly connected to server! ID: " + net.getID());
				} else if (message.startsWith("/m/")) {
					String text = message.substring(3);
					text = text.split("/e/")[0];
					console(text);
				} else if (message.startsWith("/i/")) {
					String text = "/i/" + net.getID() + "/e/";
					send(text, false);
				} else if (message.startsWith("/u/")) {
					String[] users = message.split("/u/|/n/|/e/");
					updateUsersList(Arrays.copyOfRange(users, 1, users.length - 1));
				}
			}
		}, "Listen");
		listen.start();
	}
	
	public void console(String message) {
		history.append(message + "\n\r");
		history.setCaretPosition(history.getDocument().getLength());
	}
	
	public void updateUsersList(String[] users) {
		listOnlineUsers.setListData(users);
	}
	
}