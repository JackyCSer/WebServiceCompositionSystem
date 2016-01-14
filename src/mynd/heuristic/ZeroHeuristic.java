package mynd.heuristic;

import mynd.state.State;

/**
 * 
 * @author Robert Mattmueller
 *
 */
public class ZeroHeuristic extends Heuristic {
	
	public ZeroHeuristic() {
		super(true); // Zero heuristic supports axioms.
	}

    /**
     * Get heuristic value for given state.
     * 
     * @param state state to be evaluated by heuristic
     * @return heuristic value for given state
     */
    @Override
    public double getHeuristic(State state) {
        if (state.isGoalState()) {
            return 0.0;
        }
        else {
            return 1.0;
        }
    }

}