package com.pl241.ra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Stack;

import com.pl241.frontend.Tokens;
import com.pl241.ir.TreeNode;

/**
 * 
 * @author ekinoguz
 * 
 * This class will create Register Allocation for each variable
 * 
 */
public class RegisterAllocation
{
/*
 * Algorithm:
 * http://web.cecs.pdx.edu/~mperkows/temp/register-allocation.pdf
 * Step 1 (simplify):  find a node with at most K-1 edges and cut it out of the graph.  
 * 			(Remember this node on a stack for later stages.)
 * Step 2 (color):  when the simplified subgraph has been colored, add back the node on the top 
			of the stack and assign it a color not taken by one of the adjacent nodes
 * Step 3 (spilling):  once all nodes have K or more neighbors, pick a node for spilling
			Storage on the stack
 */

	// Register Interference Graph which will be used
	// in register allocation
	private RIG graph;
	
	// dynamic graph which will be used to find alive nodes
	// while applying graph coloring algorithm
	public HashSet<Integer> aliveNodes;
	
	// result of the register allocation process
	// each Integer(id) maps to register number to be used
	private HashMap<Integer, Integer> colors;
	private LinkedHashMap<Integer, Integer> memory;
	
	private int colorSize;
	public static int fixedColorSize;
	
	private ArrayList<Integer> suspended;
	private int memoryCounter;
	
	// stack with RIGNodes
	private Stack<Integer> stack;
	
	// for clustering on phiFunction variables
	private HashMap<Integer,TreeNode> phiFunctions;
	private HashMap<Integer, ArrayList<Integer>> clusters;
	private HashMap<Integer, ArrayList<Integer>> clustersReversed;
	private HashMap<Integer, Integer> clusterColor;
	private int clusterNumber;
	
	/**
	 * 
	 * @param graph
	 * @param colorSize
	 * @param proxySize
	 */
	public RegisterAllocation(RIG graph, int colorSize, HashMap<Integer,TreeNode> phiFunctions)
	{
		this.graph = graph;
		this.colorSize = colorSize;
		this.phiFunctions = phiFunctions;
		fixedColorSize = colorSize;
		this.aliveNodes = this.graph.getAllNodeIDs();
		this.colors = new HashMap<Integer, Integer>();
		this.memory = new LinkedHashMap<Integer, Integer>();
		this.memoryCounter = 1;
		this.suspended = new ArrayList<Integer>();
		this.clusterNumber = +1;
		this.clusters = new HashMap<Integer, ArrayList<Integer>>();
		this.clustersReversed = new HashMap<Integer, ArrayList<Integer>>();
		this.clusterColor = new HashMap<Integer, Integer>();
		this.stack = new Stack<Integer>();
	}
	
	public HashMap<Integer, String> colorify()
	{
		int currentID, nodeColor;
		this.stack = new Stack<Integer>();
		// Repeat while the graph has node
		while (aliveNodes.size() > 0)
		{
			// Pick a node t with fewer than colorSize neighbors
			currentID = findNode(colorSize);
			
			// if there is no such node, call colorify with increased colorSize
			if (currentID == -1)
			{
				this.resetGraph();
				this.colorSize += 1;
				return colorify();
			}
			
			// Put t on a stack
			stack.push(currentID);
			// remove node from the RIG
			suspendNode(currentID);
			
			// check if currentID is a phi node
			if (this.phiFunctions.containsKey(currentID))
			{
				// add <currentID, clusterNumber> to HashMap
				ArrayList<Integer> local = new ArrayList<Integer>();
				local.add(this.clusterNumber);
				this.clusters.put(currentID, local);
				
				// add <clusterNumber, nodeIDs> to HashMap
				local = new ArrayList<Integer>();
				local.add(currentID);
				this.clustersReversed.put(clusterNumber, local);
				
				// get the leftChild and check cluster
				TreeNode leftChild = this.phiFunctions.get(currentID).getLeftChild();
				this.createCluster(this.phiFunctions.get(currentID), leftChild);
				
				// get the rightChild and check cluster
				TreeNode rightChild = this.phiFunctions.get(currentID).getRightChild();
				this.createCluster(this.phiFunctions.get(currentID), rightChild);
				
				clusterNumber += 1;
			}
		}
//		System.out.println(this.clusters);
//		System.out.println(this.clustersReversed);
//		System.out.println(this.stack);
		// Then start assigning colors to nodes on the
		// stack (starting with the last node added)
		while (stack.size() > 0)
		{
			nodeColor = -1;
			currentID = stack.pop();
			
			// At each step pick a color different from those
			// assigned to already colored neighbors
			nodeColor = findColor(currentID);
			if (nodeColor == -1)
			{
				this.resetGraph();
				this.colorSize += 1;
				return colorify();
				//this.Error("Could not find any possible color for node: " + currentID);
			}
			
			// if node is in cluster:
			if (this.clusters.containsKey(currentID))
			{
				for (Integer clusterNo : clusters.get(currentID))
					this.clusterColor.put(clusterNo, nodeColor);
			}
			this.colors.put(currentID, nodeColor);
			(this.graph.getRIGNode(currentID)).setColor(nodeColor+"");
		}
		
		this.adjust();
		
//		System.out.println("Cluster color");
//		System.out.println(clusterColor);
//		System.out.println(colors);
		return this.getResult();
	}

	
	private void createCluster(TreeNode main, TreeNode child)
	{
		if (child.getRefId() == -1)
			return;
		// if child is a number, return
		if (child.getType().compareType(Tokens.number) == true)
			return;
		// if child and main is a neighbor, return
		if (graph.getRIGNode(main.getId()).getNeighbors().contains(child.getRefId()) == true)
			return;
		ArrayList<Integer> local = new ArrayList<Integer>();
		
		if (this.clusters.containsKey(child.getRefId()))
			local = this.clusters.get(child.getRefId());
		local.add(this.clusterNumber);
		this.clusters.put(child.getRefId(), local);
		
		local = new ArrayList<Integer>();
		if (this.clustersReversed.containsKey(clusterNumber))
			local = this.clustersReversed.get(clusterNumber);
		local.add(child.getRefId());
		clustersReversed.put(clusterNumber, local);
		int index = stack.indexOf(new Integer(child.getRefId()));
		if (index == -1)
		{
			stack.push(child.getRefId());
			this.suspendNode(child.getRefId());
		}
		else
		{
			int top = stack.pop();
			if (index <= stack.size()-1)
				stack.push(top);
			else
				stack.add(index+1, top);
		}
	}
	
	/**
	 * 
	 * @return combine all the colors in a HashMap. Convert the hashmap
	 * 		into a format which will be used in Code Generation. Only [1,8]
	 * 		should be used as a register number. If there is a memory register,
	 * 		keep it as it is.
	 */
	private HashMap<Integer, String> getResult()
	{
		HashMap<Integer, String> out = new HashMap<Integer, String>();
		int colorCounter = 1;
		int[] finalColors = new int[this.colors.size()];
		Arrays.fill(finalColors, 0);
		for (Integer id : this.colors.keySet())
		{
			if (finalColors[this.colors.get(id)] == 0)
				finalColors[this.colors.get(id)] = colorCounter++;
			out.put(id, finalColors[this.colors.get(id)]+"");
		}
		for (Integer id : this.memory.keySet())
			out.put(id, "m"+this.memory.get(id));
		return out;
	}
	
	/**
	 * Looks at the aliveNode list
	 * 
	 * @param size which should be (k-1) if we are doing k-coloring algorithm
	 * @return id of the node which has fewer than size neighbors
	 * 		returns -1 if there is no such node
	 */
	private int findNode(int size)
	{
		size = size - 1;
		for (Integer id : aliveNodes)
			if (graph.getRIGNode(id).getAliveNeighborsSize() < size )
				return id;
		return -1;
	}
	
	/**
	 * Suspend the node
	 * Remove it from aliveNodes list
	 * Remove all edges containing given node from aliveNeighbors list
	 * @param id of the node to be suspended
	 */
	private void suspendNode(int id)
	{
		this.aliveNodes.remove(new Integer(id));
		this.suspended.add(id);
		// for all of the aliveNeighbors of id
		for (Integer neighbor : (graph.getRIGNode(id)).getAliveNeighbors())
		{
			(graph.getRIGNode(neighbor)).removeAliveNeighbor(id);
		}
	}
	
	/**
	 * Resets the graph to initial case
	 */
	private void resetGraph()
	{
		for (Integer id : suspended)
		{
			this.aliveNodes.add(id);
			this.graph.getRIGNode(id).setAliveNeighbors(this.graph.getRIGNode(id).getNeighbors());
		}
	}
	
	/**
	 * Looks at all the neighbors of given node and find a not used color
	 * for given node. If there is not any color, we should do something different...
	 * Return -1 if there is any problem
	 * 
	 * @param id of node
	 * @return color for the give node
	 */
	private int findColor(int id)
	{
		boolean possibleColors[] = new boolean[colorSize];
		Arrays.fill(possibleColors, Boolean.TRUE);
		// for each neighbors of given node, set the used colors as false
		for (Integer neighbor : (graph.getRIGNode(id)).getNeighbors())
		{
			if ((graph.getRIGNode(neighbor)).hasColor())
			{
				String c = (graph.getRIGNode(neighbor)).getColor();
				possibleColors[Integer.parseInt(c)] = false;
			}
		}
		// find an unused color
		for (int i = 0; i < possibleColors.length; i++)
			if (possibleColors[i])
				return i;
		
		// TODO: what if we don't have a possible color?
		// actually this should never happen
		
		return -1; // if we cannot find any color somehow
	}
	
	/**
	 * Check if there is any unused color
	 */
	private void adjust()
	{
		HashSet<Integer> usedColors = new HashSet<Integer>();
		for (Integer id : this.colors.values())
			usedColors.add(id);
		colorSize = usedColors.size();
		// if we are not using more colors than we have return
		if (colorSize <= fixedColorSize)
			return;
		
		// else decide on which registers will be put into memory
		// note that 2 registers will be used as proxy
		int memorySize = usedColors.size() - fixedColorSize + 2;
//		System.out.println("we are using: " + usedColors.size());
//		System.out.println("we have: " + fixedColorSize);
//		System.out.println("should be in memory: " + memorySize);
		while (memorySize > 0)
		{
			int minimumColor = getMinimumAccessedColor();
			for (Integer id : colors.keySet())
			{
				if (colors.get(id) == minimumColor)
				{
					memory.put(id, memoryCounter);
					(this.graph.getRIGNode(id)).setColor("m"+(memoryCounter++));
				}
			}
			for (Integer id : memory.keySet())
				if (colors.containsKey(id))
					colors.remove(id);
			memorySize--;
		}
	}
	
	/**
	 * 
	 * @return id of minimum accessed node
	 */
//	@Deprecated
//	private int getMinimumAccessedNode()
//	{
//		int min = Integer.MAX_VALUE;
//		int minid = -1;
//		int tmp;
//		for (Integer id : this.aliveNodes)
//		{
//			tmp = this.graph.getRIGNode(id).getCost();
//			if (tmp < min)
//			{
//				min = tmp;
//				minid = id.intValue();
//			}
//			else if (tmp == min && (id == compareAliveNeighborSize(minid, id)))
//			{
//				minid = id.intValue();
//			}
//		}
//		return minid;
//	}
	
	/**
	 * 
	 * @return the color which has total minimum access cost
	 */
	private int getMinimumAccessedColor()
	{
		int min = Integer.MAX_VALUE;
		int minid = -1;
		int[] cost = new int[colorSize];
		Arrays.fill(cost, Integer.MAX_VALUE);
		
		for (Integer id : colors.keySet())
			if (this.memory.containsKey(id) == false)
				cost[colors.get(id)] += this.graph.getRIGNode(id).getCost();
		
		for (int i = 0; i < colorSize; i++) {
			if (cost[i] < min) {
				min = cost[i];
				minid = i;
			}
		}
		return minid;
	}
	
	/**
	 * 
	 * @param node1
	 * @param node2
	 * @return id of the node which has more alive neighbors
	 */
//	private int compareAliveNeighborSize(int node1, int node2)
//	{
//		int size1 = this.graph.getRIGNode(node1).getAliveNeighborsSize();
//		int size2 = this.graph.getRIGNode(node2).getAliveNeighborsSize();
//		return (size1 >= size2) ? node1 : node2;
//	}
	
	public void checkResult()
	{
		for (Integer id : graph.getAllNodeIDs()) {
			if (colors.containsKey(id) == false)
				continue;
			for (Integer neighbor : graph.getRIGNode(id).getNeighbors()) {
				if (colors.get(id) == colors.get(neighbor)) {
					this.Error(id + " " + neighbor + " has the same color");
					return;
				}
			}
		}
		System.out.println("There is no problem in Register Allocation output!");
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}