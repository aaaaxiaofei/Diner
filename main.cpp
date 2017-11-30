#include <iostream>
#include "Diner.hpp"
#include <pthread.h>

using namespace std;

void arrive(int id) {
	cout << "a new Diner " << id << " arrive" << endl;
}

int main() {

	cout << "hello cse6431" << endl;

	pthread_t t1(arrive,0);
	pthread_t t2(arrive,1);

	t1.join(); 
	t2.join();

	Diner d1(1);
	Diner d2(2);
	Diner d3(4);
	Diner d4(10);

	cout << "arrived at " << d.getArrivalTime() << endl;



	return 0;
}