CFLAGS:=-c -Wall -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system

all: BGSclient
	g++ -pthread -o bin/BGSclient bin/ConnectionHandler.o bin/BGSclient.o bin/BgsEncoderDecoder.o $(LDFLAGS) 
	
BGSclient: bin/ConnectionHandler.o bin/BGSclient.o bin/BgsEncoderDecoder.o
	
bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/BGSclient.o: src/BGSclient.cpp
	g++ $(CFLAGS) -o bin/BGSclient.o src/BGSclient.cpp
	
bin/BgsEncoderDecoder.o: src/BgsEncoderDecoder.cpp
	g++ $(CFLAGS) -o bin/BgsEncoderDecoder.o src/BgsEncoderDecoder.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
