package WinsomeServer.impl;

import WinsomeServer.api.ServerRMInterface;
import WinsomeServer.model.User;
import WinsomeServer.utils.JsonData;
import WinsomeServer.utils.Logger;
import WinsomeServer.utils.Support;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServerRMImpl implements ServerRMInterface {
	
	private Map<String, User> usersMap;
	
	public ServerRMImpl(Map<String, User> usersMap){
		
		this.usersMap = usersMap;
	}
	
	@Override
	public String registerUser(String request) throws RemoteException {
		
		String [] token = request.split("\\s");
		String username = token[1];
		String password = token[2];
		
		if(username.length() <= 2) return  "< Error: username too short";
		if(username.length() > 10) return  "< Error: username too long";
		
		if(password.length() < 4) return  "< Error: password too short";
		if(password.length() > 20) return  "< Error: password too long";
		
		password = Support.hashPassword(password);
		// copio i tags e li setto in minuscolo
		String [] tags = new String[5];
		System.arraycopy(token, 3, tags, 0, 5);
		tags = Arrays.stream(tags).map(String::toLowerCase).toArray(String[]::new);
		
		synchronized (usersMap){
			if(usersMap.containsKey(username)) return  "< Error: the username \"" +username+ "\" is already used, please choose another username";
			
			User user = new User(username, password, tags);
			//System.out.println(user);
			usersMap.put(username, user);
		}
		
		Logger.info("New account registered: " +username);
		JsonData.writeData(usersMap, null);
		return "< New account was successfully registered";
	}
	
	@Override
	public List<String> loadFollower(String user) throws RemoteException {
		
		synchronized (usersMap){
			return usersMap.get(user).getFollowersList();
		}
	}
}
