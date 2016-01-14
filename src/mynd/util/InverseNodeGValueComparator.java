package mynd.util;

import java.util.Comparator;

import mynd.heuristic.graph.Node;


/**
 * 
 * @author Robert Mattmueller
 *
 */
public class InverseNodeGValueComparator implements Comparator<Node> {

    public int compare(Node arg0, Node arg1) {
        return (int) (arg1.getgValue() - arg0.getgValue());
    }
}
