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

public class RacerAgent extends Agent {
	private int x;
	private int y;
	private int maxX = 10;
	private int maxY = 10;
	private AID[] mapAgents;
	

	// Put agent initializations here
	protected void setup() {
		x = 0;
		y = 0;

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
					System.out.println("Trying to find maps");
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("map-agent");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the active maps");
						mapAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							mapAgents[i] = result[i].getName();
							System.out.println(mapAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
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
	
	private class RequestPerformer extends Behaviour {
		private AID map; // The agent who provides the best offer 
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		
		private int newX;
		private int newY;
		
		public void action() {
			switch (step) {
			case 0:
				Random r = new Random();
				newX = r.nextInt((maxX - x) + 1) + x;
				newY = r.nextInt((maxY - y) + 1) + y;
				
				System.out.println("newX: " + newX);
				System.out.println("newY: " + newY);
				// Send the cfp to all sellers
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
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					System.out.println("reply != null");
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int newPosition = Integer.parseInt(reply.getContent());
						if (newPosition > -1) {
							// This is the best offer at present
							x = newX;
							y = newY;
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
				reply.setContent(x + ":" + y);
				System.out.println(myAgent.getName() + " answered to agent " + msg.getSender().getName() + " with position (" + x + ";" + y + ")");

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}
	// TRY TO MOVE TO NEXT FIELD
		// IF POSSIBLE - MOVE
			// HAS TO CHECK POSSIBILITY TO MOVE (only on roads) - communication with map
			// HAS TO CHECK OTHER RACERS ON THAT FIELD (has to be empty) - communication with racers
			// HAS TO GIVE A WAY TO RACERS ON THE RIGHT SIDE - communication with racers
			// HAS TO TAKE TIME

}