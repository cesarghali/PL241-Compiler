package com.pl241.frontend;

import java.util.ArrayList;

/**
* 
* This class contains information about a variable,
* variable name, SSA index, ddress, and dimensions
* 
* @author cesarghali
*
*/
public class Array extends Variable
{
	private ArrayList<Integer> dimensions;
	
	public Array(String name, int ssaIndex, int address)
	{
		super(name, ssaIndex, address);
		this.dimensions = new ArrayList<Integer>();
	}
	
	public int getDimensionsSize()
	{
		return this.dimensions.size();
	}
	
	public int getDimension(int index)
	{
		return this.dimensions.get(index);
	}
	
	public void addDimension(int dimension)
	{
		this.dimensions.add(dimension);
	}
	
	public void addDimension(ArrayList<Integer> dimensions)
	{
		for (Integer dimension : dimensions)
		{
			this.addDimension(dimension);
		}
	}
	
	// Calculate the element address in the memory relative to the base
	// address of the stack. Element of the array are stored in memory 
	// consecutively, first dimension, second dimension, third dimension
	// and so on
	// TODO: this might be wrong, also this method is not needed anymore
	public int getElementAddress(ArrayList<Integer> indices)
	{
		int elementOffset = 0;
		for (int k = 0; k < this.dimensions.size(); k++)
		{
			int temp = 1;
			for (int l = k + 1; l < this.dimensions.size(); l++)
			{
				temp *= this.dimensions.get(l);
			}
			elementOffset += (temp * indices.get(k));
		}
		
		return elementOffset * Variable.getUnitSize();
	}
}
