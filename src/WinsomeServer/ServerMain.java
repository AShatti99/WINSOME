package WinsomeServer;

import WinsomeServer.api.ServerCallbackInterface;
import WinsomeServer.api.ServerRMInterface;
import WinsomeServer.impl.ElaborateTask;
import WinsomeServer.impl.RewardCalculation;
import WinsomeServer.impl.ServerCallbackImpl;
import WinsomeServer.impl.ServerRMImpl;
import WinsomeServer.model.Post;
import WinsomeServer.model.User;
import WinsomeServer.utils.JsonData;
import WinsomeServer.utils.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class ServerMain {
	
	// variabili per il file di configurazione
	private static int TCPPORT;
	private static int RMIPORT;
	private static int NOTIFYPORT;
	private static String MULTICAST;
	private static int MCASTPORT;
	private static int REWARDTIME;
	private static double AUTHOREWARD;
	private static int TIMEOUT;
	
	// variabili di supporto
	private static ServerSocket serverSocket;
	private static ServerCallbackImpl serverCallback;
	private static AtomicBoolean serverActive = new AtomicBoolean(true);
	
	// strutture dati
	public static Map<String, User> usersMap = new HashMap<>();  // utenti di WINSOME: <username, User>
	public static Map<String, Post> postsMap = new HashMap<>();  // post degli utenti di WINSOME -> <idPost, Post>
	public static List<String> loggedUsers = new LinkedList<>();
	
	
	public static void main(String[] args){
		Logger.info("server ready");
		
		readServerConfig();
		JsonData.readData(usersMap, postsMap);
		
		try {
			serverSocket = new ServerSocket(TCPPORT);
		}
		catch (IOException e){
			throw new RuntimeException();
		}
		// thread per la gestione delle richieste del client
		Thread SSthread = new Thread(new clientRequests());
		SSthread.start();
		// thread per il calcolo delle ricompense
		Thread RCthread = new Thread(new RewardCalculation(MULTICAST, MCASTPORT, REWARDTIME, AUTHOREWARD, usersMap, postsMap, serverActive));
		RCthread.start();
		
		try{
			// per registrarsi a WINSOME
			ServerRMImpl registration = new ServerRMImpl(usersMap);
			ServerRMInterface stub1 = (ServerRMInterface) UnicastRemoteObject.exportObject(registration, RMIPORT);
			LocateRegistry.createRegistry(RMIPORT);
			Registry r1 = LocateRegistry.getRegistry(RMIPORT);
			r1.rebind("register", stub1);
			
			// per registrarsi al servizio di notifica callback
			serverCallback = new ServerCallbackImpl();
			LocateRegistry.createRegistry(NOTIFYPORT);
			Registry r2 = LocateRegistry.getRegistry(NOTIFYPORT);
			ServerCallbackInterface stub2 = (ServerCallbackInterface) UnicastRemoteObject.exportObject(serverCallback, NOTIFYPORT);
			r2.rebind("callback", stub2);
		}
		catch (RemoteException e){
			Logger.error("RMI server problem: " +e.getMessage());
			System.exit(1);
		}
		
		try{
			// se si digita exit, restituisce il numero di utenti loggati (se e' maggiore di 0)
			// a quel punto digitando "y" si puo' decidere se chiudere effettivamente il server
			// gli utenti loggati alla prossima richiesta, riceveranno un errore, la loro
			// socket verra' chiusa e termina il pool di gestione clienti.
			// Se gli utenti loggati non fanno alcuna altra richiesta, dopo TIMEOUT ms
			// il pool termina
			while (serverActive.get()){
				Scanner scanner = new Scanner(System.in);
				String input = scanner.nextLine();
				if(input.equals("exit")){
					
					if(loggedUsers.size() > 0){
						Logger.info("there are " +loggedUsers.size()+ " logged users, are you sure? [y/n]");
						input = scanner.nextLine();
						if(input.equals("y")){
							serverActive.set(false);
							Logger.info("server in shutdown...");
							serverSocket.close();
							SSthread.join();
							RCthread.join();
						}
					}
					else {
						serverActive.set(false);
						Logger.info("server in shutdown...");
						serverSocket.close();
						SSthread.join();
						RCthread.join();
					}
				}
			}
		} catch (IOException ignored) {} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("server is closed");
		System.exit(1);
	}
	
	
	public static class clientRequests implements Runnable{
		
		@Override
		public void run() {
			
			ExecutorService pool = Executors.newCachedThreadPool();
			try{
				
				Logger.info("server waiting at the port " +TCPPORT);
				while (true){
					
					Socket socket = serverSocket.accept();
					Logger.info("\nconnection accepted to the client: " +socket.getRemoteSocketAddress());
					
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					
					pool.execute(new ElaborateTask(socket, in, out, serverCallback, usersMap, postsMap, loggedUsers, serverActive));
				}
				
			} catch (IOException ignored) {
			}
			finally {
				pool.shutdown();
				try{
					if(!pool.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS)){
						pool.shutdownNow();
					}
				}
				catch (InterruptedException e){
					pool.shutdownNow();
				}
			}
		}
	}
	
	// ---------------------------- CONFIGURATION FILE ------------------------------
	private static void readServerConfig(){
		
		String config = "serverConfig.txt";
		try(BufferedReader in = new BufferedReader(new FileReader(config))){
			
			String line, key, value;
			
			while ((line = in.readLine()) != null){
				try{
					StringTokenizer tkLine = new StringTokenizer(line);
					key = tkLine.nextToken();
					// nel caso in cui non sia un commento (#-token)
					if(!key.equals("#")){
						value = tkLine.nextToken("= ").replaceAll("\\s", "");
						// value prende il secondo token, che non puo' essere un "=-token"
						// replaceAll serve per accettare i valori con spazi o senza dopo l'= (= value o =value)
						switch (key) {
							case "TCPPORT" -> TCPPORT = Integer.parseInt(value);
							case "RMIPORT" -> RMIPORT = Integer.parseInt(value);
							case "NOTIFYPORT" -> NOTIFYPORT = Integer.parseInt(value);
							case "MULTICAST" -> MULTICAST = value;
							case "MCASTPORT" -> MCASTPORT = Integer.parseInt(value);
							case "REWARDTIME" -> REWARDTIME = Integer.parseInt(value);
							case "AUTHOREWARD" -> AUTHOREWARD = Integer.parseInt(value);
							case "TIMEOUT" -> TIMEOUT = Integer.parseInt(value);
							default -> throw new RuntimeException("key not recognized "+key);
						}
					}
				}
				catch (NoSuchElementException e){
					continue;
				}
			}
		}
		catch (IOException e){
			Logger.error("Error reading from server configuration file: " +e.getMessage());
			System.exit(1);
		}
	}
}
