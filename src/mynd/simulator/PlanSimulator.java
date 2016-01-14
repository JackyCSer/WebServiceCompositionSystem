package mynd.simulator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import mynd.Global;
import mynd.search.StateActionTable;
import mynd.state.Operator;
import mynd.state.State;

/**
 * 
 * @author Robert Mattmueller
 * 
 */
class Connector
{

	Node parent;
	Set<Node> children;
	String name;
	int unsatisfiedPreconditionCount;
	double cost;

	boolean isSafe = false;

	public Connector(Node parent, Set<Node> children, String name, double cost)
	{
		this.parent = parent;
		this.children = children;
		parent.outgoingConnectors.add(this);
		for (Node child : children)
		{
			child.incomingConnectors.add(this);
		}
		this.name = name;
		this.cost = cost;
		unsatisfiedPreconditionCount = children.size();
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Connector))
		{
			return false;
		}
		Connector c = (Connector) o;
		return parent.equals(c.parent) && children.equals(c.children);
	}

	@Override
	public int hashCode()
	{
		return parent.hashCode() + children.hashCode();
	}

}

class Node
{

	static final double UNINITIALIZED_COST_ESTIMATE = Double.POSITIVE_INFINITY;

	static int nextIndex = 0;

	State state;
	int index;
	double costEstimate;
	Set<Connector> incomingConnectors;
	Set<Connector> outgoingConnectors;
	boolean isProven = false;

	public Node(State state)
	{
		this.state = state;
		index = nextIndex++;
		costEstimate = Node.UNINITIALIZED_COST_ESTIMATE;
		if (state.isGoalState())
		{
			costEstimate = 0.0;
		}
		incomingConnectors = new LinkedHashSet<Connector>();
		outgoingConnectors = new LinkedHashSet<Connector>();
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Node))
		{
			return false;
		}
		return index == ((Node) o).index;
	}

	@Override
	public int hashCode()
	{
		return index;
	}

	/**
	 * Resets the index to 0. Necessary for running the planner more than one
	 * time in one program.
	 */
	static void resetIndex()
	{
		nextIndex = 0;
	}
}

public class PlanSimulator
{

	public static final double EPSILON = 0.0001;

	public double performValueIteration(StateActionTable policy)
	{
		// build state space
		Map<State, Node> seenStates = new LinkedHashMap<State, Node>();
		Queue<Node> queue = new LinkedList<Node>();
		Node initial = new Node(Global.problem.getSingleInitialState());
		seenStates.put(Global.problem.getSingleInitialState(), initial);
		queue.add(initial);

		while (!queue.isEmpty())
		{
			Node node = queue.poll();
			Set<Node> children = new LinkedHashSet<Node>();

			Operator op = policy.findMatchingOperator(node.state);
			Set<State> successorStates = node.state.apply(op);
			for (State successor : successorStates)
			{
				Node newNode = null;
				if (seenStates.containsKey(successor))
				{
					newNode = seenStates.get(successor);
				} else
				{
					newNode = new Node(successor);
					seenStates.put(successor, newNode);
					if (!successor.isGoalState())
					{
						queue.offer(newNode);
					}
				}
				children.add(newNode);
			}
			new Connector(node, children, op.getName(), op.getCost());
		}

		Collection<Node> nodes = seenStates.values();

		// initialize cost estimates
		for (Node node : nodes)
		{
			if (node != null)
			{
				node.costEstimate = 0.0;
			}
		}

		// auxiliary data structures
		double[] oldCostEstimate = new double[nodes.size()];

		// main loop
		boolean converged = false;
		do
		{
			// update
			for (Node node : nodes)
			{
				if (node != null)
				{
					oldCostEstimate[node.index] = node.costEstimate;
					if (!node.state.isGoalState())
					{
						node.costEstimate = Node.UNINITIALIZED_COST_ESTIMATE;
					}
				}
			}
			for (Node node : nodes)
			{
				if (node != null && !node.state.isGoalState())
				{
					for (Connector connector : node.outgoingConnectors)
					{
						double connectorValueMax = Double.NEGATIVE_INFINITY;
						double connectorValueSum = 0.0;
						for (Node child : connector.children)
						{
							double childEstimate = oldCostEstimate[child.index];
							if (childEstimate > connectorValueMax)
							{
								connectorValueMax = childEstimate;
							}
							connectorValueSum += childEstimate;
						}
						double connectorValueAvg = connectorValueSum
								/ connector.children.size();
						double connectorValue = 0.0;

						boolean useMax = false;
						if (useMax)
						{
							connectorValue = connectorValueMax;
						} else
						{
							connectorValue = connectorValueAvg;
						}

						if (connector.cost + connectorValue < node.costEstimate)
						{
							node.costEstimate = connector.cost + connectorValue;
						}
					}
				}
			}

			// convergence test
			converged = true;
			for (Node node : nodes)
			{
				if (node != null
						&& Math.abs(node.costEstimate
								- oldCostEstimate[node.index]) > EPSILON)
				{
					converged = false;
					break;
				}
			}
		} while (!converged);

		Node.resetIndex();
		return seenStates.get(Global.problem.getSingleInitialState()).costEstimate;
	}
}
