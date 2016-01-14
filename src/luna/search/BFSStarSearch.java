/**
 * @FileName: BFSStarSearch.java
 * @Description: 
 * @author Jacky ZHANG
 * @date May 8, 2015
 */
package luna.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import luna.LunaGlobal;
import luna.graph.TreeNode;
import mynd.Global;
import mynd.MyNDPlanner;
import mynd.explicit.ExplicitState;
import mynd.search.AOStarNode;
import mynd.search.AbstractSearch;
import mynd.search.Connector;
import mynd.state.Operator;
import mynd.state.State;

/**
 * @Description:
 * 
 *               Important Node: in BFS* Search, the Global.problem is FOND. so
 *               the State is ExplicitState, Operator is ExplicitOperator
 *               Global.problem is FullyObservableProblem
 * 
 * 
 */
public class BFSStarSearch extends AOStarSearch
{

	private Queue<AOStarNode> BFSQueue;

	public BFSStarSearch()
	{
		//
		// unexpandedNodes = new ArrayList<PriorityQueue<AOStarNode>>();
		BFSQueue = new LinkedList<AOStarNode>();

	}

	/**
	 * @Title: main
	 * @Description: For Test
	 * @param @param args
	 * @return void
	 * @throws
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mynd.search.AbstractSearch#run()
	 */
	@Override
	public int run()
	{
		// Start measuring search time.
		starttime = System.currentTimeMillis();
		// Get initial state and insert it into: unexpandedNodes with depth 0.
		initialNode = lookupAndInsert(Global.problem.getSingleInitialState(), 0);

		// Search until initial node is proven or disproven or a timeout occurs.
		// for test:
		if (DEBUG)
		{
			System.out.println("BFSStarSearch.run()");
		}
		// Add initial node into queue:
		BFSQueue.add((AOStarNode) initialNode);

		// Outer Loop:
		int i = 1;
		while (!BFSQueue.isEmpty())
		{
			if (DEBUG)
			{
				System.out.println();
				System.out.println();
				System.out
						.println("------------------------------------BFS* algorithm iteration: "
								+ i++ + "------------------------------------");
				System.out.println("Number of nodes in BFSQueue: "
						+ BFSQueue.size());
				System.out.println("BFSQueue: " + BFSQueue);
			}

			// 1. Inner Loop:
			doIteration();

		} // End Outer Loop:

		// Set initialNode isProven after BFS*
		initialNode.setProven(true);

		// Finish measuring search time.
		endtime = System.currentTimeMillis();

		// End
		String result = "BFS*: ";
		String resultType = "BFS*: ";
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

	/**
	 * Perform one BFS* iteration similiar to the AO* algorithm, i.e. choice of
	 * nodes to expand, expansion, and (recursive) update. Nodes of equal
	 * quality in the subgraph induced by the marked connectors are expanded
	 * simultaneously. If the subgraph induced by the marked connectors does not
	 * include any unexpanded nodes which are not proven/disproven yet, all
	 * unexpanded nodes not yet proven/disproven (outside the induced subgraph)
	 * are considered candidates for expansion.
	 */
	@Override
	public void doIteration()
	{
		if (DEBUG)
		{
			System.out.println();
			System.out
					.println("------------------------BFSStarSearch.doIteration()------------------------");

			if (Global.problem.isFullObservable)
			{
				dumpStateSpace();
			}
		}

		// BFS don't need to find nodes to expand:
		// // 1. Find nodes to expand.
		// if (DEBUG)
		// {
		// System.out.println();
		// System.out.println("1. Find nodes to expand. ");
		// }
		//
		// AOStarNode[] nodesToExpand = nodesToExpand();
		if (timeout())
		{
			return;
		}
		// assert nodesToExpand != null;

		// 2. Expand
		if (DEBUG)
		{
			System.out.println();
			System.out.println("2. Expand");
		}

		AOStarNode node = BFSQueue.poll();

		// Expand node:
		if (!node.isGoalNode)
		{
			expand(node);
		} else
		{
			node.setProven(true);
		}

		if (timeout())
		{
			String msg = "BFS* Search: Timeout! ";
			System.out.println(msg);
			logger.info(msg);
			return;
		}

		// // 3. Update
		// if (DEBUG)
		// {
		// System.out.println();
		// System.out.println("3. Update");
		// }
		//
		// // updateUntilFixpoint(nodesToExpand);
		if (DEBUG)
		{
			System.out
					.println("------------------------End BFSStarSearch.doIteration()------------------------");
		}
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
		if (!getStateNodeMap().containsKey(state.uniqueID))
		{
			// This is a new node.
			node = new AOStarNode(state, this, depth);
			getStateNodeMap().put(state.uniqueID, node);
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
			node = getStateNodeMap().get(state.uniqueID);
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
					+ node.getIndex() + "--------------------");
			System.out.println("Node " + node + " is expaneded now!");
		}

		if (!(node.isProven || node.isDisproven))
		{
			List<Operator> applicableOps;
			List<Operator> applicableOps2 = new ArrayList<Operator>();
			// here: node.state is ExplicitState:
			// Global.problem is FullyObservableProblem
			applicableOps = node.state.getApplicableOps(Global.problem
					.getOperators());

			// Set the priority of the Ops
			for (Operator op : applicableOps)
			{
				if (op.getPriority() == -1)
				{
					// Set new priority:
					op.setPriority(node.getDepth());
				}
			}
			applicableOps2.addAll(applicableOps);

			// Sort applicableOps by priority descending
			Collections.sort(applicableOps2);

			// Successor which is different from this
			boolean hasSuccessor = false;

			// Important: ONLY use the first Operator of applicableOps2 to
			// expand:
			// applicableOps2.size() >= 1;
			Operator op = applicableOps2.get(0);

			Set<State> successorStates = node.state.apply(op);

			// for test:
			if (DEBUG)
			{
				System.out.println("BFSStarSearch.expand() successorStates: ");
				int i = 1;
				for (State s : successorStates)
				{
					if (s instanceof ExplicitState)
					{
						System.out
								.println("s"
										+ i++
										+ ": "
										+ ((ExplicitState) s)
												.toStringWithPositivePropositionNames());
					}

				}
				System.out.println();
			}

			Set<AOStarNode> children = new LinkedHashSet<AOStarNode>();
			for (State successorState : successorStates)
			{
				AOStarNode newNode = lookupAndInsert(successorState,
						node.getDepth() + 1);
				if (newNode != node)
				{
					hasSuccessor = true;
				}
				children.add(newNode);

				// Add into BFSQueue:
				// Important: if and only if the newNode has NOT been expanded
				// &&
				// has NOT been added into BFSQueue
				if (!newNode.isExpanded() && !BFSQueue.contains(newNode))
				{
					BFSQueue.add(newNode);
				}

			}
			assert (!children.isEmpty());

			// Create explicit graph:
			new Connector(node, children, op);

			if (!hasSuccessor)
			{
				node.isDisproven = true; // Dead end.
			}

			NODE_EXPANSIONS++;
		} else
		{
			assert false;
		}

		// Set Expanded:
		node.setExpanded();

		// Set isProven:
		node.setProven(true);

		if (DEBUG)
		{
			System.out
					.println("--------------------End Expand Node--------------------");
		}
	}

	/**
	 * 
	 * @Title: printFinalPlanPath
	 * @Description: printFinalPlanPath BFS traverse
	 * @param @return
	 * @return String
	 * @author Lavender
	 */
	@Override
	public String printFinalPlanPath()
	{
		try
		{
			// Print
			StringBuffer buffer = new StringBuffer();

			Queue<TreeNode> queue = new LinkedList<TreeNode>();
			assert Global.problem.isFullObservable;
			Queue<AOStarNode> queue2 = new LinkedList<AOStarNode>();

			int pathCounter = 1;

			AOStarNode initialNode = this.getStateNodeMap().get(
					Global.problem.getSingleInitialState().uniqueID);
			queue2.offer(initialNode);

			TreeNode treeRoot = new TreeNode(initialNode.getIndex(), null);
			treeRoot.setParentNode(null);

			queue.offer(treeRoot);

			// BFS
			// Create a NEW tree:
			while (!queue.isEmpty())
			{
				TreeNode current = queue.poll();
				AOStarNode node = queue2.poll();

				// seenNodes.add(node);
				Set<Connector> connectors = node.outgoingConnectors;

				// seenConnectors.add(connector);
				for (Connector connector : connectors)
				{
					for (AOStarNode next : connector.children)
					{
						queue2.offer(next);

						TreeNode child = new TreeNode(next.getIndex(),
								connector.operator.getName());
						child.setParentNode(current);
						current.addChild(child);

						queue.offer(child);
					}
				}

				// Print path:
				if (node.isGoalNode)
				{
					LinkedList<Integer> idStack = new LinkedList<Integer>();
					LinkedList<String> actionStack = new LinkedList<String>();
					TreeNode temp = current;

					while (temp.getId() != treeRoot.getId())
					{
						idStack.push(temp.getId());
						actionStack.push(temp.getIncomingOperatorName());

						temp = temp.getParentNode();
					}
					idStack.push(treeRoot.getId());

					// Print
					buffer.append("Solution " + pathCounter++ + ": ");
					while (!idStack.isEmpty())
					{
						buffer.append("State: " + idStack.pop());

						if (!actionStack.isEmpty())
						{
							buffer.append(", Action: " + actionStack.pop());
							buffer.append(" -> ");
						}
					}
					buffer.append("\n");
				}
			}
			// for test:
			System.out
					.println("-------------------------------Print Path------------------------");
			System.out.println(buffer.toString());
			System.out
					.println("-------------------------------End Print Path------------------------");

			Global.problem.lunaPlannerOutput.setFinaPlanPath(buffer.toString());
			Global.problem.lunaPlannerOutput.setSolutionNumber(pathCounter);
			return buffer.toString();
		} catch (Exception e)
		{
			e.printStackTrace();
			logger.error(e);
			return null;
		}
	}

	@Override
	public void dumpPlan()
	{
		if (Global.problem.isFullObservable)
		{
			if (MyNDPlanner.runID == -1)
			{
				// Dump DOT representation of the plan if the planner runs
				// locally
				String fileName = "final_plan_"
						+ Global.algorithm.toString().toLowerCase();

				dumpStateSpace(fileName);
			}
		} else
		{
			getPlanAsDebugOutput();
		}
	}
}
