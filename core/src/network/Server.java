package network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import GameGui.GameManager;
import network.packet.Packet;

public class Server extends Thread
{
	public ServerSocket server;
	public Socket socket;
	public static MultiplayerScreen screen;
	private int numPlayers = 1;
	public static ArrayList<ServerThread> clients = new ArrayList<ServerThread>();
	private BufferedReader in;
	private PrintWriter out;
	
	public Server(int port, int numPlayers)
	{
		this.numPlayers = numPlayers;
				
		try 
		{
			server = new ServerSocket(port);
			System.out.println("Server created");
		}catch (IOException e)
		{
			e.printStackTrace();
		}
		
		this.start();
	}
	
	public void launch()
	{
		for(int i = 0; i<numPlayers; i++)
		{
				try
				{
					Socket socket = server.accept();
					System.out.println(socket + "has connected");
					ServerThread c = new ServerThread(socket);
					clients.add(c);
					c.start();
				}
				catch(Exception e){}
		}
		
		clients.get(0).out.println("loadMap");
	}
	
	public void sendData(Packet packet)
	{
		
	}
}

