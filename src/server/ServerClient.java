package server;

import java.net.InetAddress;

public class ServerClient {

	public InetAddress address;
	public String name;
	public final int ID;
	public int port;
	public int attempt = 0;

	public ServerClient(String name, InetAddress address, int port, int id) {
		this.name = name;
		this.address = address;
		this.port = port;
		this.ID = id;
	}

	public int getID() {
		return ID;
	}
	
}