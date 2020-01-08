import java.nio.*; 
import java.net.*; 
import java.nio.channels.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class MainClassWQServer 
{
	public static void main(String[] args) throws RemoteException
	{
		WQServer server = new WQServer();

		// RMI service: registry port, bind it
		LocateRegistry.createRegistry(9999);
		Registry r = LocateRegistry.getRegistry(9999);
		r.rebind(WQServer.SERVICE_NAME, server);
		System.out.println("WQServer registration service is in registry.");
		
		/* ---------------------------------------------------------------------- */
		/* -------------------- TCP WITH SELECTOR CONNECTION -------------------- */
		/* ---------------------------------------------------------------------- */
		
		// port selection
		int port;
		try 						{ port = Integer.parseInt(args[0]); } 
		catch (RuntimeException ex) { port = WQServer.DEFAULT_PORT; 	}
		
		// initialize channel and selector vars
		ServerSocketChannel serverChannel;
		Selector selector;
		try 
		{
			serverChannel = ServerSocketChannel.open();
			InetSocketAddress address = new InetSocketAddress(port);
			ServerSocket serverSocket = serverChannel.socket();
			serverSocket.bind(address);
			
			// start selector & set serverChannel non-blocking
			serverChannel.configureBlocking(false);
			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
			return;
		}
		
		while (true) 
		{
			try 
			{ 
				selector.select(); 
			} 
			catch (IOException ex) 
			{
				ex.printStackTrace();
			 	break;
			}
			
			Set <SelectionKey> readyKeys = selector.selectedKeys();
			Iterator <SelectionKey> iterator = readyKeys.iterator();
			while (iterator.hasNext()) 
			{
				SelectionKey key = iterator.next();
				// removes key from selectedSet, not from registeredSet
				iterator.remove();
				try 
				{
					if (key.isAcceptable()) 
					{
						ServerSocketChannel serverDirect = (ServerSocketChannel) key.channel();
					 	SocketChannel client = serverDirect.accept();
					 	System.out.println("Accepted connection from " + client);
					 	
					 	// set the client channel to non-blocking
					 	client.configureBlocking(false);
					 	ByteBuffer output = ByteBuffer.allocate(WQServer.MAX_MESSAGE_LENGTH);
					 	output.flip();
					 	
					 	// initialize & clean buffer
						client.read(output);
						output.clear();
					 	
					 	// register SelectionKey.OP_READ
					 	client.register(selector, SelectionKey.OP_READ, output);
					}
					else if (key.isReadable())
					{ 
						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer buffer = (ByteBuffer) key.attachment();
						
						// retrieve message from buffer
						String response = "";
						String request = BufferUtils.ReadBuffer(client, buffer);
						request = request.trim();
						
						// get the request command values
						String[] commands = request.split(" ");
						int result;
						
						// choose function
						switch (commands[0])
						{
							case "login":
								result = server.Login(commands[1], commands[2]);
								response = Integer.toString(result);
								break;
							case "add_friend":
								result = server.AddFriend(commands[2], commands[1]);
								response = Integer.toString(result);
								break;
							case "friend_list":
								break;
							case "challenge":
								break;
							case "show_scores":
								break;
							case "show_leaderboard":
								break;
							case "logout":								
								server.Logout(commands[1]);
								key.cancel();
								key.channel().close();
								break;
							default:
								break;
						}
						
						// send response
						BufferUtils.WriteBuffer(client, buffer, response);
					}
				}
				catch (IOException ex) 
				{ 
					key.cancel();
					try { key.channel().close(); }
					catch (IOException cex) {} 
				}
			}
		}
	}
}