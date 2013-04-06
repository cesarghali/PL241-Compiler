package com.pl241.frontend;

/**
 * 
 * This class contains information about a variable,
 * variable name, SSA index, and address
 * 
 * @author cesarghali
 *
 */
public class Variable 
{
	private static final int unitSize = 4;
	
	private String name;
	private int ssaIndex;
	// Contain the address of the variable relative to the base
	// address of the stack
	private int address;
	
	public Variable(String name, int ssaIndex, int address)
	{
		this.name = name;
		this.ssaIndex = ssaIndex;
		this.address = address;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public int getSSAIndex()
	{
		return this.ssaIndex;
	}
	
	public void setSSAIndex(int ssaIndex)
	{
		this.ssaIndex = ssaIndex;
	}
	
	public int getAddress()
	{
		return this.address;
	}
	
	public static int getUnitSize()
	{
		return unitSize;
	}
	
	public Variable clone()
	{
		if (this instanceof Array)
		{
			Array retArray = new Array(((Array)(this)).getName(), ((Array)(this)).getSSAIndex(),
					((Array)(this)).getAddress());
			for (int i = 0; i < ((Array)(this)).getDimensionsSize(); i++)
			{
				retArray.addDimension(((Array)(this)).getDimension(i));
			}
			
			return retArray;
		}
		else
		{
			return new Variable(this.name, this.ssaIndex, this.address);
		}
	}
}
