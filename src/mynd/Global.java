package mynd;

import java.util.Random;

import mynd.MyNDPlanner.Algorithm;
import mynd.problem.Problem;
import mynd.symbolic.BDDManager;

/**
 * Class used to hold static global settings.
 * 
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class Global
{
	/**
	 * Debug setting for global
	 */
	public static boolean GLOBAL_DEBUG = false;
	
	public static boolean SEARCH_DEBUG = false;
	
	public static boolean PROBLEM_DEBUG = false;
	
	public static boolean ABSTRACT_COST_COMPUTATION_DEBUG = false;
	
	/**
	 *  Location: mynd.explicit.ExplicitOperator
	 */
	public static boolean EXPLICIT_OPERATOR_DEBUG = false;

	/**
	 * Location: mynd.explicit.ExplicitState
	 */
	public static boolean EXPLICIT_STATE_DEBUG = false;

	/**
	 * Location: mynd.explicit.ExplicitCondition
	 */
	public static boolean EXPLICIT_CONDITION_DEBUG = false;
	
	
	
	
	/**
	 * Search algorithm used.<br/>
	 * LAO* is the default  algorithm.
	 */ 
	public static MyNDPlanner.Algorithm algorithm = Algorithm.BFS_STAR; 
	
	/**
	 * BDD Manager to handle BDDs.
	 */
	public static BDDManager BDDManager;

	/**
	 * Problem which should be solved.
	 */
	public static Problem problem;

	/**
	 * To use everywhere in the code, where a random number is needed.
	 */
	public static Random generator = new Random(1);





}
