import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;

public class UserData implements Serializable
{
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
	
	public boolean IsFriendWith(String nickUser)
	{
		if (nickUser == null)
			return false;
		return listFriends.contains(nickUser);
	}
	
	public int GetTotalScore()
	{
		int total = 0;
		for (int i = 0; i < listScores.size(); i++)
			total += listScores.get(i);
		
		return total;
	}
}
