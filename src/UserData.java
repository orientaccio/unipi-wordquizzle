import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * UserData is class that contains all the information about an user
 * These information will be used to be persisted. 
 * 
 * @author Chenxiang Zhang
 * @version 1.0
 */

public class UserData implements Serializable
{
	/**
	 * 	Class variables
	 * 
	 * 	@var	nickName: player nickname
	 * 	@var	password: player password
	 *  @var	registrationDate: player registration date
	 *  @var	listFriends: list of all the player friends
	 *  @var	listScores: list of all the player challenge scores
	 * 	@var	status: {0 = offline, 1 = online, 2 = in room}
	 *  @var	UDPAddress: UDP address for challenge
	 *  @var	UDPPort: UDP port for challenge
	 */
	private static final long serialVersionUID = 1L;
	public String nickName;
	public String password;
	public String registrationDate;
	public List<String> listFriends;
	public List<Integer> listScores;
	public int status;
	
	public InetAddress UDPAddress;
	public int UDPPort;
	
	/**
	 * 	Create a new user with nickname and password
	 * 
	 * 	@param 	nickName of the user
	 * 	@param 	password of the user
	 */
	public UserData(String nickName, String password)
	{
		this.nickName = nickName;
		this.password = password;
		this.listFriends = new ArrayList<String>();
		this.listScores = new ArrayList<Integer>();
		this.status = 0;
		
		// registration date
		Date date = Calendar.getInstance().getTime();  
		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");  
		registrationDate = dateFormat.format(date);
	}
	
	/**
	 * 	Create a deep copy of an existing user.	
	 * 
	 * 	@param	user is the user to clone
	 *  @effect	create a new copy of the user		
	 */
	public UserData(UserData user)
	{
		this.nickName = user.nickName;
		this.password = user.password;
		this.registrationDate = user.registrationDate;
		this.listFriends = new ArrayList<String>();
		this.listScores = new ArrayList<Integer>();
		this.status = user.status;
		
		for (int i = 0; i < user.listFriends.size(); i++)
			this.listFriends.add(user.listFriends.get(i));
		for (int i = 0; i < user.listScores.size(); i++)
			this.listScores.add(user.listScores.get(i));
	}
	
	/**
	 * 	Create a temp user with no information
	 * 
	 *  @effect	create a new user with no information		
	 */
	public UserData()
	{
		this.nickName = null;
		this.password = null;
		this.registrationDate = null;
		this.listFriends = new ArrayList<String>();
		this.listScores = new ArrayList<Integer>();
		this.status = 0;
	}
	
	/**
	 * 	Check if exitsts the friendship relation
	 * 
	 *  @return	true if is friend with nickUser
	 *  		false else
	 */
	public boolean IsFriendWith(String nickUser)
	{
		if (nickUser == null)
			return false;
		return listFriends.contains(nickUser);
	}
	
	/**
	 * 	Get the total score of the player = sum of all the score
	 * 
	 *  @return	sum of all the score
	 */
	public int GetTotalScore()
	{
		int total = 0;
		for (int i = 0; i < listScores.size(); i++)
			total += listScores.get(i);
		
		return total;
	}
}
