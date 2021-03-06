package net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Net {

	private DatagramSocket socket;
	private InetAddress ip;
	private Thread send;

	private int port;
	private int ID = -1;

	public Net(int port) {
		this.port = port;
	}

	public boolean openConnection(String address) {
		try {
			socket = new DatagramSocket();
			ip = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public String receive() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String message = new String(packet.getData());
		
		return message;
	}

	public void send(final byte[] data) {
		send = new Thread(() -> {
			DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
			try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, "Send");
		send.start();
	}

	public void close() {
		new Thread(() -> {
			synchronized (socket) {			
				socket.close();
			}			
		}).start();
	}
	
	public void setID(int id) {
		this.ID = id;
	}
	
	public int getID() {
		return ID;
	}
	
}