import java.util.List;
import java.io.Serializable;
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
	
	public UserData(String nickName, String password)
	{
		this.nickName = nickName;
		this.password = password;
		this.status = 0;
		this.listFriends = new ArrayList<String>();
		this.listScores = new ArrayList<Integer>();
		
		// registration date
		Date date = Calendar.getInstance().getTime();  
		DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");  
		registrationDate = dateFormat.format(date);
	}
	
	public UserData()
	{
		this.nickName = null;
		this.password = null;
		this.status = 0;
		this.listFriends = new ArrayList<String>();
		this.listScores = new ArrayList<Integer>();
	}
	
	public boolean IsFriendWith(String nickUser)
	{
		if (nickUser == null)
			return false;
		return listFriends.contains(nickUser);
	}
}
