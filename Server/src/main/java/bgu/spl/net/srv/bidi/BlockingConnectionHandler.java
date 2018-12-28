package bgu.spl.net.srv.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.ConnectionsImpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
 

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private ConnectionsImpl<T> connections;
    private int id;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader,
    		BidiMessagingProtocol<T> protocol, ConnectionsImpl<T> connections, int id) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        this.id = id;
    }

    @Override
    public void run() {
        try(Socket sock = this.sock) { //just for automatic closing
            int read;
            connections.add(this, id);
            protocol.start(id, connections);
            in = new BufferedInputStream(sock.getInputStream());
            //out = new BufferedOutputStream(sock.getOutputStream());
            
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                	protocol.process(nextMessage);
                    /*T response = protocol.process(nextMessage);
                    if (response != null) {
                        out.write(encdec.encode(response));
                        out.flush();
                    }*/
                	
                }
            }
            connections.disconnect(id);
            

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

	@Override
	public void send(T msg) {
		try{
			out = new BufferedOutputStream(sock.getOutputStream());
			out.write(encdec.encode(msg));
			out.flush();
		}
		catch(Exception e) {
			
		}
		
	}
}
