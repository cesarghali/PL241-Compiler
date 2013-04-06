package com.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import com.pl241.frontend.Token;
import com.pl241.frontend.VariablesScope;

/**
 * 
 * @author cesarghali
 * 
 */

public class MainNode extends TreeNode
{
	private ArrayList<TreeNode> statements;
	private VariablesScope scope;
	// Contains a list of functions and all the global variables that are assigned
	// in the functions
	private HashMap<String, ArrayList<String>> funcAssignedVarMap;
	// Contains a list of variables used in multiple functions
	private ArrayList<String> multiScopeVariable;

	public MainNode(Token type)
	{
		super(type);
		this.statements = new ArrayList<TreeNode>();
		this.scope = null;
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be added to the end of statements ArrayList
	 */
	public void addStatement(TreeNode statement)
	{
		this.statements.add(statement);
	}
	
	/**
	 * 
	 * @param index
	 * 		index of the statement
	 * 		be careful with bounds!
	 * @return 	
	 * 		return the statement at given index
	 */
	public TreeNode getStatement(int index)
	{
		return this.statements.get(index);
	}
	
	/**
	 * 
	 * @return size of the statements ArrayList
	 */
	public int getStatementsSize()
	{
		return this.statements.size();
	}

	public VariablesScope getScope()
	{
		return scope;
	}

	public void setScope(VariablesScope scope)
	{
		this.scope = scope;
	}
	
	public HashMap<String, ArrayList<String>> getFuncAssignedVarMap()
	{
		return this.funcAssignedVarMap;
	}
	
	public void setFuncAssignedVarMap(HashMap<String, ArrayList<String>> funcAssignedVarMap)
	{
		this.funcAssignedVarMap = funcAssignedVarMap;
	}
	
	public ArrayList<String> getMultiScopeVariable()
	{
		return this.multiScopeVariable;
	}
	
	public void setMultiScopeVariable(ArrayList<String> multiScopeVariable)
	{
		this.multiScopeVariable = multiScopeVariable;
	}
}
