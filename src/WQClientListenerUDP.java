import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class WQClientListenerUDP implements Runnable
{
	public boolean challenged = false;
	public boolean waiting = false;
	public boolean playing = false;
	private DatagramSocket socket;
	
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

			// input and output streams
			ByteArrayOutputStream oByteStream = new ByteArrayOutputStream();
			DataOutputStream oDataStream = new DataOutputStream(oByteStream);
			
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
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
