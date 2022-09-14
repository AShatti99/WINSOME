package WinsomeClient.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {
	
	void notifyNewFollower(String creator, String account) throws RemoteException;
	void notifyUnfollow(String creator, String account) throws RemoteException;
}
