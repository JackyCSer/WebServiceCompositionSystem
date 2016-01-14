package mynd.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import luna.search.AOStarSearch;
import mynd.Global;
import mynd.MyNDPlanner;
import mynd.heuristic.Heuristic;
import mynd.heuristic.pdb.AbstractCostComputation;
import mynd.symbolic.BeliefState;
import mynd.util.Pair;

/**
 * An LAO* search engine.
 * 
 * @author Robert Mattmueller
 */
public class LAOStarSearch extends AOStarSearch
{

	/**
	 * Set true for debug information output.
	 */
	public static boolean DEBUG = Global.SEARCH_DEBUG;;

	/**
	 * Create an LAO* search manager using a given heuristic estimator.
	 * 
	 * @param estimator
	 *            Heuristic estimator used for leaf nodes
	 */
	public LAOStarSearch()
	{
		super();
	}

	private Pair<Map<Connector, Integer>, Set<Connector>> computeBackwardReachableConnectors()
	{
		Set<Connector> connectorsAtCurrentDistance = new LinkedHashSet<Connector>();
		Set<Connector> backwardReachableConnectors = new LinkedHashSet<Connector>();

		Map<Connector, Integer> distanceMap = new LinkedHashMap<Connector, Integer>();
		int currentDistance = 1;

		Set<AOStarNode> goalNodes = new LinkedHashSet<AOStarNode>();
		for (AOStarNode node : getStateNodeMap().values())
		{
			if (node.isGoalNode)
			{
				goalNodes.add(node);
				for (Connector c : node.incomingConnectors)
				{
					if (!c.parent.isGoalNode && c.isSafe())
					{
						connectorsAtCurrentDistance.add(c);
						backwardReachableConnectors.add(c);
						distanceMap.put(c, currentDistance);
					}
				}
			}
		}

		while (!connectorsAtCurrentDistance.isEmpty())
		{
			currentDistance++;

			Set<Connector> newlyFoundConnectors = new LinkedHashSet<Connector>();

			for (Connector c : connectorsAtCurrentDistance)
			{
				for (Connector d : c.parent.incomingConnectors)
				{
					if (!backwardReachableConnectors.contains(d)
							&& !d.parent.isGoalNode && d.isSafe())
					{
						newlyFoundConnectors.add(d);
						backwardReachableConnectors.add(d);
						distanceMap.put(d, currentDistance);
					}
				}
			}

			connectorsAtCurrentDistance = newlyFoundConnectors;
		}

		for (AOStarNode node : getStateNodeMap().values())
		{
			if (node != null)
			{
				for (Connector c : node.outgoingConnectors)
				{
					c.setSafe(backwardReachableConnectors.contains(c));
				}
			}
		}

		return new Pair<Map<Connector, Integer>, Set<Connector>>(distanceMap,
				backwardReachableConnectors);
	}

	private void computeWeakDiscretePlanSteps(AOStarNode[] nodes)
	{
		if (DEBUG)
		{
			System.out.println("computeWeakDiscretePlanSteps of " + nodes);
		}

		Queue<AOStarNode> queue = new LinkedList<AOStarNode>();
		// Set<AOStarNode> seen = new HashSet<AOStarNode>(); // FIXME Do we need
		// a closed list here?

		// Compute weak goal distances.
		for (AOStarNode node : nodes)
		{
			boolean weakGoalDistanceReduced = false;
			for (Connector c : node.outgoingConnectors)
			{
				for (AOStarNode child : c.children)
				{
					if (child.weakGoalDistance + 1.0 < node.weakGoalDistance)
					{
						node.weakGoalDistance = child.weakGoalDistance
								+ c.baseCost;
						weakGoalDistanceReduced = true;
					}
				}
			}
			if (weakGoalDistanceReduced)
			{
				assert !queue.contains(node);
				queue.add(node);
			}
		}

		// Update parents
		while (!queue.isEmpty())
		{
			AOStarNode next = queue.poll();
			for (Connector connector : next.incomingConnectors)
			{
				AOStarNode parent = connector.parent;
				if (next.weakGoalDistance + 1.0 < parent.weakGoalDistance)
				{
					parent.weakGoalDistance = next.weakGoalDistance + 1;
					if (!queue.contains(parent))
					{
						queue.add(parent);
					}
				}
			}
		}
	}

	private boolean connectorIsPromising(Connector connector)
	{
		for (AOStarNode child : connector.children)
		{
			if (child.weakGoalDistance < connector.parent.weakGoalDistance)
			{
				return true;
			}
		}
		return false;
	}

	private int deleteUnprovenConnectors(
			Set<Connector> backwardReachableConnectors)
	{
		Set<AOStarNode> goalNodes = new LinkedHashSet<AOStarNode>();
		for (AOStarNode node : getStateNodeMap().values())
		{
			if (node != null)
			{
				if (node.isGoalNode)
				{
					goalNodes.add(node);
				}
			}
		}

		Queue<Connector> deleteQueue = new LinkedList<Connector>();

		Map<AOStarNode, Integer> counter = new LinkedHashMap<AOStarNode, Integer>();

		Set<AOStarNode> candidateParents = new LinkedHashSet<AOStarNode>();
		for (Connector c : backwardReachableConnectors)
		{
			candidateParents.add(c.parent);
		}

		for (AOStarNode parent : candidateParents)
		{
			counter.put(parent, 0);
		}

		for (Connector c : backwardReachableConnectors)
		{
			counter.put(c.parent, counter.get(c.parent) + 1);
		}

		for (Connector c : backwardReachableConnectors)
		{
			for (AOStarNode child : c.children)
			{
				if (!goalNodes.contains(child)
						&& !candidateParents.contains(child))
				{
					deleteQueue.add(c);
					break;
				}
			}
		}

		while (!deleteQueue.isEmpty())
		{
			Connector c = deleteQueue.poll();
			c.setSafe(false);

			AOStarNode parent = c.parent;
			if (counter.containsKey(parent))
			{
				counter.put(parent, counter.get(parent) - 1);
				if (counter.get(parent) == 0)
				{
					for (Connector d : parent.incomingConnectors)
					{
						deleteQueue.add(d);
					}
				}
			}
		}

		int num_safe = 0;

		for (AOStarNode node : getStateNodeMap().values())
		{
			for (Connector c : node.outgoingConnectors)
			{
				if (c.isSafe())
				{
					num_safe++;
				}
			}
		}

		return num_safe;
	}

	private void performValueIteration(Collection<AOStarNode> allNodes)
	{
		if (DEBUG)
		{
			System.out.println("performValueIteration on");
			System.out.println(allNodes);
		}
		// Auxiliary data structures.
		double[] oldCostEstimate = new double[AbstractNode.numberOfNodes()];
		Arrays.fill(oldCostEstimate, -1);
		// Connector[] oldMarkedConnectors = new
		// Connector[AbstractNode.numberOfNodes()];

		// Initialization.
		for (AOStarNode node : allNodes)
		{
			if (node.isProven)
			{
				// if (node.isGoalNode) {
				node.setCostEstimate(0.0); // TODO: Is this correct or if
											// (node.isGoalNode())
			} else if (node.isDisproven)
			{
				node.setCostEstimate(Heuristic.INFINITE_HEURISTIC);
			} else
			{
				node.setCostEstimate(node.heuristic);
			}
		}

		// main loop
		boolean converged = false;
		do
		{
			// update
			for (AOStarNode node : allNodes)
			{
				oldCostEstimate[node.index] = node.getCostEstimate();
				// oldMarkedConnectors[node.index] = node.markedConnector; //
				// TODO use for check if connectors changed?
			}
			for (AOStarNode node : allNodes)
			{
				if (!node.isProven)
				{
					node.markedConnector = null;

					for (Connector connector : node.outgoingConnectors)
					{
						// A connector is promising only if it has at least one
						// child such that the shortest path from that child to
						// a
						// goal is strictly shorter than the shortest path from
						// the parent of the connector to a goal.
						// TODO: assert that at least one outgoing connector of
						// each expanded node is promising. otherwise the node
						// should be marked as losing.
						if (connectorIsPromising(connector))
						{
							markBestConnector(connector, node, oldCostEstimate);
						}
					}

					if (node.markedConnector == null
							&& !node.outgoingConnectors.isEmpty())
					{
						for (Connector connector : node.outgoingConnectors)
						{
							markBestConnector(connector, node, oldCostEstimate);
						}
					}

					if (node.markedConnector == null)
					{
						// No outgoing connector or only connectors where at
						// least one child is a dead end.
						node.isDisproven = true;
						node.setCostEstimate(Heuristic.INFINITE_HEURISTIC);
						if (node == initialNode)
						{
							if (DEBUG)
							{
								dumpStateSpace();
							}
							break;
						}
					}
				}
			}

			// convergence test
			converged = true;

			for (AOStarNode node : allNodes)
			{
				if (Math.abs(node.getCostEstimate()
						- oldCostEstimate[node.index]) > AOStarSearch.EPSILON)
				{
					converged = false;
					break;
				}
			}

		} while (!converged);
	}

	public void markBestConnector(Connector connector, AOStarNode node,
			double[] oldCostEstimate)
	{
		double avgChildEstimate = 0;
		double maxChildEstimate = 0;

		assert connector.children.size() != 0;
		double cardinalityOfChildWorldStates = 0;
		for (AOStarNode child : connector.children)
		{
			if (child.isDisproven)
			{
				return; // Do not mark a connector which leads to a dead end.
			}

			double childEstimate = child.getCostEstimate();
			if (oldCostEstimate[child.index] != -1)
			{
				childEstimate = oldCostEstimate[child.index];
			}

			if (childEstimate > maxChildEstimate)
			{
				maxChildEstimate = childEstimate;
			}
			if (MyNDPlanner.weighBeliefStatesByCardinality
					&& node.state instanceof BeliefState)
			{
				double numWorldStates = ((BeliefState) child.state)
						.getNumberOfWorldStates();
				avgChildEstimate += numWorldStates * childEstimate;
				cardinalityOfChildWorldStates += numWorldStates;
			} else
			{
				avgChildEstimate += childEstimate;
			}
		}
		if (MyNDPlanner.weighBeliefStatesByCardinality
				&& node.state instanceof BeliefState)
		{
			avgChildEstimate /= cardinalityOfChildWorldStates;
		} else
		{
			avgChildEstimate /= connector.children.size();
		}

		boolean useMax = false; // Experiments show that it seems to be
								// preferable to average about child nodes.
		if (useMax)
		{
			if (node.markedConnector == null
					|| connector.baseCost
							+ AbstractCostComputation.DISCOUNT_FACTOR
							* maxChildEstimate < node.getCostEstimate())
			{
				node.setCostEstimate(connector.baseCost
						+ AbstractCostComputation.DISCOUNT_FACTOR
						* maxChildEstimate);
				node.markedConnector = connector;
			}
		} else
		{
			// TODO: Is discounting needed when averaging?
			if (node.markedConnector == null
					|| connector.baseCost
							+ AbstractCostComputation.DISCOUNT_FACTOR
							* avgChildEstimate < node.getCostEstimate())
			{
				node.setCostEstimate(connector.baseCost
						+ AbstractCostComputation.DISCOUNT_FACTOR
						* avgChildEstimate);
				node.markedConnector = connector;
			}
		}

	}

	/**
	 * Perform a complete run of the LAO* algorithm.
	 * 
	 * @return Indicator of result. <tt>AbstractSearch.PROTAGONIST_WINS</tt> if
	 *         the protagonist provably wins,
	 *         <tt>AbstractSearch.ANTAGONIST_WINS</tt> if the antagonist
	 *         provably wins, and <tt>AbstractSearch.TIMEOUT</tt> if time-out
	 *         occurred before proof.
	 */
	@Override
	public int run()
	{
		// Start measuring search time.
		starttime = System.currentTimeMillis();

		// Get initial state and insert it with depth 0.
		initialNode = lookupAndInsert(Global.problem.getSingleInitialState(), 0);

		// Search until initial node is proven or disproven or a timeout occurs.
		int i = 0;
		while (!initialNode.isProven && !initialNode.isDisproven && !timeout())
		{
			if (DEBUG)
			{
				System.out.println("Performing iteration " + i++
						+ " of LAO* algorithm.");
				System.out.println("  Number of nodes created: "
						+ getStateNodeMap().size());
			}
			doIterationForLAOStar();

			if (!initialNode.isProven && !initialNode.isDisproven)
			{
				unsafeAndProvenLabelling(getStateNodeMap().values());
			}
		}

		// Finish measuring search time.
		endtime = System.currentTimeMillis();

		String result = "LAO*: ";
		String resultType = "LAO*: ";
		if (initialNode.isProven)
		{
			result += "INITIAL IS PROVEN!";
			resultType += "Result: Strong cyclic plan found.";

			Global.problem.lunaPlannerOutput.setResult(result);
			Global.problem.lunaPlannerOutput.setResultType(resultType);

			System.err.println(result);
			// printStats();
			System.err.println(resultType);

			return AbstractSearch.PROTAGONIST_WINS;
		} else if (initialNode.isDisproven)
		{
			result += "INITIAL IS DISPROVEN!";
			resultType += "Result: No strong cyclic plan found.";

			Global.problem.lunaPlannerOutput.setResult(result);
			Global.problem.lunaPlannerOutput.setResultType(resultType);

			System.err.println(result);
			System.err.println(resultType);
			return AbstractSearch.ANTAGONIST_WINS;
		} else
		{
			result += "INITIAL IS UNPROVEN!";
			resultType += "Result: No strong cyclic plan found due to time-out.";

			Global.problem.lunaPlannerOutput.setResult(result);
			Global.problem.lunaPlannerOutput.setResultType(resultType);

			System.err.println(result);
			System.err.println(resultType);

			return AbstractSearch.TIMEOUT;
		}
	}

	// TODO: Do we have to check all nodes or only those which have been updated
	// before!
	// TODO: What about disproven labelling?
	protected void unsafeAndProvenLabelling(Collection<AOStarNode> nodes)
	{
		if (DEBUG)
		{
			System.out.println("Unsafe and proven labelling.");
		}
		int num_safe = 0;
		for (AOStarNode node : nodes)
		{
			for (Connector c : node.outgoingConnectors)
			{
				c.setSafe(true);
				num_safe++;
			}
		}
		int old_num_safe;
		Map<Connector, Integer> layers;
		do
		{
			old_num_safe = num_safe;

			// Backward reachable connectors.
			Pair<Map<Connector, Integer>, Set<Connector>> p = computeBackwardReachableConnectors();
			layers = p.first;
			Set<Connector> backwardReachable = p.second;

			// int intermediate_num_safe = 0;
			// for (AOStarNode node : nodes) {
			// for (Connector c : node.outgoingConnectors) {
			// if (c.isSafe) {
			// intermediate_num_safe++;
			// }
			// }
			// }

			// unprovable connectors
			num_safe = deleteUnprovenConnectors(backwardReachable);
		} while (num_safe != old_num_safe);

		LinkedList<AOStarNode> updatedNodes = new LinkedList<AOStarNode>();
		for (AOStarNode node : nodes)
		{
			Connector bestConnector = null;
			int bestDistance = -1;
			for (Connector c : node.outgoingConnectors)
			{
				if (c.isSafe())
				{
					if (node.isProven == false)
					{
						node.isProven = true;
						updatedNodes.add(node);
					}

					if (bestConnector == null || bestDistance > layers.get(c))
					{
						bestConnector = c;
						bestDistance = layers.get(c);
					}
				}
			}

			if (node.isProven)
			{
				// if (node.markedConnector != bestConnector) {
				// System.err.println("Marked connector changed in unsafeAndProvenLabelling.");
				// }
				node.markedConnector = bestConnector;
			}
		}
	}

	@Override
	protected void updateUntilFixpoint(AOStarNode[] nodes)
	{
		computeWeakDiscretePlanSteps(nodes);
		if (DEBUG)
		{
			System.out.println("Update until fixpoint.");
		}
		Queue<AOStarNode> queue = new LinkedList<AOStarNode>();
		Set<AOStarNode> seen = new LinkedHashSet<AOStarNode>();
		for (AOStarNode node : nodes)
		{
			queue.add(node);
		}

		// Compute set Z (= seen) of nodes to be updated
		while (!queue.isEmpty())
		{
			AOStarNode node = queue.poll();
			assert (!seen.contains(node));
			seen.add(node);
			for (Connector connector : node.incomingConnectors)
			{
				AOStarNode parent = connector.parent;
				if (connector != parent.markedConnector)
				{ // FIXME: If heuristic is not admissible, this is probably not
					// enough.
					continue;
				}
				if (!parent.isProven && !parent.isDisproven
						&& !seen.contains(parent) && !queue.contains(parent))
				{
					queue.offer(parent);
				}
			}
		}

		performValueIteration(seen);
	}
}
