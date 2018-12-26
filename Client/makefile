CFLAGS:=-c -Wall -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system

all: BGSclient
	g++ -pthread -o bin/BGSclient bin/ConnectionHandler.o bin/BGSclient.o $(LDFLAGS) 
	
BGSclient: bin/ConnectionHandler.o bin/BGSclient.o
	
bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/BGSclient.o: src/BGSclient.cpp
	g++ $(CFLAGS) -o bin/BGSclient.o src/BGSclient.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
