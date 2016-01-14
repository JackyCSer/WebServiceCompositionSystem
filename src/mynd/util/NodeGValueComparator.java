package mynd.util;

import java.util.Comparator;

import mynd.heuristic.graph.Node;


/**
 * 
 * @author Robert Mattmueller
 *
 */
public class NodeGValueComparator implements Comparator<Node> {

    public int compare(Node o1, Node o2) {
        // we might want to be more robust
        return (int) (o1.getgValue() - o2.getgValue());
    }
}
