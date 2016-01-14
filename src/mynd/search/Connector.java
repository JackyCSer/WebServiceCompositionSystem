package mynd.search;

import java.util.Set;

import mynd.state.Operator;

/**
 * A connector associates with a node <tt>node</tt> a set of successor nodes
 * <tt>succ_1, ..., succ_n</tt>. One connector corresponds to an AND
 * conjunction, whereas several outgoing connectors from one node are
 * interpreted disjunctively. Hence, a list of outgoing connectors corresponds
 * to a disjunction over conjunctions over possible successor states.
 * 
 * @author Robert Mattmueller
 * 
 */
public class Connector
{

	/**
	 * Parent node to which this connector is attached
	 */
	public AOStarNode parent;

	/**
	 * Child nodes
	 */
	public Set<AOStarNode> children;

	/**
	 * Operator which corresponds to this connector.
	 */
	public final Operator operator;

	private boolean isSafe = true;

	/**
	 * Base cost of this connector
	 */
	public double baseCost;

	/**
	 * True iff. all of its children are proven.
	 */
	private boolean isProven = false;

	/**
	 * True iff. at least one of its children is disproven.
	 */
	private boolean isDisproven = false;

	/**
	 * Creates a new connector. Links parent and child nodes back to this
	 * connector.
	 * 
	 * @param parent
	 *            Node to which this connector is attached
	 * @param children
	 *            Child nodes
	 * @param operator
	 *            Name of operator inducing this connector
	 */
	public Connector(AOStarNode parent, Set<AOStarNode> children,
			Operator operator)
	{
		this.parent = parent;
		// this.children = Collections.unmodifiableSet(children); // FIXME done
		// for simple min observation
		this.children = children;
		this.operator = operator;
		baseCost = operator.getCost();
		parent.outgoingConnectors.add(this);
		for (AOStarNode child : children)
		{
			child.incomingConnectors.add(this);
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Connector))
		{
			return false;
		}
		Connector c = (Connector) o;
		if (parent.equals(c.parent) && children.equals(c.children))
		{
			return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return parent.hashCode() + children.hashCode();
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append(parent.toString());
		buffer.append(" -> [ ");
		for (AOStarNode child : children)
		{
			buffer.append(child.toString());
			buffer.append(" ");
		}
		buffer.append("] hash " + hashCode());
		return buffer.toString();
	}

	public void setBaseCost(double baseCost)
	{
		this.baseCost = baseCost;
	}

	public double getBaseCost()
	{
		return this.baseCost;
	}

	/**
	 * Get the set of child nodes.
	 * 
	 * @return children
	 */
	public Set<AOStarNode> getChildren()
	{
		return children;
	}

	public void setChildren(Set<AOStarNode> children)
	{
		this.children = children;
	}

	/**
	 * Get parent node.
	 * 
	 * @return parent
	 */
	public AOStarNode getParent()
	{
		return parent;
	}

	/**
	 * Get this connector's proven status.
	 * 
	 * @return true iff. all children are proven.
	 */
	public boolean isProven()
	{
		checkProvenAndDisprovenStatus();
		return isProven;
	}

	/**
	 * Get this connector's disproven status.
	 * 
	 * @return true iff. at least one child is disproven.
	 */
	public boolean isDisproven()
	{
		checkProvenAndDisprovenStatus();
		return isDisproven;
	}

	/**
	 * Check this connector's proven status.
	 */
	private void checkProvenAndDisprovenStatus()
	{
		if (isProven || isDisproven)
		{
			return;
		}
		isProven = true;
		isDisproven = false;
		for (AOStarNode child : children)
		{
			isProven &= child.isProven;
			isDisproven |= child.isDisproven;
		}
	}

	/**
	 * Get the maximum cost estimate of this connector's children.
	 * 
	 * @return maximum child cost estimate
	 */
	public double getMaxChildEstimate()
	{
		double max = -1;
		for (AOStarNode child : children)
		{
			if (child.getCostEstimate() > max)
			{
				max = child.getCostEstimate();
			}
		}
		assert max >= 0;
		return max;
	}

	public boolean isSafe()
	{
		return isSafe;
	}

	public void setSafe(boolean isSafe)
	{
		this.isSafe = isSafe;
	}
}
