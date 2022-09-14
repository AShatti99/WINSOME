package WinsomeServer.impl;

import WinsomeClient.api.NotifyEventInterface;
import WinsomeServer.api.ServerCallbackInterface;
import WinsomeServer.utils.Logger;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.LinkedList;
import java.util.List;

public class ServerCallbackImpl extends RemoteServer implements ServerCallbackInterface {
	
	List<NotifyEventInterface> clientsList;
	
	public ServerCallbackImpl(){
		super();
		clientsList = new LinkedList<>();
	}
	
	@Override
	public synchronized void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException {
		if(!clientsList.contains(clientInterface)){
			clientsList.add(clientInterface);
			Logger.info("client registered to callback service");
		}
	}
	
	@Override
	public synchronized void unregisterForCallback(NotifyEventInterface clientInterface) throws RemoteException {
		if(clientsList.remove(clientInterface)){
			Logger.info("client unregistered to callback service");
		}
		else {
			Logger.error("unable to unregistered client to callback service");
		}
		
	}
	
	public void updateNewFollower(String creator, String account) throws RemoteException{
		doCallback1(creator, account);
	}
	
	private synchronized void doCallback1(String creator, String account) throws RemoteException{
		
		for (NotifyEventInterface client : clientsList) {
			client.notifyNewFollower(creator, account);
		}
	}
	
	public void updateUnfollow(String creator, String account) throws RemoteException{
		doCallback2(creator, account);
	}
	
	private synchronized void doCallback2(String creator, String account) throws RemoteException{
		
		for (NotifyEventInterface client : clientsList) {
			client.notifyUnfollow(creator, account);
		}
	}
}
