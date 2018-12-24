package bgu.spl.net.api.bidi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BGSProtocol implements BidiMessagingProtocol<Message> {
	private Map<String,String> registered;
	private Set<String> connected;
	
	public BGSProtocol() {
		registered = new ConcurrentHashMap<>(); 
		connected = new HashSet<>();
	}

	@Override
	public void start(int connectionId, Connections<Message> connections) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void process(Message message) {
		short code = message.getOpcode();
		String command = message.getCommand();
		switch(code) {
			case 1:
				String username = command.substring(0, command.indexOf('\0'));
				String pass = command.substring(command.indexOf('\0')+1, command.length()-2);
				if(registered.containsKey(username))
					connections.send(ERRROR + code);
				else {
					registered.put(username, pass);
					connections.send(ACK + code);
				}
			case 2:
				//username = command.substring(0, command.indexOf('\0'));
				//pass = command.substring(command.indexOf('\0')+1, command.length()-2);
				if(!registered.containsKey(username) || !registered.get(username).equals(pass) || connected.contains(username))
					connections.send(ERRROR + code);
				else {
					connected.add(username);
				}
					
		}
	}

	@Override
	public boolean shouldTerminate() {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}


