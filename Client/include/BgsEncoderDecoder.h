//
// Created by dorhe@wincs.cs.bgu.ac.il on 12/27/18.
//

#ifndef BGSENCODERDECODER_H
#define BGSENCODERDECODER_H

#include <string>
#include <array>
#include <vector>
#include <algorithm>

enum opCode{
    REGISTER,
    LOGIN,
    LOGOUT,
    FOLLOW,
    POST,
    PM,
    USERLIST,
    STAT,
    NOTIFICATION,
    ERROR,
    ACK
};




class BgsEncoderDecoder {
private:
    std::vector<char> buff;
    int counter = 0;
    void pushByte(char nextByte);
    std::string popString();
    void shortToBytes(short num, char* bytesArr);
    short bytesToShort(char* bytesArr);
    opCode commandType (std::string const& inString) {
        if (inString == "REGISTER") return REGISTER;
        if (inString == "LOGIN") return LOGIN;
        if (inString == "LOGOUT") return LOGOUT;
        if (inString == "FOLLOW") return FOLLOW;
        if (inString == "POST") return POST;
        if (inString == "PM") return PM;
        if (inString == "USERLIST") return USERLIST;
        if (inString == "STAT") return STAT;
        if (inString == "NOTIFICATION") return NOTIFICATION;
        if (inString == "ERROR") return ERROR;
        if (inString == "ACK") return ACK;
        return STAT;
    };

public:

    std::string decodeNextByte(char c);

    std::string encode(std::string message);

};

#endif
