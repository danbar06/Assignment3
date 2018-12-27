package bgu.spl.net.impl.echo;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LineMessageEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    
    public String decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (nextByte == '\n') {
            return popString();
        }

        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public  byte[] encode(String message) {
    	String code = message.substring(0, message.indexOf(" "));
    	String rest = message.substring(message.indexOf(" ")+1);
    	byte[] Opcode = null;
    	byte[] tmp =null;
    	byte[] ans;
    	switch(code) {
    		case "NOTIFICATION":
    			Opcode = shortToBytes((short) 9);
    			if(message.substring(0, 2).equals("PM"))
    				tmp = ('0'+rest+'\0').replaceFirst(" ","\0").getBytes();
    			else
    				tmp = ('1'+rest+'\0').replaceFirst(" ","\0").getBytes();
    			break;
    		case "ACK":
    			Opcode = shortToBytes((short) 10);
    			if(rest.indexOf(" ")!=-1) {
    				tmp = shortToBytes(Short.parseShort(rest.substring(0, rest.indexOf(" "))));
        			rest = rest.substring(rest.indexOf(" ")+1);
    			}
    			else
    				tmp = shortToBytes(Short.parseShort(rest));
    			break;
    		case "ERROR":
    			Opcode = shortToBytes((short) 11);
				tmp = shortToBytes(Short.parseShort(rest));
    			break;
    	}
    	if(tmp == null || Opcode == null)
    		System.out.println("tmp or Opcode in encode are not initialized");
		ans = new byte[tmp.length+Opcode.length];
		for(int i=0;i<ans.length;i++) {
			if(i<Opcode.length)
				ans[i]=Opcode[i];
			else
				ans[i]=tmp[i-Opcode.length];
		}
        return ans; //uses utf8 by default
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
    	byte[] opcode = new byte[2];
        short code = bytesToShort(opcode);
    	byte[] command = new byte[bytes.length-2];
    	opcode[0]=bytes[0];
    	opcode[1]=bytes[1];
    	if(code == 3 || code == 7) {
    		len = 0;
    		return code+" ";
    	}
    	if(code == 4) {
    		byte[] tmp = new byte[1];
    		tmp[0]=bytes[2];
    		short follow = bytesToShort(tmp);
    		tmp = new byte[2];
    		tmp[0] = bytes[3];
    		tmp[1] = bytes[4];
    		short numOfUsers = bytesToShort(tmp);
    		for(int i=5;i<bytes.length;i++)
	    		command[i-5]=bytes[i];
	        String result = new String(command, 0, len, StandardCharsets.UTF_8);
	        len = 0;
    		return code+" "+follow+" "+numOfUsers+" "+result+" ";
    	}
    	if(code !=4 && code !=7 && code !=3) {
	    	for(int i=2;i<bytes.length;i++)
	    		command[i-2]=bytes[i];
	        String result = new String(command, 0, len, StandardCharsets.UTF_8);
	        len = 0;
	        return code +" "+ result.replace('\0',' ');
    	}
    	return null;
    }
    
    public byte[] shortToBytes(short num){
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }
}
