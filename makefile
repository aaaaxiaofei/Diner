main: main.o 
	g++ -o main main.o -std=c++0x

main.o: main.cpp Diner.hpp
	g++ -c main.cpp -std=c++0x

clean: 
	rm main main.o
