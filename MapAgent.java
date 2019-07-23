package examples.WYSCIGI;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class MapAgent extends Agent {
	// array of ints that depicts map
	private int[][] map;
	// 0 - void
	// 5 - low quality road
	// 7 - mid quality road
	// 9 - high quality road
	// 1 - roadworks

	// Put agent initializations here
	protected void setup() {
		// Create the map
		map = new int[][] {
	  		{ 9, 9, 9, 9, 9, 9, 9, 9, 9, 0 },
	  		{ 0, 0, 0, 0, 0, 0, 0, 0, 9, 0 },
	  		{ 0, 0, 0, 0, 0, 0, 0, 0, 9, 0 },
	  		{ 0, 0, 0, 0, 0, 0, 0, 0, 9, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 9, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0, 9, 0 },
			{ 0, 0, 0, 0, 0, 0, 9, 9, 9, 0 },
			{ 0, 0, 0, 0, 0, 0, 9, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 9, 0, 0, 0 },
	  		{ 0, 0, 0, 0, 0, 0, 9, 9, 9, 9 }
		};

		// Register the Map in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("map-agent");
		sd.setName("JADE-RACE");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviour showing map
		addBehaviour(new TickerBehaviour(this, 1*1000) {
			protected void onTick() {
				for (int i = 0; i < map.length; i++) {
					System.out.println(Arrays.toString(map[i]));
				}
				System.out.println("---");
			}
		} );
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Map Agent "+getAID().getName()+" terminating.");
	}
/*
	public void checkMapPos(final int x, final int y) {
		addBehaviour(new OneShotBehaviour() {
			public int action() {
				return map[y][x];
			}
		} );
	}

	public void makeMove(final int x, final int y) {
		addBehaviour(new OneShotBehaviour() {
			public int action() {
				if(map[y][x] == 9) { return 10; } //time in ms to drive there
				else if(map[y][x] == 7) { return 20; }
				else if(map[y][x] == 5) { return 30; }
				else { return -1; }

			}
		} );
	}
*/
}