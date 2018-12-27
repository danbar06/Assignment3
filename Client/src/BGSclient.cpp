#include <utility>

#include <stdlib.h>
#include <thread>
#include <mutex>
#include <condition_variable>
#include "../header/ConnectionHandler.h"
#include "../header/BgsEncoderDecoder.h"

class SocketReadTask {
private:
    std::string _name;
    ConnectionHandler *_connectionHandler;
    std::mutex &_mutex;
    bool &_terminated;
    std::condition_variable &_cv;

    bool shouldTerminate(){
        std::lock_guard<std::mutex> lock(_mutex);
        return _terminated;

    }

public:
    SocketReadTask(std::string name, ConnectionHandler *connectionHandler, bool &terminated, std::mutex& mutex, std::condition_variable &cv) :
    _name(std::move(name)), _mutex(mutex),_terminated(terminated), _cv(cv){
        _connectionHandler = connectionHandler;
    }

    void run(){
        while(true) {
            unsigned long len;
            std::string answer;
            if (!_connectionHandler -> getLine(answer)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
            len = answer.length();
            answer.resize(len - 1);
            std::cout << "Reply: " << answer << std::endl;
            if (answer == "ACK 3" && shouldTerminate()) {
                _cv.notify_all();
                break;
            }
        }
        std::this_thread::yield();
    }
};





class UserReadTask {
private:
    std::string _name;
    ConnectionHandler *_connectionHandler;
    std::mutex &_mutex;
    std::unique_lock<std::mutex> &_waitOn;
    bool &_terminated;
    std::condition_variable &_cv;
    BgsEncoderDecoder _encdec;


public:
    UserReadTask(std::string name, ConnectionHandler *connectionHandler, bool &terminated, std::mutex& mutex, std::unique_lock<std::mutex> &&waitOn, std::condition_variable &cv) :
    _name(std::move(name)), _mutex(mutex), _waitOn(waitOn), _terminated(terminated), _cv(cv), _encdec(){
        _connectionHandler = connectionHandler;
    }

    void run(){
        while(true) { ;
            const short bufsize = 1024;
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            std::string toSend = _encdec.encode(line);
            std::cout << "Encoded message: " << toSend << std::endl;
            unsigned long len=toSend.length();
            if (!_connectionHandler -> sendLine(toSend)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
            if(line == "LOGOUT "){
                _mutex.lock();
                _terminated = true;
                _mutex.unlock();
                _cv.wait(_waitOn);
                break;
            }
            // connectionHandler.sendLine(line) appends '\n' to the message. Therefor we send len+1 bytes.
            std::cout << "Sent " << len+1 << " bytes to server" << std::endl;
        }
        std::this_thread::yield();
    }
};



int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: BGSclient <host> <port>" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    std::cout << "Connected" << std::endl;
    bool ter = false;
    std::mutex mutex;
    std::mutex notify;
    std::unique_lock<std::mutex> lck(notify);
    std::condition_variable cv;
    UserReadTask task1("user", &connectionHandler, ter, mutex, std::move(lck), cv);
    SocketReadTask task2("socket", &connectionHandler, ter, mutex, cv);
    std::thread th1(&UserReadTask::run, &task1);
    std::thread th2(&SocketReadTask::run, &task2);

    th1.join();
    th2.join();
    return 0;
}

