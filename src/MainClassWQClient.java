import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.registry.*;
import java.util.Scanner;

public class MainClassWQClient
{
	public static void main(String[] args) throws NotBoundException, InterruptedException, IOException
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
						response = (client.connected) ? "Logout to register a new account." :
												 		"Command incomplete.";
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
					if (!client.connected || commands.length != 2)
					{
						response = (!client.connected) ? "Not logged." :
												  		 "Command incomplete." ;
						break;
					}
					// add nickname in the command
					command = command.concat(" ").concat(client.nickname);
					
					// initialize connection variables
					client.InitializeSocket(port);
					
					// read and write
					client.WriteMessage(command);
					response = client.ReadMessage();
					
					// elaborate response
					boolean success = response.equals(Integer.toString(WQProtocol.CODE_SUCCESS));
					response = (success) ? "Friendship " + client.nickname + "-" + commands[1] + " created":
										   "Friendship failed" ;
					break;
				case WQProtocol.COMMAND_FRIENDLIST:
					// guard
					if (!client.connected || commands.length != 1)
					{
						response = (!client.connected) ? "Not logged." :
														 "Command incomplete";
					}
					
					break;
				default:
					System.out.println("Command not available.");
			}
			System.out.println(response);
		}
		while (command != "exit");
		input.close();
	} 
}
