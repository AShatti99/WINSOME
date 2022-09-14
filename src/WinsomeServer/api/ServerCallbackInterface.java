package WinsomeServer.api;

import WinsomeClient.api.NotifyEventInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerCallbackInterface extends Remote {
	
	void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException;
	void unregisterForCallback(NotifyEventInterface clientInterface) throws RemoteException;
}
