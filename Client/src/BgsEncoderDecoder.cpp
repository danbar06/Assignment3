#include "../header/BgsEncoderDecoder.h"


void BgsEncoderDecoder::pushByte(char nextByte){
    buff.push_back(nextByte);
}


std::string BgsEncoderDecoder::popString() {
    std::string ans;
    char opcode[2];
    opcode[0] = buff[0];
    opcode[1] = buff[1];
    short op = bytesToShort(&opcode[0]);
    switch(op){
        case 9: {
            std::string type;
            type = buff[2] == '0' ? "PM" : "Public";
            ans += "NOTIFICATION " + type+ " ";
            for(int i = 3 ; i < buff.size()-1 ; i++){
                ans = buff[i] == '\0' ? ans + ' ' : ans + buff[i];
            }
            break;
        }
        case 10: {
            char mOpcode[2];
            mOpcode[0] = buff[2];
            mOpcode[1] = buff[3];
            short mOp = bytesToShort(mOpcode);
            std::string toAdd = "";
            switch (mOp){
                case 4 | 7:{
                    char numOfUsers[2];
                    numOfUsers[0] = buff[4];
                    numOfUsers[1] = buff[5];
                    short num = bytesToShort(numOfUsers);
                    toAdd += num + ' ';
                    for(int i = 6 ; i < buff.size()-1 ; i++){
                        toAdd += buff[i];
                    }
                    break;
                }
                case 8:{
                    char numPosts[2];
                    char numFollowers[2];
                    char numFollowing[2];
                    numPosts[0] = buff[4];
                    numPosts[1] = buff[5];
                    numFollowers[0] = buff[6];
                    numFollowers[1] = buff[7];
                    numFollowing[0] = buff[8];
                    numFollowing[1] = buff[9];
                    short num1 = bytesToShort(numPosts);
                    short num2 = bytesToShort(numFollowers);
                    short num3 = bytesToShort(numFollowing);
                    toAdd += num1 + ' ' + num2 + ' ' + num3;

                }
            }
            ans += toAdd != "" ? "ACK " + mOp+ ' ' + toAdd : "ACK " + mOp;
        }
        case 11:{
            char mOpcode[2];
            mOpcode[0] = buff[2];
            mOpcode[1] = buff[3];
            short mOp = bytesToShort(mOpcode);
            ans += "ERROR " + mOp;
        }
    }

    buff.clear();
    return ans;

}

std::string BgsEncoderDecoder::decodeNextByte(char c) {
    if(c == '\n')
        return popString();
    pushByte(c);
    return nullptr;
}

std::string BgsEncoderDecoder::encode(std::string message) {
    std::vector<char> buffer;
    char* opcode[2];
    std::string command(message.begin(), std::find(message.begin(),message.end(), ' '));
    std::string rest(std::find(message.begin(),message.end(), ' ')+1, message.end());
    std::vector<char> messageBuffer(rest.begin(), rest.end());
    switch(commandType(command)){
        case REGISTER: {
            shortToBytes(1, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case PM: {
            shortToBytes(6, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case LOGIN: {
            shortToBytes(2, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case LOGOUT: {
            shortToBytes(3, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            buffer.push_back('\n');
            break;
        }
        case USERLIST: {
            shortToBytes(7, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            buffer.push_back('\n');
            break;
        }
        case STAT: {
            shortToBytes(8, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            std::copy(rest.begin(), rest.end(), std::back_inserter(buffer));
            buffer.push_back('\0');
            break;
        }
        case FOLLOW: {
            shortToBytes(4, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            short status = atoi(&message.at(7));
            shortToBytes(status, *opcode);
            buffer.push_back(*opcode[1]);
            std::string num(message.begin()+9, std::find(message.begin()+9,message.end(), ' '));
            short numOfUsers = stoi(num);
            shortToBytes(numOfUsers, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            std::copy(std::find(message.begin()+9,message.end(), ' ')+1, rest.end(), std::back_inserter(buffer));
            break;
        }
        case POST:{
            shortToBytes(5, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
            std::copy(rest.begin(), rest.end(), std::back_inserter(buffer));
            buffer.push_back('\0');
            buffer.push_back('\n');
            break;
        }


    }
    std::string temp(buffer.begin(),buffer.end());
    return temp;
}

void BgsEncoderDecoder::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short BgsEncoderDecoder::bytesToShort(char *bytesArr) {
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}

