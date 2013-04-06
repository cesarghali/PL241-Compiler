package com.pl241.backend;

import java.util.ArrayList;

/**
 * 
 * @author cesarghali
 *
 */

public class FunctionInfo
{
	private int id;
	private ArrayList<String> parameters;
	
	public FunctionInfo(int id, ArrayList<String> parameters)
	{
		this.id = id;
		this.parameters = parameters;
	}
	
	public int getID()
	{
		return this.id;
	}
	
	public int getParametersSize()
	{
		return this.parameters.size();
	}
	
	public String getParameter(int i)
	{
		return this.parameters.get(i);
	}
}