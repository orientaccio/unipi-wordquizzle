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
		catch (RuntimeException ex) { port = WQClient.DEFAULT_PORT_TCP;	}
		client.port = port;
		
		// user input command, response from server
		Scanner input = new Scanner(System.in);
		String commands[];
		String command 	= null;
		
		do
		{
			String response = null;
			
			// waiting for the challenge request outcome
			if (client.connected && client.listener.waiting)
			{
				String responseChallenge = client.ReadMessage();
				client.ProcessChallengeResponse(responseChallenge);
				System.out.println(responseChallenge);
			}
			
			// get user input for command
			command = input.nextLine();
			commands = command.split(" ");
			
			// register command RMI
			if (commands[0].equals(WQProtocol.COMMAND_REGISTER))
			{
				// guard: can register only if not logged
				if (client.connected || commands.length != 3)
				{
					response = (client.connected) 		? "Logout to register a new account." 	: response;
					response = (commands.length != 3) 	? "Wrong arguments." 					: response;
				}
				else
				{
					int result = server.RegisterUser(commands[1], commands[2]);
					response = (result == WQProtocol.CODE_SUCCESS) ? "Registration success." : 
																	 "Registration failed."  ;
				}
			}
			// all other commands
			else
				response = client.ProcessCommand(command, commands);

			// check response
			if (response != null)
				System.out.println(response);
		}
		while (command != "exit");
		input.close();
	} 
}