package mynd.search;

import java.util.LinkedHashSet;
import java.util.Set;

import luna.search.AOStarSearch;
import mynd.explicit.ExplicitState;
import mynd.heuristic.ZeroHeuristic;
import mynd.state.State;

/**
 * Node in the AO* search space.
 * 
 * @author Robert Mattmueller
 */
public class AOStarNode extends AbstractNode implements Comparable<AOStarNode>
{

	/**
	 * Heuristic value of this node
	 */
	int heuristic;

	/**
	 * Cost estimate of this node
	 */
	private double costEstimate;

	/**
	 * Weak goal distance of this node.
	 */
	 public double weakGoalDistance;

	/**
	 * Incoming connectors
	 */
	public Set<Connector> incomingConnectors;

	/**
	 * Outgoing connectors
	 */
	public Set<Connector> outgoingConnectors;

	/**
	 * The outgoing connector currently marked.
	 */
	public Connector markedConnector = null;

	/**
	 * Creates a new node for a given state.
	 * 
	 * @param state
	 *            State to be represented
	 * @param manager
	 *            The AOStar search instance controlling this node
	 */
	public AOStarNode(State state, AOStarSearch searchManager, int depth)
	{
		super(state, depth);
		if (searchManager.estimator == null)
		{
			searchManager.estimator = new ZeroHeuristic();
		}
		heuristic = (int) searchManager.estimator.getHeuristic(state);
		setCostEstimate(heuristic);
		isProven = state.isGoalState();
		isGoalNode = state.isGoalState();
		if (isGoalNode)
		{
			weakGoalDistance = 0;
		} else
		{
			weakGoalDistance = Double.POSITIVE_INFINITY;
		}
		// FIXME: Assumption that dead ends are reliable
		isDisproven = getCostEstimate() == AbstractNode.DISPROVEN;

		incomingConnectors = new LinkedHashSet<Connector>();
		outgoingConnectors = new LinkedHashSet<Connector>();
	}

	@Override
	public String toString()
	{
		if (this.state instanceof ExplicitState)
		{
			return "AOStartNode: (index: " + index + ", depth: " + getDepth()
					+ ", h-value: " + heuristic + ", cost: " + getCostEstimate()
					+ "), " + "state: " + ((ExplicitState) state).toStringWithPositivePropositionNames();
		} else
		{
			return "AOStartNode: (index: " + index + ", depth: " + getDepth()
					+ ", h-value: " + heuristic + ", cost: " + getCostEstimate()
					+ "), " +  "state: " + state.toString();
		}

	}

	public int getHeuristic()
	{
		return heuristic;
	}

	/**
	 * Prefer nodes with higher h-value.
	 */
	public int compareTo(AOStarNode o)
	{
		return o.heuristic - heuristic;
	}

	/**
	 * Get marked connector of this AO* node.
	 * 
	 * @return marked connector.
	 */
	public Connector getMarkedConnector()
	{
		return markedConnector;
	}

	public void setMarkedConnector(Connector c)
	{
		this.markedConnector = c;
	}

	/**
	 * Get incoming connectors of this AO* node.
	 * 
	 * @return
	 */
	public Set<Connector> getIncomingConnectors()
	{
		return incomingConnectors;
	}

	/**
	 * Get outgoing connectors of this AO* node.
	 * 
	 * @return
	 */
	public Set<Connector> getOutgoingConnectors()
	{
		return outgoingConnectors;
	}

	public double getCostEstimate()
	{
		return costEstimate;
	}

	public void setCostEstimate(double costEstimate)
	{
		this.costEstimate = costEstimate;
	}
}
