package Product;

import Utilities.DFInteraction;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import java.util.ArrayList;
import java.util.Vector;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class ProductAgent extends Agent {
    String id;
    ArrayList<String> executionPlan = new ArrayList<>();
    // TO DO: Add remaining attributes required for your implementation
    ACLMessage bestResource = null;
    boolean resourceReady = false, locationReached = false, skillDone = false;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.executionPlan = this.getExecutionList((String) args[1]);
        System.out.println("*** LOG: " + this.id + " requires: " + executionPlan);

        this.addBehaviour(new executePlanStep(this));
    }

    @Override
    protected void takeDown() {
        super.takeDown(); //To change body of generated methods, choose Tools | Templates.
    }

    private ArrayList<String> getExecutionList(String productType) {
        switch (productType) {
            case "A":
                return Utilities.Constants.PROD_A;
            case "B":
                return Utilities.Constants.PROD_B;
            case "C":
                return Utilities.Constants.PROD_C;
        }
        return null;
    }

    // *************************** FIPA REQUEST ********************************
    
    private class REInitiator extends AchieveREInitiator {
        public REInitiator(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            System.out.println("*** LOG: " + myAgent.getLocalName() + "received AGREE from " + agree.getSender().getLocalName());
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            System.out.println("*** LOG: " + myAgent.getLocalName() + " received INFORM from " + inform.getSender().getLocalName());
            if (!locationReached)
                locationReached = true;
            else
                skillDone = true;
        }
    }
    
    // ************************ FIPA CONTRACTNET *******************************

    private class CNInitiator extends ContractNetInitiator {
        public CNInitiator(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            ACLMessage bestPropose = (ACLMessage) responses.get(0);
            for (Object response : responses) {
                ACLMessage propose = (ACLMessage) response;
                if (propose.getPerformative() == ACLMessage.PROPOSE) {
                    System.out.println("*** LOG: " + myAgent.getLocalName() + " received PROPOSE from " + propose.getSender().getLocalName() + " with content = " + propose.getContent());
                    if (Integer.parseInt(propose.getContent()) < Integer.parseInt(bestPropose.getContent())) {
                        bestPropose = (ACLMessage) response;
                    }
                } else {
                    System.out.println("*** LOG: " + myAgent.getLocalName() + " received REFUSE from " + propose.getSender().getLocalName());
                }
            }
            
            bestResource = bestPropose;
            ACLMessage accept = bestPropose.createReply();
            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            acceptances.add(accept);
            System.out.println("*** LOG: " + myAgent.getLocalName() + " sent ACCEPT-PROPOSAL to " + bestPropose.getSender().getLocalName());
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            System.out.println("*** LOG: " + myAgent.getLocalName() + " received INFORM from " + inform.getSender().getLocalName());
            resourceReady = true;
        }
    }
    
    // ****************************** BEHAVIOUR ********************************

    private class executePlanStep extends SimpleBehaviour {
        public int step;
        private boolean finished;

        public executePlanStep(Agent a) {
            super(a);
            this.step = 0;
            this.finished = false;
        }

        @Override
        public void action() {
            DFAgentDescription[] resourceAgents = null;
            System.out.println("*** LOG: " + myAgent.getLocalName() + " looking for agents that can " + executionPlan.get(step));
            try {
                resourceAgents = DFInteraction.SearchInDFByName(executionPlan.get(step), myAgent);
            } catch (FIPAException e) {
                System.out.println("*** ERROR: " + myAgent.getLocalName() + " got and error searching for agent that can " + executionPlan.get(step));
            }
            
            if (resourceAgents != null) {
                System.out.println("*** LOG: " + myAgent.getLocalName() + " found " + resourceAgents.length + " agents that can " + executionPlan.get(step));
                
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (DFAgentDescription agent : resourceAgents) {
                    cfp.addReceiver(agent.getName());
                    System.out.println("*** LOG: " + myAgent.getLocalName() + " sent CFP to " + agent.getName().getLocalName());
                }
                myAgent.addBehaviour(new CNInitiator(myAgent, cfp));
                
                while (!resourceReady) {
                    try {
                        System.out.println("*** LOG: " + myAgent.getLocalName() + " waiting for INFORM");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("*** ERROR: " + myAgent.getLocalName() + " got an error while waiting for the resource to be ready");
                    }
                }
                
                DFAgentDescription[] transportAgent = null;
                System.out.println("*** LOG: " + myAgent.getLocalName() + " looking for a transport agent");
                try {
                    transportAgent = DFInteraction.SearchInDFByName("sk_move", myAgent);
                } catch (FIPAException e) {
                    System.out.println("*** ERROR: " + myAgent.getLocalName() + " got and error searching for a transport agent");
                }
                
                if (transportAgent != null) {
                    System.out.println("*** LOG: " + myAgent.getLocalName() + " found a transport agent");
                    
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(transportAgent[0].getName());
                    myAgent.addBehaviour(new REInitiator(myAgent, request));
                    
                    while (!locationReached) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            System.out.println("*** ERROR: " + myAgent.getLocalName() + " got an error while waiting for transport agent to reach location");
                        }
                    }
                    
                    request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(bestResource.getSender());
                    myAgent.addBehaviour(new REInitiator(myAgent, request));
                    
                    while (!skillDone) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            System.out.println("*** ERROR: " + myAgent.getLocalName() + " got an error while waiting for skill to be applied");
                        }
                    }
                    
                    bestResource = null;
                    resourceReady = false;
                    locationReached = false;
                    skillDone = false;
                    
                    if (step++ == executionPlan.size())
                        finished = true;
                    
                } else {
                    System.out.println("*** LOG: " + myAgent.getLocalName() + " didn't find any transport agent");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("*** ERROR: " + myAgent.getLocalName() + " got an error while waiting for new transport agent search");
                    }
                } 
            } else {
                System.out.println("*** LOG: " + myAgent.getLocalName() + " didn't find any agents that can " + executionPlan.get(step));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("*** ERROR: " + myAgent.getLocalName() + " got an error while waiting for new agent search");
                }
            }
        }

        @Override
        public boolean done() {
            return finished;
        }
    }
}
