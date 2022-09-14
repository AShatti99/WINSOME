package WinsomeServer.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class User {
	
	private String username, password;
	private String [] tags;
	private List<String> followersList, followingList;
	private double wallet;
	private List <String> transaction;
	
	public User(String username, String password, String [] tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
		followersList = new LinkedList<>();
		followingList = new LinkedList<>();
		wallet = 0;
		transaction = new LinkedList<>();
	}
	
	// metodi get
	public String getUsername() {
		return username;
	}
	public String getPassword() {
		return password;
	}
	public String[] getTags() {
		return tags;
	}
	public List<String> getFollowersList() {
		return followersList;
	}
	public List<String> getFollowingList() {
		return followingList;
	}
	public double getWallet() { return wallet;}
	public List <String> getTransaction() { return transaction; }
	
	// metodi per la lista di following
	public boolean newFollowing(String user){
		// se trovo l'utente che voglio seguire nella mia lista di following
		// allora non lo devo aggiungere di nuovo
		if(followingList.contains(user)){
			return false;
		}
		
		followingList.add(user);
		return true;
	}
	public boolean removeFollowing(String user){
		// se non trovo l'utente che voglio smettere di seguire nella mia lista di following
		// allora non lo posso rimuovere
		if(!followingList.contains(user)){
			return false;
		}
		
		followingList.remove(user);
		return true;
	}
	
	// metodi per la lista di followers
	public void newFollower(String user){
		followersList.add(user);
	}
	public void newUnfollow(String user){
		followersList.remove(user);
	}
	
	// metodi per il wallet
	public void addCash(double value){
		wallet += value;
	}
	public void addTransaction(String str){
		transaction.add(str);
	}
	
	@Override
	public String toString() {
		return "\nUsername: " +getUsername()
			       + "\nPassword: " +getPassword()
			       + "\nTags: " + Arrays.toString(getTags())
			       + "\nFollower list: " +getFollowersList()
			       + "\nFollowing list: " +getFollowingList()
			       + "\nWallet: " +getWallet()
			       + "\nTransaction: " +getTransaction()
			       + "\n";
		
	}
}
