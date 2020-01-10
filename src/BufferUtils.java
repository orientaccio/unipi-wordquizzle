import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BufferUtils
{
	public static void WriteBuffer(SocketChannel client, ByteBuffer buffer, String response) throws IOException
	{
		if (client == null || buffer == null || response == null)
			return;
		
		// writing response in socket channel
		buffer.clear();
		buffer.put(response.getBytes());
		buffer.flip();
		while (buffer.hasRemaining())
			client.write(buffer);
	}
	
	public static String ReadBuffer(SocketChannel client, ByteBuffer buffer) throws IOException
	{
		if (client == null || buffer == null)
			return null;
		
		// retrieve response from buffer
		String response = "";
		buffer.clear();
		client.read(buffer);
		buffer.flip();
		while (buffer.hasRemaining()) 
			response += (char) buffer.get();
		
		// reset buffer
		buffer.clear();
		
		return response;
	}
}
