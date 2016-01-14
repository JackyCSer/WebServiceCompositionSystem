package mynd.util;

import java.util.Comparator;

import mynd.heuristic.graph.Node;


/**
 * 
 * @author Robert Mattmueller
 *
 */
public class PairValueComparator implements Comparator<Pair<Integer, Node>> {

    public int compare(Pair<Integer, Node> o1, Pair<Integer, Node> o2) {
        // we might want to be more robust
        return o1.first - o2.first;
    }
}
