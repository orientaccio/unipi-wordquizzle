import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


/**
 * ThreadChallengeRoom is spawned by WQServer when two client
 * want to start the game.
 * This Thread manages the entire duration of the challenge.
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */
public class ThreadChallengeRoom implements Runnable
{
	public static final int TIMEOUT_ACCEPT 	= 5;
	public static final int TIMEOUT_GAME 	= 30;
	public static final int TIME_GAME 		= 15;
	public static final int N_WORDS 		= 2;
	public static final int N_PLAYERS 		= 2;
	
	public boolean started 					= false;
	public boolean refused 					= false;
	public boolean finish 					= false;
	public boolean timeout_accept 			= false;
	
	private String[] nicknames 				= new String[N_PLAYERS];
	private SelectionKey[] keys 			= new SelectionKey[N_PLAYERS];
	private SocketChannel[] socketChannels 	= new SocketChannel[N_PLAYERS];
	private ByteBuffer[] buffers 			= new ByteBuffer[N_PLAYERS];
	private int[] scoreGame 				= new int[2];
	
	private long startTime;
	private String[] wordsGame;
	
	public ThreadChallengeRoom(String nickUser, String nickFriend, SelectionKey userKey, String[] wordsGame)
	{
		this.nicknames[0] 		= nickUser;
		this.nicknames[1] 		= nickFriend;
		this.keys[0] 			= userKey;
		this.socketChannels[0] 	= (SocketChannel) userKey.channel();
		this.buffers[0] 		= (ByteBuffer) userKey.attachment();
		this.wordsGame 			= wordsGame;
	}
	
	public void run()
	{
		// initialize start time
		startTime = System.currentTimeMillis();
		try
		{
			// game does't start if not accepted/refuse/timeout
			while (!started && !refused && !timeout_accept) 
			{
				System.out.print(" ");
				timeout_accept = SecondsSinceStart() > TIMEOUT_ACCEPT;
			};

			// set response outcome
			String response;
			response = (refused) ? "The challenge has been refused"
							 	 : String.format("%d --------------------------------\n" +
								 				"Challenge WORD QUIZZLE BATTLE start!\n" +
												"You have %d seconds to translate %d words.\n" +
								 				"Challenge 1/%d: %s", 
												WQProtocol.CODE_SUCCESS, TIME_GAME, 
												N_WORDS, N_WORDS, wordsGame[0]);
			response = (timeout_accept) ? "Timeout. Challenge not accepted." 
										: response; 
			// write response outcome
			BufferUtils.WriteBuffer(socketChannels[0], buffers[0], response);
			BufferUtils.WriteBuffer(socketChannels[1], buffers[1], response);
			
			// check response outcome
			String[] tokens = response.split(" ");
			
			// challenge start
			try 
			{  
				if (Integer.parseInt(tokens[0]) == WQProtocol.CODE_SUCCESS)
				{
					// initialize selector
					Selector selector;
					selector = Selector.open();

					// register both user for selector
					socketChannels[0].register(selector, SelectionKey.OP_READ, buffers[0]);
					socketChannels[1].register(selector, SelectionKey.OP_READ, buffers[1]);
					
					while (true) 
					{
						selector.select(); 
						
						Set <SelectionKey> readyKeys = selector.selectedKeys();
						Iterator <SelectionKey> iterator = readyKeys.iterator();
						
						while (iterator.hasNext()) 
						{
							SelectionKey key = iterator.next();
							iterator.remove();
							
							if (key.isReadable())
							{
								SocketChannel client = (SocketChannel) key.channel();
								ByteBuffer buffer = (ByteBuffer) key.attachment();
								
								// retrieve message from buffer
								String wordResponse = "BESTIA DI SATANA";
								String request = BufferUtils.ReadBuffer(client, buffer);

								// TODO process translation and assign points
								
								
								// write 
								BufferUtils.WriteBuffer(client, buffer, wordResponse);
							}
						}
					}
				} 
		    }
			// challenge refused/timeout -> finish room
			catch (NumberFormatException ex) 
			{  
				return;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void SetFriendSocketChannel(SelectionKey friendKey)
	{
		this.keys[1] = friendKey;
		socketChannels[1] = (SocketChannel) friendKey.channel();
		buffers[1] = (ByteBuffer) friendKey.attachment();
	}
	
	public void StartGame()
	{
		started = true;
		TranslateWords();
	}
	
	public boolean InRoom(String nickname)
	{
		return nickname.equals(this.nicknames[1]);
	}
	
	public boolean InRoom(SelectionKey key)
	{
		return key.equals(keys[0]) || key.equals(keys[1]);
	}
	
	private void TranslateWords()
	{
		
	}
	
	private int SecondsSinceStart() {
        long nowTime = System.currentTimeMillis();
        return (int)((nowTime - this.startTime) / 1000);
    }
}
