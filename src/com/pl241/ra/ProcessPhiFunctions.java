package com.pl241.ra;

import java.util.ArrayList;
import java.util.HashMap;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;
import com.pl241.ir.BlockNode;
import com.pl241.ir.TreeNode;

public class ProcessPhiFunctions
{
	/**
	 * 
	 * @param phiContainers Contains a reference to all the blocks that contain phi functions
	 * 
	 */
	public static void eliminate(HashMap<BlockNode, BlockNode> phiContainers/*, HashMap<Integer, String> colors*/)
	{
		ArrayList<Integer> toBeDeleted = new ArrayList<Integer>();
		for (BlockNode container : phiContainers.keySet())
		{
			if (container.hasParent() == false)
			{
				continue;
			}
			
			toBeDeleted.clear();
			
			BlockNode lastLeftBranch = null;
			BlockNode lastRightBranch = null;

			if (container.getType().compareType(Tokens.fiToken) == true)
			{
				if (container.getParentsSize() == 1)
				{
					lastLeftBranch = ((BlockNode)(container.getLastParent()));
					lastRightBranch = phiContainers.get(container);
				}
				else
				{
					lastLeftBranch = ((BlockNode)(container.getParentBeforeLast()));
					lastRightBranch = ((BlockNode)(container.getLastParent()));
				}
				
				for (int i = 0; i < container.getStatementsSize(); i++)
				{
					if (container.getStatement(i).getType().compareType(Tokens.phiSSAToken) == false)
					{
						break;
					}
					
					int phiId = container.getStatement(i).getId();
					
					// Add to the lastLeftBranch
					if (container.getStatement(i).getLeftChild().getType().compareType(Tokens.number) == true)
					{
						TreeNode leftChild = container.getStatement(i).getLeftChild();
						container.getStatement(i).setLeftChild(null);
						leftChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(leftChild);
						
						if (lastLeftBranch.getStatementsSize() == 0)
						{
							lastLeftBranch.addStatement(move);
						}
						else
						{
							if (lastLeftBranch.getLastStatement().getType().compareType(Tokens.braSSAToken) == true)
							{
								lastLeftBranch.addStatementBeforeLast(move);
							}
							else
							{
								lastLeftBranch.addStatement(move);
							}
						}
					}
					else if (container.getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
					{
						TreeNode leftChild = container.getStatement(i).getLeftChild();
						container.getStatement(i).setLeftChild(null);
						leftChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(leftChild);

						if (lastLeftBranch.getStatementsSize() == 0)
						{
							lastLeftBranch.addStatement(move);
						}
						else
						{
							if (lastLeftBranch.getLastStatement().getType().compareType(Tokens.braSSAToken) == true)
							{
								lastLeftBranch.addStatementBeforeLast(move);
							}
							else
							{
								lastLeftBranch.addStatement(move);
							}
						}
					}
					
					// Add to lastRightBranch
					if (container.getStatement(i).getRightChild().getType().compareType(Tokens.number) == true)
					{
						TreeNode rightChild = container.getStatement(i).getRightChild();
						container.getStatement(i).setLeftChild(null);
						rightChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(rightChild);
						
						if (lastRightBranch.getType().compareType(Tokens.ifToken) == false)
						{
							lastRightBranch.addStatement(move);
						}
						else
						{
							lastRightBranch.addStatementAtBeginning(move);
						}
					}
					else if (container.getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
					{
						TreeNode rightChild = container.getStatement(i).getRightChild();
						container.getStatement(i).setLeftChild(null);
						rightChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(rightChild);

						if (lastRightBranch.getType().compareType(Tokens.ifToken) == false)
						{
							lastRightBranch.addStatement(move);
						}
						else
						{
							lastRightBranch.addStatementAtBeginning(move);
						}							
					}
					
					toBeDeleted.add(i);
				}
				
				for (int i = toBeDeleted.size() - 1; i >= 0; i--)
				{
					container.deleteStatement(toBeDeleted.get(i));
				}
			}
			else if (container.getType().compareType(Tokens.whileToken) == true)
			{
				lastLeftBranch = container;
				lastRightBranch = ((BlockNode)(container.getParent(0)));
				
				for (int i = 0; i < container.getStatementsSize(); i++)
				{
					if (container.getStatement(i).getType().compareType(Tokens.phiSSAToken) == false)
					{
						break;
					}
					
					int phiId = container.getStatement(i).getId();
//					String phiColor = colors.get(phiId);
//					
//					if (phiColor == null)
//					{
//						toBeDeleted.add(i);
//						continue;
//					}
					
					// Add to the lastLeftBranch
					if (container.getStatement(i).getLeftChild().getType().compareType(Tokens.number) == true)
					{
						TreeNode leftChild = container.getStatement(i).getLeftChild();
						container.getStatement(i).setLeftChild(null);
						leftChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(leftChild);
						
						TreeNode containerParent = container.getLastParent();
						if ((containerParent.getType().compareType(Tokens.funcSeqToken) == true) ||
							(containerParent.getType().compareType(Tokens.varDeclSeqToken) == true) ||
							(containerParent.getType().compareType(Tokens.mainToken) == true) ||
							(containerParent.getType().compareType(Tokens.ifToken) == true))
						{
							// Create a newBlock and insert it between container and containerParent
							BlockNode newBlock = new BlockNode(Token.getToken(Tokens.statSeqToken));
							newBlock.addStatement(move);
							
							// Unlink container and containerParent and insert the newBlock between container and containerParent
							container.deleteParent(containerParent);
							if (containerParent.getLeftChild() == container)
							{
								containerParent.setLeftChild(null);
								containerParent.setLeftChild(newBlock);
							}
							else if (containerParent.getRightChild() == container)
							{
								containerParent.setRightChild(null);
								containerParent.setRightChild(newBlock);
							}
							newBlock.setLeftChild(container);
						}
						else
						{
							((BlockNode)(containerParent)).addStatement(move);							
						}
					}
					else if (container.getStatement(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
					{
						TreeNode leftChild = container.getStatement(i).getLeftChild();
						container.getStatement(i).setLeftChild(null);
						leftChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(leftChild);
						
						TreeNode containerParent = container.getLastParent();
						if ((containerParent.getType().compareType(Tokens.funcSeqToken) == true) ||
							(containerParent.getType().compareType(Tokens.varDeclSeqToken) == true) ||
							(containerParent.getType().compareType(Tokens.mainToken) == true) ||
							(containerParent.getType().compareType(Tokens.ifToken) == true))
						{
							// Create a newBlock and insert it between container and containerParent
							BlockNode newBlock = new BlockNode(Token.getToken(Tokens.statSeqToken));
							newBlock.addStatement(move);
							
							// Unlink container and containerParent and insert the newBlock between container and containerParent
							container.deleteParent(containerParent);
							if (containerParent.getLeftChild() == container)
							{
								containerParent.setLeftChild(null);
								containerParent.setLeftChild(newBlock);
							}
							else if (containerParent.getRightChild() == container)
							{
								containerParent.setRightChild(null);
								containerParent.setRightChild(newBlock);
							}
							newBlock.setLeftChild(container);
						}
						else
						{
							((BlockNode)(containerParent)).addStatement(move);							
						}
					}
					
					// Add to the lastRightBlock
					if (container.getStatement(i).getRightChild().getType().compareType(Tokens.number) == true)
					{
						TreeNode rightChild = container.getStatement(i).getRightChild();
						container.getStatement(i).setLeftChild(null);
						rightChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(rightChild);
						
						lastRightBranch.addStatementBeforeLast(move);
					}
					else if (container.getStatement(i).getRightChild().getType().compareType(Tokens.statToken) == true)
					{
						TreeNode rightChild = container.getStatement(i).getRightChild();
						container.getStatement(i).setLeftChild(null);
						rightChild.clearParents();
						
						TreeNode move = new TreeNode(Token.getToken(Tokens.movSSAToken));
						move.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), phiId));
						move.setRightChild(rightChild);

						lastRightBranch.addStatementBeforeLast(move);
					}
					
					toBeDeleted.add(i);
				}
				
				for (int i = toBeDeleted.size() - 1; i >= 0; i--)
				{
					container.deleteStatement(toBeDeleted.get(i));
				}
			}
		}
	}
}
