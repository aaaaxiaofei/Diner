#ifndef DINER_HPP
#define DINER_HPP

class Diner {

private:
	int arrivalTime;

public:
	Diner();
	Diner(int t) : arrivalTime(t) {};

	int getArrivalTime() {
		return arrivalTime;
	}
};


#endif