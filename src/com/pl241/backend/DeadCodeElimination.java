package com.pl241.backend;

import java.util.ArrayList;
import java.util.HashMap;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;
import com.pl241.ir.BlockNode;
import com.pl241.ir.TreeNode;

/**
 * 
 * This class does dead code detection and elimination. The basic idea
 * is that it checks all conditions and see if they always true or false
 * and act based on it. It also remove necessary phi functions.
 * 
 * @author cesarghali
 *
 */
public class DeadCodeElimination
{
	// A map between blocks containing conditions (if or while) and their
	// corresponding blocks where the phi functions are located
	private ArrayList<BlockNode> conditionBlocks;
	// A map between phi functions ID and their later usage
	private HashMap<Integer, ArrayList<TreeNode>> phiIDUsageMap;
	// Contains <id, list of statements that refer to this id> mapping
	private HashMap<Integer, ArrayList<TreeNode>> statsIDMap;
	// Contains <id, value> map that is use for constant folding
	private HashMap<Integer, Integer> cfMap;
	
	// A flag that say there is error in execution
	private boolean errorFlag;
	
	
	public DeadCodeElimination(ArrayList<BlockNode> conditionBlocks,
			HashMap<Integer, ArrayList<TreeNode>> phiIDUsageMap,
			HashMap<Integer, ArrayList<TreeNode>> statsIDMap)
	{
		this.conditionBlocks = conditionBlocks;
		this.phiIDUsageMap = phiIDUsageMap;
		this.statsIDMap = statsIDMap;
		this.cfMap = new HashMap<Integer, Integer>();
	}
	
	@SuppressWarnings("incomplete-switch")
	public void DCEParser()
	{
		for (BlockNode conditionBlock : this.conditionBlocks)
		{
			if ((conditionBlock.hasParent() == false) || (conditionBlock.getPhiBlock().getLeftChild() == null))
			{
				continue;
			}
			
			// 1 means remove then or loop body, 2 means remove else
			int removeThenOrElse = 0;
			boolean stopLoop = false;
			
			for (int i = 0; i < ((BlockNode)(conditionBlock)).getStatementsSize(); i++)
			{
				if (((BlockNode)(conditionBlock)).getStatement(i).getType().compareType(Tokens.cmpSSAToken) == true)
				{
					TreeNode leftChild = ((BlockNode)(conditionBlock)).getStatement(i).getLeftChild().clone();
					TreeNode rightChild = ((BlockNode)(conditionBlock)).getStatement(i).getRightChild().clone();
					
					if ((leftChild.getType().compareType(Tokens.number) == true) &&
						(rightChild.getType().compareType(Tokens.number) == true))
					{
						switch (((BlockNode)(conditionBlock)).getStatement(i + 1).getType().getType())
						{
							case bneSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) == Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
									
								break;
								
							case beqSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) != Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
								break;
								
							case bleSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) > Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
								break;
								
							case bgtSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) <= Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
								break;
								
							case bgeSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) < Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
								break;
								
							case bltSSAToken:
								// Then always executed and else should be removed
								if (Integer.parseInt(leftChild.getType().getCharacters()) >= Integer.parseInt(rightChild.getType().getCharacters()))
								{
									removeThenOrElse = 2;
									stopLoop = true;
								}
								// Else always executed and then should be removed
								else
								{
									removeThenOrElse = 1;
									stopLoop = true;
								}
								break;
						}
					}
				}
				
				if (stopLoop == true)
				{
					break;
				}
			}
			
			if (conditionBlock.getType().compareType(Tokens.ifToken) == true)
			{
				if (removeThenOrElse != 0)
				{
					// Get the then block
					TreeNode beginThenBlock = conditionBlock.getLeftChild();
					TreeNode endThenBlock = conditionBlock.getPhiBlock().getParent(0);
					
					// Get the else block
					TreeNode beginElseBlock = conditionBlock.getRightChild();
					TreeNode endElseBlock = null;
					if (beginElseBlock != null)
					{
						endElseBlock = conditionBlock.getPhiBlock().getParent(1);
					}
					
					// Get the fi block
					TreeNode fiBlock = conditionBlock.getPhiBlock();
					
					// Unlink the if statement structure
					conditionBlock.setLeftChild(null);
					conditionBlock.setRightChild(null);
					beginThenBlock.clearParents();
					if (beginElseBlock != null)
					{
						beginElseBlock.clearParents();
					}
					endThenBlock.setLeftChild(null);
					if (endElseBlock != null)
					{
						endElseBlock.setLeftChild(null);
					}
					fiBlock.clearParents();
					
					// Remove then: link else instead of then
					if (removeThenOrElse == 1)
					{
						// Link else block (if exists) in then block place
						if (beginElseBlock != null)
						{
							conditionBlock.setLeftChild(beginElseBlock);
							endElseBlock.setLeftChild(fiBlock);
						}
						else
						{
							conditionBlock.setLeftChild(fiBlock);
						}
					}
					// Remove else: link then instead of else
					else if (removeThenOrElse == 2)
					{
						// Link then block back
						conditionBlock.setLeftChild(beginThenBlock);
						endThenBlock.setLeftChild(fiBlock);
						if (beginElseBlock != null)
						{
							((BlockNode)(endThenBlock)).deleteLastStatement();
						}
					}
					
					// Remove all phi functions and replace all their usage appropriately
					for (int i = conditionBlock.getPhiBlock().getStatementsSize() - 1; i >= 0; i--)
					{
						if (conditionBlock.getPhiBlock().getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
						{
							int phiID = conditionBlock.getPhiBlock().getStatement(i).getId();
							TreeNode phiReplacement = null;
							// Remove then
							if (removeThenOrElse == 1)
							{
								phiReplacement = conditionBlock.getPhiBlock().getStatement(i).getRightChild();
							}
							// Remove else
							else if (removeThenOrElse == 2)
							{
								phiReplacement = conditionBlock.getPhiBlock().getStatement(i).getLeftChild();
							}
							
							// Fix any future usage of the phi function
							for (TreeNode phiUsage : this.phiIDUsageMap.get(phiID))
							{
								phiUsage.setType(phiReplacement.getType().clone());
								if (phiReplacement.getType().compareType(Tokens.statToken) == true)
								{
									if (this.cfMap.containsKey(phiReplacement.getRefId()) == true)
									{
										phiUsage.setType(Token.getToken(String.valueOf(this.cfMap.get(phiReplacement.getRefId()))));
									}
									else
									{
										phiUsage.setRefId(phiReplacement.getRefId());
									}
									
									if (this.statsIDMap.containsKey(phiReplacement.getRefId()) == false)
									{
										this.statsIDMap.put(phiReplacement.getRefId(), new ArrayList<TreeNode>());
									}
									this.statsIDMap.get(phiReplacement.getRefId()).add(phiUsage);
								}
								
								if (phiReplacement.getType().compareType(Tokens.number) == true)
								{
									if (phiUsage.hasParent() == true)
									{
										this.performConstantFolding(phiUsage.getParent(0));
									}
								}
							}
							
							// Delete the phi function
							conditionBlock.getPhiBlock().deleteStatement(i);
						}
					}
					
					// Relink the condition (if) block
					TreeNode conditionBlockParent = conditionBlock.getLastParent();
					TreeNode conditionBlockChild = conditionBlock.getLeftChild();
					conditionBlock.clearParents();
					conditionBlockChild.clearParents();
					conditionBlock.setLeftChild(null);
					conditionBlockParent.setLeftChild(conditionBlockChild);
					
					// Relink the fi block if it's empty
					if (((BlockNode)(fiBlock)).getStatementsSize() == 0)
					{
						TreeNode fiBlockParent = fiBlock.getParent(0);
						TreeNode fiBlockChild = fiBlock.getLeftChild();
						conditionBlock.getPhiBlock().clearParents();
						fiBlockChild.deleteParent(fiBlock);
						conditionBlock.getPhiBlock().setLeftChild(null);
						fiBlockParent.setLeftChild(fiBlockChild);
					}
				}
			}
			else if (conditionBlock.getType().compareType(Tokens.whileToken) == true)
			{
				if (removeThenOrElse == 1)
				{
					// Get the body block
					TreeNode beginBodyBlock = conditionBlock.getRightChild();
					TreeNode endBodyBlock = conditionBlock.getPhiBlock().getLastParent();
					
					// Get the fi block
					TreeNode odBlock = conditionBlock.getLeftChild();
					
					// Unlink the while loop body
					conditionBlock.setRightChild(null);
					beginBodyBlock.clearParents();
					endBodyBlock.setLeftChild(null);					
					conditionBlock.deleteLastParent();

					// Remove all phi functions and replace all their usage appropriately
					for (int i = conditionBlock.getPhiBlock().getStatementsSize() - 1; i >= 0; i--)
					{
						if (conditionBlock.getPhiBlock().getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
						{
							int phiID = conditionBlock.getPhiBlock().getStatement(i).getId();
							TreeNode phiReplacement = conditionBlock.getPhiBlock().getStatement(i).getLeftChild();
							
							// Fix any future usage of the phi function
							for (TreeNode phiUsage : this.phiIDUsageMap.get(phiID))
							{
								phiUsage.setType(phiReplacement.getType().clone());
								if (phiReplacement.getType().compareType(Tokens.statToken) == true)
								{
									if (this.cfMap.containsKey(phiReplacement.getRefId()) == true)
									{
										phiUsage.setType(Token.getToken(String.valueOf(this.cfMap.get(phiReplacement.getRefId()))));
									}
									else
									{
										phiUsage.setRefId(phiReplacement.getRefId());
									}
									
									if (this.statsIDMap.containsKey(phiReplacement.getRefId()) == false)
									{
										this.statsIDMap.put(phiReplacement.getRefId(), new ArrayList<TreeNode>());
									}
									this.statsIDMap.get(phiReplacement.getRefId()).add(phiUsage);
								}
								
								if (phiReplacement.getType().compareType(Tokens.number) == true)
								{
									if (phiUsage.hasParent() == true)
									{
										this.performConstantFolding(phiUsage.getParent(0));
									}
								}
							}
							
							// Delete the phi function
							conditionBlock.getPhiBlock().deleteStatement(i);
						}
					}
					
					// Unlink the condition (while) block
					TreeNode conditionBlockParent = conditionBlock.getLastParent();
					TreeNode conditionBlockChild = conditionBlock.getLeftChild();
					conditionBlock.clearParents();
					conditionBlockChild.clearParents();
					conditionBlock.setLeftChild(null);
					conditionBlockParent.setLeftChild(conditionBlockChild);
					
					// Unlink the fi block if it's empty
					if (((BlockNode)(odBlock)).getStatementsSize() == 0)
					{
						TreeNode odBlockParent = odBlock.getParent(0);
						TreeNode odBlockChild = odBlock.getLeftChild();
						conditionBlock.getPhiBlock().clearParents();
						odBlockChild.clearParents();
						conditionBlock.getPhiBlock().setLeftChild(null);
						odBlockParent.setLeftChild(odBlockChild);
					}
				}
				else if (removeThenOrElse == 2)
				{
					System.out.println("Warrning: Possible infinite loop detected");
				}
			}
		}
	}
	
	private void performConstantFolding(TreeNode stat)
	{
		if ((stat.getLeftChild().getType().compareType(Tokens.number) == true) ||
			(stat.getRightChild().getType().compareType(Tokens.number) == true))
		{
			int result;
			switch (stat.getType().getType())
			{
				case addSSAToken:
					result = Integer.parseInt(stat.getLeftChild().getType().getCharacters()) +
							Integer.parseInt(stat.getRightChild().getType().getCharacters());
					break;
					
				case subSSAToken:
					result = Integer.parseInt(stat.getLeftChild().getType().getCharacters()) -
							Integer.parseInt(stat.getRightChild().getType().getCharacters());
					break;

				case mulSSAToken:
					result = Integer.parseInt(stat.getLeftChild().getType().getCharacters()) *
							Integer.parseInt(stat.getRightChild().getType().getCharacters());
					break;

				case divSSAToken:
					result = Integer.parseInt(stat.getLeftChild().getType().getCharacters()) /
							Integer.parseInt(stat.getRightChild().getType().getCharacters());
					break;
					
				default:
					return;
			}
			
			if (result < 0)
			{
				this.Error("Invalid number value '" + String.valueOf(result) + "'");
				this.errorFlag = true;
			}
			
			stat.getContainer().deleteStatement(stat);
			
			this.cfMap.put(stat.getId(), result);
			
			for (TreeNode statUsage : this.statsIDMap.get(stat.getId()))
			{
				statUsage.setType(Token.getToken(String.valueOf(result)));
				
				if (statUsage.hasParent() == true)
				{
					if (statUsage.getParent(0).getLeftChild() == statUsage)
					{
						if (statUsage.getParent(0).getRightChild().getType().compareType(Tokens.number) == true)
						{
							this.performConstantFolding(statUsage.getParent(0));
						}
					}
					else
					{
						if (statUsage.getParent(0).getLeftChild().getType().compareType(Tokens.number) == true)
						{
							this.performConstantFolding(statUsage.getParent(0));
						}
					}
				}
			}
		}
	}
	
	public boolean getErrorFlag()
	{
		return this.errorFlag;
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}
