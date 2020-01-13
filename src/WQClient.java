import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * WQClient is class containing all the information of client.
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

public class WQClient
{
	/**
	 * 	Class variables
	 * 
	 * 	@var	nickName: player nickname
	 * 	@var	connected: is the player logged
	 *  @var	port: port for communication TCP
	 *  @var	address: address for communication TCP
	 *  @var	client: channel for communication TCP
	 * 	@var	buffer: buffer for communication TCP
	 *  @var	socketUDP: socket for UDP challenge 
	 *  @var	listener: thread listener for UDP challenge 
	 */
	
	public static int DEFAULT_PORT_TCP = 1919;
	public String nickname;
	public boolean connected;
	public int port;
	
	private SocketAddress address;
	private SocketChannel client;
	private ByteBuffer buffer;
	
	public DatagramSocket socketUDP;
	public WQClientListenerUDP listener;
	
	// constructor
	public WQClient() throws SocketException
	{
		connected = false;
		nickname = null;
		
		address = null;
		client = null;
		buffer = null;
		socketUDP = null;
		listener = null;
	}
	
	// process the input command
	public String ProcessCommand(String command, String[] commands) throws IOException, ParseException
	{
		boolean success = false;
		String response = "";
		
		// process command
		switch (commands[0])
		{
			case WQProtocol.COMMAND_LOGIN:
				// guard
				if (connected || commands.length != 3)
				{
					response = (connected)				? "Already logged."  : response;
					response = (commands.length != 3) 	? "Wrong arguments." : response;
					break;
				}
				
				// initialize connection variables
				InitializeSocket();
				String address = GetAddressSocketUDP();
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// response positive -> set connected = true
				connected = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
				nickname = (connected) ? commands[1] : null;
				response = (connected) ? "Login success." :
										 "Login failed."  ;
				
				if (!connected)
					break;
				
				// send UDP info to server
				address += " " + nickname;
				address = address.substring(1);
				WriteMessage(address);
				break;
			case WQProtocol.COMMAND_ADDFRIEND:
				// guard
				if (!connected || commands.length != 2 || commands[1].equals(nickname))
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 2) 	? "Wrong arguments." : response;
					response = (commands[1].equals(nickname)) ? "Friend invalid." : response;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// elaborate response
				success = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
				response = (success) ? "Friendship " + nickname + "-" + commands[1] + " created"
									 : "Command failed" ;
				break;
			case WQProtocol.COMMAND_CHALLENGE:
				// guard
				if (!connected || commands.length != 2)
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 1) 	? "Wrong arguments." : response ;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// elaborate response
				success = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
				response = (success) ? "Challenge sent. Waiting for acceptance..." 
									 : "Command failed" ;
				listener.waiting = success;
				break;
			case WQProtocol.COMMAND_SHOWSCORES:
				// guard
				if (!connected || commands.length != 1)
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 1) 	? "Wrong arguments." : response ;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// elaborate response
				success = response != null;
				response = (success) ? response : "Command failed" ;
				break;
			case WQProtocol.COMMAND_FRIENDLIST:
				// guard
				if (!connected || commands.length != 1)
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 1) 	? "Wrong arguments." : response ;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// parsing JSON response
				JSONParser parserFriendlist = new JSONParser(); 
				JSONObject friends = (JSONObject) parserFriendlist.parse(response);
				JSONArray arrayFriends = (JSONArray) friends.get("friendlist");
				response = "";
				for (int i = 0; i < arrayFriends.size(); i++)
				{
					response += arrayFriends.get(i);
					response += (i < arrayFriends.size() - 1) ? ", " : "";
				}
				
				// elaborate response
				success = response != null;
				response = (success) ? response : "Command failed" ;
				break;
			case WQProtocol.COMMAND_SHOWLEADERBOARD:
				// guard
				if (!connected || commands.length != 1)
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 1) 	? "Wrong arguments." : response ;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// parsing JSON response
				JSONParser parserLeaderboard = new JSONParser(); 
				JSONObject leaderboard = (JSONObject) parserLeaderboard.parse(response);
				JSONArray arrayUsers = (JSONArray) leaderboard.get("leaderboard");
				response = "";
				for (int i = 0; i < arrayUsers.size(); i++)
				{
					JSONObject user = (JSONObject) arrayUsers.get(i);
					response += user.get("nickname") + " ";
					response += user.get("score");
					response += (i < arrayUsers.size() - 1) ? ", " : "";
				}
				
				// elaborate response
				success = response != null;
				response = (success) ? response : "Command failed" ;
				break;
			case WQProtocol.COMMAND_LOGOUT:
				// guard
				if (!connected || commands.length != 1)
				{
					response = (!connected) 			? "Not logged." 	 : response;
					response = (commands.length != 1) 	? "Wrong arguments." : response ;
					break;
				}
				
				// add nickname in the command
				command = command.concat(" ").concat(nickname);
				
				// write and read
				WriteMessage(command);
				response = ReadMessage();
				
				// response positive -> set connected = true
				connected = response.equals(Integer.toString(WQProtocol.CODE_FAIL));
				response = (connected) ? "Logout failed." 
									   : "Logout success.";
				break;
			default:
				// response to challenge
				if (listener != null && listener.challenged)
				{
					// add nickname in the command
					command = command.concat(" ").concat(nickname);
					
					// check valid response
					String[] tmp = command.split(" ");
					if (tmp.length != 2)
					{
						response = "Command not available.";
						break;						
					}
					
					// write and read
					WriteMessage(command);
					response = ReadMessage();
					ProcessChallengeResponse(response);
					break;
				}
				// response with translation words
				if (listener != null && listener.playing)
				{
					// add nickname in the command
					command = command.concat(" ").concat(nickname);
					
					// write and read
					WriteMessage(command);
					response = ReadMessage();
					
					// check if end game -> reset status
					String[] responses = response.split(" ");
					if (responses[0].equals("END"))
					{
						listener.playing = false;
						listener.waiting = false;
						listener.challenged = false;
					}
					break;
				}
				
				// unknown command
				response = "Command not available.";
		}
		return response;
	}
	
	// initialize TCP and UDP sockets
	public void InitializeSocket() throws IOException
	{
		address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		client = SocketChannel.open(address);
		buffer = ByteBuffer.allocate(WQProtocol.MAX_MESSAGE_LENGTH);

		// tmp socket to get a random port
		DatagramSocket tmpSocket = new DatagramSocket();
		int tmpPort = tmpSocket.getLocalPort();
		tmpSocket.close();
		
		// real socket
		SocketAddress addressUDP = new InetSocketAddress(InetAddress.getLoopbackAddress(), tmpPort);
		socketUDP = new DatagramSocket(addressUDP);
		
		// start Thread Listener
		StartUDPListener(socketUDP);
	}
	
	// write message in buffer
	public void WriteMessage(String command) throws IOException
	{
		BufferUtils.WriteBuffer(client, buffer, command);
	}
	
	// read message from buffer
	public String ReadMessage() throws IOException
	{
		return BufferUtils.ReadBuffer(client, buffer);
	}
	
	// get socketUDP <address, port>
	public String GetAddressSocketUDP()
	{
		return socketUDP.getLocalAddress() + " " + socketUDP.getLocalPort();
	}

	// check if the challenge has been accepted or refused or timeout
	public void ProcessChallengeResponse(String response)
	{
		String[] tokens = response.split(" ");
		
		// challenge start
		try 
		{  
			if (Integer.parseInt(tokens[0]) == WQProtocol.CODE_SUCCESS)
			{
				listener.waiting = false;
				listener.challenged = false;
				listener.playing = true;
			} 
			else
			{
				listener.waiting = false;
				listener.challenged = false;
				listener.playing = false;
			}
	    }
		// challenge refused/timeout
		catch (NumberFormatException e) 
		{  
			listener.waiting = false;
			listener.challenged = false;
			listener.playing = false;
		}
	}
	
	// start the thread listener UDP
	public void StartUDPListener(DatagramSocket socket)
	{
		// create UDP socket
		socketUDP = socket;

		// start thread listener
		listener = new WQClientListenerUDP(socketUDP);
		Thread t = new Thread(listener);
		t.setDaemon(true);
		t.start();
	}
}
