package com.pl241.ra;

import java.util.HashSet;

/**
 * 
 * @author ekinoguz
 *
 * Register Interference Graph (RIG) node
 */
public class RIGNode
{
	private int id;
	private String color;
	private HashSet<Integer> neighbors;
	private HashSet<Integer> aliveNeighbors;
	private int cost;
	
	public RIGNode(int id)
	{
		this.id = id;
		this.color = ""; // node does not have any color initially
		this.neighbors = new HashSet<Integer>();
		this.aliveNeighbors = new HashSet<Integer>();
		this.cost = 0;
	}

	public int getNeighborsSize()
	{
		return this.neighbors.size();
	}
	
	/**
	 * 
	 * @param id of node to be added to both neighbors list
	 */
	public void addNeighbor(int id)
	{
		this.neighbors.add(id);
		this.aliveNeighbors.add(id);
	}
	
	public HashSet<Integer> getNeighbors()
	{
		return this.neighbors;
	}
	
	public int getAliveNeighborsSize()
	{
		return this.aliveNeighbors.size();
	}
	
	public HashSet<Integer> getAliveNeighbors()
	{
		return this.aliveNeighbors;
	}
	
	public void setAliveNeighbors(HashSet<Integer> in)
	{
		this.aliveNeighbors = new HashSet<>(in);
	}
	
	public void removeAliveNeighbor(int removeID)
	{
		this.aliveNeighbors.remove(new Integer(removeID));
	}
	
	public int getId()
	{
		return id;
	}


	public void setId(int id)
	{
		this.id = id;
	}


	public String getColor()
	{
		return color;
	}

	public boolean hasColor() 
	{
		return	!this.color.equals(""); 
	}
	

	public void setColor(String color)
	{
		this.color = color;
	}
	
	public void incrementCost()
	{
		this.cost++;
	}
	
	public int getCost()
	{
		return this.cost;
	}
	
	/**
	 * @return String representation of RIGNode
	 */
	public String toString()
	{
		return this.id + "\tcolor: " + this.color + "\tcost: " + this.cost + "\tneighbors: " + this.neighbors;
	}
}
