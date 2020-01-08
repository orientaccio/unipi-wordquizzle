public interface IWQServer
{
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
	public int Login(String nickUser, String password);
	
	/**
	 * Logout of an online user closing TCP
	 * 
	 * @param	nickUser is the nickname	
	 * @effect	logout of the user
	 */
	public void Logout(String nickUser);
	
	/**
	 * The current user adds a friend to his friend-list
	 * 
	 * @param	nickUser is the nickname
	 * @param	nickFriend is the nickname of friend
	 * @return	1 if success
	 * 			0 error: nickFriend not found,
	 * 					friendship already enstablished
	 */
	public int AddFriend(String nickUser, String nickFriend);
	
	/**
	 * Show all the friends of the current user
	 * 
	 * @param	nickUser is the nickname
	 * @effect	returns a JSON object containing all the friends	
	 */
	public void ShowFriendList(String nickUser);
	
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
	public int Challenge(String nickUser, String nickFriend);
	
	/**
	 * Show the score of every game of the player
	 * 
	 * @param	nickUser is the nickname
	 * @effect  show the scores
	 */
	public void ShowScore(String nickUser);
	
	/**
	 * Show the score leaderboard of the user and his friends in JSON
	 * 
	 * @param	nickUser is the nickname
	 * @effect	show leaderboad
	 */
	public int ShowLeaderboard(String nickUser);
}
