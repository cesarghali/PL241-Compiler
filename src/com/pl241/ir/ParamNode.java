package com.pl241.ir;

import java.util.ArrayList;
import com.pl241.frontend.Token;

/**
 * 
 * @author cesarghali
 * 
 */

public class ParamNode extends TreeNode
{
	private ArrayList<Token> parameters;

	public ParamNode(Token type)
	{
		super(type);
		this.parameters = new ArrayList<Token>();
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be added to the end of parameter ArrayList
	 */
	public void addParameter(Token parameter)
	{
		this.parameters.add(parameter);
	}
	
	/**
	 * 
	 * @param index
	 * 		index of the parameter
	 * 		be careful with bounds!
	 * @return 	
	 * 		return the parameter at given index
	 */
	public Token getParameter(int index)
	{
		return this.parameters.get(index);
	}
	
	/**
	 * 
	 * @return size of the parameters ArrayList
	 */
	public int getParametersSize()
	{
		return this.parameters.size();
	}
	
	/**
	 * @return String representation of ArrayNode
	 */
	public String toString()
	{
		return super.toString() + ", " + this.parameters.toString();
	}
}
