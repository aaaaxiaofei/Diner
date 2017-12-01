package restaurant;

public class Diner implements Runnable, Comparable<Diner> {
	private static final int EATING_DURATION = 30;
	private final int dinerID;
	private final int arrivalTime;
	private Order order;
	private Restaurant restaurant;
	
	private Table table;
	private int seatedTime;
	private int servedTime;
	
	public Diner(int id, int arrival, Order order, Restaurant restaurant) {
		dinerID = id + 1;
		arrivalTime = arrival;
		this.order = order;
		this.restaurant = restaurant;
	}
	public int getID() {
		return dinerID;
	}
	public int getArrivalTime() {
		return arrivalTime;
	}
	public Table getTable() {
		return table;
	}
	public int getSeatedTime() {
		return seatedTime;
	}
	public int getServedTime() {
		return servedTime;
	}
	public void setTable(Table table) {
		this.table = table;
		if (table == null) return;
		int time = this.table.getCurrentTime();
		if (time < arrivalTime) {
			seatedTime = arrivalTime;
			this.table.setCurrentTime(seatedTime);
		} else
			seatedTime = time;
		this.table.setOrder(order);
	}
	public int getFinishedTime() {
		return servedTime + EATING_DURATION;
	}
	@Override
	public void run() {
		restaurant.enter(this);
		servedTime = table.getServedTime();
		restaurant.startEating(this);
		restaurant.leave(this);
	}
	@Override
	public int compareTo(Diner arg0) {
		if (arrivalTime == arg0.arrivalTime)
			return 0;
		else if (arrivalTime < arg0.arrivalTime)
			return -1;
		else
			return 1;
	}
}
