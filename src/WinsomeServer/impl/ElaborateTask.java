package WinsomeServer.impl;

import WinsomeServer.model.Post;
import WinsomeServer.model.User;
import WinsomeServer.utils.JsonData;
import WinsomeServer.utils.Logger;
import WinsomeServer.utils.Support;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class ElaborateTask implements Runnable {
	
	// per comunicare con il client
	private Socket socket;
	private BufferedReader in;
	private DataOutputStream out;
	
	// variabili di supporto
	private ServerCallbackImpl serverCallback;
	private String currentUser;
	private AtomicBoolean serverActive;
	
	// strutture dati
	private Map<String, User> usersMap;  // utenti di WINSOME: <username, User>
	private Map<String, Post> postsMap;   // post di WINSOME: <idPost, Post>
	public List<String> loggedUsers;
	
	public ElaborateTask(Socket socket, BufferedReader in, DataOutputStream out, ServerCallbackImpl serverCallback, Map<String, User> usersMap, Map<String, Post> postsMap, List<String> loggedUsers, AtomicBoolean serverActive) {
		
		this.socket = socket;
		this.in = in;
		this.serverCallback = serverCallback;
		this.out = out;
		this.usersMap = usersMap;
		this.postsMap = postsMap;
		this.loggedUsers = loggedUsers;
		this.serverActive = serverActive;
	}
	
	@Override
	public void run() {
		
		boolean endRequest = false;
		String input, msg, cmd, cmd2, idPost;
		List <Post> postList;
		int size;
		double cash;
		
		try{
			
			while (!endRequest){
				
				Logger.info("waiting command from client...");
				input = in.readLine();
				
				if(input == null) break;         // la socket nel client e' stata chiusa, allora la chiudo anche qui
				if(!serverActive.get()) break;   // il server e' stato chiuso
				
				String [] token = input.split("\\s");;
				cmd = token[0];
				Logger.info("command received: " +cmd);
				
				switch (cmd){
					
					case "login" -> {
						msg = loginAccount(token);
						sendReply(msg);
					}
					case "logout" -> {
						currentUser = in.readLine();
						sendReply("< Bye " +currentUser + "!");
						endRequest = true;
					}
					case "follow" -> {
						currentUser = in.readLine();
						msg = followAccount(currentUser, token);
						sendReply(msg);
					}
					case "unfollow" -> {
						currentUser = in.readLine();
						msg = unfollowAccount(currentUser, token);
						sendReply(msg);
					}
					case "list" -> {
						cmd2 = token[1];
						
						switch (cmd2){
							case "users" -> {
								currentUser = in.readLine();
								msg = listUsers(currentUser);
								sendReply(msg);
							}
							case "following" -> {
								currentUser = in.readLine();
								msg = listFollowing(currentUser);
								sendReply(msg);
							}
						}
					}
					case "post" -> {
						currentUser = in.readLine();
						msg = createPost(currentUser, input);
						sendReply(msg);
					}
					case "blog" -> {
						currentUser = in.readLine();
						postList = blogPost(currentUser);
						size = postList.size();
						out.writeBytes(size + "\n");
						out.flush();
						if(size > 0){
							for(Post post : postList){
								sendReply(post.showPost());
							}
						}
					}
					case "show" -> {
						cmd2 = token[1];
						switch (cmd2){
							case "feed" -> {
								currentUser = in.readLine();
								postList = showFeed(currentUser);
								size = postList.size();
								out.writeBytes(size + "\n");
								out.flush();
								if(size > 0){
									for(Post post : postList){
										sendReply(post.showPost());
									}
								}
							}
							case "post" -> {
								idPost = token[2];
								msg = showPost(idPost);
								sendReply(msg);
							}
						}
					}
					case "delete" -> {
						currentUser = in.readLine();
						idPost = token[1];
						msg = deletePost(currentUser, idPost);
						sendReply(msg);
					}
					case "rewin" -> {
						currentUser = in.readLine();
						idPost = token[1];
						msg = rewinPost(currentUser, idPost);
						sendReply(msg);
					}
					case "rate" -> {
						currentUser = in.readLine();
						idPost = token[1];
						String vote = token[2];
						msg = ratePost(currentUser, idPost, vote);
						sendReply(msg);
					}
					case "comment" -> {
						currentUser = in.readLine();
						msg = commentPost(currentUser, input);
						sendReply(msg);
					}
					case "wallet" -> {
						if(token.length == 1){
							currentUser = in.readLine();
							cash = getWallet(currentUser);
							out.writeBytes(cash + "\n");
							out.flush();
							if(cash > 0){
								List <String> list = getTransaction(currentUser);
								size = list.size();
								out.writeBytes(size + "\n");
								out.flush();
								for(String str : list){
									sendReply(str);
								}
							}
						}
						else {
							currentUser = in.readLine();
							double rate = getWalletBTC();
							cash = rate * getWallet(currentUser);
							out.writeBytes(cash + "\n");
							out.flush();
						}
					}
				}
			}
			
			synchronized (loggedUsers){
				loggedUsers.remove(currentUser);
			}
			socket.close();
			Logger.info("connection closed to client: " +socket.getRemoteSocketAddress());
			in.close();
			out.close();
			
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	// inviare risposta al client
	public void sendReply(String msg){
		
		try {
			out.writeBytes(msg + "\n");
			out.flush();
			
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	
	// -------------------------------- LOGIN --------------------------------------
	public String loginAccount(String [] token){
		
		String username = token[1];
		String password = token[2];
		
		// controllo se l'username esiste
		if(!usersMap.containsKey(username)){
			return  "< Error: this username doesn't belong to any account";
		}
		
		// faccio l'hash della password fornita per confrontarla
		password = Support.hashPassword(password);
		
		// se non corrispondono lancio un messaggio di errore
		if(!usersMap.get(username).getPassword().equals(password)){
			return "< Error: wrong password";
		}
		
		synchronized (loggedUsers){
			if(loggedUsers.contains(username)){
				return "< Error: you're already logged in to another device";
			}
			else {
				loggedUsers.add(username);
			}
		}
		currentUser = username;
		return "< Successfully logged in";
	}
	
	// -------------------------------- FOLLOW ----------------------------------
	private String followAccount(String currentUser, String[] token) {
		
		String creator = token[1];
		
		// controllo che "currentUser" non sia la stessa persona che vuole seguire
		if(currentUser.equals(creator)){
			return "< Error: you can't follow yourself";
		}
		
		// controllo se l'utente che "currentUser" vuole seguire esiste
		if(!usersMap.containsKey(creator)){
			return "< Error: this username doesn't exist";
		}
		
		synchronized (usersMap){
			// se questo metodo restituisce false, significa che creator
			// era gia' nella lista di following di account
			// in caso non ci sia, lo aggiunge
			if(!usersMap.get(currentUser).newFollowing(creator)){
				return "< Error: you already followed this account";
			}
			
			// aggiorno la lista di followers di creator
			usersMap.get(creator).newFollower(currentUser);
		}
		
		try{
			// avviso creator che "currentUser" ha incominciato a seguirlo
			serverCallback.updateNewFollower(creator, currentUser);
		}
		catch (RemoteException e){
			e.printStackTrace();
		}
		
		JsonData.writeData(usersMap, null);
		return "< Great! Now you follow " + creator + "'s account";
	}
	
	// -------------------------------- UNFOLLOW ----------------------------------
	public String unfollowAccount (String currentUser, String [] token){
		
		String creator = token[1];
		
		// controllo che "currentUser" non sia la stessa persona che vuole smettere di seguire
		if(currentUser.equals(creator)){
			return "< Error: you can't unfollow yourself";
		}
		
		// controllo se l'utente che "currentUser" vuole smettere di seguire esiste
		if(!usersMap.containsKey(creator)){
			return "< Error: this username doesn't exist";
		}
		
		synchronized (usersMap){
			// se questo metodo restituisce false, significa che creator
			// non era presente nella lista di following di account.
			// In caso ci sia, lo rimuove
			if(!usersMap.get(currentUser).removeFollowing(creator)){
				return "< Error: you don't follow " +creator+"'s account";
			}
			
			// aggiorno la lista di followers di creator
			usersMap.get(creator).newUnfollow(currentUser);
		}
		
		try{
			// avviso creator che "currentUser" ha smesso di seguirlo
			serverCallback.updateUnfollow(creator, currentUser);
		}
		catch (RemoteException e){
			e.printStackTrace();
		}
		
		JsonData.writeData(usersMap, null);
		return "< Ok! Now you don't follow " + creator + "'s account anymore";
	}
	
	// -------------------------------- LIST USERS ----------------------------------
	public String listUsers (String currentUser){
		
		User user = usersMap.get(currentUser);
		List <String> tags = Arrays.asList(user.getTags());
		List <String> usersWithCommonTags = new LinkedList<>();
		
		synchronized (usersMap){
			// per ogni utente w di userMap
			for(User w : usersMap.values()){
				// se w e' diverso dall'utente loggato
				if(!w.equals(user)){
					// allora controllo i tag di w
					for(String t : w.getTags()){
						// se nella lista dell'utente loggato e' presente un tag in comune con w
						if(tags.contains(t)){
							// allora aggiungi w alla lista da restituire
							usersWithCommonTags.add(w.getUsername());
							// e controlli il prossimo w
							break;
						}
					}
				}
			}
		}
		
		return usersWithCommonTags.toString();
	}
	
	// ----------------------------------- LIST FOLLOWING ----------------------------
	public String listFollowing (String currentUser){
		
		synchronized (usersMap){
			return usersMap.get(currentUser).getFollowingList().toString();
		}
	}
	
	// ---------------------------------- CREATE POST -------------------------
	public String createPost(String currentUser, String input){
		
		// controllo che la richiesta abbia 4 virgolette: 2 per il titolo, 2 per il contenuto
		if(Support.countQuote(input) != 4) return "< Error: please the format has to be <\"title\"> <\"post\">";
		
		// divido titolo e contenuto
		List <String> list = Support.cleanPost(input);
		String title = list.get(1);
		String content = list.get(2);
		
		if(title.length() > 20) return "< Error: title has a maximum lenght of 20 characters";
		if(content.length() > 500) return "< Error: content has a maximum length of 500 characters";
		
		// mi trovo l'id che deve essere il piu' grande id tra i post +1
		String id = String.valueOf(Support.maxId(postsMap));
		Post post = new Post(id, currentUser, title, content);
		postsMap.put(id, post);
		
		JsonData.writeData(null, postsMap);
		return "< New post published!";
	}
	
	// ----------------------------------- BLOG POST ------------------------------
	public List <Post> blogPost(String user){
		
		// inserisco nella lista da restituire tutti i post che hanno come autore "user"
		List <Post> list = new LinkedList<>();
		for(Map.Entry <String, Post> entry : postsMap.entrySet()){
			if(entry.getValue().getAuthor().equals(user)){
				list.add(entry.getValue());
			}
		}
		
		return list;
	}
	
	// ----------------------------------- SHOW FEED ------------------------------
	public List <Post> showFeed(String user){
		
		// inserisco nella lista da restituire tutti i post creati dai following di "user"
		User u = usersMap.get(user);
		List <Post> list = new LinkedList<>();
		
		synchronized (postsMap){
			for(String w : u.getFollowingList()){
				for(Map.Entry <String, Post> entry : postsMap.entrySet()){
					if(entry.getValue().getAuthor().equals(w)){
						list.add(entry.getValue());
					}
				}
			}
		}
		
		return list;
	}
	
	// ----------------------------- SHOW POST ---------------------------------
	public String showPost(String idPost){
		
		Post post;
		
		synchronized (postsMap){
			
			// se get restituisce false, significa che il post non e' presente nella postMap
			post = postsMap.get(idPost);
			
			if(post == null){
				return "< Error: post not existing or deleted";
			}
		}
		
		return post.showInteraction();
	}
	
	// ------------------------------ DELETE POST -------------------------------
	public String deletePost(String user, String id){
		
		synchronized (postsMap){
			
			Post post = postsMap.get(id);
			
			// se get restituisce false, significa che il post non e' presente nella postMap
			if(post == null){
				return "< Error: post not existing or deleted";
			}
			
			// controllo che l'autore del post e "user" siano lo stesso utente
			if(!post.getAuthor().equals(user)){
				return "< Error: you are not the owner of this post";
			}
			
			postsMap.remove(id);
		}
		
		JsonData.writeData(null, postsMap);
		return "< post deleted correctly";
	}
	
	// ---------------------------- REWIN POST ---------------------------------
	public String rewinPost(String user, String idPost){
		
		synchronized (postsMap){
			
			Post post = postsMap.get(idPost);
			
			// se get restituisce false, significa che il post non e' presente nella postMap
			if(post == null){
				return "< Error: post not existing or deleted";
			}
			
			// controllo che l'autore del post e "user" NON siano lo stesso utente
			if(post.getAuthor().equals(user)){
				return "< Error: you can't rewin your post";
			}
			
			// mi trovo l'id che deve essere il piu' grande id tra i post +1
			String id = String.valueOf(Support.maxId(postsMap));
			// aggiungo al titolo la digitura "REWINED FROM"
			Post rewin = new Post(id, user, Support.addString(post.getTitle(), post.getAuthor(), " REWINED FROM "), post.getContent());
			postsMap.put(id, rewin);
		}
		
		JsonData.writeData(null, postsMap);
		return "< post rewined correctly";
	}
	
	// ---------------------------------- RATE POST -----------------------------
	public String ratePost(String user, String idPost, String vote){
		
		if(!vote.equals("+1") && !vote.equals("-1")) return "< Error: vote can only be +1 or -1";
		
		synchronized (postsMap){
		
			Post post = postsMap.get(idPost);
			
			if(post == null){
				return "< Error: post not existing or deleted";
			}
			if(user.equals(post.getAuthor())){
				return "< Error: you can't rate your post";
			}
			if(!usersMap.get(user).getFollowingList().contains(post.getAuthor())){
				return "< Error: this post is not in your feed";
			}
			if(post.searchWhoRated(user)){
				return "< Error: you have already voted this post";
			}
			
			post.rate(user, Integer.parseInt(vote));
		}
		
		JsonData.writeData(null, postsMap);
		return "< post was voted correctly";
	}
	
	// -------------------------------- COMMENT POST ---------------------------
	public String commentPost(String user, String input){
		
		if(Support.countQuote(input) != 2) return "< Error: please the format has to be <\"comment\">";
		List <String> list = Support.cleanPost(input);
		String idPost = list.get(1);
		
		synchronized (postsMap){
			
			Post post = postsMap.get(idPost);
			
			if(post == null){
				return "< Error: post not existing or deleted";
			}
			
			String comment = list.get(2);
			// non era richiesto, pero' mi e' sembrato logico dare un limite
			if(comment.length() > 140){
				return "< Error: comment has a maximum lenght of 140 characters";
			}
			if(user.equals(post.getAuthor())){
				return "< Error: you can't comment your post";
			}
			if(!usersMap.get(user).getFollowingList().contains(post.getAuthor())){
				return "< Error: this post is not in your feed";
			}
			
			// aggiungo al comment la digitura BY
			post.addComment(Support.addString(comment, user, " BY "), user);
		}
		
		JsonData.writeData(null, postsMap);
		return "< post commented correctly";
	}
	
	// ------------------------------ WALLET ----------------------------------
	public Double getWallet(String user) {
		
		synchronized (usersMap) {
			return usersMap.get(user).getWallet();
		}
	}
	public List <String> getTransaction(String user){
		
		synchronized (usersMap){
			return usersMap.get(user).getTransaction();
		}
	}
	
	// ------------------------------ WALLET BTC ------------------------------
	public double getWalletBTC(){
		
		String rate = null;
		try{
			URL url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=8&col=1&format=plain&rnd=new");
			URLConnection uc = url.openConnection();
			InputStream raw = uc.getInputStream();
			rate = new String(raw.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		return Double.parseDouble(rate);
	}
}
