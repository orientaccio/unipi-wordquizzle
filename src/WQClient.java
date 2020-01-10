import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WQClient
{
	public static int DEFAULT_PORT_TCP = 1919;
	public static int DEFAULT_PORT_UDP = 1920;
	public String nickname;
	public boolean connected;
	
	private SocketAddress address;
	private SocketChannel client;
	private ByteBuffer buffer;
	
	public DatagramSocket socketUDP;
	public WQClientListenerUDP listener;
	
	public WQClient() throws SocketException
	{
		connected = false;
		nickname = null;
	}
	
	public void InitializeSocket(int port) throws IOException
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
	
	public void WriteMessage(String command) throws IOException
	{
		BufferUtils.WriteBuffer(client, buffer, command);
	}
	
	public String ReadMessage() throws IOException
	{
		return BufferUtils.ReadBuffer(client, buffer);
	}
	
	public String GetAddressSocketUDP()
	{
		return socketUDP.getLocalAddress() + " " + socketUDP.getLocalPort();
	}
	
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
