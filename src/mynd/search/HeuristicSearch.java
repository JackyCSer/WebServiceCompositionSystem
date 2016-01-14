package mynd.search;

import mynd.heuristic.Heuristic;
import mynd.state.State;

/**
 * A heuristic search algorithm is an explicit state search algorithm guided by
 * a heuristic function.
 * 
 * @author Robert Mattmueller
 */
public abstract class HeuristicSearch extends AbstractSearch {

    /**
     * Heuristic estimator for the initialization of leaf nodes
     */
    protected Heuristic estimator;

    /**
     * Perform one iteration of the search algorithm, i.e. choice of nodes to
     * expand, expansion, and (recursive) update.
     * 
     */
    public abstract void doIteration();

    /**
     * Look up a state in the database of encountered states and return a node
     * encapsulating this state. Either return a node that already exists if the
     * state has been encountered before, or create a new node for the state.
     * 
     * @param state
     *            State for which to find a corresponding node.
     * @return The node corresponding to the given state.
     */
    public abstract AbstractNode lookupAndInsert(State state, int depth);

    /**
     * Initialize or set the heuristic estimator to be used during search.
     * 
     * @param estimator
     *            The heuristic estimator to be used during search
     */
    public void setEstimator(Heuristic estimator) {
        this.estimator = estimator;
    }
    

}
