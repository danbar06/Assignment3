package bgu.spl.net.api.bidi;

public class Message {
	private short Opcode;
	private String command;

	
	public Message(String s){
		Opcode = Short.parseShort(s.substring(0, 2));
		command = s.substring(2);
	}
	
	public short getOpcode() {
		return Opcode;
	}

	public String getCommand() {
		return command;
	}

}
