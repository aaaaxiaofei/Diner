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
	public void printInfo() {
		System.out.printf("%d - Diner %d's order is ready, Diner %d, start eating.\n", servedTime, dinerID, dinerID);
		// System.out.printf("%d - Diner %d arrives.\n", arrivalTime, dinerID);
		// System.out.printf("%d - Diner %d is seated at Table %d.\n", arrivalTime, dinerID, table.getNumber());
		// System.out.printf("%d - Cook %d processs Diner %d's order.\n", arrivalTime, table.getCook().getNumber(), dinerID);

		// System.out.printf("arrival: %d, seated: %d, table: %d, cook: %d \n",
		// 		arrivalTime, seatedTime, table.getNumber(), table.getCook().getNumber());
		// for (int i = 0; i < Restaurant.NUMBER_OF_FOOD_TYPES; ++i) {
		// 	System.out.printf(", %d", (order.getTimeForDishType(i) == null ? 0 : order.getTimeForDishType(i)));
		// }
		// System.out.printf(", served: %d\n", servedTime);
	}
	@Override
	public void run() {
		restaurant.enter(this);
		servedTime = table.getServedTime();
		printInfo();
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
