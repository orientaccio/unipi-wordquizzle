import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * ThreadChallengeRoom is spawned by WQServer every time one client
 * sends the challenge request to his friend.
 * This Thread manages the entire duration of the challenge.
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

public class ThreadChallengeRoom implements Runnable
{
	// game rules constrains
	public static final int TIMEOUT_ACCEPT 	= 5;
	public static final int TIMEOUT_GAME 	= 30;
	public static final int TIME_GAME 		= 15;
	public static final int N_WORDS 		= 3;
	public static final int N_PLAYERS 		= 2;
	
	// game points values
	private static final int POINTS_VICTORY = 3;
	private static final int POINTS_CORRECT = 2;
	private static final int POINTS_WRONG	= -1;
	private static final int POINTS_NULL	= 0;
	
	// machine-state of this room
	public boolean started 					= false;
	public boolean refused 					= false;
	public boolean finish 					= false;
	public boolean timeout_accept 			= false;
	
	// player variables
	private String[] nicknames 				= new String[N_PLAYERS];
	private SelectionKey[] keys 			= new SelectionKey[N_PLAYERS];
	private SocketChannel[] socketChannels 	= new SocketChannel[N_PLAYERS];
	private ByteBuffer[] buffers 			= new ByteBuffer[N_PLAYERS];
	
	private int[] wordsCounter				= new int[N_PLAYERS];
	private int[] wordsCorrect				= new int[N_PLAYERS];
	private int[] wordsWrong				= new int[N_PLAYERS];
	private int[] scoreGame 				= new int[N_PLAYERS];
	
	private long startTime;
	private List<String> wordsGame;
	private List<String> wordsTranslated;
	
	// constructor
	public ThreadChallengeRoom(String nickUser, String nickFriend, SelectionKey userKey, List<String> words)
	{
		this.nicknames[0] 		= nickUser;
		this.nicknames[1] 		= nickFriend;
		this.keys[0] 			= userKey;
		this.socketChannels[0] 	= (SocketChannel) keys[0].channel();
		this.buffers[0] 		= (ByteBuffer) keys[0].attachment();
		this.wordsGame 			= words;
		this.wordsTranslated 	= new ArrayList<String>();
		
		wordsCounter[0] = 0;
		wordsCounter[1] = 0;
		wordsCorrect[0] = 0;
		wordsCorrect[1] = 0;
		wordsWrong[0] = 0;
		wordsWrong[1] = 0;
	}
	
	public void run()
	{
		// initialize start time for TIMEOUT_ACCEPT
		startTime = System.currentTimeMillis();
		try
		{
			// game does't start if not accepted/refuse/timeout
			while (!started && !refused && !timeout_accept) 
			{
				System.out.print("");
				timeout_accept = SecondsSinceStart() > TIMEOUT_ACCEPT;
			};

			// initialize start time for TIMEOUT_GAME
			startTime = System.currentTimeMillis();
			
			// set response outcome
			String response;
			response = (refused) ? "The challenge has been refused"
							 	 : String.format("%d --------------------------------\n" +
								 				"Challenge WORD QUIZZLE BATTLE start!\n" +
												"You have %d seconds to translate %d words.\n" +
								 				"Challenge 1/%d: %s", WQProtocol.CODE_SUCCESS,
												TIME_GAME, N_WORDS, N_WORDS, wordsGame.get(0));
			response = (timeout_accept) ? "Timeout. Challenge not accepted." 
										: response; 
			// write response outcome
			BufferUtils.WriteBuffer(socketChannels[0], buffers[0], response);
			BufferUtils.WriteBuffer(socketChannels[1], buffers[1], response);
			
			// challenge refused/timeout
			if (refused || timeout_accept)
			{
				finish = true;
				return;
			}
			
			// initialize selector
			Selector selector = Selector.open();

			// register both user for selector
			socketChannels[0].register(selector, SelectionKey.OP_READ, buffers[0]);
			socketChannels[1].register(selector, SelectionKey.OP_READ, buffers[1]);
			
			// writing challenges
			while (!FinishGame()) 
			{
				selector.selectNow(); 
				
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
						String request = BufferUtils.ReadBuffer(client, buffer);
						
						// split request to get nickname
						String[] requests = request.split(" ");
						
						// check if player 0 or 1
						int indexPlayer = (requests[requests.length-1].equals(nicknames[1])) ? 1 : 0;
						
						// update score based on the translation correctness
						wordsCounter[indexPlayer]++;
						wordsCorrect[indexPlayer] += (wordsTranslated.contains(requests[0])) ? 1 : 0;
						wordsWrong[indexPlayer]   += (wordsTranslated.contains(requests[0])) ? 0 : 1;
						
						// if player has finished skip and wait for the next player
						if (FinishGame() || wordsCounter[indexPlayer] >= N_WORDS)
							continue;
						
						// write new word
						String newWord = wordsGame.get(wordsCounter[indexPlayer]);
						response = String.format("Challenge %d/%d: %s", 
												wordsCounter[indexPlayer]+1, N_WORDS, newWord);
						BufferUtils.WriteBuffer(client, buffer, response);
					}
				}
			}
			
			// finish game write statistics
			String response0 = CalculateResults(0, 1);
			String response1 = CalculateResults(1, 0);
			BufferUtils.WriteBuffer(socketChannels[0], buffers[0], response0);
			BufferUtils.WriteBuffer(socketChannels[1], buffers[1], response1);			
			
			// finish challenge -> close all
			selector.close();
			finish = true;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// set player[1] info
	public void SetFriendSocketChannel(SelectionKey friendKey)
	{
		this.keys[1] = friendKey;
		socketChannels[1] = (SocketChannel) keys[1].channel();
		buffers[1] = (ByteBuffer) keys[1].attachment();
	}
	
	// start the challenge game
	// start the challenge
	public void StartGame()
	{
		started = true;
		TranslateWords();
	}
	
	// is the player in this room
	
	// is the player in the room
	public boolean InRoom(String nickname)
	{
		return nickname.equals(this.nicknames[1]);
	}
	
	// is the player in this room
	
	// is the player in the room
	public boolean InRoom(SelectionKey key)
	{
		return key.equals(keys[0]) || key.equals(keys[1]);
	}

	// calculate result string to send to player
	
	// get nicknames of the players
	public String[] GetNicknames()
	{
		return nicknames;
	}
	
	// get scores of the players
	public int[] GetScores()
	{
		return scoreGame;
	}
	
	/* ---------------------------------------------------------------------- */
	/* -------------------------- PRIVATE FUNCTIONS ------------------------- */
	/* ---------------------------------------------------------------------- */
	
	// calculate outcome of the challenge
	private String CalculateResults(int indexPlayer, int indexEnemy)
	{
		// calculate points
		CalculatePoints(indexPlayer);
		CalculatePoints(indexEnemy);
		
		// response message
		String result = String.format("END GAME. \n" + 
									"Words correct: %d\n" +
									"Words wrong:   %d\n" +
									"Words null:    %d\n" +
									"-----------------\n" +
									"Your score:    %d\n" +
									"Enemy score:   %d\n" +
									"-----------------\n", 
									wordsCorrect[indexPlayer], wordsWrong[indexPlayer],
									N_WORDS-wordsCorrect[indexPlayer]-wordsWrong[indexPlayer], 
									scoreGame[indexPlayer], scoreGame[indexEnemy]);
		
		// add victory point
		boolean winner = scoreGame[indexPlayer] > scoreGame[indexEnemy];
		scoreGame[indexPlayer] += winner ? POINTS_VICTORY : 0;
		
		// add winner message
		if (winner)
			result += String.format("You are the winner. You won %d extra points.\n" +
									"Final score: %d", POINTS_VICTORY, scoreGame[indexPlayer]);
		else if (scoreGame[indexPlayer] == scoreGame[indexEnemy])
			result += String.format("Draw.");
		else
			result += String.format("You lost the game.");
		
		return result;
	}
	
	// calculate points
	private void CalculatePoints(int index)
	{
		int wordsNull = N_WORDS - wordsCorrect[index] - wordsWrong[index];
		scoreGame[index] = wordsCorrect[index] * POINTS_CORRECT +
						wordsWrong[index] * POINTS_WRONG +
						wordsNull * POINTS_NULL;
	}
	
	// finish the game
	
	// check if the game is finished: timeout/end words
	
	// is the game finished? (end words/timeout)
	private boolean FinishGame()
	{
		return (wordsCounter[0] >= N_WORDS && wordsCounter[1] >= N_WORDS) 
			|| (SecondsSinceStart() > TIMEOUT_GAME);
	}	
	
	// translate words using MyMemory API
	// translate all the words in wordsGame using MyMemory API
	
	// translate all the words
	private void TranslateWords()
	{
		HttpURLConnection conn;
		BufferedReader rd = null;
		
		for (int i = 0; i < wordsGame.size(); i++)
		{
			try
			{
				// send request
				String urlGET = String.format("https://api.mymemory.translated.net/get?q=%s!&langpair=it|en", 
											wordsGame.get(i));	
				conn = (HttpURLConnection) new URL(urlGET).openConnection();
				conn.setRequestMethod("GET");
				
				// read result
				rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder result = new StringBuilder();
				String line;
				while ((line = rd.readLine()) != null) 
				   result.append(line);
				
				// process in JSON MyMemory's result
				String word = null;
				JSONParser parser = new JSONParser();
				try 
				{
					Object obj = parser.parse(result.toString());
					JSONObject responseFull = (JSONObject) obj;
					JSONObject responseData = (JSONObject) responseFull.get("responseData");
					word = (String) responseData.get("translatedText");
				}
				catch (org.json.simple.parser.ParseException e) { e.printStackTrace(); }
				wordsTranslated.add(word);

				// close reader
				rd.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			} 
		}
	}	
	
	// time counter since startTime
	// process result from MyMemory API

	// seconds since start
	
	// count seconds from start of game
	private int SecondsSinceStart() 
	{
        long nowTime = System.currentTimeMillis();
        return (int)((nowTime - this.startTime) / 1000);
    }
}
