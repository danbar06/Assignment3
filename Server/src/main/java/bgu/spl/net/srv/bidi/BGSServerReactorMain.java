package bgu.spl.net.srv.bidi;
import bgu.spl.net.api.bidi.BGSProtocol;
import bgu.spl.net.api.bidi.LineMessageEncoderDecoder;

public class BGSServerReactorMain {

	public static void main(String[] args) {
		Server.reactor(
				 6,
				 7777,
				 () ->  new BGSProtocol(),
				 () ->  new LineMessageEncoderDecoder()
				 ).serve();

	}
	

}
