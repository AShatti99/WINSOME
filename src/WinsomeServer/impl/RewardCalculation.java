package WinsomeServer.impl;

import WinsomeServer.model.Post;
import WinsomeServer.model.User;
import WinsomeServer.utils.JsonData;
import WinsomeServer.utils.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RewardCalculation implements Runnable{
	
	private String MULTICAST;
	private int MCASTPORT;
	private int REWARDTIME;
	private double AUTHOREWARD;
	private double CURATOREWARD;
	private Map<String, User> usersMap;
	private Map<String, Post> postsMap;
	private AtomicBoolean serverActive;
	
	public RewardCalculation(String MULTICAST, int MCASTPORT, int REWARDTIME, double AUTHOREWARD, Map<String, User> usersMap, Map<String, Post> postsMap, AtomicBoolean serverActive){
		
		this.MULTICAST = MULTICAST;
		this.MCASTPORT = MCASTPORT;
		this.REWARDTIME = REWARDTIME;
		this.AUTHOREWARD = AUTHOREWARD;
		CURATOREWARD = 100 - AUTHOREWARD;
		this.usersMap = usersMap;
		this.postsMap = postsMap;
		this.serverActive = serverActive;
	}
	
	
	@Override
	public void run() {
		
		try(DatagramSocket datagramSocket = new DatagramSocket()){
			InetAddress group = InetAddress.getByName(MULTICAST);
			
			while (serverActive.get()){
				Thread.sleep(REWARDTIME);
				synchronized (postsMap){
					for(Map.Entry<String, Post> entry : postsMap.entrySet()){
						
						Post post = entry.getValue();
						
						// formula: max (sum_(p=0)^(newPeopleLikes) (Lp), 0)
						List<String> listUpvote = post.getNewPeopleUpvote();
						int newPeopleUpvote = post.getNewPeopleUpvote().size();
						int newPeopleDownvote = post.getNewPeopleDownvote().size();
						int newPeopleRating = Math.max(newPeopleUpvote - newPeopleDownvote, 0);
						
						// formula: (sum_(p=0)^(newPeopleCommenting) (2/(1+e^(Cp-1))))
						List<String> listComment = post.getNewPeopleComment();
						float newPeopleComment = 0;
						for(String user : listComment){
							int n = Collections.frequency(listComment, user);
							newPeopleComment += (2 /(1 + Math.exp(-n + 1)));
						}
						
						// formula completa
						double tot = ((Math.log(newPeopleRating+1) + Math.log(newPeopleComment+1))/(post.getIteration()+1));
						// divido il numero trovato nella percentuale d'autore e di curatore
						double authorReward = tot * (AUTHOREWARD/100);
						double curatorReward = tot * (CURATOREWARD/100);
						
						// se e' maggiore di 0, dopo aver trovato l'utente, gli aumento il wallet
						if(authorReward > 0){
							User user = usersMap.get(post.getAuthor());
							user.addCash(authorReward);
							String timestamp = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").format(new Date());
							user.addTransaction(timestamp + " | author reward  | " + authorReward);
						}
						
						// mi trovo tutti i curatori e gli aumento il wallet nel caso in cui l'aumento sia superiore a 0
						List <String> curators = new LinkedList<>();
						curators.addAll(listComment);
						curators.addAll(listUpvote);
						for(String curator : curators){
							double reward = (curatorReward/curators.size());
							if(reward > 0){
								User user = usersMap.get(curator);
								user.addCash(reward);
								String timestamp = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").format(new Date());
								user.addTransaction(timestamp + " | curator reward | " + reward);
							}
						}
						// resetto le variabili per il calcolo della formula e aumento di 1 il numero di iterazioni
						post.resetNewInteraction();
					}
					Logger.info("rewards calculated");
					JsonData.writeData(usersMap, postsMap);
					String msg = "Update rewards";
					byte [] b = msg.getBytes();
					DatagramPacket dp = new DatagramPacket(b, b.length, group, MCASTPORT);
					datagramSocket.send(dp);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
}
