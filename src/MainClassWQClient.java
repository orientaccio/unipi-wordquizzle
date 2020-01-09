import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.*;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MainClassWQClient
{
	public static void main(String[] args) throws NotBoundException, InterruptedException, IOException, ParseException
	{
		WQClient client = new WQClient();
		
		// lookup in the registry to find the register service
		Registry r = LocateRegistry.getRegistry(9999);
		IWQServerRMI server = (IWQServerRMI) r.lookup(WQServer.SERVICE_NAME);
		
		// TCP connection variable
		int port;
		try 						{ port = Integer.parseInt(args[0]); } 
		catch (RuntimeException ex) { port = WQClient.DEFAULT_PORT; }
		
		// user input command, response from server
		Scanner input = new Scanner(System.in);
		String commands[];
		String command 	= null;
		
		do
		{
			boolean success = false;
			String response = null;
			
			// get user input for command: register/login
			command = input.nextLine();
			commands = command.split(" ");

			// execute command
			int result = 0;
			switch (commands[0])
			{
				case WQProtocol.COMMAND_REGISTER:
					// guard: can register only if not logged
					if (client.connected || commands.length != 3)
					{
						response = (client.connected) 		? "Logout to register a new account." 	: response;
						response = (commands.length != 3) 	? "Wrong arguments." 				: response;
						break;
					}
					result = server.RegisterUser(commands[1], commands[2]);
					response = (result == WQProtocol.CODE_SUCCESS) ? "Registration success." : 
																	 "Registration failed."  ;
					break;
				case WQProtocol.COMMAND_LOGIN:
					// guard
					if (commands.length != 3)
					{
						response = "Command incomplete.";
						break;
					}
					
					// initialize connection variables
					client.InitializeSocket(port);
					
					// read write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
					// response positive -> set connected = true
					client.connected = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
					client.nickname = (client.connected) ? commands[1] : null;
					response = (client.connected) ? "Login success." :
											 		"Login failed."  ;
					break;
				case WQProtocol.COMMAND_ADDFRIEND:
					// guard
					if (!client.connected || commands.length != 2 || commands[1].equals(client.nickname))
					{
						response = (!client.connected) 		? "Not logged." 				 : response;
						response = (commands.length != 2) 	? "Wrong arguments." : response;
						response = (commands[1].equals(client.nickname)) ? "Friend invalid." : response;
						break;
					}
					
					// add nickname in the command
					command = command.concat(" ").concat(client.nickname);
					
					// read and write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
					// elaborate response
					success = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
					response = (success) ? "Friendship " + client.nickname + "-" + commands[1] + " created":
										   "Command failed" ;
					break;
				case WQProtocol.COMMAND_SHOWSCORES:
					// guard
					if (!client.connected || commands.length != 1)
					{
						response = (!client.connected) 		? "Not logged." 	 : response;
						response = (commands.length != 1) 	? "Wrong arguments." : response ;
					}
					
					// add nickname in the command
					command = command.concat(" ").concat(client.nickname);
					
					// read and write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
					// elaborate response
					success = response != null;
					response = (success) ? response : "Command failed" ;
					break;
				case WQProtocol.COMMAND_FRIENDLIST:
					// guard
					if (!client.connected || commands.length != 1)
					{
						response = (!client.connected) 		? "Not logged." 	 : response;
						response = (commands.length != 1) 	? "Wrong arguments." : response ;
					}
					
					// add nickname in the command
					command = command.concat(" ").concat(client.nickname);
					
					// read and write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
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
					if (!client.connected || commands.length != 1)
					{
						response = (!client.connected) 		? "Not logged." 	 : response;
						response = (commands.length != 1) 	? "Wrong arguments." : response ;
					}
					
					// add nickname in the command
					command = command.concat(" ").concat(client.nickname);
					
					// read and write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
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
				default:
					System.out.println("Command not available.");
			}
			if (response != null)
				System.out.println(response);
		}
		while (command != "exit");
		input.close();
	} 
}
