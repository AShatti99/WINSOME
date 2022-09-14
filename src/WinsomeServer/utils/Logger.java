package WinsomeServer.utils;

public class Logger {
	
	public static void info(String args){
		System.out.println(args);
	}
  
	public static void input(){
		System.out.print("\n> ");
	}
  
	public static void welcome(){
		System.out.println("""
			< Type: register <username> <password> <5 tags> to create an account\s
			        login <username> <password> to login in your account
			        exit to close the client application\s"""
		);
	}
	
	public static void help(){
		System.out.println("""
			< Type: logout to exit the account
			        list users to see who has a tag in common with you
			        follow <username> to follow someone
			        unfollow <username> to unfollow someone
			        list followers to see your followers
			        list following to see your following
			        post <"title"> <"content"> to create a post
			        delete <idPost> to delete one of your posts
			        blog to see your post
			        show post <idPost> to see a single post
			        show feed to see posts of your following
			        rewin <idPost> to share a post by one of your following
			        rate <idPost> <vote> to vote a post by one of your following
			        comment <idPost> <"comment"> to comment a post by one of your following
			        wallet to find out your account balance
			        wallet btc to convert your balance to bitcoin\s"""
		);
		
	}
  
	public static void error(String args){
		System.err.println(args);
	}
}
