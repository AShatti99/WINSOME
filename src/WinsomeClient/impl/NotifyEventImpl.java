package WinsomeClient.impl;

import WinsomeClient.api.NotifyEventInterface;
import WinsomeServer.utils.Logger;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
	
	private String user;
	private List<String> followersList;
	
	public NotifyEventImpl(String user, List <String> followersList) throws RemoteException {
		super();
		this.user = user;
		this.followersList = followersList;
	}
	
	@Override
	public void notifyNewFollower(String creator, String account) throws RemoteException {
		if(user.equals(creator)){
			followersList.add(account);
			Logger.info("\n< " + account + " followed you!");
			Logger.input();
		}
	}
	
	@Override
	public void notifyUnfollow(String creator, String account) throws RemoteException {
		if(user.equals(creator)){
			followersList.remove(account);
			Logger.info("\n< " + account + " unfollowed you");
			Logger.input();
		}
	}
}
