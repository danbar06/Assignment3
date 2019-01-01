package bgu.spl.net.api.bidi;
import bgu.spl.net.srv.bidi.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionsImpl<T> implements Connections<T> {

	private Map<Integer,ConnectionHandler<T>> uniqueId; 

	
	public ConnectionsImpl() {
		uniqueId = new ConcurrentHashMap<>();
	}
	
	public void add(ConnectionHandler<T> c, int id) {
		uniqueId.put(id, c);
	}
	
	
	@Override
	public boolean send(int connectionId, T msg) {
		ConnectionHandler<T> c = uniqueId.get(connectionId);
		if(c!=null) {
			c.send(msg);
			return true;
		}
		return false;
	}

	@Override
	public void broadcast(T msg) {
		for(Map.Entry<Integer, ConnectionHandler<T>> pair : uniqueId.entrySet()) {
			pair.getValue().send(msg);
		}
	}

	@Override
	public void disconnect(int connectionId) {
		uniqueId.remove(connectionId);		
		
	}

}
