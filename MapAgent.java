package examples.WYSCIGI;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class MapAgent extends Agent {
	private AID[] racerAgents;
	// array of ints that depicts map
	private int[][] map;
    //private int[][] map_const;
	private Map<String, List<Integer>> racersMap = new HashMap<>();
	// -1 - any racer (graphical: *)
	// 0 - void
	// 5 - high quality road
	// 7 - mid quality road
	// 9 - low quality road
	// 1 - roadworks
    private int maxX;
    private int maxY;
    private int laps;

	// Put agent initializations here
	protected void setup() {
	
		// Create the map
		/*map = new int[][] {
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
		};*/
		
		map = new int[][] {
	  		{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
	  		{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
	  		{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
	  		{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
			{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
			{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
			{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
			{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
			{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 },
	  		{ 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 }
		};
        /*map = new int[][] {
	  		{ 9, 5, 7 },
	  		{ 0, 0, 5 },
	  		{ 0, 0, 9 }
		};*/
        //map_const = map;
        laps = 3;
        maxX = map[0].length;
        maxY = map.length;

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

		// Add the behaviour of showing map
		addBehaviour(new TickerBehaviour(this, 1*1000) {
			protected void onTick() {
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("racer-agent");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template); 
					//System.out.println("Found the following racer agents:");
					racerAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						racerAgents[i] = result[i].getName();
						//System.out.println(racerAgents[i].getName());
					}
				}
				catch (FIPAException fe) {
					fe.printStackTrace();
				}
				myAgent.addBehaviour(new printMap());
				System.out.println("---");
			}
		} );
		
		addBehaviour(new CheckPosition());
        addBehaviour(new CheckPositionInform());
		addBehaviour(new sendSizeOfMap());
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
	}

	private class CheckPosition extends CyclicBehaviour {
		public void action() {

			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null && msg.getContent() != null) {
				// CFP Message received. Process it
				String position = msg.getContent();
				ACLMessage reply = msg.createReply();

				String[] tempArray;
				tempArray = position.split(":");
				
				int x = Integer.parseInt(tempArray[0]);
				int y = Integer.parseInt(tempArray[1]);
                                
				int roadType = map[y][x];
				
				reply.setPerformative(ACLMessage.PROPOSE);
				reply.setContent(Integer.toString(roadType) + ":" + Integer.toString(maxX) + ":" + Integer.toString(maxY));
				
				myAgent.send(reply);				
			}
			else {
				block();
			}
		}
	}
    
    private class CheckPositionInform extends CyclicBehaviour {
		public void action() {
            
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	
	private class printMap extends Behaviour {
        //int[][] map_copy = map_const;
		private int repliesCnt = 0; // The counter of replies from racers
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
                //map_copy = map_const;
				// Send the cfp to all racers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < racerAgents.length; ++i) {
					cfp.addReceiver(racerAgents[i]);
					//System.out.println("Sent CFP to " + racerAgents[i].getName());
				} 
				cfp.setContent("RACER-POSITION");
				cfp.setConversationId("racer-pos");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("racer-pos"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:      
				// Receive the position reply
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						int x = Integer.parseInt(reply.getContent().split(":")[0]);
						int y = Integer.parseInt(reply.getContent().split(":")[1]);
						
                        int lap = Integer.parseInt(reply.getContent().split(":")[2]);
                        if(laps <= lap) { System.out.println("Linie mety przekroczyl kierowca " + reply.getSender().getName()); }
												
						System.out.println("x: " + x + " y: " + y);
						List<Integer> position = new ArrayList<Integer>();
						position.add(y);
						position.add(x);
						racersMap.put(reply.getSender().getLocalName(), position);
					}
					else {
						//System.out.println("Attempt failed!");
					}
					repliesCnt++;
					if (repliesCnt >= racerAgents.length) {
						// We received all replies
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			int[][] map_copy = new int[map.length][];
			for (int i = 0; i < map.length; i++) {
				map_copy[i] = Arrays.copyOf(map[i], map[i].length);
			}

			for (Map.Entry<String, List<Integer>> entry : racersMap.entrySet()) {
				List<Integer> tmp = entry.getValue();
				int y = tmp.get(0);
				int x = tmp.get(1);
				
				map_copy[y][x] = -1;
			}
			
			if(step == 2) {
				for (int i = 0; i < map.length; i++) {
					String line = Arrays.toString(map_copy[i]);
					line = line.replace("-1", "*");
					System.out.println(line);
				}
				return true;
			}
			return false;
		}
	}
	
	private class sendSizeOfMap extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchConversationId("map-agent-sieze-of-map");
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String position = msg.getContent();
				ACLMessage reply = msg.createReply();

				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(Integer.toString(maxX) + ":" + Integer.toString(maxY));
				
				myAgent.send(reply);				
			}
			else {
				block();
			}
		}
	}
}