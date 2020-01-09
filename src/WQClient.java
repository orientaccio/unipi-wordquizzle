import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WQClient
{
	public static int DEFAULT_PORT = 1919;
	public String nickname;
	public boolean connected;
	
	private SocketAddress address;
	private SocketChannel client;
	private ByteBuffer buffer;
	
	public WQClient()
	{
		connected = false;
		nickname = null;
	}
	
	public void InitializeSocket(int port) throws IOException
	{
		address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
		client = SocketChannel.open(address);
		buffer = ByteBuffer.allocate(WQProtocol.MAX_MESSAGE_LENGTH);
	}
	
	public void WriteMessage(String command) throws IOException
	{
		BufferUtils.WriteBuffer(client, buffer, command);
	}
	
	public String ReadMessage() throws IOException
	{
		return BufferUtils.ReadBuffer(client, buffer);
	}
}
