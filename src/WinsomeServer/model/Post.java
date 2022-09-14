package WinsomeServer.model;

import java.util.LinkedList;
import java.util.List;

public class Post {
	
	private String idPost;
	private String author;
	private String title;
	private String content;
	private List<String> whoRated;
	private int upvote;
	private int downvote;
	private List <String> comment;
	
	// per il calcolo delle ricompense
	private List <String> newPeopleUpvote;
	private List <String> newPeopleDownvote;
	private List <String> newPeopleComment;
	private int iteration;
	
	public Post(String idPost, String author, String title, String content){
		this.idPost = idPost;
		this.author = author;
		this.title = title;
		this.content = content;
		whoRated = new LinkedList<>();
		upvote = 0;
		downvote = 0;
		comment = new LinkedList<>();
		
		newPeopleUpvote = new LinkedList<>();
		newPeopleDownvote = new LinkedList<>();
		newPeopleComment = new LinkedList<>();
		iteration = 0;
	}
	
	// metodi get
	public String getIdPost() {
		return idPost;
	}
	public String getAuthor() {
		return author;
	}
	public String getTitle() { return title; }
	public String getContent() {
		return content;
	}
	public int getUpvote() { return upvote;}
	public int getDownvote() {
		return downvote;
	}
	
	public List<String> getComment() {
		return comment;
	}
	
	public List<String> getNewPeopleUpvote() {
		return newPeopleUpvote;
	}
	
	public List<String> getNewPeopleDownvote() {
		return newPeopleDownvote;
	}
	public List <String> getNewPeopleComment() { return newPeopleComment;}
	public int getIteration() {return iteration;}
	
	public boolean searchWhoRated(String user){
		
		return whoRated.contains(user);
	}
	
	public void rate(String user, int vote){
		if(vote > 0){
			putUpvote(user);
		}
		else {
			putDownvote(user);
		}
		whoRated.add(user);
	}
	
	public void putUpvote(String user){
		upvote++;
		newPeopleUpvote.add(user);
	}
	
	public void putDownvote(String user){
		downvote++;
		newPeopleDownvote.add(user);
	}
	
	public void addComment(String c, String user){
		comment.add(c);
		newPeopleComment.add(user);
	}
	
	public void resetNewInteraction(){
		newPeopleUpvote.clear();
		newPeopleDownvote.clear();
		newPeopleComment.clear();
		iteration++;
	}
	
	public String showInteraction(){
		return getTitle() + " " + getContent() + " " + getUpvote() + " "
			       + getDownvote() + " " + getComment();
	}
	
	public String showPost() {
		return getIdPost() + " " + getAuthor() + " " + getTitle();
	}
}
