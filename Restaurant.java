package restaurant;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;

public class Restaurant {
	public static int NUMBER_OF_FOOD_TYPES;
	
	private PriorityQueue<Diner> arrivedDiners;
	private AtomicInteger numberOfRemainDiners;

	private PriorityQueue<Table> tables;
	private BlockingQueue<Table> unassignedTables;
	
	private PriorityQueue<Cook> availableCooks;
	private final int numberOfCooks;
	private PriorityQueue<Cook> cookingCooks;
	private AtomicInteger numberOfPreparingOrders;
	
	private List<Machine> machines;

	public Map<Integer,String> message;

	public Restaurant(int numTables, int numCooks, List<Integer> arrival) {
		arrivedDiners = new PriorityQueue<Diner>();
		numberOfRemainDiners = new AtomicInteger(arrival.size());

		tables = new PriorityQueue<Table>(numTables);
		for (int i = 0; i < numTables; ++i)
			tables.add(new Table(i));
		unassignedTables = new ArrayBlockingQueue<Table>(numTables, true);

		availableCooks = new PriorityQueue<Cook>();
		numberOfCooks = numCooks;
		cookingCooks = new PriorityQueue<Cook>();
		numberOfPreparingOrders = new AtomicInteger(1);
		
		machines = new ArrayList<Machine>();

		message = new HashMap<Integer,String>(); 
	}
	// Initialize machine
	public void addMachine(Machine machine) {
		synchronized (machines) {
			machines.add(machine);
		}
	}

	// Diner enter into restaurant
	public void enter(Diner diner) {
		synchronized (arrivedDiners) {
			arrivedDiners.offer(diner);
			
			writeMessage(diner.getArrivalTime(), "Diner " + String.valueOf(diner.getID()) + " arrives.\n");
			arrivedDiners.notifyAll();
			try {
				while (arrivedDiners.size() < numberOfRemainDiners.get()-numberOfPreparingOrders.get()+1 ||
						diner != arrivedDiners.peek())
					arrivedDiners.wait();
				arrivedDiners.poll();
				Table table = tables.poll();
				diner.setTable(table);
				unassignedTables.put(table);

				writeMessage(diner.getSeatedTime(), "Diner " + String.valueOf(diner.getID()) + 
							" is seated at Table " + String.valueOf(table.getNumber()) + "\n");
				writeMessage(diner.getSeatedTime(), "Cook " + String.valueOf(table.getCook().getNumber()) + 
							 " processes Diner " + String.valueOf(diner.getID()) + "\'s order.\n");
				// writeMessage(diner.getArrivalTime(), "Diner " + String.valueOf(diner.getID()) + " is seated.");
			} catch (InterruptedException e) {}
		}
	}

	// Diner start eating.
	public void startEating(Diner diner) {
		String dinerID = String.valueOf(diner.getID());
		writeMessage(diner.getServedTime(), "Diner " + dinerID + "\'s order is ready, Diner " + dinerID + " start eating.\n");
	}

	// Diner leaves the restaurant.
	public void leave(Diner diner) {
		Table table = diner.getTable();
		diner.setTable(null);
		table.setCurrentTime(diner.getFinishedTime());
		
		synchronized (tables) {
			tables.add(table);
		}
		String dinerID = String.valueOf(diner.getID());
		writeMessage(table.getCurrentTime(), "Diner " + dinerID + " finishes. Diner " + 
					 dinerID + " leaves the restaurant.\n");
					 
		synchronized (arrivedDiners) {
			if (numberOfRemainDiners.decrementAndGet() == 0) {
				writeMessage(table.getCurrentTime(), "The last diner leaves the restaurant.\n");
				printInfo();
				System.exit(0);
			}
			arrivedDiners.notifyAll();
		}		
	}

	// Assign cook.
	public void assignCook(Cook cook) {
		try {
			synchronized (availableCooks) {
				availableCooks.add(cook);
				availableCooks.notifyAll();
				while (availableCooks.size() < numberOfCooks-numberOfPreparingOrders.get()+1 ||
						availableCooks.peek() != cook)
					availableCooks.wait();
				availableCooks.poll();
			}
			cook.setTable(unassignedTables.take());
		} catch (InterruptedException e) {}
	}

	// Cook prepare Order
	public void preparingOrder(Cook cook) {
		Machine machine = null;
		do {
			synchronized (cookingCooks) {
				cookingCooks.add(cook);
				cookingCooks.notifyAll();
				while (cookingCooks.size() < numberOfPreparingOrders.get() || cook != cookingCooks.peek())
					try {
						cookingCooks.wait();
					} catch (InterruptedException e) {}
				cookingCooks.poll();
			}
			// find the machine with earliest available time to prepare the remaining order
			machine = null;
			for (int i = 0; i < NUMBER_OF_FOOD_TYPES; ++i) {
				int amount = cook.getOrder().getOrderAmount(i);
				if (amount == 0) continue;
				if (machine == null)
					machine = machines.get(i);
				else {
					if (machine.getCurrentTime() > machines.get(i).getCurrentTime())
						machine = machines.get(i);
				}
			}
			if (machine != null) {				
				cook.prepare(machine);
				int currentTime = cook.getCurrentTime() - machine.getUnitDuration();
				writeMessage(currentTime, "Cook " + String.valueOf(cook.getNumber()) + 
							" uses the " + machine.getStringType() + " machine.\n");
				if (isNextOrderAvailable(cook)) {
					numberOfPreparingOrders.incrementAndGet();
					synchronized (arrivedDiners) {
						arrivedDiners.notifyAll();
					}
					synchronized (availableCooks) {
						availableCooks.notifyAll();
					}
				}			
			}
		} while (machine != null);
		if (numberOfPreparingOrders.get() > 1)
			numberOfPreparingOrders.decrementAndGet();
		synchronized (cookingCooks) {
			cookingCooks.notifyAll();
		}
	}

	private boolean isNextOrderAvailable(Cook cook) {
		Diner diner = arrivedDiners.peek();
		Cook nextCook = availableCooks.peek();
		Table table = tables.peek();

		if (diner == null || nextCook == null || table == null) return false;
		
		if (nextCook.getCurrentTime() < cook.getCurrentTime() &&
				diner.getArrivalTime() < cook.getCurrentTime() &&
				table.getCurrentTime() < cook.getCurrentTime()) {
			return true;
		}
		return false;
	}

	private String timeString(int number) {
		int hour = number / 60, minute = number % 60;
		String hourFmt = String.format("%02d", hour);
		String minuteFmt = String.format("%02d", minute);
		return hourFmt + ":" + minuteFmt;
	}

	// Output
	public void printInfo() {
		Writer writer = null;
		SortedSet<Integer> keys = new TreeSet<Integer>(message.keySet());
		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("output.txt"), "utf-8"));
		    for (int key : keys) { 
			   writer.write(message.get(key));
			}	    
		} catch (IOException ex) {
		  	System.exit(-1);
		} finally {
		   try {writer.close();} catch (Exception ex) {/*ignore*/}
		}
	}
	// Read input
	public static int readNonWhitespaceChar(BufferedReader reader) {
		int r = -1;
		try {
			r = reader.read();
			while (Character.isWhitespace(r))
				r = reader.read();
		} catch (IOException e) {
			System.exit(-1);
		}
		return r;
	}
	public static int readInt(BufferedReader reader) {
		char ch = (char) readNonWhitespaceChar(reader);
		int result = 0;
		int r = -1;
		if (ch != '-')
			result = Character.digit(ch, 10);
		try {
			r = reader.read();
			while (Character.isDigit(r)) {
				result = result*10 + Character.digit(r, 10);
				r = reader.read();
			}
			if (ch == '-')
				result *= -1;
		} catch (IOException e) {
			System.exit(-1);
		}
		return result;
	}
	public void writeMessage(int time, String log) {
		synchronized(message) {
			String existing = message.get(time);
			String pre = timeString(time) + " - ";
			String newMessage = existing == null ? pre + log : existing + pre + log;
			message.put(time, newMessage);
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BufferedReader reader = null;
		try {
			File inputFile = new File(args[0]);
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.exit(-1);
		}

		NUMBER_OF_FOOD_TYPES = 3;		
		int numDiners = readInt(reader);
		int numTables = readInt(reader);
		int numCooks = readInt(reader);
		List<Integer> arrival = new ArrayList<Integer>();
		List<Integer> burgers = new ArrayList<Integer>();
		List<Integer> fries = new ArrayList<Integer>();
		List<Integer> coke = new ArrayList<Integer>();
		for (int i = 0; i < numDiners; ++i) {
			arrival.add(readInt(reader));
			burgers.add(readInt(reader));
			fries.add(readInt(reader));
			coke.add(readInt(reader));
		}
		Restaurant restaurant = new Restaurant(numTables, numCooks, arrival);
		restaurant.addMachine(new Machine(0, 5));
		restaurant.addMachine(new Machine(1, 3));
		restaurant.addMachine(new Machine(2, 1));
		
		for (int i = 0; i < numCooks; ++i)
			(new Thread(new Cook(i, restaurant))).start();
		for (int i = 0; i < numDiners; ++i) {
			Order order = new Order(burgers.get(i), fries.get(i), coke.get(i));
			(new Thread(new Diner(i, arrival.get(i), order, restaurant))).start();
		}	
	}
}
