package WinsomeServer.utils;

import WinsomeServer.model.Post;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Support {
	
	public static boolean check(boolean logged, int tokens, int rightValue, String cmd){
		
		return numberArguments(tokens, rightValue, cmd) && isLogged(logged, cmd);
	}
	
	public static boolean numberArguments(int tokens, int rightValue, String cmd){
		
		if(cmd.equals("post")){
			if(tokens < rightValue){
				Logger.info("< Error: to create a post you need a title and a content, in this order");
				return false;
			}
			return true;
		}
		
		if(cmd.equals("comment")){
			if(tokens < rightValue){
				Logger.info("< Error: to comment you need a idPost and a comment, in this order");
				return false;
			}
			return true;
		}
		
		
		if(tokens != rightValue){
			
			switch (cmd){
				case "register" -> Logger.info("< Error: to register you need a username, password and 5 tags, in this order");
				case "login" -> Logger.info("< Error: to login you need a username and a password, in this order");
				case "logout" -> Logger.info("< Error: to sign out just type logout");
				case "follow" -> Logger.info("< Error: to follow someone you need just his username");
				default -> Logger.info("< Error: unrecognized command, type help to see the list of available commands");
			}
			return false;
		}
		
		return true;
	}
  
	public static boolean isLogged(boolean logged, String cmd){
		
		switch (cmd) {
			case "register" -> {
				if(logged) {
					Logger.info("< Error: please log out before registering a new account");
					return false;
				}
			}
			case "login" -> {
				if(logged) {
					Logger.info("< Error: please log out before logging in with a new account");
					return false;
				}
			}
			case "exit" -> {
				if(logged) {
					Logger.info("< Error: please log out before closing client application");
					return false;
				}
			}
			default -> {
				if(!logged) {
					Logger.info("< Error: please login before doing this operation");
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static String cleanString(String str){
		
		str = str.replaceAll("[\\[\\]]", "").replaceAll(",", "");
		return str;
	}
	
	public static List <String> cleanPost(String str){
		
		List<String> list = new ArrayList<>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str);
		while (m.find()) list.add(m.group(1));
		
		return list;
	}
	
	public static String addString(String str, String user, String op){
		
		str = str.substring(0, str.length()-1);
		str = str + op + user;
		str = str + "\"";
		
		return str;
	}
	
	public static int countQuote(String str){
		
		Pattern p = Pattern.compile("([\"])");
		Matcher m = p.matcher(str);
		int c = 0;
		while (m.find()) c++;
		return c;
	}
	
	public synchronized static int maxId(Map<String, Post> postMap){
		int max = 999, n;
		for(Map.Entry <String, Post> entry : postMap.entrySet()){
			n = Integer.parseInt(entry.getKey());
			if(n > max){
				max = n;
			}
		}
		return max+1;
	}
	
	public static String hashPassword(String password) {
		
		StringBuilder sb = null;
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(password.getBytes());
			byte [] bytes = md.digest();
			
			sb = new StringBuilder();
			for (byte aByte : bytes) {
				
				sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}
