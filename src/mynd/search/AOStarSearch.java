package mynd.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import luna.LunaGlobal;
import luna.web.model.LunaPlannerOutput;
import mynd.Global;
import mynd.MyNDPlanner;
import mynd.explicit.ExplicitState;
import mynd.simulator.PlanSimulator;
import mynd.state.Operator;
import mynd.state.State;
import mynd.util.AOStarNodeComparator;

/**
 * An AO* search engine working similarly as described in Nilsson (1980).
 * Aggregation of estimates is done by minimizing over OR choices and maximizing
 * over AND choices. // TODO Implemented as described?
 * 
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class AOStarSearch extends HeuristicSearch
{

	/**
	 * Expansion criteria.
	 */
	public enum ExpansionRules
	{
		MIN_H, MAX_H, MIN_DEPTH, MAX_DEPTH, OLDEST, NEWEST, RANDOM
	};

	/**
	 * Expansion strategies.
	 */
	public enum ExpansionStrategy
	{
		LINEAR, ALTERNATE
	};

	/**
	 * If no unexpanded node could be traced in partial best solution, which
	 * expansion strategy should be used. Linear means that always
	 * maxNumberOfNodesToExpandInOneStep nodes are selected by the same
	 * criteria. Alternate means that criteria are alternated.
	 */
	public static ExpansionStrategy expansionStrategy;

	/**
	 * Give criteria how unexpanded nodes should be selected from AND/OR-graph.
	 */
	public static ExpansionRules[][] expansionRules;

	/**
	 * Set this to true if you want to track the depth of a node.
	 */
	public static boolean depthIsRelevant = false;

	/**
	 * Epsilon (for double comparison)
	 */
	protected static final double EPSILON = 0.0001;

	/**
	 * Indicates that neither the cost estimate nor the proof status of a node
	 * was changed during an update step.
	 */
	private static final int UPDATED_NOTHING = 0;

	/**
	 * Indicates that the proof status (and thus also the cost estimate) of a
	 * node was changed during an update step.
	 */
	private static final int UPDATED_PROOF_STATUS = 1;

	/**
	 * Indicates that only the cost estimate, but not the proof status of a node
	 * was changed during an update step.
	 */
	private static final int UPDATED_COST_ESTIMATE = 2;

	/**
	 * Maximal number of nodes which is expanded in one Iteration. // FIXME: Is
	 * not used correctly!!!
	 */
	public static int maxNumberOfNodesToExpandInOneStep = 1;

	/**
	 * If no node to expand is found in a best partial solution graph, then
	 * expand rateOfNodesToExpandInOneStep of the unexpanded nodes (but not more
	 * than maxNumberOfNodesToExpandInOneStep).
	 */
	public static double rateOfNodesToExpandInOneStep = 1.0;

	/**
	 * Prefer sensing operator in POND-planning. Apply exactly one sensing
	 * operator, if one is applicable and leads to a splitting.
	 */
	public static boolean sensingFirst = false;

	/**
	 * As soon as a proven connector is found propagate it. Setting to false
	 * leads to much slower AO*, but to better, e.g. cheaper plans.
	 */
	public static boolean fastUpdate = true;

	/**
	 * Only apply one sensing op, which causes a splitting.
	 */
	public static boolean restrictSensingOps = false;

	/**
	 * Choose first sensing op, when using "restrictSensingOps".
	 */
	public static boolean useFirstSensingOp = false;

	/**
	 * Collect all operators which are only sensing, not causative.
	 */
	private List<Operator> sensingOps = new ArrayList<Operator>();

	/**
	 * Collect all operators, which are causative.
	 */
	private List<Operator> causativeOps = new ArrayList<Operator>();

	/**
	 * Mapping from unique hash value of world states to nodes in AND/OR graph
	 * representing them.
	 */
	protected Map<BigInteger, AOStarNode> stateNodeMap = new LinkedHashMap<BigInteger, AOStarNode>(
			10000);

	/**
	 * Queues of all created nodes still awaiting expansion.
	 */
	protected ArrayList<PriorityQueue<AOStarNode>> unexpandedNodes;

	/**
	 * Does anything changed during update after an expansion.
	 */
	protected boolean nothingChanged = false; // Only used for original
	// AO*-algorithm and not in
	// LAO*.
	// TODO [issue #26] support LAO*.

	/**
	 * If nothing changed while updating after an expansion, tracing could be
	 * improved by starting with lastExpandedNode.
	 */
	// protected AOStarNode lastExpandedNode = null; // Only used for original
	// AO*-algorithm and not in
	// LAO*.
	// TODO [issue #26] support LAO*.

	private StateActionTable explicitStateActionTable;

	private int alternatingIndex = 0;

	private int dumpingCounterPlan = 0;

	private int dumpingCounterStateSpace = 0;

	/**
	 * Set DEBUG true for more outputs.
	 */
	public static boolean DEBUG = Global.SEARCH_DEBUG;

	/**
	 * Create an AO* search manager using a given heuristic estimator.
	 * 
	 * @param problem
	 *            The problem to be solved
	 */
	public AOStarSearch()
	{
		// No expansion rules were set by options. Use following as default:
		if (expansionRules == null)
		{
			assert expansionStrategy == null;
			expansionStrategy = ExpansionStrategy.ALTERNATE;
			expansionRules = new ExpansionRules[2][];
			ExpansionRules alternative1[] = new ExpansionRules[2];
			alternative1[0] = ExpansionRules.MIN_H;
			alternative1[1] = ExpansionRules.OLDEST;
			ExpansionRules alternative2[] = new ExpansionRules[1];
			alternative2[0] = ExpansionRules.OLDEST;
			expansionRules[0] = alternative1;
			expansionRules[1] = alternative2;
		}
		unexpandedNodes = new ArrayList<PriorityQueue<AOStarNode>>(
				expansionRules.length);
		for (int i = 0; i < expansionRules.length; i++)
		{
			unexpandedNodes.add(new PriorityQueue<AOStarNode>(10000,
					new AOStarNodeComparator(i)));
		}
		if (!Global.problem.isFullObservable)
		{
			// Collect all operators which are sensing and are not causative.
			for (Operator op : Global.problem.getOperators())
			{
				if (op.isSensing && !op.isCausative)
				{
					sensingOps.add(op);
				} else
				{
					causativeOps.add(op);
				}
			}
		}
	}

	/**
	 * Perform one iteration of the AO* algorithm, i.e. choice of nodes to
	 * expand, expansion, and (recursive) update. Nodes of equal quality in the
	 * subgraph induced by the marked connectors are expanded simultaneously. If
	 * the subgraph induced by the marked connectors does not include any
	 * unexpanded nodes which are not proven/disproven yet, all unexpanded nodes
	 * not yet proven/disproven (outside the induced subgraph) are considered
	 * candidates for expansion.
	 */
	@Override
	public void doIteration()
	{
		if (DEBUG)
		{
			System.out.println();
			System.out.println("------------------------AOStarSearch.doIteration()------------------------");
			System.out.println("New iteration: AOStarSearch.doIteration()");
			System.out.println("Unexpanded nodes:");
			System.out.println(unexpandedNodes.get(0));

			if (Global.problem.isFullObservable)
			{
				dumpStateSpace();
				dumpPartialSolution();
			}
		}

		if (unexpandedNodes.get(0).isEmpty())
		{
			// There is no unexpanded node in the graph.
			initialNode.isDisproven = true;
		} else
		{
			// 1. Find nodes to expand.
			if (DEBUG)
			{
				System.out.println();
				System.out.println("1. Find nodes to expand. ");
			}

			AOStarNode[] nodesToExpand = nodesToExpand();
			if (timeout())
			{
				return;
			}
			assert nodesToExpand != null;

			// 2. Expand
			if (DEBUG)
			{
				System.out.println();
				System.out.println("2. Expand");
			}

			for (AOStarNode node : nodesToExpand)
			{
				expand(node);
				if (timeout())
				{
					return;
				}
			}
			assert nodesToExpand.length > 0;

			// 3. Update
			if (DEBUG)
			{
				System.out.println();
				System.out.println("3. Update");
			}

			updateUntilFixpoint(nodesToExpand);
		}
		
		if (DEBUG)
		{
			System.out.println("------------------------End AOStarSearch.doIteration()------------------------");
		}
	}

	@Override
	public void dumpPlan()
	{
		// printPolicyToFile
		printPolicyToFile();

		if (Global.problem.isFullObservable)
		{
			System.out.println(getExplicitStateActionTable().toStringPolicy());
			// printPolicyToFile();
			if (MyNDPlanner.runID == -1
					&& getExplicitStateActionTable().getNumberOfEntries() <= 120)
			{
				// Dump DOT representation of the plan if the planner runs
				// locally and there are at most 100 entries.
				// /*
				// dumpPartialSolution("plan_final_" +
				// MyNDPlanner.getNameOfProblemInstance());
				//
				String filePathname = LunaGlobal.projectRootPath
						+ LunaGlobal.plannerOutputFolderName + "final_plan";

				dumpPartialSolution(filePathname);

			}
			// getPlanAsDebugOutput();
		} else
		{
			getPlanAsDebugOutput();
		}
	}

	public void printPolicyToFile()
	{
		String id = "";
		if (MyNDPlanner.runID > -1)
		{
			id = Integer.toString(MyNDPlanner.runID) + "_";
		}

		String policyFilePathname = LunaGlobal.projectRootPath
				+ LunaGlobal.plannerOutputFolderName + "policy" + id + ".txt";

		File policyFile = new File(policyFilePathname);

		// MyNDPlanner.getNameOfProblemInstance()
		// + ".fond_plan");

		if (Global.problem.isFullObservable)
		{
			try
			{
				FileWriter writer = new FileWriter(policyFile);
				writer.write(getExplicitStateActionTable().toStringPolicy());
				writer.flush();
				writer.close();

				logger.debug(policyFile.getAbsolutePath());
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		} else
		{
			// getPlanAsDebugOutput();
		}
	}

	public void dumpStateSpace()
	{
		dumpStateSpace("explicit_graph_" + dumpingCounterStateSpace);
		dumpingCounterStateSpace++;
	}

	public void dumpStateSpace(String fileName)
	{
		String filePathname = LunaGlobal.projectRootPath
				+ LunaGlobal.plannerOutputFolderName + fileName + ".dot";

		File plan = new File(filePathname);

		logger.debug(plan.getAbsolutePath());
		try
		{
			FileWriter writer = new FileWriter(plan);
			writer.write(new GraphvizWriter(this).createOutput(true));
			writer.flush();
			writer.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void dumpPartialSolution()
	{
		String filePathname = LunaGlobal.projectRootPath
				+ LunaGlobal.plannerOutputFolderName + "best_partial_plan_"
				+ dumpingCounterPlan;

		dumpPartialSolution(filePathname);
		dumpingCounterPlan++;
	}

	public void dumpPartialSolution(String filename)
	{

		File plan = new File(filename + ".dot");

		try
		{
			FileWriter writer = new FileWriter(plan);
			writer.write(new GraphvizWriter(this).createOutput(false));
			writer.flush();
			writer.close();

			logger.debug(plan.getAbsolutePath());

		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Expands a given node by creating all outgoing connectors and the
	 * corresponding successor states. New states and connectors are installed
	 * in the explicit game graph representation.
	 * 
	 * @param node
	 *            Node to expand
	 */
	private void expand(AOStarNode node)
	{
		if (DEBUG)
		{
			System.out.println("--------------------Expand Node: "
					+ node.index + "--------------------");
			System.out.println("Node " + node + " is expaneded now!");
		}

		node.setExpanded();
		for (PriorityQueue<AOStarNode> queue : unexpandedNodes)
		{
			queue.remove(node); // O(n) - TODO: improve this!
		}
		if (!(node.isProven || node.isDisproven))
		{
			List<Operator> applicableOps;
			if (restrictSensingOps)
			{
				assert (!Global.problem.isFullObservable);
				// We apply only one sensing op to reduce branching.
				List<Operator> applicableSensingOps = node.state
						.getApplicableOps(sensingOps);
				if (!applicableSensingOps.isEmpty())
				{
					if (useFirstSensingOp)
					{
						// We use the first sensing op.
						applicableOps = new LinkedList<Operator>(
								Arrays.asList(applicableSensingOps.get(0)));
					} else
					{
						// We use a random sensing op.
						int random = Global.generator
								.nextInt(applicableSensingOps.size());
						applicableOps = new LinkedList<Operator>(
								Arrays.asList(applicableSensingOps.get(random)));
					}
					if (!sensingFirst)
					{
						// We also apply causative ops.
						applicableOps.addAll(node.state
								.getApplicableOps(causativeOps));
					}
				} else
				{
					// No applicable sensing op. Apply all causative actions.
					applicableOps = node.state.getApplicableOps(causativeOps);
				}
			} else
			{
				applicableOps = node.state.getApplicableOps(Global.problem
						.getOperators());
			}
			boolean hasSuccessor = false; // Successor which is different from
			// node.
			for (Operator op : applicableOps)
			{
				Set<State> successorStates = node.state.apply(op);
				
				// FIXME: for test:
				if (DEBUG)
				{
					
					System.out.println("AOStarSearch.expand() successorStates: ");
					int i = 1;
					for(State s: successorStates)
					{
						if (s instanceof ExplicitState)
						{
							System.out.println( "s" + i++ +": "+ ((ExplicitState) s).toStringWithPositivePropositionNames());
						}
						
					}
					System.out.println();
				}
				
				if (successorStates.isEmpty())
				{
					continue; // FIXME: ?
				}
				Set<AOStarNode> children = new LinkedHashSet<AOStarNode>();
				for (State successor : successorStates)
				{
					AOStarNode newNode = lookupAndInsert(successor,
							node.getDepth() + 1);
					if (newNode != node)
					{
						hasSuccessor = true;
					}
					children.add(newNode);
				}
				assert (!children.isEmpty());
				new Connector(node, children, op);
			}
			if (!hasSuccessor)
			{
				node.isDisproven = true; // Dead end.
			}
			// FIXME what if last expanded node was a dead end?
			// lastExpandedNode = node; // only used for original AO* algorithm
			NODE_EXPANSIONS++;
		} else
		{
			assert false;
		}

		if (DEBUG)
		{
			System.out
					.println("--------------------End Expand Node--------------------");
		}
	}

	private Set<AOStarNode> fillStateActionTable(StateActionTable table,
			AOStarNode node, Set<AOStarNode> seen)
	{
		if (!node.state.isGoalState())
		{
			// System.out.println("node: " + node);
			// System.out.println(node.state.equals(Global.problem.getSingleInitialState()));
			assert node.markedConnector.operator != null;
			// if
			// (Global.problem.getOriginalOperatorMap().get(node.markedConnector.operator.getName())
			// == null) {
			// System.out.println(node.markedConnector.operator.getName());
			// assert false;
			// }
			table.addEntry(node.state, node.markedConnector.operator);
			seen.add(node);
			for (AOStarNode child : node.markedConnector.children)
			{
				if (!seen.contains(child))
				{
					seen = fillStateActionTable(table, child, seen);
				}
			}
		}
		return seen;
	}

	/**
	 * Return a plan in the form of an explicit state-luna.action table. Don't
	 * call this during search (!).
	 * 
	 * @return Explicit state-action table representing the plan that was found.
	 */
	@Override
	public StateActionTable getExplicitStateActionTable()
	{
		if (explicitStateActionTable == null)
		{
			explicitStateActionTable = new StateActionTable();
			Set<AOStarNode> seen = new LinkedHashSet<AOStarNode>();
			fillStateActionTable(explicitStateActionTable,
					(AOStarNode) initialNode, seen);
		}
		return explicitStateActionTable;
	}

	/**
	 * Test whether a node for a given state has already been allocated or not.
	 * If there is already such a node, return it, otherwise create a new node
	 * for the given state and associate it with the state in the state-node
	 * mapping <tt>setOfAllNodes</tt>. Return the new node.
	 * 
	 * @param state
	 *            State to be represented by a node in the game graph
	 * @return The unique node corresponding to the given state, either newly
	 *         created or old.
	 */
	@Override
	public AOStarNode lookupAndInsert(State state, int depth)
	{
		AOStarNode node;
		if (!stateNodeMap.containsKey(state.uniqueID))
		{
			// This is a new node.
			node = new AOStarNode(state, this, depth);
			stateNodeMap.put(state.uniqueID, node);
			if (node.isProven || node.isDisproven)
			{
				node.setExpanded();
			} else
			{
				for (PriorityQueue<AOStarNode> queue : unexpandedNodes)
				{
					queue.add(node);
				}
			}
			if (DEBUG)
			{
				System.out.println("New node " + node);
			}
		} else
		{
			node = stateNodeMap.get(state.uniqueID);
			if (!state.equals(node.state))
			{
				assert false;
			}
			if (depthIsRelevant)
			{
				// depth = shortest path from root to the node
				if (node.getDepth() > depth)
				{
					node.setDepth(depth);
					if (!node.isExpanded())
					{ // FIXME: Avoid this if depth is not
						// used.
						for (PriorityQueue<AOStarNode> queue : unexpandedNodes)
						{
							queue.remove(node); // O(n)
							queue.add(node);
						}
					}
				}
			}
			if (DEBUG)
			{
				System.out.println("Known node " + node);
			}
		}
		return node;
	}

	/**
	 * Find nodes to expand.
	 * 
	 * @return A set of nodes which will be expanded in the next step.
	 */
	private AOStarNode[] nodesToExpand()
	{
		AOStarNode initial;
		// More efficient for AO*-algorithm. But only if exactly one node is
		// expanded. Not used for LAO*.
		// if (nothingChanged) {
		// initial = lastExpandedNode;
		// System.err.println("Nothing changed!");
		// }
		// else {
		initial = (AOStarNode) initialNode;
		// }
		PriorityQueue<AOStarNode> nodesToExpand = traceMarkedConnectors(initial);
		if (timeout())
		{
			return null;
		}
		AOStarNode[] bestNodes = null;
		if (!nodesToExpand.isEmpty())
		{ // Case 1: There is an unexpanded node
			// in current best solution graph.
			if (DEBUG)
			{
				System.out.println();
				System.out.println("	1.2 Find nodes to expand.");
				System.out.println("AOStartSearch.nodesToExpand()");
				System.out
						.println("There is at least one unexpaneded node in current best solution graph.");
			}
			bestNodes = new AOStarNode[nodesToExpand.size()];
			int num = nodesToExpand.size();
			for (int i = 0; i < num; i++)
			{
				bestNodes[i] = nodesToExpand.poll();
			}
		} else
		{ // Case 2: Choose maxNumberOfNodesToExpand other unexpanded
			// node from the AND/OR-graph.
			// System.err.println("Case 2: Tracing was not successful!");
			PriorityQueue<AOStarNode> unexpanded = unexpandedNodes
					.get(alternatingIndex);
			int size = 0;
			if (rateOfNodesToExpandInOneStep == 1.0)
			{
				size = Math.min(maxNumberOfNodesToExpandInOneStep,
						unexpanded.size());
			} else
			{
				if (unexpanded.size() > 0)
				{
					size = (int) (rateOfNodesToExpandInOneStep * unexpanded
							.size()) + 1;
				}
			}
			bestNodes = new AOStarNode[size];
			for (int i = 0; i < size; i++)
			{
				bestNodes[i] = unexpanded.poll();
			}
			alternatingIndex = (alternatingIndex + 1) % unexpandedNodes.size();
		}
		return bestNodes;
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		for (AOStarNode node : stateNodeMap.values())
		{
			buffer.append("node ");
			buffer.append(node.toString());
			buffer.append("\n");
			for (Connector conn : node.outgoingConnectors)
			{
				buffer.append("    ");
				buffer.append(conn.toString());
				buffer.append("\n");
			}
		}
		return buffer.toString();
	}

	/**
	 * Trace down all marked connectors starting from a given state, collecting
	 * all unexpanded nodes encountered. Prune trace at proven and disproven
	 * nodes.
	 * 
	 * @param initial
	 *            initial node to start from
	 * @return The set of all unexpanded nodes in the partial solution graph
	 *         induced by the marked connectors which are neither proven nor
	 *         disproven sorted by decreasing h-Values. // TODO remove sorting?
	 */
	protected PriorityQueue<AOStarNode> traceMarkedConnectors(AOStarNode initial)
	{
		if (DEBUG)
		{
			System.out.println();
			System.out.println("	1.1 Trace down all marked connectors...");
			System.out
					.println("AOStarSearch.traceMarkedConnectors(AOStarNode initial)");
		}

		PriorityQueue<AOStarNode> result = new PriorityQueue<AOStarNode>();
		Set<AOStarNode> seen = new HashSet<AOStarNode>();
		Queue<AOStarNode> queue = new LinkedList<AOStarNode>();
		queue.offer(initial);

		while (!queue.isEmpty())
		{
			if (timeout())
			{
				return null;
			}

			AOStarNode next = queue.poll();
			seen.add(next);

			if (!next.isProven && !next.isDisproven)
			{
				if (!next.isExpanded())
				{
					if (DEBUG)
					{
						System.out.println("Add unexpanded Node: " + next
								+ " to the set of all unexpanded nodes. ");
					}
					result.add(next);
				} else
				{
					if (next.markedConnector != null)
					{
						if (DEBUG)
						{
							System.out
									.println("Node "
											+ next
											+ " has a marked connector. Add children of marked connector to queue.");
						}
						for (AOStarNode child : next.markedConnector.children)
						{
							if (!seen.contains(child) && !queue.contains(child))
							{
								queue.offer(child);
							}
						}
					}
					// TODO check this!
					else
					{
						System.err
								.println("Strange case where no connector is marked while tracing...");
						assert false;
						// if (DEBUG) {
						// System.out.println("Node " + next +
						// " has no marked connector. Add all children to queue.");
						// }
						// for (Connector connector : next.outgoingConnectors) {
						// for (AOStarNode child : connector.children) {
						// if (!seen.contains(child) && !queue.contains(child))
						// {
						// queue.offer(child);
						// }
						// }
						// }
					}
				}
			}
		}
		return result;
	}

	/**
	 * Update the given node by (re)computing its cost estimate as a function of
	 * the cost estimates of nodes reached via connectors. If applicable, the
	 * flag indicating that the given node has been solved is set. Connector
	 * marks are changed where necessary.
	 * 
	 * @return An integer indicating what changes the update caused.
	 *         <tt>AOStar.UPDATED_NOTHING</tt> if neither proof status nor cost
	 *         estimate were changed, <tt>AOStar.UPDATED_PROOF_STATUS</tt> if
	 *         both proof status and cost estimate were changed,
	 *         <tt>AOStar.UPDATED_COST_ESTIMATE</tt> if cost estimate but not
	 *         the proof status was changed.
	 */
	private int update(AOStarNode node)
	{
		assert node.isExpanded();
		if (DEBUG)
		{
			System.out.println("\nUpdate node... " + node);
		}
		if (node.isProven || node.isDisproven)
		{
			new Exception(
					"Error: Must not call update(node) on nodes already decided.")
					.printStackTrace();
			System.exit(20);
		}
		// Note: Only used if fastUpdate == true.
		// Use first proven connector and break. Else
		// minimize among all proven connectors.
		boolean useFirstProvenConnector = false;

		if (node.markedConnector != null && node.markedConnector.isProven()
				&& fastUpdate)
		{
			node.isProven = true;
			node.costEstimate = node.markedConnector.getMaxChildEstimate()
					+ node.markedConnector.baseCost;
			if (!useFirstProvenConnector)
			{
				Connector oldMarkedConnector = node.markedConnector;
				// minimize among all proven connectors
				for (Connector out : node.outgoingConnectors)
				{
					if (out != oldMarkedConnector && out.isProven())
					{
						if (out.getMaxChildEstimate() + out.baseCost < node.costEstimate)
						{
							node.markedConnector = out;
							node.costEstimate = out.getMaxChildEstimate()
									+ out.baseCost;
						}
					}
				}
			}
			return AOStarSearch.UPDATED_PROOF_STATUS;
		}

		Connector oldMarkedConnector = node.markedConnector;
		double oldCostEstimate = node.costEstimate; // initially this is the
													// h-value.
		if (node.markedConnector == null)
		{
			node.costEstimate = AbstractNode.DISPROVEN;
		} else
		{
			node.costEstimate = node.markedConnector.getMaxChildEstimate()
					+ node.markedConnector.baseCost;
		}

		boolean allConnectorsDisproven = true;
		assert node.outgoingConnectors.size() > 0;
		// Find the cheapest outgoing (maybe proven) connector
		for (Connector out : node.outgoingConnectors)
		{
			if (!out.isDisproven())
			{
				allConnectorsDisproven = false;
			}
			if (out != oldMarkedConnector)
			{
				if (out.isProven() && fastUpdate)
				{
					if (!node.isProven)
					{ // first proven connector we found
						node.markedConnector = out;
						node.costEstimate = out.getMaxChildEstimate()
								+ out.baseCost;
						node.isProven = true;
						if (useFirstProvenConnector)
						{
							return UPDATED_PROOF_STATUS;
						}
					} else
					{ // check if this proven connector is cheaper
						if (out.getMaxChildEstimate() + out.baseCost < node.costEstimate)
						{
							node.markedConnector = out;
							node.costEstimate = out.getMaxChildEstimate()
									+ out.baseCost;
						}
					}
				} else
				{
					if (out.getMaxChildEstimate() + out.baseCost < node.costEstimate)
					{
						node.markedConnector = out;
						node.costEstimate = out.getMaxChildEstimate()
								+ out.baseCost;
					}
				}
			}
		}
		if (allConnectorsDisproven)
		{
			node.isDisproven = true;
			node.markedConnector = null;
		}
		if (node.isProven || node.isDisproven)
		{
			assert !node.isProven || node.markedConnector != null;
			return UPDATED_PROOF_STATUS;
		} else
			if (Math.abs(node.costEstimate - oldCostEstimate) < AOStarSearch.EPSILON)
			{
				assert node.markedConnector != null;
				return UPDATED_NOTHING;
			} else
			{
				assert node.markedConnector != null;
				return UPDATED_COST_ESTIMATE;
			}
	}

	/**
	 * Update nodes by propagating cost estimates and proof/disproof information
	 * backward through the game graph.
	 * 
	 * @param nodes
	 *            Nodes to start the update with
	 */
	protected void updateUntilFixpoint(AOStarNode[] nodes)
	{
		Queue<AOStarNode> queue = new LinkedList<AOStarNode>();
		Set<AOStarNode> seen = new HashSet<AOStarNode>();

		// Set of nodes with changes of marked connectors during this update
		// process.
		Set<AOStarNode> updated = new HashSet<AOStarNode>();
		for (AOStarNode node : nodes)
		{
			queue.add(node);
		}

		while (!queue.isEmpty())
		{
			if (timeout())
			{
				return;
			}
			AOStarNode node = queue.poll();
			seen.add(node);
			double oldCostEstimate = node.costEstimate;
			Connector oldMarkedConnector = node.markedConnector;
			int updateEffect;
			if (node.isDisproven)
			{
				updateEffect = AOStarSearch.UPDATED_PROOF_STATUS;
			} else
			{
				updateEffect = update(node);
			}
			if (node.markedConnector != oldMarkedConnector)
			{
				updated.add(node);
			}
			if (DEBUG)
			{
				System.out.println("Update state: " + updateEffect);
			}

			switch (updateEffect)
			{
			case AOStarSearch.UPDATED_PROOF_STATUS:
				for (Connector connector : node.incomingConnectors)
				{
					AOStarNode parent = connector.parent;
					if (connector != parent.markedConnector
							&& !connector.isProven())
					{
						if (node.costEstimate >= oldCostEstimate)
						{
							continue;
						}
					}
					if (!parent.isProven && !parent.isDisproven
							&& !queue.contains(parent))
					{
						queue.offer(parent);
					}
				}
				break;
			case AOStarSearch.UPDATED_COST_ESTIMATE:
				for (Connector connector : node.incomingConnectors)
				{
					AOStarNode parent = connector.parent;
					if (connector != parent.markedConnector)
					{
						// If this connector is not marked, there is an other
						// connector marked since it is proven or cheaper.
						if (node.costEstimate >= oldCostEstimate)
						{
							continue;
						}
						// Else: Node became cheaper. Update parent.
					}

					// Cost estimates of marked parents are only updated one
					// time.
					if (!parent.isProven && !parent.isDisproven
							&& !seen.contains(parent)
							&& !queue.contains(parent))
					{
						queue.offer(parent);
					}
				}
				break;
			case AOStarSearch.UPDATED_NOTHING:
				break;
			default:
				assert false;
				break;
			}
		}
	}

	/**
	 * Print statistics about number of created and number of expanded nodes
	 * during search.
	 */
	@Override
	public void printStats(boolean simulatePlan)
	{
		int proven = 0;
		for (AOStarNode node : stateNodeMap.values())
		{
			if (node.isProven)
			{
				++proven;
			}
		}
		System.err.println("Out of " + stateNodeMap.size() + " nodes, "
				+ proven + " are proven");
		System.err.println("Number of node expansions: " + NODE_EXPANSIONS);
		StateActionTable table = getExplicitStateActionTable();
		System.err.println("Policy entries: " + table.getNumberOfEntries());
		if (simulatePlan)
		{
			long simulatorTime = System.currentTimeMillis();
			double planCost = new PlanSimulator().performValueIteration(table);
			long simulatorEndTime = System.currentTimeMillis();
			System.err.println("Plan cost (expected number of steps to goal): "
					+ planCost);
			System.err.println("Plan simulator time: "
					+ (simulatorEndTime - simulatorTime) / 1000 + " seconds.");
		}
		System.err.println("Number of sensing applications in policy: "
				+ table.getNumberOfSensingApplication());
	}

	/**
	 * Perform a complete run of the AO* algorithm.
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
		while (!initialNode.isProven && !initialNode.isDisproven && !timeout())
		{
			doIteration();
		}

		// Finish measuring search time.
		endtime = System.currentTimeMillis();
		
		// 
		String result;
		
		
		if (initialNode.isProven)
		{
			result = "The protagonist has got a winning strategy.";
			Global.problem.lunaPlannerOutput.setResult(result);
			
			System.err
					.println("\nResult: The protagonist has got a winning strategy.");
			// printStats();
			return AbstractSearch.PROTAGONIST_WINS;
		} else
		{
			if (initialNode.isDisproven)
			{
				result = "The protagonist does not have a winning strategy. ";
				Global.problem.lunaPlannerOutput.setResult(result);
				
				System.err
						.println("\nResult: The protagonist does not have a winning strategy.");
				return AbstractSearch.ANTAGONIST_WINS;
			} else
			{
				result = "No winning strategy for the protaginist found due to time-out.";
				Global.problem.lunaPlannerOutput.setResult(result);
				
				System.err
						.println("\nResult: No winning strategy for the protaginist found due to time-out.");
				return AbstractSearch.TIMEOUT;
			}
		}
	}

	/**
	 * Get initial node of this search.
	 * 
	 * @return initial node.
	 */
	public AOStarNode getInitNode()
	{
		return (AOStarNode) initialNode;
	}

	public AOStarNode setInitNode(AOStarNode init)
	{
		initialNode = lookupAndInsert(init.state, 0);
		return (AOStarNode) initialNode;
	}

	/**
	 * Delete state-action table.
	 */
	public void deleteStateActionTable()
	{
		explicitStateActionTable = null;
	}

	/**
	 * Apply an operator to the node and create corresponding connectors.
	 * 
	 * @param node
	 * @param op
	 * @return the set of successors.
	 */
	public Set<AOStarNode> apply(AOStarNode node, Operator op)
	{
		assert stateNodeMap != null;
		assert node != null;
		assert stateNodeMap.containsKey(node.state.uniqueID);
		Set<AOStarNode> successorNodes = new HashSet<AOStarNode>();
		Set<State> successorStates = node.state.apply(op);
		if (!successorStates.isEmpty())
		{
			for (State successor : successorStates)
			{
				AOStarNode newNode = lookupAndInsert(successor,
						node.getDepth() + 1);
				successorNodes.add(newNode);
			}
			assert (!successorNodes.isEmpty());
			new Connector(node, successorNodes, op);
		}
		return successorNodes;
	}

	public AOStarSearch getCleanedSearchOnlyContainsThePlan()
	{
		AOStarSearch result = new AOStarSearch();
		AOStarNode init = result.setInitNode(new AOStarNode(
				getInitNode().state, result, 0));
		assert init != null;
		AOStarNode node = getInitNode();
		Queue<AOStarNode> queue = new LinkedList<AOStarNode>();
		Set<AOStarNode> seen = new HashSet<AOStarNode>();
		queue.add(node);
		seen.add(node);
		Map<AOStarNode, AOStarNode> equivalentNodes = new HashMap<AOStarNode, AOStarNode>();
		assert init != null;
		equivalentNodes.put(node, init);
		while (!queue.isEmpty())
		{
			node = queue.poll();
			assert equivalentNodes.containsKey(node);
			if (node.getMarkedConnector() != null)
			{
				AOStarNode equivalentNode = equivalentNodes.get(node);
				Operator op = node.getMarkedConnector().operator;
				assert equivalentNode != null;
				Set<AOStarNode> successors = result.apply(equivalentNode, op);
				equivalentNode.setMarkedConnector(equivalentNode
						.getOutgoingConnectors().iterator().next());
				Map<State, AOStarNode> map = new HashMap<State, AOStarNode>();
				for (AOStarNode succ : successors)
				{
					map.put(succ.state, succ);
				}

				for (AOStarNode child : node.getMarkedConnector().getChildren())
				{
					if (!seen.contains(child))
					{
						assert map.get(child.state) != null;
						equivalentNodes.put(child, map.get(child.state));
						seen.add(child);
						queue.offer(child);
					}
				}
			} else
			{
				assert node.isGoalNode;
				assert equivalentNodes.get(node).state.isGoalState();
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mynd.search.AbstractSearch#printFinalExplictGraph()
	 */
	@Override
	public void printFinalExplictGraph()
	{
		dumpStateSpace("final_explicit_graph");

	}
}
