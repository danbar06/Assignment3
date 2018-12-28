package bgu.spl.net.api.bidi;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
		currentUser = "";
		synchronized(lock) {
			if(connected==null) connected = new HashSet<>();
			if(savedMsg==null) savedMsg = new LinkedList<>();
		}
	}

	@Override
	public void process(String message) {
		int code = Integer.parseInt(message.substring(0, message.indexOf(" ")));
		String command = message.substring(message.indexOf(" ")+1);
		Map<String,String> registered = usersInfo.registered;
		Map<String,Integer> userToId = usersInfo.userToId;
		Map<String,Set<String>> whoFollowsMe = usersInfo.whoFollowsMe;
		Map<String,Queue<String>> pending = usersInfo.pending;
		Map<String, AtomicInteger[]> stat = usersInfo.stat;
		String username;
		String pass;
		switch(code) {
			case 1:
				username = command.substring(0, command.indexOf(" "));
				pass = command.substring(command.indexOf(" ")+1, command.length()-1);
				if(registered.containsKey(username)) {
					connections.send(id,"ERROR 1");
				}
				else {
					registered.put(username, pass);
					userToId.put(username, id);
					whoFollowsMe.put(username, new HashSet<>());
					pending.put(username, new ConcurrentLinkedQueue<>());
					stat.put(username, new AtomicInteger[3]);
					connections.send(id,"ACK 1");
				}
				break;
			case 2:
				username = command.substring(0, command.indexOf(" "));
				pass = command.substring(command.indexOf(" ")+1, command.length()-1);
				if(!registered.containsKey(username) || !registered.get(username).equals(pass))
					connections.send(id,"ERROR 2");
				else {
					if(currentUser.equals(""))
						connections.send(id,"ERROR 2");
					else {
						synchronized(connected) {
							currentUser=username;
							connected.add(currentUser);
							userToId.put(currentUser, id);
							connections.send(id,"ACK 2");
							for(String s: pending.get(currentUser))
								connections.send(id, s);
						}
					}
				}
				break;
			case 3:
				if(currentUser.equals(""))
					connections.send(id,"ERROR 3");
				else {
					synchronized(connected) {
						connected.remove(currentUser);
						userToId.remove(currentUser,id);
						connections.send(id,"ACK 3");
						shouldTerminate=true;
					}
				}
				break;
			case 4:
				if(!currentUser.equals("")) {
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
							synchronized(whoFollowsMe) {
								if(canFollow(username)) {
									whoFollowsMe.get(username).add(currentUser);
									stat.get(username)[1].incrementAndGet();
									stat.get(currentUser)[2].incrementAndGet();
									succeed +=username+'\0';
									countSucceed++;
								}
							}
							command=command.substring(index+1);
						}
					}
					else {
						for(int i=0;i<numOfUsers;i++) {
							int index=command.indexOf(" ");
							username=command.substring(0, index);
							synchronized(whoFollowsMe) {
								if(canUnfollow(username)) {
									whoFollowsMe.get(username).remove(currentUser);
									stat.get(username)[1].decrementAndGet();
									stat.get(currentUser)[2].decrementAndGet();
									succeed +=username+'\0';
									countSucceed++;
								}
							}
							command=command.substring(index+1);
						}
					}
					if(countSucceed!=0)
						connections.send(id,"ACK 4 " + follow +" "+countSucceed+" "+succeed);
					else
						connections.send(id,"ERROR 4");
				}
				else
					connections.send(id,"ERROR 4");	
				break;

			case 5:
				if(currentUser.equals(""))
					connections.send(id,"ERROR 5");
				else {
					stat.get(currentUser)[0].incrementAndGet();
					String content =command.substring(0, command.length()-1);
					synchronized(savedMsg) {savedMsg.add(content);}
					Set<String> toSend = new HashSet<>(whoFollowsMe.get(currentUser));
					toSend.addAll(Tagged(command));
					synchronized(connected) {
						for(String s: toSend) {
							if(connected.contains(s))
								connections.send(userToId.get(s),"NOTIFICATION Public "+currentUser+" "+content);
							else
								pending.get(s).add("NOTIFICATION Public "+currentUser+" "+content);
						}
					}
				}
				break;

			case 6:
				String recipient=command.substring(0, command.indexOf(" "));
				if(currentUser.equals("") || !registered.containsKey(recipient))
					connections.send(id,"ERROR 6");
				else {
					synchronized(savedMsg) {savedMsg.add(command);}
					String content=command.substring(command.indexOf(' ')+1, command.length()-1);
					synchronized(connected) {
						if(connected.contains(recipient))
							connections.send(userToId.get(recipient),"NOTIFICATION PM "+currentUser+" "+content);
						else
							pending.get(recipient).add("NOTIFICATION PM "+currentUser+" "+content);
					}
				}
				break;

			case 7:
				if(currentUser.equals(""))
					connections.send(id,"ERROR 7");
				else {
					String ans;
					synchronized(registered) {
						ans="ACK 7 "+registered.entrySet().size()+" ";
						for (Map.Entry<String, String> pair : registered.entrySet()) {
							ans+=pair.getKey()+'\0';
						}
					}
					connections.send(id, ans);
				}
				break;

			case 8:
				username = command.substring(0, command.length()-1);
				if(currentUser.equals("") || !registered.containsKey(username))
					connections.send(id,"ERROR 8");
				else {
					String ans = stat.get(username)[0].get()+" "+stat.get(username)[1].get()+" "+stat.get(username)[2].get();
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
		Map<String,String> registered = usersInfo.registered;
		if(whoFollowsMe.get(s).contains(currentUser) || !registered.containsKey(s))
			return false;
		return true;
	}
	private boolean canUnfollow(String s) {
		Map<String,Set<String>> whoFollowsMe = usersInfo.whoFollowsMe;
		Map<String,String> registered = usersInfo.registered;
		if(!registered.containsKey(s) || !whoFollowsMe.get(s).contains(currentUser))
			return false;
		return true;
	}

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}
	
	

}