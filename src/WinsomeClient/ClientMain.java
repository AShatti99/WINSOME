package WinsomeClient;

import WinsomeClient.api.NotifyEventInterface;
import WinsomeClient.impl.NotifyEventImpl;
import WinsomeClient.impl.ReceiveRewardMessage;
import WinsomeServer.api.ServerCallbackInterface;
import WinsomeServer.api.ServerRMInterface;
import WinsomeServer.utils.Logger;
import WinsomeServer.utils.Support;

import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class ClientMain {
	
	// variabili file di configurazione
	private static String SERVER;
	private static int TCPPORT;
	private static int RMIPORT;
	private static int NOTIFYPORT;
	private static String MULTICAST;
	private static int MCASTPORT;
	
	// variabili per RMI
	private static ServerRMInterface rmi;
	private static ServerCallbackInterface server;
	private static NotifyEventInterface stub;
	
	// variabili per comunicare con il server
	private static Socket socket;
	private static BufferedReader in;
	private static DataOutputStream out;
	private static MulticastSocket multicastSocket;
	private static NetworkInterface netIf;
	private static InetSocketAddress group;
	private static Thread RRMthread;
	
	// variabili di supporto
	private static boolean logged = false;
	private static String lastAccountLogged = null;
	private static String msg;
	
	// strutture dati
	private static List<String> followersList = new LinkedList<>();   // followers utente
	
	
	public static void main(String[] args) {
		
		Logger.info("Client application start");
		
		// si legge il file di configurazione client
		readClientConfig();
		
		try{
			Registry r = LocateRegistry.getRegistry(RMIPORT);
			rmi = (ServerRMInterface) r.lookup("register");
		}
		catch (RemoteException | NotBoundException e){
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
		
		boolean exit = false;
		Scanner scanner = new Scanner(System.in);
		String input, cmd, cmd2;
		
		Logger.info("Welcome in WINSOME!");
		Logger.welcome();
		while (!exit){
			
			Logger.input();
			input = scanner.nextLine();                     // richiesta client
			String [] token = input.split("\\s");           // la si divide in token
			cmd = token[0];                                 // tramite il primo si estrapola il comando
			
			switch (cmd) {
				
				// register username password tag1 tag2 tag3 tag4 tag5
				case "register" -> {
					if(!Support.check(logged, token.length, 8, cmd)) break;
					registerAccount(input);
				}
				// login username password
				case "login" -> {
					if(!Support.check(logged, token.length, 3, cmd)) break;
					initializeSocket();
					msg = sendTask(input, null);
					Logger.info(msg);
					if(!msg.startsWith("< Error")) loginAccount(token[1]); //token[1] = username
					else closeSocket();
				}
				// logout
				case "logout" -> {
					if(!Support.check(logged, token.length, 1, cmd)) break;
					msg = sendTask(input, lastAccountLogged);
					Logger.info(msg);
					logout();
					Logger.info("Welcome in WINSOME!\n");
					Logger.welcome();
				}
				// follow username / unfollow username / delete idpost / rewin idpost
				case "follow", "unfollow", "delete", "rewin" -> {
					if(!Support.check(logged, token.length, 2, cmd)) break;
					msg = sendTask(input, lastAccountLogged);
					Logger.info(msg);
				}
				// list...
				case "list" -> {
					if(!Support.numberArguments(token.length, 2, cmd)) break;
					cmd2 = token[1];
					
					switch (cmd2){
						// list users / list following
						case "users", "following" -> {
							if(!Support.isLogged(logged, cmd2)) break;
							msg = sendTask(input, lastAccountLogged);
							readListUsers(msg, token[0] + token[1]);
						}
						// list followers
						case "followers" -> {
							if(!Support.isLogged(logged, cmd2)) break;
							listFollowers();
						}
						default -> Logger.info("< Error: unrecognized command, type help to see the list of available commands");
					}
				}
				// post "title" "content" / rate idPost vote / comment idPost "comment"
				case "post", "rate", "comment" -> {
					if(!Support.check(logged, token.length, 3, cmd)) break;
					msg = sendTask(input, lastAccountLogged);
					Logger.info(msg);
				}
				// blog
				case "blog" -> {
					if(!Support.check(logged, token.length, 1, cmd)) break;
					readListPost(input, lastAccountLogged, cmd);
				}
				// show...
				case "show" -> {
					if(token.length == 1){
						Logger.info("< Error: unrecognized command, type help to see the list of available commands");
						break;
					}
					cmd2 = token[1];
					
					switch (cmd2){
						// show feed
						case "feed" -> {
							if(!Support.check(logged, token.length, 2, cmd2)) break;
							readListPost(input, lastAccountLogged, cmd);
						}
						// show post idpost
						case "post" -> {
							if(!Support.check(logged, token.length, 3, cmd2)) break;
							msg = sendTask(input, lastAccountLogged);
							if(!msg.startsWith("< Error")) showPost(msg);
							else Logger.info(msg);
						}
						default -> Logger.info("< Error: unrecognized command, type help to see the list of available commands");
					}
				}
				// wallet
				case "wallet" -> {
					if(token.length == 1){
						if(!Support.isLogged(logged, cmd)) break;
						readWallet(input, lastAccountLogged);
						
					} else if (token[1].equals("btc")) {
						if(!Support.check(logged, token.length, 2, cmd)) break;
						msg = sendTask(input, lastAccountLogged);
						Logger.info("< " +msg + " bitcoin");
					}
					else {
						Logger.info("< Error: unrecognized command, type help to see the list of available commands");
					}
				}
				case "exit" -> {
					if(!Support.check(logged, token.length, 1, cmd)) break;
					exit = true;
				}
				case "help" -> {
					if(logged) Logger.help();
					else Logger.welcome();
				}
				default -> {
					Logger.info("< Error: unrecognized command, type help to see the list of available commands");
				}
			}
		}
		
		Logger.info("\nClient application closed");
	}
	
	// --------------------------------- SERVER COMUNICATION --------------------------
	public static void initializeSocket(){
		
		try{
			socket = new Socket(InetAddress.getByName(SERVER), TCPPORT);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException e){
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static String sendTask(String request, String user){  // per inviare una richiesta al server
		
		try {
			out.writeBytes(request + "\n");
			if(user != null){
				out.writeBytes(user + "\n");
			}
			out.flush();
			msg = in.readLine();
		}
		catch (IOException e){
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
		
		return msg;
	}
	
	public static void closeSocket(){
		
		try {
			socket.close();
			in.close();
			out.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	public static void readListUsers(String str, String cmd){  // leggere lista di users o following
		
		str = Support.cleanString(str);
		String [] users = str.split("\\s");
		
		switch (cmd){
			
			case "listusers" -> {
				if(str.length() != 0){
					Logger.info("< Users with a common tag: ");
					int i = 0;
					while (i < users.length){
						Logger.info("< " + users[i]);
						i++;
					}
				}
				else {
					Logger.info("< Nobody has a tag in common with you");
				}
			}
			case "listfollowing" -> {
				if(str.length() != 0){
					Logger.info("< List of following: ");
					int i = 0;
					while (i < users.length){
						Logger.info("< " + users[i]);
						i++;
					}
				}
				else {
					Logger.info("< You don't follow anybody");
				}
			}
		}
	}
	
	public static void readListPost(String request, String user, String cmd){ // lettura post nel caso di "blog" o "show feed"
		
		try{
			out.writeBytes(request + "\n");
			out.writeBytes(user + "\n");
			out.flush();
			msg = in.readLine();
			
			if(msg == null) return;
			int size = Integer.parseInt(msg);
			
			if(size == 0){
				if(cmd.equals("blog")) Logger.info("< You don't have any post in your blog");
				
				else if(cmd.equals("show")) Logger.info("< You don't have any post in your feed");
				
				return;
			}
			
			int i = 0;
			while (i < size){
				msg = in.readLine();
				List <String> list = Support.cleanPost(msg);
				String idPost = list.get(0);
				String author = list.get(1);
				String title = list.get(2);
				msg = "< Idpost: " + idPost + "\n  Author: " + author + "\n  Title: " + title;
				
				Logger.info(msg);
				i++;
			}
		}
		catch (IOException e) {
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void readWallet(String request, String user){
		
		try{
			out.writeBytes(request + "\n");
			out.writeBytes(user + "\n");
			out.flush();
			msg = in.readLine();
			
			if(msg == null) return;
			
			Logger.info("< Wallet: " + msg);
			if(Double.parseDouble(msg) > 0){
				int size = Integer.parseInt(in.readLine());
				Logger.info("< Transaction: ");
				int i=0;
				while (i < size){
					msg = in.readLine();
					Logger.info("  " +msg);
					i++;
				}
			}
			else {
				Logger.info("  No transaction");
			}
			
		} catch (IOException e) {
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	// ---------------------------- REGISTER ACCOUNT -------------------------------
	private static void registerAccount(String request){
		
		try{
			msg = rmi.registerUser(request);
			Logger.info(msg);
		}
		catch (RemoteException | NoSuchAlgorithmException e){
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	// ---------------------------- LOGIN ACCOUNT -------------------------------
	private static void loginAccount(String user){
		
		
		try{
			// carico i follower dell'utente nella struttura locale
			followersList = rmi.loadFollower(user);
			
			// registro l'utente al servizio di notifica callback
			Registry r = LocateRegistry.getRegistry(SERVER, NOTIFYPORT);
			server = (ServerCallbackInterface) r.lookup("callback");
			NotifyEventInterface callbackObj = new NotifyEventImpl(user, followersList);
			stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
			server.registerForCallback(stub);
			
			group = new InetSocketAddress(MULTICAST, MCASTPORT);
			multicastSocket = new MulticastSocket(MCASTPORT);
			netIf = NetworkInterface.getByName("wlan1");
			multicastSocket.joinGroup(group, netIf);
			RRMthread = new Thread(new ReceiveRewardMessage(multicastSocket));
			RRMthread.start();
			
			lastAccountLogged = user;
			logged = true;
			
		} catch (NotBoundException | IOException e) {
			Logger.error("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	// ---------------------------- LOGOUT -----------------------------------------
	public static void logout(){
		
		try {
			followersList.clear();
			lastAccountLogged = null;
			logged = false;
			
			server.unregisterForCallback(stub);
			
			closeSocket();
			
			multicastSocket.leaveGroup(group, netIf);
			multicastSocket.close();
		} catch (IOException e) {
			Logger.info("Server is closed");
			//e.printStackTrace();
			System.exit(1);
		}
	}
	
	// ---------------------------- LIST FOLLOWERS ---------------------------------
	public static void listFollowers(){
		
		if(followersList.size() == 0){
			Logger.info("< You don't have any follower");
		}
		else{
			Logger.info("< Your follower: ");
			for(String user : followersList){
				Logger.info("< " + user);
			}
		}
	}
	
	// ------------------------------ SHOW POST -----------------------------------
	public static void showPost(String str){
		
		str = Support.cleanString(str);
		List <String> list = Support.cleanPost(str);
		String title = list.get(0);
		String content = list.get(1);
		String upvote = list.get(2);
		String downvote = list.get(3);
		msg = "< Title: " + title + "\n  Content: " + content
			      + "\n  Upvotes: " + upvote + "\n  Downvotes: " + downvote;
		Logger.info(msg);
		int i=4, j = 1;
		if(i == list.size()) Logger.info("  No Comments");
		else {
			while (i < list.size()){
				Logger.info("  Comment " +j+ ": "+list.get(i));
				i++;
				j++;
			}
		}
	}
	
	
	
	// ---------------------------- CONFIGURATION FILE ------------------------------
	private static void readClientConfig(){
		
		String config = "clientConfig.txt";
		try(BufferedReader in = new BufferedReader(new FileReader(config))){
			
			String line, key, value;
			
			while ((line = in.readLine()) != null){
				try{
					StringTokenizer tkLine = new StringTokenizer(line);
					key = tkLine.nextToken();
					// nel caso in cui non sia un commento (#-token)
					if(!key.equals("#")){
						// value prende il secondo token, che non puo' essere un "=-token"
						// replaceAll serve per accettare i valori con spazi o senza dopo l'= (= value o =value)
						value = tkLine.nextToken("= ").replaceAll("\\s", "");
						switch (key) {
							case "SERVER" -> SERVER = value;
							case "TCPPORT" -> TCPPORT = Integer.parseInt(value);
							case "RMIPORT" -> RMIPORT = Integer.parseInt(value);
							case "NOTIFYPORT" -> NOTIFYPORT = Integer.parseInt(value);
							case "MULTICAST" -> MULTICAST = value;
							case "MCASTPORT" -> MCASTPORT = Integer.parseInt(value);
							default -> throw new RuntimeException("key not recognized: " +key);
						}
					}
				}
				catch (NoSuchElementException e){
					continue;
				}
			}
		}
		catch (IOException e){
			throw new RuntimeException("Error reading from client configuration file");
		}
	}
}
