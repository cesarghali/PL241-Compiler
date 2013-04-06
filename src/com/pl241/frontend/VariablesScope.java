package com.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author cesarghali
 *
 * Keep track of all variables declared in a specific scope
 */
public class VariablesScope
{
	// The outer hashmap maps between scope name (function name) and
	// another hashmap that contains all the variables inside that scope
	// The inner hashmap maps between the variable names and their information
	// index
	private HashMap<String, HashMap<String, Variable>> scope;
	
	public VariablesScope()
	{
		this.scope = new HashMap<String, HashMap<String, Variable>>();
	}
	
	// Used along with the deepClone function
	/*private VariablesScope(HashMap<String, HashMap<String, Variable>> scope)
	{
		this.scope = scope;
	}*/
	
	public void addScope(String scopeName)
	{
		this.scope.put(scopeName, new HashMap<String, Variable>());
	}
	
	public HashMap<String, Variable> getScope(String scopeName)
	{
		return this.scope.get(scopeName);
	}
	
	public boolean containScope(String scopeName)
	{
		return this.scope.containsKey(scopeName);
	}
	
	public void addVariable(String scopeName, String variableName, int address)
	{
		// the index of a variable in SSA format is 0 when it's first added
		this.scope.get(scopeName).put(variableName, new Variable(variableName, 0, address));
	}
	
	public void addArray(String scopeName, String arrayName, int address, ArrayList<Integer> dimensions)
	{
		// the index of a variable in SSA format is 0 when it's first added
		Array array = new Array(arrayName, 0, address);
		array.addDimension(dimensions);
		this.scope.get(scopeName).put(arrayName, array);
	}
	
	public void addParameter(String scopeName, String variableName, int address)
	{
		// the index of a parameter in SSA format is 1 when it's first added
		// because passing a value to the function is let assignment of the
		// parameters
		this.scope.get(scopeName).put(variableName, new Variable(variableName, 1, address));
	}
	
	public void modifyVariable(String scopeName, String variableName, Integer variableSSAIndex)
	{
		this.scope.get(scopeName).get(variableName).setSSAIndex(variableSSAIndex);
	}
	
	public Integer getVariableSSAIndex(String scopeName, String variableName)
	{
		return this.scope.get(scopeName).get(variableName).getSSAIndex();
	}
	
	public int getVariableAddress(String scopeName, String variableName)
	{
		return this.scope.get(scopeName).get(variableName).getAddress();
	}
	
	public int getArrayBaseAddress(String scopeName, String arrayName)
	{
		return this.scope.get(scopeName).get(arrayName).getAddress();
	}
	
	public int getArrayElementAddress(String scopeName, String arrayName, ArrayList<Integer> indices)
	{
		return ((Array)(this.scope.get(scopeName).get(arrayName))).getElementAddress(indices);
	}
	
	public boolean containVariable(String scopeName, String variableName)
	{
		return this.scope.get(scopeName).containsKey(variableName);
	}
	
	public HashMap<String, Variable> cloneScope(String scopeName)
	{
		HashMap<String, Variable> retMap = new HashMap<String, Variable>();
		for (String varName : this.scope.get(scopeName).keySet())
		{
			retMap.put(varName, this.scope.get(scopeName).get(varName).clone());
		}
		
		return retMap;
	}
	
	public HashMap<String, Integer> cloneSSAIndices(String scopeName)
	{
		HashMap<String, Integer> retMap = new HashMap<String, Integer>();
		for (String varName : this.scope.get(scopeName).keySet())
		{
			retMap.put(varName, this.scope.get(scopeName).get(varName).getSSAIndex());
		}
		
		return retMap;
	}
	
	public Array getArray(String scopeName, String arrayName)
	{
		return ((Array)(this.scope.get(scopeName).get(arrayName)));
	}
	
	public boolean isArray(String scopeName, String varName)
	{
		Variable var = this.scope.get(scopeName).get(varName);
		if (var instanceof Array)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Deep clone the current variable scope instance, by cloning all the scopes, their variables
	 * and their variables SSA indices.
	 * 
	 * @return A cloned version of the current variable scope instance
	 */
	/*public VariablesScope deepClone()
	{
		HashMap<String, HashMap<String, Integer>> clonedScope = new HashMap<String, HashMap<String, Integer>>();
		for (String scopeName : this.scope.keySet())
		{
			HashMap<String, Integer> clonedVarSSA = new HashMap<String, Integer>();
			for (String varName : this.scope.get(scopeName).keySet())
			{
				clonedVarSSA.put(varName, this.scope.get(scopeName).get(varName));
			}
			
			clonedScope.put(scopeName, clonedVarSSA);
		}
		
		return new VariablesScope(clonedScope);
	}*/
}
