import java.rmi.*;

public interface IWQServerRMI extends Remote 
{
	String SERVICE_NAME = "WQServerRegisterService";	
	
	/**
	 * Register a new user to the server using RMI
	 * 
	 * @param	nickUser is the nickname
	 * @param	password is the password of nickname	
	 * @return	1 if success
	 * 			0 error: nickname not available,
	 * 					password null
	 */
	public int RegisterUser(String nickUser, String password) throws RemoteException;
}
