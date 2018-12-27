
package bgu.spl.net.api.bidi;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BGSProtocol implements BidiMessagingProtocol<String> {
	private volatile static Set<String> connected;
	private volatile static LinkedList<String> savedMsg;
	private UsersInfo usersInfo;
	private final static Object lock = new Object();
	private Connections<String> connections;
	private int id;
	private String currentUser;
	private boolean shouldTerminate;
	
	
	@Override
	public void start(int connectionId, Connections<String> connections) {
		this.connections=connections;
		this.id=connectionId;
		this.usersInfo = UsersInfo.getInstance();
		shouldTerminate=false;
		synchronized(lock) {
			if(connected==null) connected = new HashSet<>();
			if(savedMsg==null) savedMsg = new LinkedList<>();
		}
	}

	@Override
	public void process(String message) {
		String code = message.substring(0, message.indexOf(" "));
		String command = message.substring(message.indexOf(" ")+1);
		Map<String,String> registered = usersInfo.registered;
		Map<String,Integer> userToId = usersInfo.userToId;
		Map<String,Set<String>> whoFollowsMe = usersInfo.whoFollowsMe;
		Map<String,Queue<String>> pending = usersInfo.pending;
		Map<String,int[]> stat = usersInfo.stat;
		String username;
		String pass;
		switch(code) {
			case "REGISTER":
				username = command.substring(0, command.indexOf(" "));
				pass = command.substring(command.indexOf(" ")+1, command.length()-1);
				if(registered.containsKey(username)) {
					connections.send(id,"ERROR 1");
				}
				else {
					connections.send(id,"ACK 1");
					registered.put(username, pass);
					userToId.put(username, id);
					whoFollowsMe.put(username, new HashSet<>());
					pending.put(username, new ConcurrentLinkedQueue<>());
					stat.put(username, new int[3]);
				}
				break;
			case "LOGIN":
				username = command.substring(0, command.indexOf(" "));
				pass = command.substring(command.indexOf(" ")+1, command.length()-1);
				if(!registered.containsKey(username) || !registered.get(username).equals(pass))
					connections.send(id,"ERROR 2");
				else synchronized(connected) {
					if(connected.contains(username))
						connections.send(id,"ERROR 2");
					else {
						connected.add(username);
						currentUser=username;
						userToId.put(currentUser, id);
						connections.send(id,"ACK 2");
						for(String s: pending.get(currentUser))
							connections.send(id, s);
					}
				}
				break;
			case "LOGOUT":
				if(!connected.contains(currentUser))
					connections.send(id,"ERROR 3");
				else {
					connected.remove(currentUser);
					userToId.remove(currentUser,id);
					connections.send(id,"ACK 3");
					shouldTerminate=true;
				}
				break;
			case "FOLLOW":
				if(connected.contains(currentUser)) {
					int follow = Integer.parseInt(command.substring(0, command.indexOf(" ")));
					command=command.substring(command.indexOf(" ")+1);
					int numOfUsers = Integer.parseInt(command.substring(0, command.indexOf(" ")));
					command=command.substring(command.indexOf(" ")+1);
					int countSucceed=0;
					String succeed="";
					if(follow==0) {
						for(int i=0;i<numOfUsers;i++) {
							int index=command.indexOf(" ");
							username=command.substring(0, index);
							if(canFollow(username)) {
								whoFollowsMe.get(username).add(currentUser);
								stat.get(username)[1]++;
								succeed +=username;
								countSucceed++;
							}
							command=command.substring(index+1);
						}
						stat.get(currentUser)[2] +=countSucceed;
					}
					else {
						for(int i=0;i<numOfUsers;i++) {
							int index=command.indexOf(" ");
							username=command.substring(0, index);
							if(canUnfollow(username)) {
								whoFollowsMe.get(username).remove(currentUser);
								stat.get(username)[1]--;
								succeed +=username;
								countSucceed++;
							}
							command=command.substring(index+1);
						}
						stat.get(currentUser)[2] -=countSucceed;
					}
					if(countSucceed!=0)
						connections.send(id,"ACK 4 " + follow +" "+countSucceed+" "+succeed+'\0');
					else
						connections.send(id,"ERROR 4");
				}
				else
					connections.send(id,"ERROR 4");	
				break;

			case "POST":
				if(!connected.contains(currentUser))
					connections.send(id,"ERROR 5");
				else {
					stat.get(currentUser)[0]++;
					Set<String> toSend = new HashSet<>(whoFollowsMe.get(currentUser));
					toSend.addAll(Tagged(command));
					for(String s: toSend) {
						if(connected.contains(s))
							connections.send(userToId.get(s),"NOTIFICATION Public "+currentUser+" "+command);
						else
							pending.get(s).add("NOTIFICATION Public "+currentUser+" "+command);
					}
				}
				break;

			case "PM":
				String recipient=command.substring(0, command.indexOf(" "));
				if(!connected.contains(currentUser) || !registered.containsKey(recipient))
					connections.send(id,"ERROR 6");
				else {
					savedMsg.add(command);
					String content=command.substring(command.indexOf(' ')+1, command.length()-1);
					if(connected.contains(recipient))
						connections.send(userToId.get(recipient),"NOTIFICATION PM "+currentUser+" "+content);
					else
						pending.get(recipient).add("NOTIFICATION PM "+currentUser+" "+content);
				}
				break;

			case "USERLIST":
				if(!connected.contains(currentUser))
					connections.send(id,"ERROR 7");
				else {
					String ans="ACK 7 "+registered.entrySet().size();
					for (Map.Entry<String, String> pair : registered.entrySet()) {
						ans+=pair.getKey();
					}
					connections.send(id, ans);
				}
				break;

			case "STAT":
				username = command.substring(0, command.length()-1);
				if(!connected.contains(currentUser) || !registered.containsKey(username))
					connections.send(id,"ERROR 8");
				else {
					String ans = stat.get(username)[0]+" "+stat.get(username)[1]+" "+stat.get(username)[2];
					connections.send(id,"ACK 8 "+ans);
				}	
				break;

		}	
	}
	
	private Set<String> Tagged(String command) {
		Set<String> ans = new HashSet<>();
		int index = command.indexOf('@');
		String cut;
		String username;
		while(index!=-1) {
			cut=command.substring(index);
			username=cut.substring(0, cut.indexOf(" "));
			ans.add(username);
			cut=cut.substring(cut.indexOf(" "));
			index = cut.indexOf('@');
			cut=cut.substring(index);
		}
		return ans;
	}

	private boolean canFollow(String s) {
		Map<String,Set<String>> whoFollowsMe = usersInfo.whoFollowsMe;
		if(whoFollowsMe.get(s).contains(currentUser))
			return false;
		return true;
	}
	private boolean canUnfollow(String s) {
		Map<String,Set<String>> whoFollowsMe = usersInfo.whoFollowsMe;
		if(!whoFollowsMe.get(s).contains(currentUser))
			return false;
		return true;
	}

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
	
	

}
