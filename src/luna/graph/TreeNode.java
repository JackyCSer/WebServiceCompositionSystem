/**
 * @FileName: TreeNode.java
 * @Description: 
 * @author Jacky ZHANG
 * @date May 3, 2015
 */
package luna.graph;

import java.util.HashSet;
import java.util.Set;

/**
 * @Description:
 */
public class TreeNode
{
	TreeNode parentNode;
	Set<TreeNode> childrenNodes;

	final int id;
	private final String incomingOperatorName;

	public TreeNode(int id, String incomingOperatorName)
	{
		this.id = id;
		this.childrenNodes = new HashSet<TreeNode>();
		this.incomingOperatorName = incomingOperatorName;
	}

	public int getId()
	{
		return id;
	}

	public TreeNode getParentNode()
	{
		return parentNode;
	}

	public void setParentNode(TreeNode parentNode)
	{
		this.parentNode = parentNode;
	}

	public Set<TreeNode> getChildrenNodes()
	{
		return childrenNodes;
	}

	public void addChild(TreeNode childNode)
	{
		this.childrenNodes.add(childNode);
	}

	public String getIncomingOperatorName()
	{
		return incomingOperatorName;
	}

	public void setChildrenNodes(Set<TreeNode> childrenNodes)
	{
		this.childrenNodes = childrenNodes;
	}

}
