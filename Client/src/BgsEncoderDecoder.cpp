#include "../include/BgsEncoderDecoder.h"
using namespace std;


void BgsEncoderDecoder::pushByte(char nextByte){
    buff.push_back(nextByte);
}


string BgsEncoderDecoder::popString() {
    string ans;
    char opcode[2];
    opcode[0] = buff[0];
    opcode[1] = buff[1];
    short op = bytesToShort(&opcode[0]);
    switch(op){
        case 9: {

            string type;
            type = buff[2] == '0' ? "PM" : "Public";
            ans += "NOTIFICATION " + type+ " ";
            for(unsigned int i = 3 ; i < buff.size()-1 ; i++){
                ans = buff[i] == '\0' ? ans + ' ' : ans + buff[i];
            }
            break;
        }
        case 10: {
            char mOpcode[2];
            mOpcode[0] = buff[2];
            mOpcode[1] = buff[3];
            short mOp = bytesToShort(mOpcode);
            string toAdd = "";
            switch (mOp){
                case 4:{
                    char numOfUsers[2];
                    numOfUsers[0] = buff[4];
                    numOfUsers[1] = buff[5];
                    short num = bytesToShort(numOfUsers);
                    toAdd += to_string(num) + ' ';
                    for(unsigned int i = 6 ; i < buff.size()-1 ; i++){
                        toAdd += buff[i] == '\0' ? ' ' : buff[i];
                    }
                    break;
                }
                case 7:{
                    char numOfUsers[2];
                    numOfUsers[0] = buff[4];
                    numOfUsers[1] = buff[5];
                    short num = bytesToShort(numOfUsers);
                    toAdd += to_string(num) + ' ';
                    for(unsigned int i = 6 ; i < buff.size()-1 ; i++){
                        toAdd += buff[i] == '\0' ? ' ' : buff[i];
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
                    toAdd += to_string(num1) + ' ' + to_string(num2) + ' ' + to_string(num3);

                }
            }
            ans += toAdd != "" ? "ACK " + to_string(mOp) + ' ' + toAdd : "ACK " + to_string(mOp);
            break;
        }
        case 11:{
            char mOpcode[2];
            mOpcode[0] = buff[2];
            mOpcode[1] = buff[3];
            short mOp = bytesToShort(mOpcode);
            ans += "ERROR " + to_string(mOp);
        }
    }
    this->counter = 0;
    this->opcode = -1;
    this->subOpcode = -1;
    this-> numOfUsers = -1;
    this-> zeroCounter = 0;
    buff.clear();
    return ans;

}

string BgsEncoderDecoder::decodeNextByte(char c) {
    pushByte(c);
    counter++;
    if(counter == 2){
        char opcode[2];
        opcode[0] = buff[0];
        opcode[1] = buff[1];
        this->opcode = bytesToShort(&opcode[0]);
    }
    if(counter == 4){
        char opcode[2];
        opcode[0] = buff[2];
        opcode[1] = buff[3];
        this->subOpcode = bytesToShort(&opcode[0]);
    }
    switch(opcode) {
        case 9:{
            if(c == '\0') zeroCounter++;
            if(zeroCounter == 2){
                return popString();
            }
            break;
        }
        case 10: {
            switch (subOpcode) {
                case 1: case 2: case 3: case 5: case 6: {
                    if(counter == 4)
                        return popString();
                }
                case 4: case 7: {
                    if(counter == 6) {
                        char opcode[2];
                        opcode[0] = buff[4];
                        opcode[1] = buff[5];
                        this->numOfUsers = bytesToShort(&opcode[0]);
                    }
                    numOfUsers = c == '\0' ? numOfUsers-1 : numOfUsers;
                    if(numOfUsers == 0){
                        return popString();
                    }
                    break;
                }
                case 8:{
                    if(counter == 10){
                        return popString();
                    }
                    break;
                }

            }
            break;
        }
        case 11: {
            if(counter == 4)
                return popString();
        }
    }
    return "-1";
}

string BgsEncoderDecoder::encode(string message) {
    vector<char> buffer;
    char opcode[2];
    string command(message.begin(), find(message.begin(),message.end(), ' '));

    string rest(find(message.begin(), message.end(), ' ') , message.end());
    vector<char> messageBuffer(rest.begin(), rest.end());
    switch(commandType(command)){
        case REGISTER: {
            messageBuffer.erase(messageBuffer.begin());
            shortToBytes(1, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case PM: {
            messageBuffer.erase(messageBuffer.begin());
            shortToBytes(6, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case LOGIN: {
            messageBuffer.erase(messageBuffer.begin());
            shortToBytes(2, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            for (char c : messageBuffer) {
                buffer.push_back(c != ' ' ? c : '\0');
            }
            buffer.push_back('\0');
            break;
        }
        case LOGOUT: {
            shortToBytes(3, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            break;
        }
        case USERLIST: {
            shortToBytes(7, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            break;
        }
        case STAT: {
            rest.erase(0,1);
            shortToBytes(8, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            copy(rest.begin(), rest.end(), back_inserter(buffer));
            buffer.push_back('\0');
            break;
        }
        case FOLLOW: {
            rest.erase(0,1);
            shortToBytes(4, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            short status = atoi(&message.at(7));
            shortToBytes(status, &opcode[0]);
            buffer.push_back(opcode[1]);
            string num(message.begin()+9, find(message.begin()+9,message.end(), ' '));
            short numOfUsers = stoi(num);
            shortToBytes(numOfUsers, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            vector<char> followers(find(message.begin()+9,message.end(), ' ')+1, message.end());
            for(unsigned int i = 0; i < followers.size() ; i++)
                buffer.push_back(followers[i] != ' ' ? followers[i] : '\0');
            buffer.push_back('\0');
            break;
        }
        case POST:{
            rest.erase(0,1);
            shortToBytes(5, &opcode[0]);
            buffer.push_back(opcode[0]);
            buffer.push_back(opcode[1]);
            copy(rest.begin(), rest.end(), back_inserter(buffer));
            buffer.push_back('\0');
            break;
        }
        default: {
            break;
        }

    }
    string temp(buffer.begin(),buffer.end());
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

