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
		catch (RuntimeException ex) { port = WQServer.DEFAULT_PORT_TCP;	}
		
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
					 	ByteBuffer output = ByteBuffer.allocate(WQProtocol.MAX_MESSAGE_LENGTH);
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
						String response = null;
						String request = BufferUtils.ReadBuffer(client, buffer);
						request = request.trim();
						
						// get the request command values
						String[] commands = request.split(" ");
						int result;
						
						// choose function
						switch (commands[0])
						{
							case WQProtocol.COMMAND_LOGIN:
								result = server.Login(commands[1], commands[2]);
								response = Integer.toString(result);
								break;
							case WQProtocol.COMMAND_ADDFRIEND:
								result = server.AddFriend(commands[2], commands[1]);
								response = Integer.toString(result);
								break;							
							case WQProtocol.COMMAND_CHALLENGE:
								result = server.Challenge(commands[2], commands[1]);
								response = Integer.toString(result);
								break;
							case WQProtocol.COMMAND_SHOWSCORES:
								response = server.ShowScore(commands[1]);
								break;							
							case WQProtocol.COMMAND_FRIENDLIST:
								response = server.ShowFriendList(commands[1]).toString();
								break;
							case WQProtocol.COMMAND_SHOWLEADERBOARD:
								response = server.ShowLeaderboard(commands[1]).toString();
								break;
							case WQProtocol.COMMAND_LOGOUT:								
								server.Logout(commands[1]);
								key.cancel();
								key.channel().close();
								client.close();
								break;
							default:
								// read user UDP address and port
								if (commands.length == 3)
									server.SetUserUDPAddress(commands[2], commands[0], commands[1]);
								// read answer for challenge (Y/N) 
								if (commands.length == 1 && commands[0].length() == 1)
									response = server.ResponseChallenge(commands[0]);
						}
						
						// send response
						BufferUtils.WriteBuffer(client, buffer, response);
//						System.out.println(response);
					}
				}
				catch (IOException ex) 
				{ 
					ex.printStackTrace();
					key.cancel();
					try { key.channel().close(); }
					catch (IOException cex) {} 
				}
			}
		}
	}
}
