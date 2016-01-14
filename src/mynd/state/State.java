/**
 * Interface to use both types of states (e.g. belief and explicit states) into the Node class.
 */
package mynd.state;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import mynd.Global;
import mynd.explicit.ExplicitState;
import mynd.heuristic.pdb.Abstraction;

/**
 * @author Manuela Ortlieb
 * 
 */
public abstract class State
{

	/**
	 * Indicates if this state is an abstract or a concrete state.
	 */
	protected final boolean isAbstractedState;

	/**
	 * If this is an abstracted state, there has to be a an corresponding
	 * abstraction containing abstracted operators.
	 */
	public final Abstraction abstraction;

	/**
	 * Unique id which is used to identify a state.
	 */
	public final BigInteger uniqueID;

	/**
	 * Indicates if all operators were checked yet.
	 */
	private boolean applicableOpsInitialized = false;

	/**
	 * Note: This is only used to assert, that applicability for each operator
	 * is checked on exactly one set of operators. To avoid inconsistence.
	 */
	private Collection<Operator> checkedOps;

	/**
	 * After checking applicability of every operator this list contains each
	 * applicable operator. TODO: Does it make sense to use a list instead of a
	 * set?
	 */
	private List<Operator> applicableOps;

	public State(BigInteger uniqueID, boolean isAbstractedState,
			Abstraction abstraction)
	{
		this.uniqueID = uniqueID;
		this.isAbstractedState = isAbstractedState;
		this.abstraction = abstraction;
		assert (!isAbstractedState || (abstraction != null));
		assert ((abstraction != null) || !isAbstractedState);
	}

	public boolean isGoalState()
	{
		if (isAbstractedState)
		{
			return abstraction.goal.isSatisfiedIn(this);
		}
		return Global.problem.getGoal().isSatisfiedIn(this);
	}

	public List<Operator> getApplicableOps(Collection<Operator> ops)
	{
		if (!applicableOpsInitialized)
		{
			List<Operator> applicableOps = new ArrayList<Operator>();
			for (Operator op : ops)
			{
				if (isApplicable(op))
				{
					applicableOps.add(op);
				}
			}
			this.applicableOps = Collections.unmodifiableList(applicableOps);
			applicableOpsInitialized = true;
			checkedOps = ops;
		} else
		{
			if (!ops.equals(checkedOps))
			{
				applicableOpsInitialized = false;
				getApplicableOps(ops);
			}
		}
		return applicableOps;
	}

	public abstract Set<State> apply(Operator operator);

	/** 
	 * @Title: applyForLAOStar 
	 * @Description: TODO
	 * @param @param op
	 * @param @return 
	 * @return Set<State>
	 * @throws 
	 */ 
	public abstract Set<State> applyForLAOStar(Operator op);
	
	public void free()
	{
	};

	public abstract List<ExplicitState> getAllExplicitWorldStates();

	public abstract void dump();

	public abstract boolean equals(Object obj);

	/**
	 * Note: It is important that this method is overwritten by symbolic
	 * operator, since sensing operators should not be applicable if there is no
	 * splitting.
	 * 
	 * @param operator
	 * @return
	 */
	public abstract boolean isApplicable(Operator operator);

	/**
	 * Reset applicable operators to enforce that all operators are tested
	 * again. Note: Necessary after determinization for initial state.
	 */
	public void resetApplicableOps()
	{
		if (applicableOpsInitialized)
		{
			applicableOpsInitialized = false;
			applicableOps = new ArrayList<Operator>(applicableOps.size());
			checkedOps = null;
		}
	}


}
