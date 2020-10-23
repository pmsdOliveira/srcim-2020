package Transport;

import jade.core.Agent;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import Libraries.ITransport;
import Resource.ResourceAgent;
import Utilities.Constants;
import Utilities.DFInteraction;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public class TransportAgent extends Agent {

    String id;
    ITransport myLib;
    String description;
    String[] associatedSkills;

    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        this.id = (String) args[0];
        this.description = (String) args[1];

        //Load hw lib
        try {
            String className = "Libraries." + (String) args[2];
            Class cls = Class.forName(className);
            Object instance;
            instance = cls.newInstance();
            myLib = (ITransport) instance;
            System.out.println(instance);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(TransportAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        myLib.init(this);
        this.associatedSkills = myLib.getSkills();
        System.out.println("Transport Deployed: " + this.id + " Executes: " + Arrays.toString(associatedSkills));

        // TO DO: Register in DF
        try {
            DFInteraction.RegisterInDF(this, this.associatedSkills, Constants.DFSERVICE_RESOURCE);
        } catch (FIPAException e) {
            System.out.println("Error registering " + this.id + " in DF");
        }
        
        // TO DO: Add responder behaviour/s
        this.addBehaviour(new REResponder(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }
    
    private class REResponder extends AchieveREResponder {
       public REResponder(Agent a, MessageTemplate mt) {
           super(a, mt);
       } 
       
       @Override
       protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
           System.out.println("*** LOG: " + myAgent.getLocalName() + " received REQUEST from " + request.getSender().getLocalName());
           ACLMessage agree = request.createReply();
           agree.setPerformative(ACLMessage.AGREE);
           System.out.println("*** LOG: " + myAgent.getLocalName() + " sent AGREE to " + request.getSender().getLocalName());
           return agree;
       }
       
       @Override
       protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
           myLib.executeMove("Nowhere", "Operator");
           ACLMessage inform = request.createReply();
           inform.setPerformative(ACLMessage.INFORM);
           System.out.println("*** LOG: " + myAgent.getLocalName() + " sent INFORM to " + request.getSender().getLocalName());
           return inform;
       }
    }
    
}
