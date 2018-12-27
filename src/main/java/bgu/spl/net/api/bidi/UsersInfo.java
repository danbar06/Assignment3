package bgu.spl.net.api.bidi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class UsersInfo {
	public Map<String,String> registered;
	public Map<String,Integer> userToId;
	public Map<String,Set<String>> whoFollowsMe;
	public Map<String,Queue<String>> pending;
	public Map<String, AtomicInteger[]> stat;//3 space vector, first num of posts, second num of followers, third num of following
	private static UsersInfo instance = new UsersInfo();

	private UsersInfo() {
		registered = Collections.synchronizedMap(new LinkedHashMap<String, String>());
		userToId = new ConcurrentHashMap<>();
		whoFollowsMe = new ConcurrentHashMap<>();
		pending = new ConcurrentHashMap<>();
		stat = new ConcurrentHashMap<>();
	}
	
	public static UsersInfo getInstance() {
		return instance;
	}

}
