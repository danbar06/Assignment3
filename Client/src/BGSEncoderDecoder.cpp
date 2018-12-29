#include "../header/BGSEncoderDecoder.h"


void BgsEncoderDecoder::pushByte(char nextByte){
    buff.push_back(nextByte);
}

/*
std::string BgsEncoderDecoder::popString() {
    char opcode[]
}*/


void BgsEncoderDecoder::shortToBytes(short num, char *bytesArr) {
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short BgsEncoderDecoder::bytesToShort(char *bytesArr) {
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result |= (short)(bytesArr[1] & 0xff);
    return result;
}


std::string BgsEncoderDecoder::decodeNextByte(char c) {
    if(c == '\n')
        return popString();
    pushByte(c);
    return nullptr;
}

char* BgsEncoderDecoder::encode(std::string message) {
    std::vector<char> buffer;
    char* opcode[2];
    std::string command(message.begin(), std::find(message.begin(),message.end(), ' '));
    std::string rest(std::find(message.begin(),message.end(), ' ')+1, message.end());
    std::vector<char> messageBuffer(rest.begin(), rest.end());
    switch(hashit(command)){
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
            break;
        }
        case USERLIST: {
            shortToBytes(7, *opcode);
            buffer.push_back(*opcode[0]);
            buffer.push_back(*opcode[1]);
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
            if(numOfUsers > 9)
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
            break;
        }


    }

    return &buffer[0];
}

