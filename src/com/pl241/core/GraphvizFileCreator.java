package com.pl241.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.pl241.frontend.Parser;
import com.pl241.frontend.Tokens;
import com.pl241.ir.BlockNode;
import com.pl241.ir.DesignatorNode;
import com.pl241.ir.FunctionBlockNode;
import com.pl241.ir.TreeNode;
import com.pl241.ir.TreeNode.DeleteReason;
import com.pl241.ra.RIG;
import com.pl241.ra.RIGNode;

/**
 * @author ekinoguz
 * 
 * Graphviz file creator which will create .dot file for visualization of graph
 * and save the file in /graphviz/ folder
 */

public class GraphvizFileCreator
{
	public enum GraphType { General, SSA }

	private static final String BLOCK_NODE = ", style=bold, shape=box, color=black];\n";
	private static final String DESIGNATOR_NODE = ", style=dotted, shape=diamond, color=red];\n";
	private static final String FUNCTIONBLOCK_NODE = ", style=dashed, shape=triangle, color=blue];\n";
	private static final String INSTR_NODE = ", style=rounded, shape=record, color=green];\n";
	private static final String CSE_DEL_INSTR_NODE = ", style=filled, shape=record, color=green, fillcolor=\"#ff00005f\"];\n";
	private static final String CP_DEL_INSTR_NODE = ", style=filled, shape=record, color=green, fillcolor=\"#ffd7005f\"];\n";
	private static final String FUNC_NODE = ", style=dashed, shape=record, color=blue];\n";
	private static final String ARGS_NODE = ", style=dotted, shape=record, color=red];\n";
	private static final String OTHER_NODE = ", style=rounded, shape=square, color=green];\n";

	private static final String LABEL_BEGIN = " [label=\"";
	private static final String GRAPH_COLOR_BEGIN = "\", style=filled, fillcolor=\"#";
	private static final String GRAPH_COLOR_END = "5f\"];\n";
	
	/**
	 * This method will parse the <inputFileName> file and create a graphviz
	 * <outputFilename> file
	 * 
	 * @param outputFilename
	 * @param inputFileName
	 */
	public static void createFile(String outputFilename, String inputFileName, boolean verbose, GraphType type,
			int numOfPasses)
	{
		Parser par = Parser.getInstance(inputFileName);
		TreeNode head = par.computation()[0];
		createFile(outputFilename, head, verbose, type, numOfPasses);
	}

	/**
	 * 
	 * @param outputFilename name of file to be created. it should end with .dot
	 * @param graph RIG which will be visualized
	 */
	public static void createRIG(String outputFilename, RIG graph)
	{
		// beginning of structure
		String structure = "graph G {\n";
		
		// keep track of neighbor relations to eliminiate duplicate arrows
		HashSet<String> neighborRelation = new HashSet<String>();
		//structure += createGraph(head, verbose, type, numOfPasses, "", "");
		// for each RIGNode in graph
		for (int id : graph.getAllNodeIDs())
		{
			RIGNode node = graph.getRIGNode(id);
			// if node does not have any neighbors
			if (node.getNeighborsSize() == 0)
				structure += id + ";\n";
			for (int neighbor : node.getNeighbors())
			{
				if (neighborRelation.contains(neighbor+"-"+id))
					continue;
				structure += id + " -- " + neighbor + ";\n";
				neighborRelation.add(id+"-"+neighbor);
			}
		}
		// end of structure
		structure += "}";
		Utils.saveFile(outputFilename, structure);	
	}
	
	/**
	 * 
	 * @param outputFilename name of file to be created. it should end with .dot
	 * @param graph RIG which will be visualized
	 * @param colorSize 
	 */
	public static void createRIG(String outputFilename, RIG graph, HashMap<Integer, String> colors)
	{
		// create random colors
		HashMap<String, String> randomColors = new HashMap<String, String>();
		Random rg = new Random();
		for (String id : colors.values())
		{
			int random1 = rg.nextInt(16) * 16;
			int random2 = rg.nextInt(16) * 16;
			int random3 = rg.nextInt(16) * 16;
			randomColors.put(id, Integer.toHexString(random1)+Integer.toHexString(random2)+Integer.toHexString(random3));
		}
		// beginning of structure
		String structure = "graph G {\n";
		
		// keep track of neighbor relations to eliminiate duplicate arrows
		HashSet<String> neighborRelation = new HashSet<String>();
		//structure += createGraph(head, verbose, type, numOfPasses, "", "");
		// for each RIGNode in graph
		for (int id : graph.getAllNodeIDs())
		{
			RIGNode node = graph.getRIGNode(id);
			structure += id + LABEL_BEGIN + colors.get(id) + ": " + node.getCost() + " x (" + id  + ") " +
					GRAPH_COLOR_BEGIN + randomColors.get(colors.get(id)) + GRAPH_COLOR_END;
			// if node does not have any neighbors
			if (node.getNeighborsSize() == 0)
				structure += id + ";\n";
			for (int neighbor : node.getNeighbors())
			{
				if (neighborRelation.contains(neighbor+"-"+id))
				{
					continue;
				}
				structure += id + " -- " + neighbor + ";\n";
				neighborRelation.add(id + "-" + neighbor);
			}
		}
		// end of structure
		structure += "}";
		Utils.saveFile(outputFilename, structure);	
	}
	
	/**
	 * This method will create a graphviz for the graph that start at <head>
	 * <outputFilename> file
	 * 
	 * @param outputFilename
	 * @param head
	 */
	public static void createFile(String outputFilename, TreeNode head, boolean verbose, GraphType type,
			int numOfPasses)
	{
		if (head == null)
		{
			return;
		}
		
		String structure = createDotStructure(head, verbose, type, numOfPasses);
		Utils.saveFile(outputFilename, structure);
	}
	
	private static String createDotStructure(TreeNode head, boolean verbose, GraphType type, int numOfPasses)
	{
		// beginning of structure
		String structure = "digraph G {\n";
		structure += createGraph(head, verbose, type, numOfPasses, "", "");
		
		// end of structure
		structure += "}";
		return structure;
	}
	
	/**
	 * Preorder traversal of given graph.
	 * 
	 * @param head
	 * @return representation of all nodes for dot file.
	 */
	private static String createGraph(TreeNode head, boolean verbose, GraphType type, int numOfPasses,
			String header, String nodeStyle)
	{
		String out = "";
		if (head != null)
		{
			if (head.getTraversedPasses() == numOfPasses)
			{
				return "";
			}
			
			if (head.getLeftChild() != null)
			{
				out += head.getId() + " -> " + head.getLeftChild().getId() + ";\n";
			}
			if (head.getRightChild() != null)
			{
				out += head.getId() + " -> " + head.getRightChild().getId() + ";\n";
			}
			
			if (head instanceof DesignatorNode)
			{
				if (((DesignatorNode)(head)).getExpressionsSize() > 0)
				{
					for (int i = 0; i < ((DesignatorNode)(head)).getExpressionsSize(); i++)
					{
						out += createGraph(((DesignatorNode)(head)).getExpression(i), verbose, type,
								numOfPasses, "", "");
					}
					//out += head.getId() + "-> " + ((DesignatorNode)(head)).getExpression(0).getId() + ";\n";
				}
				if (head.getParentsSize() == 0 && header != "")
				{
					out += head.getId() + " [label=\"";
					out += header + "\\l|";
					out += head.toGraph(verbose, type) + "\\l\"" + nodeStyle;
				}
				else
				{
					out += head.getId() + " [label=\"" + head.toGraph(verbose, type) + "\"" + DESIGNATOR_NODE;
				}
			}
			else if (head instanceof BlockNode)
			{
				if (((BlockNode)(head)).getStatementsSize() > 0)
				{
					for (int i = 0; i < ((BlockNode)(head)).getStatementsSize(); i++)
					{
						if (((BlockNode)(head)).getType().compareType(Tokens.argsToken) == true)
						{
							out += createGraph(((BlockNode)(head)).getStatement(i), verbose, type,
									numOfPasses, "block: " + head.getId() + "\\nargs: " + (i + 1), ARGS_NODE);
						}
						else
						{
							if (((BlockNode)(head)).getStatement(i).getDeleteReason() == DeleteReason.NotDeleted)
							{
								out += createGraph(((BlockNode)(head)).getStatement(i), verbose, type,
										numOfPasses, "block: " + head.getId() + "\\ninstr: " + (i + 1), INSTR_NODE);
							}
							else if (((BlockNode)(head)).getStatement(i).getDeleteReason() == DeleteReason.CSE)
							{
								out += createGraph(((BlockNode)(head)).getStatement(i), verbose, type,
										numOfPasses, "block: " + head.getId() + "\\ninstr: " + (i + 1), CSE_DEL_INSTR_NODE);
							}
							else if (((BlockNode)(head)).getStatement(i).getDeleteReason() == DeleteReason.CP)
							{
								out += createGraph(((BlockNode)(head)).getStatement(i), verbose, type,
										numOfPasses, "block: " + head.getId() + "\\ninstr: " + (i + 1), CP_DEL_INSTR_NODE);
							}
						}
					}
					//out += head.getId() + "-> " + ((BlockNode)(head)).getStatement(0).getId() + ";\n";
				}
				out += head.getId() + " [label=\"" + head.toGraph(verbose, type) + "\"" + BLOCK_NODE;
			}
			else if (head instanceof FunctionBlockNode)
			{
				if (((FunctionBlockNode)(head)).getFunctionsSize() > 0)
				{
					for (int i = 0; i < ((FunctionBlockNode)(head)).getFunctionsSize(); i++)
					{
						out += createGraph(((FunctionBlockNode)(head)).getFunction(i), verbose, type,
								numOfPasses, "block: " + head.getId() + "\\nfunc: " + (i + 1), FUNC_NODE);
					}
					//out += head.getId() + "-> " + ((FunctionBlockNode)(head)).getFunction(0).getId() + ";\n";
				}
				out += head.getId() + " [label=\"" + head.toGraph(verbose, type) + "\"" + FUNCTIONBLOCK_NODE;
			}
			else
			{
				if (head.getParentsSize() == 0 && header != "")
				{
					out += head.getId() + " [label=\"";
					out += header + "\\l|";
					out += head.toGraph(verbose, type) + "\\l\"" + nodeStyle;
				}
				else
				{
					out += head.getId() + " [label=\"" + head.toGraph(verbose, type) + "\\l\"" + OTHER_NODE;
				}
			}
			
			head.setTraversedPasses(numOfPasses);
			
			out += createGraph(head.getLeftChild(), verbose, type, numOfPasses, "", "");
			out += createGraph(head.getRightChild(), verbose, type, numOfPasses, "", "");
		}
		return out;
	}

//	public static void main(String[] args)
//	{
//		GraphvizFileCreator.createFile("graphviz/test.dot", "codes/code_04");
//	}
}
