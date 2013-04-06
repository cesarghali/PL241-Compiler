package com.pl241.ir;

import java.util.ArrayList;
import com.pl241.frontend.Token;

/**
 * 
 * @author cesarghali
 *
 */
public class ArrayNode extends TreeNode
{
	private ArrayList<Integer> numbers;
	
	public ArrayNode(Token type)
	{
		super(type);
		numbers = new ArrayList<Integer>();
	}
	
	/**
	 * 
	 * @return
	 * 		size of the numbers ArrayList
	 */
	public int getNumbersSize()
	{
		return numbers.size();
	}
	
	/**
	 * 
	 * @param number
	 * 		adds the number to numbers ArrayList
	 */
	public void addNumber(int number)
	{
		numbers.add(new Integer(number));
	}
	
	/**
	 * 
	 * @param index
	 * 		index of the number to be returned
	 * @return
	 * 		number at the given index
	 */
	public int getNumber(int index)
	{
		return new Integer(numbers.get(index));
	}
	
	/**
	 * @return String representation of ArrayNode
	 */
	public String toString()
	{
		return super.toString() + ", " + numbers.toString();
	}
}
