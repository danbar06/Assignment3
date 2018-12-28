package bgu.spl.net.impl.echo;

import bgu.spl.net.api.bidi.BGSProtocol;
import bgu.spl.net.srv.bidi.*;


public class EchoServerMain {

	public static void main(String[] args) {
		Server.threadPerClient(
				 7777,
				 () ->  new BGSProtocol(),
				 () ->  new LineMessageEncoderDecoder()
				 ).serve();
	}

}
