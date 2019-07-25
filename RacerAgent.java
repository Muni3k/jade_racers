package examples.WYSCIGI;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import java.util.concurrent.TimeUnit;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

/*
TO DO:
- zczytywanie parametrow maxX i maxY od agenta mapy - jak KURWA przekazac zmienne maxX i maxY miedzy klasami?
- komunikacja miedzy kierowca a mapa musi byc w kolejnosci: CFP > PROPOSE > ACCEPT_PROPOSAL > INFORM
- ustepowanie innym kierowcom z prawej strony (uwzgledniac kierunek porszuszania sie?)
- definiowanie ilosci okrazen obok zmiennej mapy (implementacja liczenia okrazen oraz powrotu z mety na start)
- zdefiniowanie warunku stopu i pokazania wynikow wyscigu
*/

public class RacerAgent extends Agent {
	private int x;
	private int y;
	
	private int oldX;
	private int oldY;
	
	private int maxX;
	private int maxY;
    
	private int oldTypeRoad = 9;
	private AID[] mapAgents;

	// Put agent initializations here
	protected void setup() {
		x = 0;
		y = 0;
        maxX = 1;
        maxY = 1;

		// Register the Racer in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("racer-agent");
		sd.setName("JADE-RACE");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new sendPos());
		addBehaviour(new TickerBehaviour(this, 1*1000) {
			protected void onTick() {
					//System.out.println("Trying to find maps");
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("map-agent");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						//System.out.println("Found the following active maps:");
						mapAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							mapAgents[i] = result[i].getName();
							//System.out.println(mapAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new makeMove());
			}
		});
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
		System.out.println("Racer Agent "+getAID().getName()+" terminating.");
	}
	
	private class makeMove extends Behaviour {
		private AID map;
		private MessageTemplate mt;
		private int step = 0;
		
		private int newX;
		private int newY;
        
        private int maxX = 10; //temporary
	    private int maxY = 10; //temporary
		
		public void action() {
			switch (step) {
			case 0: //send CFP
				Random r = new Random();
                    
                System.out.println("maxx " + maxX);
                System.out.println("maxy " + maxY);
                    
                newX = r.nextInt(((x+1) - (x-1)) + 1) + (x-1);
				newY = r.nextInt(((y+1) - (y-1)) + 1) + (y-1);
                if(newX >= maxX) { newX = maxX-1; }
                if(newY >= maxY) { newY = maxY-1; }
                if(newX < 0) { newX = 0; }
                if(newY < 0) { newY = 0; }
				
				//System.out.println("newX: " + newX);
				//System.out.println("newY: " + newY);
				// Send the cfp to all maps
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < mapAgents.length; ++i) {
					cfp.addReceiver(mapAgents[i]);
				} 
				cfp.setContent(newX + "," + newY);
				cfp.setConversationId("racer-agent-move");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("racer-agent-move"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals from map agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
                        String[] tempArray;
				        tempArray = reply.getContent().split(",");
						int newPosition = Integer.parseInt(tempArray[0]);
						if (newPosition > 1) { //has to be 1 because all below are not 'drivable' fields
							oldX = x;
							oldY = y;
							oldTypeRoad = newPosition;
							x = newX;
							y = newY;
                            
                            try
                            {
                                TimeUnit.SECONDS.sleep(newPosition/10);
                            }
                            catch(InterruptedException ex)
                            {
                                Thread.currentThread().interrupt();
                            }
                            
                            maxX = Integer.parseInt(tempArray[1]);
                            maxY = Integer.parseInt(tempArray[2]);
						}
					}
					step = 2; 
				}
				else {
					block();
				}
				break;
			}        
		}
		
		public boolean done() {
			if(step == 2) {
				return true;
			}
			return false;
		}
	}
	
	private class sendPos extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// process it
				ACLMessage reply = msg.createReply();

				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(x + ":" + y + ":" + oldX + ":" + oldY + ":" + oldTypeRoad);
				//System.out.println(myAgent.getName() + " answered to agent " + msg.getSender().getName() + " with position (" + x + ";" + y + ")");

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
}