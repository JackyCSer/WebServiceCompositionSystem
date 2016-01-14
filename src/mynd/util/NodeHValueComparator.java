package mynd.util;

import java.util.Comparator;

import mynd.heuristic.graph.Node;


/**
 * 
 * @author Robert Mattmueller
 *
 */
public class NodeHValueComparator implements Comparator<Node> {

	/**
	 * Compare two nodes by h-value.
	 */
    public int compare(Node o1, Node o2) {
        return (int) (o1.gethValue() - o2.gethValue());
    }

}
