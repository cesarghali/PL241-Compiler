package com.pl241.ir;

import java.util.ArrayList;
import com.pl241.frontend.Token;

/**
 * 
 * @author ekinoguz
 *
 */

public class DesignatorNode extends TreeNode
{
	private ArrayList<TreeNode> expressions;
	
	public DesignatorNode(Token type)
	{
		super(type);
		expressions = new ArrayList<TreeNode>();
	}
	
	/**
	 * 
	 * @param expression
	 * 		will be added to the end of expressions ArrayList
	 */
	public void addExpression(TreeNode expression)
	{
		expressions.add(expression);
	}
	
	/**
	 * 
	 * 
	 * @param index
	 * 		index of the expression
	 * 		be careful with bounds!
	 * @return 	
	 * 		return the expression at given index
	 */
	public TreeNode getExpression(int index)
	{
		return expressions.get(index);
	}
	
	/**
	 * 
	 * @return size of the expressions ArrayList
	 */
	public int getExpressionsSize()
	{
		return expressions.size();
	}
	
	public void clearExpressions()
	{
		this.expressions.clear();
	}
	
	/**
	 * @return String representation of the Designator
	 */
	public String toString()
	{
		return super.toString(); //+ ", " + expressions.toString();
	}
}
