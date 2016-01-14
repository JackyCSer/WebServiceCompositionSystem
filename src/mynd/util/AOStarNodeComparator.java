package mynd.util;

import java.util.Comparator;

import luna.search.AOStarSearch;
import mynd.search.AOStarNode;

/**
 * 
 * @author Manuela Ortlieb
 * 
 */

public class AOStarNodeComparator implements Comparator<AOStarNode>
{

	private int indexOfRuleArray;

	public static boolean DEBUG = false;

	public AOStarNodeComparator(int indexOfRuleArray)
	{
		this.indexOfRuleArray = indexOfRuleArray;
		if (DEBUG)
		{
			System.out.println("AOStarNodeComparator with rules: ");
			for (int i = 0; i < AOStarSearch.expansionRules[indexOfRuleArray].length; i++)
				System.out
						.print(AOStarSearch.expansionRules[indexOfRuleArray][i]
								+ ", ");
			System.out.println();
		}
	}

	private int compareRecursive(AOStarNode o1, AOStarNode o2, int indexOfRule)
	{
		if (indexOfRule == AOStarSearch.expansionRules[indexOfRuleArray].length)
			return 0;
		int result = 0;
		switch (AOStarSearch.expansionRules[indexOfRuleArray][indexOfRule])
		{
		case MAX_DEPTH:
			result = o2.getDepth() - o1.getDepth();
			break;
		case MAX_H:
			result = o2.getHeuristic() - o1.getHeuristic();
			break;
		case MIN_DEPTH:
			result = o1.getDepth() - o2.getDepth();
			break;
		case MIN_H:
			result = o1.getHeuristic() - o2.getHeuristic();
			break;
		case NEWEST:
			result = o2.getIndex() - o1.getIndex();
			break;
		case OLDEST:
			result = o1.getIndex() - o2.getIndex();
			break;
		case RANDOM:
			result = o1.random - o2.random;
			break;
		default:
			assert false;
			break;
		}
		if (result == 0)
			compareRecursive(o1, o2, indexOfRule + 1);
		return result;
	}

	public int compare(AOStarNode o1, AOStarNode o2)
	{
		if (DEBUG)
		{
			System.out.println("Call of compare with rules");
			for (int i = 0; i < AOStarSearch.expansionRules[indexOfRuleArray].length; i++)
				System.out
						.print(AOStarSearch.expansionRules[indexOfRuleArray][i]
								+ ", ");
			System.out.println();
		}
		return compareRecursive(o1, o2, 0);
	}

}
