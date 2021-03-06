package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Server {

	private List<ServerClient> clients = new ArrayList<ServerClient>();
	private List<Integer> clientResponse = new ArrayList<Integer>();

	private DatagramSocket socket;
	private Thread serverRun, manageClients, receiveData, sendData;

	private final int MAX_ATTEMPTS = 5;
	
	private int port;
	private boolean running = false;
	private boolean raw = false;

	public Server(int port) {
		this.port = port;

		try { 
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}

		serverRun = new Thread(() -> {
			running = true;
			System.out.println("Server started on port: " + port);
			manage();
			receive();
			Scanner scanner = new Scanner(System.in);
			while (running) {
				String text = scanner.nextLine();
				if (!text.startsWith("/")) {
					sendToAll("/m/Server: " + text + "/e/");
					continue;
				}
				text = text.substring(1);
				if (text.equals("raw")) {
					if (raw)
						System.out.println("Raw mode off.");
					else
						System.out.println("Raw mode on.");
					raw = !raw;
				} else if (text.equals("clients")) {
					System.out.println("Clients:");
					System.out.println("~~~~~~~~");
					for (int i = 0; i < clients.size(); i++) {
						ServerClient c = clients.get(i);
						System.out.println(c.name + "(" + c.getID() + "): " + c.address.toString() + ":" + c.port);
					}
					System.out.println("~~~~~~~~");
				} else if (text.startsWith("kick")) {
					String name = text.split(" ")[1];
					int id = -1;
					boolean isNumber = true;					
					try {
						id = Integer.parseInt(name);					
					} catch (NumberFormatException e) {
						isNumber = false;
						e.printStackTrace();
					}
					if (isNumber) {
						boolean exist = false;
						for (int i = 0; i < clients.size(); i++) {
							if (clients.get(i).getID() == id) {
								exist = true;
								break;
							}
						}
						if (exist) 
							disconect(id, true);
						else 
							System.out.println("Client with id: " + id + " doesn't exist! Check ID number.");
					} else {
						for (int i = 0; i < clients.size(); i++) {
							ServerClient c = clients.get(i);
							if (name.equals(c.name)) {
								disconect(c.getID(), true);
								break;
							}
						}
					}
				} else if (text.equals("greetings")) {
					for (int i = 0; i < clients.size(); i++) {
						ServerClient c = clients.get(i);
						String message = "/m/Server: Hello, dear " + c.name + ". Glad to see you.";
						sendToAll(message + "/e/");
					}					
				} else if (text.equals("clear") || text.equals("cls")) {
					for(int i = 0; i < 50; i++)
					    System.out.println("");
				} else if (text.equals("datatime")) {
					DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					Date date = new Date();
					System.out.println(dateFormat.format(date));
				} else if (text.equals("help") || text.equals("?")) {
					printHelp();
				} else if (text.equals("poweroff")) {
					shutDown();
				} else {
					System.out.println("Unknown command.");
					printHelp();
				}
			}
			scanner.close();
		}, "Run");
		serverRun.start();
	}
	
	private void manage() {
		manageClients = new Thread(() -> {
			while (running) {
				sendToAll("/i/server");
				sendStatus();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for (int i = 0; i < clients.size(); i++) {
					ServerClient c = clients.get(i);
					if (!clientResponse.contains(c.getID())) {
						if (c.attempt >= MAX_ATTEMPTS) {
							disconect(c.getID(), false);
						} else {
							c.attempt++;
						}
					} else {
						clientResponse.remove(new Integer(c.getID()));
						c.attempt = 0;
					}
				}
			}
		}, "Manage");
		manageClients.start();
	}

	private void receive() {
		receiveData = new Thread(() -> {
			while (running) {
				byte[] data = new byte[1024];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				try {
					socket.receive(packet);
				} catch (SocketException e) {
					System.out.println("Terminated!\nAll clients disconected\nServer is shuted down");
				} catch (IOException e) {
					e.printStackTrace();
				}
				process(packet);
			}
		}, "Receive");
		receiveData.start();
	}
	
	private void send(final byte[] data, final InetAddress address, final int port) {
		sendData = new Thread(() -> {
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "Send");
		sendData.start();
	}
	
	private void send(String message, InetAddress address, int port) {
		message += "/e/";
		send(message.getBytes(), address, port);
	}
	
	private void sendStatus() {
		if (clients.size() <= 0) return;
		String users = "/u/";
		for (int i = 0; i < clients.size() - 1; i++)
			users += clients.get(i).name + "/n/";

		users += clients.get(clients.size() - 1).name + "/e/";
		sendToAll(users);
	}
	
	private void sendToAll(String message) {
		if (message.startsWith("/m/")) {
			String text = message.substring(3);
			text = text.split("/e/")[0];
			System.out.println(message);
		}
		
		if (raw) 
			System.out.println(message);
		
		for (int i = 0; i < clients.size(); i++) {
			ServerClient client = clients.get(i);
			send(message.getBytes(), client.address, client.port);
		}
	}
	
	private void process(DatagramPacket packet) {
		String str = new String(packet.getData());

		if (raw) 
			System.out.println(str);
		
		if (str.startsWith("/c/")) {
			int id = UniqueIdentifier.getIdentifier();
			String name = str.split("/c/|/e/")[1];
			System.out.println(name + "(" + id + ") connected");
			clients.add(new ServerClient(name, packet.getAddress(), packet.getPort(), id));
			String ID = "/c/" + id;
			send(ID, packet.getAddress(), packet.getPort());
		} else if (str.startsWith("/m/")) {
			sendToAll(str);
		} else if (str.startsWith("/d/")) {
			String id = str.split("/d/|/e/")[1];
			disconect(Integer.parseInt(id), true);
		} else if (str.startsWith("/i/")) {
			clientResponse.add(Integer.parseInt(str.split("/i/|/e/")[1]));
		} else {
			System.out.println(str);			
		}
	}
	
	private void printHelp() {
		System.out.println("Here is a list of all available commands:");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("/raw - enable raw mode.");
		System.out.println("/clients - show all connected clients.");
		System.out.println("/kick [user ID or username] - kicks a user");
		System.out.println("/datatime - show current data and time.");
		System.out.println("/greetings - show simple greetings to all clients in chat.");
		System.out.println("/clear or /cls - clear console log.");
		System.out.println("/help or /? - shows this help message.");
		System.out.println("/poweroff - shuts down the server.");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
	
	private void disconect(int id, boolean status) {
		ServerClient c = null;
		boolean kicked = false;
		for (int i = 0; i < clients.size(); i++) { 
			if (clients.get(i).getID() == id) {
				c = clients.get(i);
				clients.remove(i);
				kicked = true;
				break;
			}
		}
		
		if (!kicked)
			return;
		
		String message = "";
		if (status)
			message = "Client " + c.name + " (ID: " + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " disconnected."; 
		else
			message = "Client " + c.name + " (ID: " + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " timed out.";
		
		System.out.println(message);
	}
	
	private void shutDown() {
		for (int i = 0; i < clients.size(); i++ )
			disconect(clients.get(i).getID(), true);
		
		running = false;
		socket.close();
	}
}