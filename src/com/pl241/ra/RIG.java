package com.pl241.ra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 
 * @author ekinoguz
 * 
 * Register Interference Graph (RIG)
 */

public class RIG
{
	// complete graph which will be created and nothing will be deleted
	private HashMap<Integer, RIGNode> graph;
	
	public RIG()
	{
		this.graph = new HashMap<Integer, RIGNode>();
	}
	
	/**
	 * 
	 * @param id
	 * @return RIGNode with given id
	 */
	public RIGNode getRIGNode(int id)
	{
		return graph.get(id);
	}
	
	public HashSet<Integer> getAllNodeIDs()
	{
		return new HashSet<Integer>(graph.keySet());
	}
	
	/**
	 * @param nodeID
	 * @param neighbors
	 */
	public void addNode(int nodeID, HashSet<Integer> neighbors)
	{
		if (this.graph.containsKey(nodeID) == false)
		{
			this.graph.put(nodeID, new RIGNode(nodeID));
		}
		RIGNode node = this.graph.get(nodeID);
		for (int neighborID : neighbors)
		{
			if (neighborID == nodeID)
			{
				continue;
			}
			
			if (this.graph.containsKey(neighborID) == false)
			{
				this.graph.put(neighborID, new RIGNode(neighborID));
			}
			RIGNode neighborNode = this.graph.get(neighborID);
			
			// Create the neighbor relationship
			node.addNeighbor(neighborID);
			neighborNode.addNeighbor(nodeID);
		}
	}
	
	public void increaseCosts(ArrayList<Integer> addedIDs)
	{
		for (int id : addedIDs)
		{
			this.graph.get(id).incrementCost();
		}
	}
	
	public String toString()
	{
		return this.graph.toString();
	}
}
