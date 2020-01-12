public class WQProtocol
{
	public static final int MAX_MESSAGE_LENGTH 			= 1024;
	public static final int CODE_SUCCESS 				= 200;
	public static final int CODE_FAIL	 				= 400;
	public static final String COMMAND_REGISTER 		= "register_user";
	public static final String COMMAND_LOGIN 			= "login";
	public static final String COMMAND_ADDFRIEND 		= "add_friend";
	public static final String COMMAND_FRIENDLIST 		= "friendlist";
	public static final String COMMAND_CHALLENGE 		= "challenge";
	public static final String COMMAND_SHOWSCORES 		= "score";
	public static final String COMMAND_SHOWLEADERBOARD 	= "leaderboard";
	public static final String COMMAND_LOGOUT 			= "logout";
	public static final String RESPONSE_CHALLENGE		= "challenged";
}
