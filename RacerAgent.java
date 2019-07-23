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

public class RacerAgent extends Agent {
	private int x;
	private int y;

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
	}  // End of inner class OfferRequestsServer

	// RACER - TRY TO MOVE TO NEXT FIELD (ASKS MAP)
		// IF POSSIBLE - MOVE
			// HAS TO CHECK POSSIBILITY TO MOVE (only on roads) - communication with map
			// HAS TO CHECK OTHER RACERS ON THAT FIELD (has to be empty) - communication with racers
				// HAS TO GIVE A WAY TO RACERS ON THE RIGHT SIDE - communication with racers
			// HAS TO TAKE TIME

}