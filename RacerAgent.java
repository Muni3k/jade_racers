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

public class RacerAgent extends Agent {
	private int x;
	private int y;
    
    private int lap;
		
	private int maxX;
	private int maxY;
    
	private AID[] mapAgents;

	// Put agent initializations here
	protected void setup() {
		x = 0;
		y = 0;
        lap = 1;
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
					myAgent.addBehaviour(new sendAskForSizeOfMap());
					myAgent.addBehaviour(new getSizeOfMap());
					myAgent.addBehaviour(new makeMove());
					myAgent.addBehaviour(new sendPos());
										
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
	}
	
	private class makeMove extends Behaviour {
		private AID map;
		private MessageTemplate mt;
		private int step = 0;
		
		private int newX;
		private int newY;
        private int newPosition;
        
		public void action() {
			switch (step) {
			case 0: //send CFP
				Random r = new Random();
                newX = r.nextInt(((x+1) - (x-1))) + x; //;r.nextInt(((x+1) - (x-1)) + 1) + (x-1);
				newY = r.nextInt(((y+1) - (y-1))) + y; //r.nextInt(((y+1) - (y-1)) + 1) + (y-1);
                if(newX >= maxX) { newX = maxX-1; }
                if(newY >= maxY) { newY = maxY-1; }
                if(newX < 0) { newX = 0; }
                if(newY < 0) { newY = 0; }
				
				System.out.println("newx: " + newX + " newy: " + newY);
				// Send the cfp to all maps
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < mapAgents.length; ++i) {
					cfp.addReceiver(mapAgents[i]);
				} 
				cfp.setContent(newX + ":" + newY);
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
				        tempArray = reply.getContent().split(":");
						newPosition = Integer.parseInt(tempArray[0]);
                        maxX = Integer.parseInt(tempArray[1]);
                        maxY = Integer.parseInt(tempArray[2]);
					}
					step = 2; 
				}
            case 2:
                ACLMessage order;
                if (newPosition > 1) { //has to be 1 because all below are not 'drivable' fields
				    order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                } else {
                    order = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                }
                for (int i = 0; i < mapAgents.length; ++i) {
					order.addReceiver(mapAgents[i]);
				}
                order.setContent("rsp");
				order.setConversationId("racer-agent-move");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("racer-agent-move"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				reply = myAgent.receive(mt);
				if (reply != null) {
					if (reply.getPerformative() == ACLMessage.INFORM) {
                        x = newX;
                        y = newY;

                        if(x == maxX-1 && y == maxY-1) { lap++; x = 0; y = 0; }

                        try
                        {
                            TimeUnit.SECONDS.sleep(newPosition/10);
                        }
                        catch(InterruptedException ex)
                        {
                            Thread.currentThread().interrupt();
                        }
                        
					} else {
						System.out.println("ERROR");
					}
					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}
		
		public boolean done() {
			if(step == 4) {
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
				reply.setContent(x + ":" + y + ":" + lap);

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	
	private class sendAskForSizeOfMap extends OneShotBehaviour{
		public void action() {
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			for (int i = 0; i < mapAgents.length; ++i) {
				cfp.addReceiver(mapAgents[i]);
			} 
	
			cfp.setConversationId("map-agent-sieze-of-map");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);
		}
	}
	
	private class getSizeOfMap extends CyclicBehaviour{
		
		public void action() {
			// Prepare the template to get proposals
			MessageTemplate mt = MessageTemplate.MatchConversationId("map-agent-sieze-of-map");
			
			ACLMessage reply = myAgent.receive(mt);
			if (reply != null) {
				
				String[] tempArray;
				tempArray = reply.getContent().split(":");
				maxX = Integer.parseInt(tempArray[0]);
				maxY = Integer.parseInt(tempArray[1]);
			}
		}
	}
}