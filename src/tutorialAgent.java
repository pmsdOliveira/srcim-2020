import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;

import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.FSMBehaviour;

public class tutorialAgent extends Agent {
    @Override
    protected void setup() {
        this.addBehaviour(new mySimpleBehaviour(this));
        this.addBehaviour(new myOneShotBehaviour());
        this.addBehaviour(new myWakerBehaviour(this, 1000));
        this.addBehaviour(new myCyclicBehaviour());
        this.addBehaviour(new myTickerBehaviour(this, 2000));
        
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("tutorial");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(tutorialAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new simpleBeh(this, "SB1"));
        sb.addSubBehaviour(new simpleBeh(this, "SB2"));
        sb.addSubBehaviour(new simpleBeh(this, "SB3"));
        this.addBehaviour(sb);
        
        ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        pb.addSubBehaviour(new simpleBeh(this, "SB1"));
        pb.addSubBehaviour(new simpleBeh(this, "SB2"));
        pb.addSubBehaviour(new simpleBeh(this, "SB3"));
        this.addBehaviour(pb);
        
        FSMBehaviour fsmb = new FSMBehaviour();
        fsmb.registerFirstState(new simpleBeh(this, "A"), "St_A");
        fsmb.registerState(new simpleBeh(this, "B"), "St_B");
        fsmb.registerState(new simpleBeh(this, "C"), "St_C");
        fsmb.registerLastState(new simpleBeh(this, "D"), "St_D");
        fsmb.registerTransition("St_A", "St_B", 0);
        fsmb.registerTransition("St_B", "St_C", 1);
        fsmb.registerTransition("St_C", "St_D", 2);
        addBehaviour(fsmb);
    }
    
    /*
    *   O Simple Behaviour é responsável por executar uma determinada tarefa
    *   ciclicamente até que esta termine, como tal neste exemplo é pedido que
    *   se faça um Simple Behaviour que corra quatro vezes até terminar.
    */
    class mySimpleBehaviour extends SimpleBehaviour {
        int step = 0;
        
        public mySimpleBehaviour(Agent a) {
            super(a);
        }
        
        @Override
        public void action() {
            System.out.println(step);
            if (step++ == 3)
                finished = true;
        }
        
        private boolean finished = false;
        
        @Override
        public boolean done() {
            System.out.println("Terminei o Simple Behaviour");
            return finished;
        }
    }
    
    /*
    *   O One Shot Behaviour quando é adicionado é responsável por fazer uma
    *   tarefa uma única vez. Na prática é como se chamasse um Simple Behaviour 
    *   mas em que o método done() faz logo return true, terminando assim a
    *   execução logo na primeira chamada. Implemente no agente utilizado como 
    *   referência um behaviour deste género e verifique o resultado.
    */
    class myOneShotBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("OneShotBehaviour: Disparou o behaviour");
        }
    }
    
    /*
    *   O Waker Behaviour tem um comportamento idêntico ao anteriormente
    *   apresentado com a diferença que quando este é chamado recebe como
    *   parâmetro um valor do tipo long que indica o tempo (em milissegundos)
    *   de espera até que este comportamento seja disparado. Ou seja, se
    *   lançarmos um comportamento deste género com 1000 no parâmetro no
    *   construtor deste behaviour este ficar “adormecido” durante 1000
    *   milissegundos até ser disparado e correr uma vez.
    */
    class myWakerBehaviour extends WakerBehaviour {
        public myWakerBehaviour(Agent a, long timeout) {
            super(a, timeout);
            System.out.println("Start - " + System.currentTimeMillis());
        }
        
        @Override
        protected void onWake() {
            System.out.println("End - " + System.currentTimeMillis());
        }
    }
    
    /*
    *   Cada agente é uma Thread e como tal se durante a execução de um
    *   comportamento este demorar muito tempo a processar informação ou a
    *   realizar uma determinada tarefa, os restantes behaviours ficam
    *   bloqueados e o agente também “congelado”. Como tal os ciclos codificados
    *   dentro dos comportamentos devem ser implementados recorrendo a Cyclic
    *   Behaviours, que em cada chamada, correm uma iteração, ciclicamente.
    */
    class myCyclicBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            System.out.println("CyclicBehaviour: Disparou o behaviour");
        }
    }
    
    /*
    *   O Ticker Behaviour permite correr uma tarefa periodicamente, ou seja, à
    *   semelhança ao que acontece com o Cyclic Behaviour, este arranca e corre 
    *   ciclicamente desde a instanciação até que o agente deixa a plataforma.
    *   A diferença deste behaviour para o anterior é que existe uma janela de
    *   tempo entre execuções. Experimente a utilização deste behaviour com o
    *   seguinte código.
    */
    class myTickerBehaviour extends TickerBehaviour {
        public myTickerBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            System.out.println("TickerBehaviour: Disparou o behaviour");
        }
    }
    
    private class searchInDF extends OneShotBehaviour {
        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("tutorial");
            dfd.addServices(sd);
            DFAgentDescription[] result = null;
            try {
                result = DFService.search(myAgent, dfd);
            } catch (FIPAException ex) {
                Logger.getLogger(tutorialAgent.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println(result[0].getName());
        }
    }
    
    private class simpleBeh extends SimpleBehaviour {
        private boolean finished = false;
        int step = 0;
        String currentState;
        
        public simpleBeh(Agent a, String crtSta) {
            super(a);
            this.currentState = crtSta;
        }
        
        @Override
        public void action() {
            System.out.println("SimpleBehaviour: SubBehaviour: " + currentState + " - step: " + ++step);
            if (step == 3)
                finished = true;
        }
        
        @Override
        public boolean done() {
            return finished;
        }
        
        @Override
        public int onEnd() {
            switch(currentState) {
                case "A": return 0;
                case "B": return 1;
                case "C": return 2;
            }
            
            return -1;
        }
    }
}