import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.net.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * WQServer is the server which manages all the game
 * 
 * The server persists all the information about
 * <registrations, friendship relations, user scores>
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

public class WQServer extends UnicastRemoteObject implements IWQServer, IWQServerRMI
{
	// Class static variables
	public static final int DEFAULT_PORT_TCP = 1919;
	private static final long serialVersionUID = 1L;
	private static final String PATH_USERDATA = "UserData.json";
	private static final String PATH_WORDS = "words.json";
	
	/**
	 * 	Class variables
	 * 
	 * 	@var	words: dictionary containing all the words from the file "dictionary.dat"
	 * 	@var	users: dictionary containing all the registered user on the server
	 *  @var	rooms: all the room spawned for the challenge
	 *  @var	words: all the words of the dictionary
	 */
	private ThreadPoolExecutor executor;
	private HashMap<String, UserData> users;
	private List<ThreadChallengeRoom> rooms;
	private List<String> words;
	
	// constructor
 	public WQServer() throws RemoteException
	{
		super();
		words = new ArrayList<String>();
		users = new HashMap<String, UserData>();
		
		// initialize executor
		executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		
		// initialize rooms
		rooms = new ArrayList<ThreadChallengeRoom>();
		
		// load information about users, words from JSON
		ReadUserDataJSON();
		ReadWordsJSON();
	}
	
	/* ---------------------------------------------------------------------- */
	/* ------------------------ INTERFACE FUNCTIONS ------------------------- */
	/* ---------------------------------------------------------------------- */
 	
	/**
	 * Register a new user to the server using RMI
	 * 
	 * @param	nickUser is the nickname
	 * @param	password is the password of nickname	
	 * @return	1 if success
	 * 			0 error: nickname not available,
	 * 					password null
	 * @throws ParseException 
	 */
	public int RegisterUser(String nickUser, String password) throws RemoteException
	{
		if (nickUser == null || password == null)
			return WQProtocol.CODE_FAIL;
		if (UserExists(nickUser))
			return WQProtocol.CODE_FAIL;
		
		UserData tmp = new UserData(nickUser, password);
		users.put(nickUser, tmp);
		SaveUserDataJSON();
		
		return WQProtocol.CODE_SUCCESS;
	}
	
	/**
	 * Login of an already registered user creating a TCP connection,
	 * which will be used for all the following interactions
	 * from the player
	 * 
	 * @param	nickUser is the nickname
	 * @param	password is the password of nickname	
	 * @return	1 if success
	 * 			0 error: already logged in,
	 * 					password wrong
	 */
	public int Login(String nickUser, String password)
	{
		if (nickUser == null || password == null)
			return WQProtocol.CODE_FAIL;
		
		UserData tmp = users.get(nickUser);
		if (tmp == null || !tmp.password.equals(password) || tmp.status == 1)
			return WQProtocol.CODE_FAIL;
		
		// login success
		tmp.status = 1;
		return WQProtocol.CODE_SUCCESS;
	}
	
	/**
	 * Logout of an online user closing TCP
	 * 
	 * @param	nickUser is the nickname	
	 * @effect	logout of the user
	 */
	public int Logout(String nickUser)
	{
		if (nickUser == null)
			return WQProtocol.CODE_FAIL;
		
		UserData tmp = users.get(nickUser);
		tmp.status = 0;
		return WQProtocol.CODE_SUCCESS;
	}
	
	/**
	 * The current user adds a friend to his friend-list
	 * 
	 * @param	nickUser is the nickname
	 * @param	nickFriend is the nickname of friend
	 * @return	1 if success
	 * 			0 error: nickFriend not found,
	 * 					friendship already enstablished
	 */
	public int AddFriend(String nickUser, String nickFriend)
	{
		if (nickUser == null || nickFriend == null)
			return WQProtocol.CODE_FAIL;

		// friend not found on this server
		UserData tmpUser = users.get(nickUser);
		UserData tmpFriend = users.get(nickFriend);
		if (tmpUser == null || tmpFriend == null)
			return WQProtocol.CODE_FAIL;
		
		// friendship already enstablished
		if (tmpUser.listFriends.contains(tmpFriend.nickName))
			return WQProtocol.CODE_FAIL;
		
		// add friend success
		tmpUser.listFriends.add(nickFriend);
		tmpFriend.listFriends.add(nickUser);
		
		// save JSON
		SaveUserDataJSON();
		
		return WQProtocol.CODE_SUCCESS;
	}

	/**
	 * The server sends a challenge request to the nickFriend using UDP, 
	 * if accepted in time (T1) the challenge begins.
	 * The server chooses K random words from the dictionary,
	 * then sends the word one by one to the players.
	 * Game ends when timer ends (T2) or there's no more words.
	 * 
	 * Words are translated using the service
	 * "https://mymemory.translated.net/doc/spec.php"
	 * the translations are initialized and
	 * saved for the entire game.
	 * 
	 * Correct answer gives the player (X) points
	 * Wrong answer substracts (Y) points
	 * The server declares the winner with more points.
	 * 
	 * @param	nickUser is the nickname
	 * @param	nickFriend is the nickname of friend	
	 * @return	1 if success
	 * 			0 error: nickFriend not found in friend-list
	 * 			2 if challenge not accepted
	 * @throws IOException 
	 */
	public int Challenge(String nickUser, String nickFriend) throws IOException
	{
		if (nickUser == null || nickFriend == null)
			return WQProtocol.CODE_FAIL;
		
		// nickFriend not found on this server
		UserData tmpUser = users.get(nickUser);
		UserData tmpFriend = users.get(nickFriend);
		if (tmpUser == null || tmpFriend == null || tmpFriend.status == 0 || tmpFriend.status == 2)
			return WQProtocol.CODE_FAIL;
		
		// friendship not enstablished
		if (!tmpUser.listFriends.contains(tmpFriend.nickName))
			return WQProtocol.CODE_FAIL;
		
		// UDP request to friend
		ForwardChallengeUDP(tmpUser, tmpFriend);
		return WQProtocol.CODE_SUCCESS;
	}

	/**
	 * Show the score of every game of the player
	 * 
	 * @param	nickUser is the nickname
	 * @effect  show the scores
	 */
	public String ShowScore(String nickUser)
	{
		if (nickUser == null)
			return null;
		
		// friend not found on this server
		UserData tmpUser = users.get(nickUser);
		if (tmpUser == null)
			return null;
		
		return Integer.toString(tmpUser.GetTotalScore());
	}

	/**
	 * Show all the friends of the current user
	 * 
	 * @param	nickUser is the nickname
	 * @return	returns a JSON object containing all the friends	
	 */
	public JSONObject ShowFriendList(String nickUser)
	{
		if (nickUser == null)
			return null;
		
		// user not found on this server
		UserData tmpUser = users.get(nickUser);
		if (tmpUser == null)
			return null;
		
		// create JSON response
		JSONObject friends = new JSONObject();
		JSONArray friendlist = new JSONArray();
		for (int i = 0; i < tmpUser.listFriends.size(); i++)
			friendlist.add(tmpUser.listFriends.get(i));
		friends.put("friendlist", friendlist);
		return friends;
	}
	
	/**
	 * Show the score leaderboard of the user and his friends in JSON
	 * 
	 * @param	nickUser is the nickname
	 * @effect	show leaderboad
	 */
	public JSONObject ShowLeaderboard(String nickUser)
	{
		if (nickUser == null)
			return null;
		
		// user not found on this server
		UserData tmpUser = users.get(nickUser);
		if (tmpUser == null)
			return null;
		
		// create leaderboard
		JSONObject leaderboard = new JSONObject();
		JSONArray arrayUsers = new JSONArray();
		
		List<String> tmpFriends = new UserData(tmpUser).listFriends;
		while(tmpFriends.size() > 0)
		{
			int max = 0;
			int index = 0;
			for (int i = 0; i < tmpFriends.size(); i++)
			{
				int score = users.get(tmpFriends.get(i)).GetTotalScore();
				if (score > max)
				{
					max = score;
					index = i;
				}
			}
			JSONObject user = new JSONObject();
			user.put("nickname", tmpFriends.get(index));
			user.put("score", max);
			arrayUsers.add(user);
			tmpFriends.remove(index);
		}
		leaderboard.put("leaderboard", arrayUsers);
		return leaderboard;
	}
	
	/* ---------------------------------------------------------------------- */
	/* -------------------------- PUBLIC FUNCTIONS -------------------------- */
	/* ---------------------------------------------------------------------- */
	
	// response for the challenge
	public String ResponseChallenge(String challengedAnswer, String nickFriend, SelectionKey friendKey) throws IOException
	{
		String response = null;
		
		for (int i = 0; i < rooms.size(); i++)
		{
			// join the correct room
			if (rooms.get(i).InRoom(nickFriend))
			{
				switch(challengedAnswer)
				{
					// start challenge
					case "Y":
						users.get(nickFriend).status = 2;
						rooms.get(i).SetFriendSocketChannel(friendKey);
						rooms.get(i).StartGame();
						break;
					// refuse challenge
					case "N":
						rooms.get(i).refused = true;
						response = "Challenge refused.";
						
						// reset status player 0
						String[] nicknames = rooms.get(i).GetNicknames();
						users.get(nicknames[0]).status = 1;
						
						// remove room
						rooms.remove(i);
						break;
					// timeout/error
					default:
						response = "Challenge timeout/Error.";
				}
				break;
			}
		}
		return response;
	}
	
	// create threadChallengeRoom
	public void CreateChallengeRoom(String userNick, String friendNick, SelectionKey userKey)
	{		
		// choose words
		List<String> wordsGame = RandomWords();
		
		// set status: inRoom
		users.get(userNick).status = 2;
		
		// start thread challenge room
		ThreadChallengeRoom room = new ThreadChallengeRoom(userNick, friendNick, userKey, wordsGame);
		executor.execute(room);
		rooms.add(room);
	}
	
	// set the user UDP variables
	public void SetUserUDPAddress(String user, String address, String port) throws UnknownHostException
	{
		UserData tmp = users.get(user);
		tmp.UDPAddress = InetAddress.getByName(address);
		tmp.UDPPort = Integer.parseInt(port);
	}

	// is the player in a room <-> status = playing
	public boolean InRooms(SelectionKey key)
	{
		for (int i = 0; i < rooms.size(); i++)
		{
			if (rooms.get(i).InRoom(key))
			{
				// challenge not finished
				if (!rooms.get(i).finish)
					return true;
				// challenge finished
				else
				{
					// get players info and close room
					String[] nicknames = rooms.get(i).GetNicknames();
					int[] scores = rooms.get(i).GetScores();
					rooms.remove(i);
					
					// save new scores in JSON and reset status
					for (int j = 0; j < scores.length; j++)
					{
						users.get(nicknames[j]).listScores.add(scores[j]);
						users.get(nicknames[j]).status = 1;
					}
					SaveUserDataJSON();
					return false;
				}
			}
		}
		return false;
	}
	
	/* ---------------------------------------------------------------------- */
	/* -------------------------- PRIVATE FUNCTIONS ------------------------- */
	/* ---------------------------------------------------------------------- */
	
	// check if the user exits on this server
	private boolean UserExists(String nickUser)
	{
		if (nickUser == null)
			return false;
		
		for (Map.Entry<String, UserData> entry : users.entrySet()) 
		{
		    String key = entry.getKey();
		    if (nickUser.equals(key))
		    	return true;
		}
		
		return false;
	}
	
	// forwards the UDP challenge request to the friend
	private void ForwardChallengeUDP(UserData user, UserData friend) throws IOException
	{
		// create socket and datagram
		DatagramSocket socket = new DatagramSocket();
		byte[] data = new byte[50];
		DatagramPacket packet = new DatagramPacket(data, data.length, 
												friend.UDPAddress,
												friend.UDPPort);
		String request = user.nickName + " challenged you. Accept? (Y/N)";
		
		// input and output streams
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		DataOutputStream dataStream = new DataOutputStream(byteStream);
		
		// write request
		dataStream.writeUTF(request);
		data = byteStream.toByteArray();
		packet.setData(data, 0, data.length);
		packet.setLength(data.length);
		socket.send(packet);
		System.out.println("REQUEST UDP: \t" + request);
		
		// reset output streams
		byteStream.reset(); 
		dataStream.flush();
		socket.close();
	}
	
	// save all userData in JSON
	@SuppressWarnings("unchecked")
	private void SaveUserDataJSON()
	{
		// all the user list
        JSONArray jsonArray = new JSONArray();
		
        // create list of users to write
        for (Map.Entry<String, UserData> entry : users.entrySet()) 
		{
		    JSONObject obj = new JSONObject();
            obj.put("nickname", entry.getValue().nickName);
            obj.put("password", entry.getValue().password);
            obj.put("registrationDate", entry.getValue().registrationDate);

            JSONArray friendlist = new JSONArray();
            for (int i = 0; i < entry.getValue().listFriends.size(); i++)
            	friendlist.add(entry.getValue().listFriends.get(i));
            obj.put("friendlist", friendlist);

            JSONArray scorelist = new JSONArray();
            for (int i = 0; i < entry.getValue().listScores.size(); i++)
            	scorelist.add(new Integer(entry.getValue().listScores.get(i)));
            obj.put("scorelist", scorelist);
            
            // add the new object to jsonArray
            jsonArray.add(obj);
		}
        
        // write JSON
        try (FileWriter file = new FileWriter(PATH_USERDATA)) 
        {
            file.write(jsonArray.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	// read and initialize all userData from JSON
	private void ReadUserDataJSON()
	{
		JSONParser parser = new JSONParser();
		try 
		{
			Object obj = parser.parse(new FileReader(PATH_USERDATA));
			JSONArray listUsers = (JSONArray) obj;  
			for (int i = 0; i < listUsers.size(); i++)
			{
				JSONObject userJSON = (JSONObject) listUsers.get(i);
				UserData tmp = new UserData();
				tmp.nickName = (String) userJSON.get("nickname");
				tmp.password = (String) userJSON.get("password");
				tmp.registrationDate = (String) userJSON.get("registrationDate");
				
				JSONArray listFriends = (JSONArray) userJSON.get("friendlist");
				for (int j = 0; j < listFriends.size(); j++)
					tmp.listFriends.add((String) listFriends.get(j));

				JSONArray listScores= (JSONArray) userJSON.get("scorelist");
				for (int j = 0; j < listScores.size(); j++)
					tmp.listScores.add(new Integer(((Long) listScores.get(j)).intValue()));

				users.put(tmp.nickName, tmp);
			}
		}
		catch (FileNotFoundException e) 				{ e.printStackTrace(); }
		catch (IOException e) 							{ e.printStackTrace(); } 
		catch (org.json.simple.parser.ParseException e) { e.printStackTrace(); }
	} 
	
	// read all dictionary words
	private void ReadWordsJSON()
	{
		JSONParser parser = new JSONParser();
		try 
		{
			Object obj = parser.parse(new FileReader(PATH_WORDS));
			JSONObject wordsOBJ = (JSONObject) obj;
			JSONArray listWords = (JSONArray) wordsOBJ.get("words");
			
			for (int i = 0; i < listWords.size(); i++)
				words.add((String) listWords.get(i));
		}
		catch (FileNotFoundException e) 				{ e.printStackTrace(); }
		catch (IOException e) 							{ e.printStackTrace(); } 
		catch (org.json.simple.parser.ParseException e) { e.printStackTrace(); }
	}	
	
	// choose random words from dictionary, no duplicates
	private List<String> RandomWords()
	{
		List<String> wordsGame = new ArrayList<String>();
		for (int i = 0; i < ThreadChallengeRoom.N_WORDS; i++)
		{
			int randomIndex;
			do
				randomIndex = (int) (Math.random() * words.size());
			while (wordsGame.contains(words.get(randomIndex)));
			wordsGame.add(words.get(randomIndex));
		}
		return wordsGame;
	}
	
	// print all users registered
	@SuppressWarnings("unused")
	private void PrintUsers()
	{
		for (Map.Entry<String, UserData> entry : users.entrySet())
		{
			System.out.println(entry.getValue().nickName);
			System.out.println(entry.getValue().password);
			System.out.println(entry.getValue().registrationDate);
			for (String value : entry.getValue().listFriends)
				System.out.print(value + ", ");
			for (Integer value : entry.getValue().listScores)
				System.out.print(value + ", ");
			System.out.println();
			System.out.println("-----------------------------");
		}
	}

	// print all words in a list
	@SuppressWarnings("unused")
	private void PrintWords(List<String> tmpWords)
	{
		for (int i = 0; i < tmpWords.size(); i++)
			System.out.println(tmpWords.get(i));
	}
}
