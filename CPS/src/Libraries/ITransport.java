package Libraries;

import jade.core.Agent;

/**
 *
 * @author Ricardo Silva Peres <ricardo.peres@uninova.pt>
 */
public interface ITransport {
    public void init(Agent a);
    public String[] getSkills();
    public boolean executeMove(String origin, String destination);
}
