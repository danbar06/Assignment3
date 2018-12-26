package bgu.spl.net.impl.echo;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EchoProtocol implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private Connections<String> connections;
    private int connectionId;
    private Map<String,Integer> users;

    @Override
    
    public void start(int id, Connections<String> c) {
    	this.connections = c;
    	this.connectionId = id;
    	if(users == null) users = new ConcurrentHashMap<>();
    }
    
    public void process(String msg) {
    	System.out.println(this);
    	shouldTerminate = "bye".equals(msg);
    	String s = createEcho(msg);
    	System.out.println("[" + LocalDateTime.now() + "]: " + msg);
    	if((msg.substring(0, msg.indexOf(' ')).equalsIgnoreCase("register"))){
    		users.putIfAbsent(msg.substring(msg.indexOf(' ')+1), connectionId);
    	}
    	else if(msg.indexOf('@') != -1) {
    		Integer i = users.get(msg.substring(msg.indexOf('@')+1, msg.indexOf(' ')-1));
    		if(i != null)
    			connections.send(users.get(i), s);
    	}
        
    	
        
        
    	else connections.broadcast(s);
    }

    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
