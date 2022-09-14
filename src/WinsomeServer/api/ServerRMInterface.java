package WinsomeServer.api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ServerRMInterface extends Remote {
	
	// per registrare un utente a WINSOME
	String registerUser(String request) throws RemoteException, NoSuchAlgorithmException;

	// per caricare i follower di un utente al momento del login
	List<String> loadFollower(String user) throws RemoteException;
}
