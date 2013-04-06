package com.pl241.ir;

import java.util.ArrayList;
import com.pl241.frontend.Token;

/**
 * 
 * @author cesarghali
 * 
 */

public class FunctionBlockNode extends TreeNode
{
	private ArrayList<FuncProcNode> functions;

	public FunctionBlockNode(Token type)
	{
		super(type);
		this.functions = new ArrayList<FuncProcNode>();
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be added to the end of functions ArrayList
	 */
	public void addFunction(FuncProcNode function)
	{
		this.functions.add(function);
	}
	
	/**
	 * return the function at given index
	 * 
	 * @param index
	 * 		index of the function
	 * @return 	
	 * 		null if index is outOfBounds
	 * 		function otherwise
	 */
	public FuncProcNode getFunction(int index)
	{
		if (index >= 0 && index < this.getFunctionsSize())
		{
			return this.functions.get(index);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * 
	 * @return size of the functions ArrayList
	 */
	public int getFunctionsSize()
	{
		return this.functions.size();
	}
}
