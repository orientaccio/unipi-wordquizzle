/**
 * WQServer is the server which manages all the game
 * 
 * The server persists all the information about
 * <registrations, friendship relations, user scores>
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WQServer extends UnicastRemoteObject implements IWQServer, IWQServerRMI
{
	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_PORT = 1919;
	public static final int MAX_MESSAGE_LENGTH = 100;
	private static final String PATH_USERDATA = "UserData.json";

	/**
	 * 	Class variables 
	 * 
	 * 	@var	words: dictionary containing all the words from the file "dictionary.dat"
	 * 	@var	users: dictionary containing all the registered user on the server
	 */
	private List<String> words;
	private HashMap<String, UserData> users;
	
 	public WQServer() throws RemoteException
	{
		super();
		words = new ArrayList<String>();
		users = new HashMap<String, UserData>();
		
		// load information about users from JSON
		ReadUserDataJSON();
		//PrintUsers();
	}
	
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
		SaveUserDataJSON(tmp);
		
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
	public void Logout(String nickUser)
	{
		if (nickUser == null)
			return;
		
		UserData tmp = users.get(nickUser);
		tmp.status = 0;
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
		SaveUserDataJSON(tmpUser);
		
		return WQProtocol.CODE_SUCCESS;
	}
	
	/**
	 * Show all the friends of the current user
	 * 
	 * @param	nickUser is the nickname
	 * @effect	returns a JSON object containing all the friends	
	 */
	public void ShowFriendList(String nickUser)
	{
		
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
	 */
	public int Challenge(String nickUser, String nickFriend)
	{
		return 0;
	}
	
	/**
	 * Show the score of every game of the player
	 * 
	 * @param	nickUser is the nickname
	 * @effect  show the scores
	 */
	public void ShowScore(String nickUser)
	{
	}
	
	/**
	 * Show the score leaderboard of the user and his friends in JSON
	 * 
	 * @param	nickUser is the nickname
	 * @effect	show leaderboad
	 */
	public int ShowLeaderboard(String nickUser)
	{
		return 0;
	}
	
	/* -----------------------------------------------------------*/
	/* -------------------- PRIVATE FUNCTIONS --------------------*/
	/* -----------------------------------------------------------*/
	
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
	
	@SuppressWarnings("unchecked")
	private void SaveUserDataJSON(UserData user)
	{
//		JSONParser parser = new JSONParser();
//		try 
//		{
//			Object obj = parser.parse(new FileReader(PATH_USERDATA));
//			JSONArray listUsers = (JSONArray) obj;  
//			for (int i = 0; i < listUsers.size(); i++)
//			{
//				JSONObject userJSON = (JSONObject) listUsers.get(i);
//				String tmp = (String) userJSON.get("nickname");
//				if (user.nickName.equals(tmp))
//				{
//					// update friendlist - scorelist
//		            JSONArray friendlist = new JSONArray();
//		            for (int j = 0; j < user.listFriends.size(); j++)
//		            	friendlist.add(user.listFriends.get(j));
//		            userJSON.put("friendlist", friendlist);
//
//		            JSONArray scorelist = new JSONArray();
//		            for (int j = 0; j < user.listScores.size(); j++)
//		            	scorelist.add(user.listScores.get(j));
//		            userJSON.put("scorelist", scorelist);
//		            
//					return;
//				}
//			}
//			
//			// not found, append the new user
//			JSONObject newUser = new JSONObject();
//			newUser.put("nickname", user.nickName);
//			newUser.put("nickname", user.nickName);
//			newUser.put("nickname", user.nickName);
//			
//			listUsers.add(obj);
//		}
//		catch (FileNotFoundException e) 				{ e.printStackTrace(); }
//		catch (IOException e) 							{ e.printStackTrace(); } 
//		catch (org.json.simple.parser.ParseException e) { e.printStackTrace(); }
//		
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
            	scorelist.add(entry.getValue().listScores.get(i));
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
	
	private void ReadUserDataJSON()
	{
		JSONParser parser = new JSONParser();
		try 
		{
			Object obj = parser.parse(new FileReader(PATH_USERDATA));
			JSONArray listUsers = (JSONArray) obj;  
			for (int i = 0; i < listUsers.size(); i++)
			{
				JSONObject user = (JSONObject) listUsers.get(i);
				UserData tmp = new UserData();
				tmp.nickName = (String) user.get("nickname");
				tmp.password = (String) user.get("password");
				tmp.registrationDate = (String) user.get("registrationDate");
				
				JSONArray listFriends = (JSONArray) user.get("friendlist");
				for (int j = 0; j < listFriends.size(); j++)
					tmp.listFriends.add((String) listFriends.get(j));

				JSONArray listScores= (JSONArray) user.get("scorelist");
				for (int j = 0; j < listScores.size(); j++)
					tmp.listScores.add((Integer) listScores.get(j));
				
				users.put(tmp.nickName, tmp);
			}
		}
		catch (FileNotFoundException e) 				{ e.printStackTrace(); }
		catch (IOException e) 							{ e.printStackTrace(); } 
		catch (org.json.simple.parser.ParseException e) { e.printStackTrace(); }
	} 
	
	private void PrintUsers()
	{
		for (Map.Entry<String, UserData> entry : users.entrySet())
		{
			System.out.println(entry.getValue().nickName);
			System.out.println(entry.getValue().password);
			System.out.println(entry.getValue().registrationDate);
			for (String value : entry.getValue().listFriends)
				System.out.print(value + " - ");
			for (Integer value : entry.getValue().listScores)
				System.out.print(value + " - ");
			System.out.println();
		}
	}
}