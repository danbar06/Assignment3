package bgu.spl.net.api.bidi;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LineMessageEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int counter=-1;
    private short currentCode =-1;
	private short numOfUsers =-1;


    
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
    			if(rest.substring(0, 2).equals("PM"))
    				tmp = ('0'+rest.substring(3)+'\0').replaceFirst(" ","\0").getBytes();
    			else
    				tmp = ('1'+rest.substring(7)+'\0').replaceFirst(" ","\0").getBytes();
    			break;
    		case "ACK":
    			Opcode = shortToBytes((short) 10);
    			if(rest.indexOf(" ")!=-1) {
    				short mOpcode =Short.parseShort(rest.substring(0, rest.indexOf(" ")));
        			rest = rest.substring(rest.indexOf(" ")+1);
        			short NumOfUsersOrPosts =Short.parseShort(rest.substring(0, rest.indexOf(" ")));
        			byte[] optional;
    				if(mOpcode==4 | mOpcode==7) {
    						String UsersList =rest.substring(rest.indexOf(" ")+1);
    						optional = UsersList.getBytes();
    						tmp = new byte[optional.length+4];
    						for(int i=4;i<tmp.length;i++) {
    							tmp[i]=optional[i-4];
    						}
    						optional= shortToBytes(mOpcode);
    						tmp[0]=optional[0];
    						tmp[1]=optional[1];
    						optional= shortToBytes(NumOfUsersOrPosts);
    						tmp[2]=optional[0];
    						tmp[3]=optional[1];
    				}
    				else {
    					tmp = new byte[8];
						optional= shortToBytes(mOpcode);
						tmp[0]=optional[0];
						tmp[1]=optional[1];
						optional= shortToBytes(NumOfUsersOrPosts);
						tmp[2]=optional[0];
						tmp[3]=optional[1];
						rest = rest.substring(rest.indexOf(' ')+1);
						optional= shortToBytes(Short.parseShort(rest.substring(0,rest.indexOf(' '))));
						tmp[4]=optional[0];
						tmp[5]=optional[1];
						rest = rest.substring(rest.indexOf(' ')+1);
						optional= shortToBytes(Short.parseShort(rest));
						tmp[6]=optional[0];
						tmp[7]=optional[1];
    				}
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
    
    @Override
    public String decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        pushByte(nextByte);
    	if(len==2) {
    		byte[] arr={bytes[0], bytes[1]};
    		currentCode = bytesToShort(arr);
    	}
        if (nextByte == '\0') {
            counter++;
        }
        switch(currentCode) {
        case 1:case 2:case 6:
        	if(counter==2) {
        		return popString();
        	}
        	break;

        case 3:case 7:
        	return popString();
        case 4:
        	if((len == 3 && bytes[2]==0) || (len == 4 && bytes[3]==0) || (len == 5 && bytes[4]==0))
        		counter--;
        	if(len == 5) {
        		byte[] arr ={bytes[3],bytes[4]};
        		numOfUsers = bytesToShort(arr);
        	}
        	if(counter == numOfUsers) {
        		numOfUsers = -1;
        		return popString();
        	}
        	break;

        case 5:case 8:
        	if(counter==1) {
            	return popString();
        	}
        	break;
        }
		return null; //not a line yet
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
    	currentCode = -1;
    	counter=-1;
    	byte[] opcode = new byte[2];
    	byte[] command;
    	opcode[0]=bytes[0];
    	opcode[1]=bytes[1];     
    	short code = bytesToShort(opcode);
    	if(code == 3 || code == 7) {
    		len = 0;
    		return code+" ";
    	}
    	if(code == 4) {
        	command = new byte[len-5];
    		byte[] tmp = new byte[2];
    		tmp[1]=bytes[2];
    		short follow = bytesToShort(tmp);
    		tmp[0] = bytes[3];
    		tmp[1] = bytes[4];
    		short numOfUsers = bytesToShort(tmp);
    		for(int i=5;i<len;i++){
	    		if(bytes[i]=='\0') command[i-5]=' ';
	    		else command[i-5] = bytes[i];
	    	}
	        String result = new String(command, 0, command.length, StandardCharsets.UTF_8);
	        len = 0;
    		return code+" "+follow+" "+numOfUsers+" "+result;
    	}
    	if(code !=3 && code !=4 && code !=7) {
        	command = new byte[len-2];
	    	for(int i=2;i<len;i++) {
	    		if(bytes[i]=='\0') command[i-2]=' ';
	    		else command[i-2] = bytes[i];
	    	}
	        String result = new String(command, 0, command.length, StandardCharsets.UTF_8);
	        len = 0;
	        return code +" "+ result;
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
