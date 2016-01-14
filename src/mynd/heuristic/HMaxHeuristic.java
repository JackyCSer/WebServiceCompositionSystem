package mynd.heuristic;

import mynd.heuristic.FFHeuristic.RPGStrategy;
import mynd.state.State;

public class HMaxHeuristic extends Heuristic {
    
    FFHeuristic heuristic = new FFHeuristic(RPGStrategy.MAX);
    
    public HMaxHeuristic() {
        super(true);
    }

    @Override
    public double getHeuristic(State state) {
        return heuristic.getHeuristic(state);
    }

}
