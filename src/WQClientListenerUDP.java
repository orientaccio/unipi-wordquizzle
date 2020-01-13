import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * WQClientListenerUDP is class that handles all the 
 * UDP challenge request from the server.
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

public class WQClientListenerUDP implements Runnable
{
	/**
	 * 	Class variables
	 * 
	 * 	@var	challenged: is the player being challenged by a friend
	 * 	@var	waiting: is the player waiting for the friend to accept the challenge
	 *  @var	playing: is the player playing a challenge
	 *  @var	socket: socket for UDP challenge request
	 */
	
	public boolean challenged = false;
	public boolean waiting = false;
	public boolean playing = false;
	private DatagramSocket socket;
	
	// constructor
	public WQClientListenerUDP(DatagramSocket socket)
	{
		this.socket = socket;
	}
	
	public void run()
	{
		try 							{ Thread.sleep(1000); 	} 
		catch (InterruptedException e1) { e1.printStackTrace(); }
		
		while (true)
		{
			// can't receive other challenge
			if (challenged || waiting || playing)
				continue;
			
			// create packet
			byte[] buffer = new byte[50];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			// receive message
			try
			{
				socket.receive(packet);
				ByteArrayInputStream iByteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
				DataInputStream iDataStream = new DataInputStream(iByteStream);
				String message = iDataStream.readUTF();
				System.out.println(message);
				
				// check if not null
				if (message != null)
					challenged = true;
				else
					System.out.println("MESSAGGIO VUOTO");
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
