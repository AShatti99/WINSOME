package WinsomeServer.utils;

import WinsomeServer.model.Post;
import WinsomeServer.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonData {
	
	static String fileUsers = "users.json";
	static String filePosts = "posts.json";
	
	public static synchronized void writeData(Map<String, User> userMap, Map <String, Post> postMap) {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		if(userMap != null){
			List<User> listUser = new ArrayList<>(userMap.values());
			String jsonUser = gson.toJson(listUser);
			writeFile(jsonUser, fileUsers);
		}
		if(postMap != null){
			List<Post> listPost = new ArrayList<>(postMap.values());
			String jsonPost = gson.toJson(listPost);
			writeFile(jsonPost, filePosts);
		}
		
		Logger.info("finished to write json file");
	}
	
	private static void writeFile(String json, String file){
		
		try(FileOutputStream fout = new FileOutputStream(file);
		    FileChannel out = fout.getChannel()){
			
			ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
			out.write(buffer);
		}
		catch (IOException e){
			throw new RuntimeException("Error in writing json " +file);
		}
	}
	
	public static synchronized void readData(Map<String, User> userMap, Map <String, Post> postMap){
		
		try{
			
			JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(fileUsers)));
			
			reader.beginArray();
			while (reader.hasNext()){
				
				User user = new Gson().fromJson(reader, User.class);
				userMap.put(user.getUsername(), user);
			}
			reader.endArray();
			
			reader = new JsonReader(new InputStreamReader(new FileInputStream(filePosts)));
			
			reader.beginArray();
			while (reader.hasNext()){
				
				Post post = new Gson().fromJson(reader, Post.class);
				postMap.put(post.getIdPost(), post);
			}
			reader.endArray();
			
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException("Error in reading json file");
		}
		
		Logger.info("finished to read json file\n");
	}
}
