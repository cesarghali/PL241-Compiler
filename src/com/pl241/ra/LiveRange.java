package com.pl241.ra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.pl241.frontend.Tokens;
import com.pl241.ir.BlockNode;
import com.pl241.ir.FunctionBlockNode;
import com.pl241.ir.TreeNode;
import com.pl241.ir.TreeNode.DeleteReason;

/**
 * 
 * @author cesarghali
 *
 */

public class LiveRange
{
	private TreeNode mainNode;
	private TreeNode endNode;
	private HashMap<Integer, TreeNode> phiFunctions;
	// Contains the IDs of the global variables
	private HashSet<Integer> globalVariableIDs;
	
	public LiveRange(TreeNode mainNode, TreeNode endNode, HashSet<Integer> globalVariableIDs)
	{
		this.mainNode = mainNode;
		this.endNode = endNode;
		this.globalVariableIDs = globalVariableIDs;
		
		this.phiFunctions = new HashMap<Integer, TreeNode>();
	}
	
	public RIG Calculate()
	{
		RIG graph = new RIG();
		HashSet<Integer> liveRange;
		
		// Traverse the program graph looking for function declarations,
		// if any is found, calculate their live range and update RIG
		TreeNode currentNode = this.mainNode.getLeftChild();
		while(currentNode.getType().compareType(Tokens.endofProgToken) == false)
		{
			if (currentNode.getType().compareType(Tokens.funcSeqToken) == true)
			{
				for (int i = 0; i < ((FunctionBlockNode)(currentNode)).getFunctionsSize(); i++)
				{
					liveRange = new HashSet<Integer>(this.globalVariableIDs);
					this.parseFuncBody(((FunctionBlockNode)(currentNode)).getFunction(i).getLastBlock(), liveRange, graph);
				}
				
				break;
			}
			
			currentNode = currentNode.getLeftChild();
		}
		
		// Calculate live range for main function and update RIG
		liveRange = new HashSet<Integer>(this.globalVariableIDs);
		this.parseFuncBody(this.endNode.getLastParent(), liveRange, graph);
		
		return graph;
	}
	
	private void parseFuncBody(TreeNode currentNode, HashSet<Integer> liveRange, RIG graph)
	{
		// mainToken means the end of the main function, funcToken or procToken means the end
		// of a function or a procedure
		while ((currentNode.getType().compareType(Tokens.mainToken) == false) &&
			   (currentNode.getType().compareType(Tokens.funcToken) == false) &&
			   (currentNode.getType().compareType(Tokens.procToken) == false))
		{
			if (currentNode.getType().compareType(Tokens.statSeqToken) == true)
			{
				this.parseStatSequence(currentNode, liveRange, graph);
				currentNode = currentNode.getLastParent();
			}
			else if (currentNode.getType().compareType(Tokens.fiToken) == true)
			{
				currentNode = this.parseIfStatement(currentNode, liveRange, graph);
			}
			else if (currentNode.getType().compareType(Tokens.odToken) == true)
			{
				currentNode = this.parseWhileStatement(currentNode, liveRange, graph);
			}
			else
			{
				currentNode = currentNode.getLastParent();
			}
		}
	}
	
	private void parseStatSequence(TreeNode currentNode, HashSet<Integer> liveRange, RIG graph)
	{
		ArrayList<Integer> addedIDs = new ArrayList<Integer>();

		BlockNode statSeqBlock = ((BlockNode)(currentNode));
		for (int i = statSeqBlock.getStatementsSize() - 1; i >=0; i--)
		{
			if (statSeqBlock.getStatement(i).getDeleteReason() != DeleteReason.NotDeleted)
			{
				continue;
			}
			
			addedIDs.clear();
			
			// If the statement is phi, skip it
			if (statSeqBlock.getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
			{
				this.phiFunctions.put(statSeqBlock.getStatement(i).getId(), statSeqBlock.getStatement(i));
				continue;
			}
			// If the statement is one of these, remove the ID of the statement from the live range list
			// and add all references to statements in the children to the live range list
			else if ((statSeqBlock.getStatement(i).getType().compareType(Tokens.addSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.subSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.mulSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.divSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.cmpSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.addaSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.bneSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.beqSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.bleSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.bgtSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.bgeSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.bltSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.loadSSAToken) == true) ||
				(statSeqBlock.getStatement(i).getType().compareType(Tokens.storeSSAToken) == true))
			{
				liveRange.remove(statSeqBlock.getStatement(i).getId());
				if (statSeqBlock.getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
				{
					if (statSeqBlock.getStatement(i).getLeftChild().getRefId() != -1)
					{
						liveRange.add(statSeqBlock.getStatement(i).getLeftChild().getRefId());
						addedIDs.add(statSeqBlock.getStatement(i).getLeftChild().getRefId());
					}
				}
				if (statSeqBlock.getStatement(i).getRightChild() != null)
				{
					if (statSeqBlock.getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
					{
						if (statSeqBlock.getStatement(i).getRightChild().getRefId() != -1)
						{
							liveRange.add(statSeqBlock.getStatement(i).getRightChild().getRefId());
							addedIDs.add(statSeqBlock.getStatement(i).getRightChild().getRefId());
						}
					}
				}
			}
			// If the statement is a move statement, which can only exists of arrays are used, remove
			// the left child from, and add the right child to the live range list 
			else if (statSeqBlock.getStatement(i).getType().compareType(Tokens.movSSAToken) == true)
			{
				liveRange.remove(statSeqBlock.getStatement(i).getLeftChild().getRefId());
				if (statSeqBlock.getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
				{
					if (statSeqBlock.getStatement(i).getRightChild().getRefId() != -1)
					{
						liveRange.add(statSeqBlock.getStatement(i).getRightChild().getRefId());
						addedIDs.add(statSeqBlock.getStatement(i).getRightChild().getRefId());
					}
				}

			}
			// If the statement is a function call, add all references to statements in the parameters
			// (if any) to the live range list
			// Or if the statement is a predefined function (read, write or wln), add the right child (if any,
			// which only exists in the case of write) to the liva range list, and remove the ID of the
			// statement from the live range list (which is only meaningful in case of read)
			else if ((statSeqBlock.getStatement(i).getType().compareType(Tokens.callToken) == true) ||
					(statSeqBlock.getStatement(i).getType().compareType(Tokens.predefToken) == true))
			{
				liveRange.remove(statSeqBlock.getStatement(i).getId());
				if (statSeqBlock.getStatement(i).getRightChild() != null)
				{
					for (int j = 0; j < ((BlockNode)(statSeqBlock.getStatement(i).getRightChild())).getStatementsSize(); j++)
					{
						if (((BlockNode)(statSeqBlock.getStatement(i).getRightChild())).getStatement(j).getType().compareType(Tokens.statToken) == true)
						{
							if (((BlockNode)(statSeqBlock.getStatement(i).getRightChild())).getStatement(j).getRefId() != -1)
							{
								liveRange.add(((BlockNode)(statSeqBlock.getStatement(i).getRightChild())).getStatement(j).getRefId());
								addedIDs.add(((BlockNode)(statSeqBlock.getStatement(i).getRightChild())).getStatement(j).getRefId());
							}
						}
					}
				}
			}
			// If the statement is return, and if the left child (if any) is a reference to a statement,
			// add it to the live range list
			else if (statSeqBlock.getStatement(i).getType().compareType(Tokens.returnToken) == true)
			{
				if (statSeqBlock.getStatement(i).getLeftChild() != null)
				{
					if (statSeqBlock.getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
					{
						if (statSeqBlock.getStatement(i).getLeftChild().getRefId() != -1)
						{
							liveRange.add(statSeqBlock.getStatement(i).getLeftChild().getRefId());
							addedIDs.add(statSeqBlock.getStatement(i).getLeftChild().getRefId());
						}
					}
				}
			}
			
			// Add the live range to the graph
			for (int id : liveRange)
			{
				graph.addNode(id, liveRange);
			}
			
			// Increase the costs of the added IDs.
			graph.increaseCosts(addedIDs);
		}
	}
	
	private TreeNode parseIfStatement(TreeNode currentNode, HashSet<Integer> liveRange, RIG graph)
	{
		ArrayList<Integer> addedIDs = new ArrayList<Integer>();

		// Here, currentNode is the fiNode
		this.parseStatSequence(currentNode, liveRange, graph);
		
		// Clone the live range to be parse in then block
		HashSet<Integer> thenCloneLiveRange = new HashSet<Integer>(liveRange);
		addedIDs.clear();
		for (int i = ((BlockNode)(currentNode)).getStatementsSize() - 1; i >= 0; i--)
		{
			if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
			{
				thenCloneLiveRange.remove(((BlockNode)(currentNode)).getStatement(i).getId());
				if (((BlockNode)(currentNode)).getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
				{
					if (((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId() != -1)
					{
						thenCloneLiveRange.add(((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId());
						addedIDs.add(((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId());
					}
				}
				
				// Add the live range to the graph
				for (int id : thenCloneLiveRange)
				{
					graph.addNode(id, thenCloneLiveRange);
				}
			}
		}
		
		// Increase the costs of the added IDs.
		graph.increaseCosts(addedIDs);
		
		TreeNode thenBlock = currentNode.getParent(0);
		while (thenBlock.getType().compareType(Tokens.ifToken) == false)
		{
			if (thenBlock.getType().compareType(Tokens.statSeqToken) == true)
			{
				this.parseStatSequence(thenBlock, thenCloneLiveRange, graph);
				thenBlock = thenBlock.getLastParent();
			}
			else if (thenBlock.getType().compareType(Tokens.fiToken) == true)
			{
				thenBlock = this.parseIfStatement(thenBlock, thenCloneLiveRange, graph);
			}
			else if (thenBlock.getType().compareType(Tokens.odToken) == true)
			{
				thenBlock = this.parseWhileStatement(thenBlock, thenCloneLiveRange, graph);
			}
		}
		
		// Clone the live range to be parse in else block
		addedIDs.clear();
		HashSet<Integer> elseCloneLiveRange = new HashSet<Integer>(liveRange);
		for (int i = ((BlockNode)(currentNode)).getStatementsSize() - 1; i >= 0; i--)
		{
			if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
			{
				elseCloneLiveRange.remove(((BlockNode)(currentNode)).getStatement(i).getId());
				if (((BlockNode)(currentNode)).getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
				{
					if (((BlockNode)(currentNode)).getStatement(i).getRightChild().getRefId() != -1)
					{
						elseCloneLiveRange.add(((BlockNode)(currentNode)).getStatement(i).getRightChild().getRefId());
						addedIDs.add(((BlockNode)(currentNode)).getStatement(i).getRightChild().getRefId());
					}
				}
				
				// Add the live range to the graph
				for (int id : elseCloneLiveRange)
				{
					graph.addNode(id, elseCloneLiveRange);
				}
			}
		}
		if (currentNode.getParentsSize() > 1)
		{
			// Increase the costs of the added IDs.
			graph.increaseCosts(addedIDs);
			
			TreeNode elseBlock = currentNode.getParent(1);
			while (elseBlock.getType().compareType(Tokens.ifToken) == false)
			{
				if (elseBlock.getType().compareType(Tokens.statSeqToken) == true)
				{
					this.parseStatSequence(elseBlock, elseCloneLiveRange, graph);
					elseBlock = elseBlock.getLastParent();
				}
				else if (elseBlock.getType().compareType(Tokens.fiToken) == true)
				{
					elseBlock = this.parseIfStatement(elseBlock, elseCloneLiveRange, graph);
				}
				else if (elseBlock.getType().compareType(Tokens.odToken) == true)
				{
					elseBlock = this.parseWhileStatement(elseBlock, elseCloneLiveRange, graph);
				}
			}
		}
		
		// Combine then and else live range lists
		liveRange.clear();
		for (int id : thenCloneLiveRange)
		{
			liveRange.add(id);
		}
		for (int id : elseCloneLiveRange)
		{
			liveRange.add(id);
		}
		
		// Add the live range to the graph
		for (int id : liveRange)
		{
			graph.addNode(id, liveRange);
		}
		
		// From now on, thenBlock is the ifBlock
		this.parseStatSequence(thenBlock, liveRange, graph);
		
		return thenBlock.getLastParent();
	}
	
	private TreeNode parseWhileStatement(TreeNode currentNode, HashSet<Integer> liveRange, RIG graph)
	{
		ArrayList<Integer> addedIDs = new ArrayList<Integer>();
		
		// Here, currentNode is the od Node
		// Parse the od node as it 
		this.parseStatSequence(currentNode, liveRange, graph);
		
		// Move up to the od parent which is the while node
		TreeNode whileBlock = currentNode.getLastParent();
		TreeNode bodyBlock = null;
		for (int j = 0; j < 2; j++)
		{
			// Parse the while node
			this.parseStatSequence(whileBlock, liveRange, graph);
			
			// Clone the live range to be parse in loop body
			HashSet<Integer> bodyCloneLiveRange = new HashSet<Integer>(liveRange);
			addedIDs.clear();
			for (int i = ((BlockNode)(whileBlock)).getStatementsSize() - 1; i >= 0; i--)
			{
				if (((BlockNode)(whileBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
				{
					bodyCloneLiveRange.remove(((BlockNode)(whileBlock)).getStatement(i).getId());
					if (((BlockNode)(whileBlock)).getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
					{
						if (((BlockNode)(whileBlock)).getStatement(i).getRightChild().getRefId() != -1)
						{
							bodyCloneLiveRange.add(((BlockNode)(whileBlock)).getStatement(i).getRightChild().getRefId());
							addedIDs.add(((BlockNode)(whileBlock)).getStatement(i).getRightChild().getRefId());
						}
					}
					
					// Add the live range to the graph
					for (int id : bodyCloneLiveRange)
					{
						graph.addNode(id, bodyCloneLiveRange);
					}
				}
			}
			
			// Increase the costs of the added IDs.
			graph.increaseCosts(addedIDs);
	
			// Get the body block node which is parent with index 0
			// of the while block
			bodyBlock = whileBlock.getParent(0);
			while (bodyBlock.getType().compareType(Tokens.whileToken) == false)
			{
				if (bodyBlock.getType().compareType(Tokens.statSeqToken) == true)
				{
					this.parseStatSequence(bodyBlock, bodyCloneLiveRange, graph);
					bodyBlock = bodyBlock.getLastParent();
				}
				else if (bodyBlock.getType().compareType(Tokens.fiToken) == true)
				{
					bodyBlock = this.parseIfStatement(bodyBlock, bodyCloneLiveRange, graph);
				}
				else if (bodyBlock.getType().compareType(Tokens.odToken) == true)
				{
					bodyBlock = this.parseWhileStatement(bodyBlock, bodyCloneLiveRange, graph);
				}
			}
			
			// From now on, bodyBlock is the while block
			
			// Copy the body live range into the live range list
			liveRange = new HashSet<Integer>(bodyCloneLiveRange);
		}
		
		// From now on, bodyBlock is the while block

		// Parse while block again as a regular statements sequence block
		this.parseStatSequence(bodyBlock, liveRange, graph);

		// Add the left operands of the phi functions to the live range
		// and remove any reference of the phi function from the live range
		addedIDs.clear();
		for (int i = ((BlockNode)(whileBlock)).getStatementsSize() - 1; i >= 0; i--)
		{
			if (((BlockNode)(bodyBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
			{
				liveRange.remove(((BlockNode)(bodyBlock)).getStatement(i).getId());
				if (((BlockNode)(bodyBlock)).getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
				{
					if (((BlockNode)(bodyBlock)).getStatement(i).getLeftChild().getRefId() != -1)
					{
						liveRange.add(((BlockNode)(bodyBlock)).getStatement(i).getLeftChild().getRefId());
						addedIDs.add(((BlockNode)(bodyBlock)).getStatement(i).getLeftChild().getRefId());
					}
				}
				
				// Add the live range to the graph
				for (int id : liveRange)
				{
					graph.addNode(id, liveRange);
				}
			}
		}
		
		// Increase the costs of the added IDs.
		graph.increaseCosts(addedIDs);
		
		// Return the block before the while block which is the last parent
		// of the while block
		return whileBlock.getLastParent();
	}
	
	public HashMap<Integer,TreeNode> getPhiFunctions()
	{
		return this.phiFunctions;
	}
}
