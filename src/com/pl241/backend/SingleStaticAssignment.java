package com.pl241.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.pl241.frontend.Array;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;
import com.pl241.frontend.Variable;
import com.pl241.frontend.VariablesScope;
import com.pl241.ir.BlockNode;
import com.pl241.ir.DesignatorNode;
import com.pl241.ir.FunctionBlockNode;
import com.pl241.ir.LoadStoreNode;
import com.pl241.ir.MainNode;
import com.pl241.ir.ParamNode;
import com.pl241.ir.TreeNode;
import com.pl241.ir.TreeNode.DeleteReason;

/**
 * 
 * @author cesarghali
 *
 * This class will convert the graph that is the output of the
 * parser into SSA representation
 * 
 */
public class SingleStaticAssignment
{
	// The main node which is the head of the IR graph
	private TreeNode mainNode;
	// The data structure that holds all scopes and their variables
	private VariablesScope scope;
	// Contains a list of functions and all the global variables that are assigned
	// in the functions
	private HashMap<String, ArrayList<String>> funcAssignedVarMap;
	// Contains a list of variables used in multiple functions
	private ArrayList<String> multiScopeVariable;
	// The name if the main scope
	private String mainScopeName = "main";
	// Contains <id, statement> mapping
	private HashMap<Integer, TreeNode> statsMap;
	// Contains <id, list of statements that refer to this id> mapping
	private HashMap<Integer, ArrayList<TreeNode>> statsIDMap;
	// Contains <funcName, id> mapping
	private HashMap<String, FunctionInfo> funcAddrMap;
	// Number of instructions in the eliminated instruction list
	private final int numOfElimInstr = 7;
	// Contains all CSE instructions marked as deleted
	private HashMap<TreeNode, BlockNode> cseDeletedInstructions;
	// Contains all Copy Propagation(CP) instructions marked as deleted
	private HashMap<TreeNode, BlockNode> cpDeletedInstructions;
	// Contains the level of nested while loop
	private int loopLevel = 0;
	// Contains the list of nodes to be fixed while inside a loop
	private HashMap<String, ArrayList<TreeNode>> nodesToFixInLoops;
	// Contains the list of variables that will have phi while inside a loop
	private ArrayList<String> varWithPhiInLoop;
	
	/* Used for deadcode detection and elimination */
	// A map between blocks containing conditions (if or while) and their
	// corresponding blocks where the phi functions are located
	private ArrayList<BlockNode> conditionBlocks;
	// A map between phi functions ID and their later usage
	private HashMap<Integer, ArrayList<TreeNode>> phiIDUsageMap;
	
	// A flag that say there is error in execution
	private boolean errorFlag;
	
	// Contains the IDs of the phi functions with possible variables that might
	// be used without being initialized
	private HashMap<Integer, TreeNode> mightUseWOInit;
	// A map of global variables names and their IDs
	private HashMap<String, Integer> globalVarIDMap;
	
	// Contains a reference to all the blocks that contain phi functions
	private HashMap<BlockNode, BlockNode> phiContainers;
	
	private HashMap<String, Integer> arrayCSE;
	private HashSet<String> arraysChangedInLoop;
	

	public SingleStaticAssignment(TreeNode mainNode)
	{
		this.mainNode = mainNode;
		if (this.mainNode != null)
		{
			this.scope = ((MainNode)(this.mainNode)).getScope();
			this.funcAssignedVarMap = ((MainNode)(this.mainNode)).getFuncAssignedVarMap();
			this.multiScopeVariable = ((MainNode)(this.mainNode)).getMultiScopeVariable();
			this.statsMap = new HashMap<Integer, TreeNode>();
			this.statsIDMap = new HashMap<Integer, ArrayList<TreeNode>>();
			this.funcAddrMap = new HashMap<String, FunctionInfo>();
			this.cseDeletedInstructions = new HashMap<TreeNode, BlockNode>();
			this.cpDeletedInstructions = new HashMap<TreeNode, BlockNode>();
			this.nodesToFixInLoops = new HashMap<String, ArrayList<TreeNode>>();
			this.varWithPhiInLoop = new ArrayList<String>();
			
			this.conditionBlocks = new ArrayList<BlockNode>();
			this.phiIDUsageMap = new HashMap<Integer, ArrayList<TreeNode>>();
			
			this.errorFlag = false;
			
			this.mightUseWOInit = new HashMap<Integer, TreeNode>();
			this.globalVarIDMap = new HashMap<String, Integer>();
			
			this.phiContainers = new HashMap<BlockNode, BlockNode>();
			
			this.arrayCSE = new HashMap<String, Integer>();
			this.arraysChangedInLoop = new HashSet<String>();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void ParseSSA()
	{
		if (this.mainNode == null)
		{
			this.Error("Flow control graph does not exist");
			this.errorFlag = true;
			return;
		}
		
		// Contains a list of possible eliminated instructions 
		// ADD_INDEX = 0;
		// SUB_INDEX = 1;
		// MUL_INDEX = 2;
		// DIV_INDEX = 3;
		// CMP_INDEX = 4;
		// LOAD_INDEX = 5;
		// MOVE_INDEX = 6;
		ArrayList<TreeNode>[] elimInsts;
		elimInsts = new ArrayList[this.numOfElimInstr];
		for (int i = 0; i < this.numOfElimInstr; i++)
		{
			elimInsts[i] = new ArrayList<TreeNode>();
		}
		
		// Contains the mapping between the variable in copy propagation
		HashMap<String, CPMapInfo> cpMap = new HashMap<String, CPMapInfo>();
		
		TreeNode currentBlock = this.mainNode;
		// while we are not at the end of computation
		while (currentBlock.getType().compareType(Tokens.periodToken) == false)
		{
			// If currentBlock is main, move on
			if (currentBlock.getType().compareType(Tokens.mainToken) == true)
			{
				currentBlock = currentBlock.getLeftChild();
			}
			// If currentBlock is variable declaration, remove it from the IR
			else if (currentBlock.getType().compareType(Tokens.varDeclSeqToken) == true)
			{
				this.parseVarDeclSeq(currentBlock);
				currentBlock = currentBlock.getLeftChild();
			}
			// If the currentBlock is a function sequence block
			else if (currentBlock.getType().compareType(Tokens.funcSeqToken) == true)
			{
				this.parseFuncSequence(currentBlock);
				currentBlock = currentBlock.getLeftChild();
			}
			// If the currentBlock is either statement sequence, fi or od, parse it and move on
			else if ((currentBlock.getType().compareType(Tokens.statSeqToken) == true) ||
					 (currentBlock.getType().compareType(Tokens.fiToken) == true) ||
					 (currentBlock.getType().compareType(Tokens.odToken) == true))
			{
				this.parseStatSequence(currentBlock, this.mainScopeName, false, null, null, null, null, elimInsts, cpMap);
				
				currentBlock = currentBlock.getLeftChild();
			}
			// If the currentBlock is if statement parse it, then set the currentBlock to the
			// returned value of the parseIfStatement function which is a pointer to the fiBlock
			else if (currentBlock.getType().compareType(Tokens.ifToken) == true)
			{
				currentBlock = this.parseIfStatement(currentBlock, this.mainScopeName, null, null, null, elimInsts,
						cpMap);
			}
			// If the currentBlock is while statement parse it, then set the currentBlock to the
			// returned value of the parseWhileStatement function which is a pointer to the odBlock
			else if (currentBlock.getType().compareType(Tokens.whileToken) == true)
			{
				this.arraysChangedInLoop.clear();

				HashMap<String, Integer> oldMainScope = this.scope.cloneSSAIndices(this.mainScopeName);
				currentBlock = this.parseWhileStatement(currentBlock, this.mainScopeName, new HashMap<String, ArrayList<TreeNode>>(),
						oldMainScope, oldMainScope, elimInsts, cpMap);
			}
		}
		
		currentBlock.setType(Token.getToken(Tokens.endofProgToken));
	}
	
	private void parseVarDeclSeq(TreeNode currentNode)
	{
		BlockNode varDeclSeqBlock = ((BlockNode)(currentNode));
		ArrayList<TreeNode> variables = new ArrayList<TreeNode>();
		for (int i = 0; i < varDeclSeqBlock.getStatementsSize(); i++)
		{
			TreeNode varNode = varDeclSeqBlock.getStatement(i);
			do
			{
				// Unlink the variable name node from the tree
				TreeNode leftChild = varNode.getLeftChild();
				leftChild.clearParents();
				varNode.setLeftChild(null);
				// Add the variable to the list of variables to be added to the
				// varDeclSeqBlock block
				variables.add(leftChild);
				
				this.globalVarIDMap.put(leftChild.getType().getCharacters(), leftChild.getId());
				
				varNode = varNode.getRightChild();
			} while (varNode.getType().compareType(Tokens.semiToken) == false);
		}
		
		varDeclSeqBlock.clearStatements();
		for (TreeNode variable : variables)
		{
			varDeclSeqBlock.addStatement(variable);
		}
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param currentBlock
	 * 
	 * Each function has different eliminated instruction list
	 */
	private void parseFuncSequence(TreeNode currentBlock)
	{
		for (int i = 0; i < ((FunctionBlockNode)(currentBlock)).getFunctionsSize(); i++)
		{
			String funcName = ((FunctionBlockNode)(currentBlock)).getFunction(i).getIdentifier().getCharacters();
			
			// Contains a list of possible eliminated instructions 
			// ADD_INDEX = 0;
			// SUB_INDEX = 1;
			// MUL_INDEX = 2;
			// DIV_INDEX = 3;
			// CMP_INDEX = 4;
			// LOAD_INDEX = 5;
			ArrayList<TreeNode>[] elimInsts = new ArrayList[this.numOfElimInstr];
			for (int j = 0; j < this.numOfElimInstr; j++)
			{
				elimInsts[j] = new ArrayList<TreeNode>();
			}
			
			// Contains the mapping between the variable in copy propagation
			HashMap<String, CPMapInfo> cpMap = new HashMap<String, CPMapInfo>();
			
			// Add the function name and the function info to the funcAddrMap
			ParamNode paramNode = ((ParamNode)(((FunctionBlockNode)(currentBlock)).getFunction(i).getRightChild()));
			ArrayList<String> parameters = new ArrayList<String>();
			if (paramNode != null)
			{
				for (int j = 0; j < paramNode.getParametersSize(); j++)
				{
					parameters.add(paramNode.getParameter(j).getCharacters());
				}
			}
			this.funcAddrMap.put(funcName, new FunctionInfo(((FunctionBlockNode)(currentBlock)).getFunction(i).getId(), parameters));
		
			// Remove the right child of the function block which is ParamNode
			((FunctionBlockNode)(currentBlock)).getFunction(i).setRightChild(null);
			
			// Add the parameters as statements to the function body. Also add them to cpMap
			// First get the body first block
			TreeNode bodyFirstBlock = ((FunctionBlockNode)(currentBlock)).getFunction(i).getLeftChild();
			while (bodyFirstBlock.getType().compareType(Tokens.varDeclSeqToken) == true)
			{
				bodyFirstBlock = bodyFirstBlock.getLeftChild();
			}
			
			TreeNode param;
			if (bodyFirstBlock.getType().compareType(Tokens.statSeqToken) == true)
			{
				// If the first block in the function body is statements sequence block, add
				// each parameter as a designator node at the beginning of the statements list
				for (int j = 0; j < parameters.size(); j++)
				{
					param = new DesignatorNode(Token.getToken(parameters.get(j)));
					((BlockNode)(bodyFirstBlock)).addStatementAtBeginning(param);
					
					// Add a mapping to this variable into the cpMap
					cpMap.put(parameters.get(j), new CPMapInfo(param, ""));
				}
			}
			else
			{
				// If the first block is not a statements sequence block, create a block node,
				// and add each parameter to it
				BlockNode paramBlock = new BlockNode(Token.getToken(Tokens.statSeqToken));
				for (int j = 0; j < parameters.size(); j++)
				{
					param = new DesignatorNode(Token.getToken(parameters.get(j)));
					paramBlock.addStatementAtBeginning(param);
					
					// Add a mapping to this variable into the cpMap
					cpMap.put(parameters.get(j), new CPMapInfo(param, ""));
				}
				
				// Insert it as the first block in the function body
				TreeNode bodyFirstBlockParent = bodyFirstBlock.getLastParent();
				if (bodyFirstBlock.getType().compareType(Tokens.whileToken) == false)
				{
					bodyFirstBlock.clearParents();
									
				}
				else
				{
					bodyFirstBlock.deleteParent(bodyFirstBlockParent);
				}
				
				bodyFirstBlockParent.setLeftChild(paramBlock);
				paramBlock.setLeftChild(bodyFirstBlock);
			}
			
			
			// Parse the function body
			TreeNode funcCurrentBlock = ((FunctionBlockNode)(currentBlock)).getFunction(i).getLeftChild();
			while (funcCurrentBlock != null)
			{
				// If funcCurrentBlock is variable declaration block, remove it from IR
				if (funcCurrentBlock.getType().compareType(Tokens.varDeclSeqToken) == true)
				{
					TreeNode next = funcCurrentBlock.getLeftChild();
					funcCurrentBlock.getParent(0).setLeftChild(next);
					funcCurrentBlock = next;
				}
				// If the funcCurrentBlock is either statement sequence, fi or od, parse it and move on				
				else if ((funcCurrentBlock.getType().compareType(Tokens.statSeqToken) == true) ||
					(funcCurrentBlock.getType().compareType(Tokens.fiToken) == true) ||
					(funcCurrentBlock.getType().compareType(Tokens.odToken) == true))
				{
					this.parseStatSequence(funcCurrentBlock, funcName, false, null, null, null, null, elimInsts, cpMap);
					
					funcCurrentBlock = funcCurrentBlock.getLeftChild();
				}
				// If the funcCurrentBlock is if statement parse it, then set the currentBlock to the
				// returned value of the parseIfStatement function which is a pointer to the fiBlock
				else if (funcCurrentBlock.getType().compareType(Tokens.ifToken) == true)
				{
					funcCurrentBlock = this.parseIfStatement(funcCurrentBlock, funcName, null, null, null, elimInsts,
							cpMap);
				}
				// If the funcCurrentBlock is while statement parse it, then set the currentBlock to the
				// returned value of the parseWhileStatement function which is a pointer to the odBlock
				else if (funcCurrentBlock.getType().compareType(Tokens.whileToken) == true)
				{
					this.arraysChangedInLoop.clear();

					funcCurrentBlock = this.parseWhileStatement(funcCurrentBlock, funcName,
							new HashMap<String, ArrayList<TreeNode>>(), this.scope.cloneSSAIndices(funcName),
							this.scope.cloneSSAIndices(this.mainScopeName), elimInsts, cpMap);
				}
			}
		}
	}
	
	private void parseStatSequence(TreeNode currentBlock, String scopeName, boolean createPhiStats,
			HashMap<String, TreeNode> varIndex, HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex,
			HashMap<String, Integer> oldCurrentScope, HashMap<String, Integer> oldMainScope,
			ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap)
	{
		// This array list will contain all statements in SSA and two operands format
		ArrayList<TreeNode> statements = new ArrayList<TreeNode>();
		// Add all phi statements to the beginning of the statements list
		for (int i = 0; i < ((BlockNode)(currentBlock)).getStatementsSize(); i++)
		{
			if ((((BlockNode)(currentBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true) ||
				(((BlockNode)(currentBlock)).getStatement(i).getType().compareType(Tokens.ident) == true))
			{
				statements.add(((BlockNode)(currentBlock)).getStatement(i));
				this.statsMap.put(((BlockNode)(currentBlock)).getStatement(i).getId(),
						((BlockNode)(currentBlock)).getStatement(i));
				this.statsIDMap.put(((BlockNode)(currentBlock)).getStatement(i).getId(), new ArrayList<TreeNode>());
			}
		}
		
		// Parse all let, return and call statements
		for (int i = 0; i < ((BlockNode)(currentBlock)).getStatementsSize(); i++)
		{
			if (((BlockNode)(currentBlock)).getStatement(i).getType().compareType(Tokens.letToken) == true)
			{
				this.parseAssignment(currentBlock, ((BlockNode)(currentBlock)).getStatement(i), scopeName, statements, createPhiStats,
						varIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
			}
			else if (((BlockNode)(currentBlock)).getStatement(i).getType().compareType(Tokens.returnToken) == true)
			{
				TreeNode retNode = this.parseReturnStatement(currentBlock, ((BlockNode)(currentBlock)).getStatement(i), scopeName,
						statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
				statements.add(retNode);
				this.statsMap.put(retNode.getId(), retNode);
				this.statsIDMap.put(retNode.getId(), new ArrayList<TreeNode>());
			}
			else if (((BlockNode)(currentBlock)).getStatement(i).getType().compareType(Tokens.callToken) == true)
			{
				String functionName = ((BlockNode)(currentBlock)).getStatement(i).getLeftChild().getType().getCharacters();
				TreeNode callNode = this.parseFuncCall(currentBlock, ((BlockNode)(currentBlock)).getStatement(i), scopeName,
						statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
				
				// Forget all CP maps for global variables assigned in the called function
				if (this.funcAssignedVarMap.containsKey(functionName) == true)
				{
					for (String globalVarName : this.funcAssignedVarMap.get(functionName))
					{
						cpMap.remove(globalVarName);
					}
				}
				
				statements.add(callNode);
				this.statsMap.put(callNode.getId(), callNode);
				this.statsIDMap.put(callNode.getId(), new ArrayList<TreeNode>());
			}
		}
		
		// Deleting all statements from currentBlock then Add all statements to currentBlock
		((BlockNode)(currentBlock)).clearStatements();
		for (int i = 0; i < statements.size(); i++)
		{
			((BlockNode)(currentBlock)).addStatement(statements.get(i));
			
			if (statements.get(i).getDeleteReason() == DeleteReason.CSE)
			{
				this.cseDeletedInstructions.put(statements.get(i), ((BlockNode)(currentBlock)));
			}
			else if (statements.get(i).getDeleteReason() == DeleteReason.CP)
			{
				this.cpDeletedInstructions.put(statements.get(i), ((BlockNode)(currentBlock)));
			}
		}
		// Clear the statements array list
		statements.clear();
	}
	
	@SuppressWarnings("incomplete-switch")
	private void parseAssignment(TreeNode currentBlock, TreeNode letNode, String scopeName, ArrayList<TreeNode> statements,
			boolean createPhiStats, HashMap<String, TreeNode> varIndex, HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex,
			HashMap<String, Integer> oldCurrentScope, HashMap<String, Integer> oldMainScope,
			ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap)
	{
		if ((letNode.getLeftChild().getType().compareType(Tokens.plusToken) == true) ||
			(letNode.getLeftChild().getType().compareType(Tokens.minusToken) == true) ||
			(letNode.getLeftChild().getType().compareType(Tokens.timesToken) == true) ||
			(letNode.getLeftChild().getType().compareType(Tokens.divToken) == true))
		{
			this.parseAssignment(currentBlock, letNode.getLeftChild(), scopeName, statements, createPhiStats,
					varIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
		}
		else if (letNode.getLeftChild().getType().compareType(Tokens.callToken) == true)
		{
			String functionName = letNode.getLeftChild().getLeftChild().getType().getCharacters();
			// Parse the function call statement
			TreeNode callNode = this.parseFuncCall(currentBlock, letNode.getLeftChild(), scopeName, statements,
					varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
			
			// Forget all CP maps for global variables assigned in the called function
			if (this.funcAssignedVarMap.containsKey(functionName) == true)
			{
				for (String globalVarName : this.funcAssignedVarMap.get(functionName))
				{
					cpMap.remove(globalVarName);
				}
			}
			
			// Add the callNode to the statements list and to the statement map
			statements.add(callNode);
			this.statsMap.put(callNode.getId(), callNode);
			this.statsIDMap.put(callNode.getId(), new ArrayList<TreeNode>());
			
			// Create a new node with the reference ID of the letNode
			TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), callNode.getId());
			
			// Replace statNode with letNode
			TreeNode.Implant(statNode, callNode);
		}
		
		if ((letNode.getRightChild().getType().compareType(Tokens.plusToken) == true) ||
			(letNode.getRightChild().getType().compareType(Tokens.minusToken) == true) ||
			(letNode.getRightChild().getType().compareType(Tokens.timesToken) == true) ||
			(letNode.getRightChild().getType().compareType(Tokens.divToken) == true))
		{
			this.parseAssignment(currentBlock, letNode.getRightChild(), scopeName, statements, createPhiStats,
					varIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
		}
		else if (letNode.getRightChild().getType().compareType(Tokens.callToken) == true)
		{
			String functionName = letNode.getRightChild().getLeftChild().getType().getCharacters();
			// Parse the function call statement
			TreeNode callNode = this.parseFuncCall(currentBlock, letNode.getRightChild(), scopeName, statements,
					varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap);
			
			// Forget all CP maps for global variables assigned in the called function
			if (this.funcAssignedVarMap.containsKey(functionName) == true)
			{
				for (String globalVarName : this.funcAssignedVarMap.get(functionName))
				{
					cpMap.remove(globalVarName);
				}
			}
			
			// Add the callNode to the statements list and to the statement map
			statements.add(callNode);
			this.statsMap.put(callNode.getId(), callNode);
			this.statsIDMap.put(callNode.getId(), new ArrayList<TreeNode>());
			
			// Create a new node with the reference ID of the letNode
			TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), callNode.getId());
			
			// Replace statNode with letNode
			TreeNode.Implant(statNode, callNode);
		}
		
		// If the node is let should, we create a new index for the leftChild ident
		if (letNode.getType().compareType(Tokens.letToken) == true)
		{
			// If right child is DesignatorNode, it means that it is either variable or
			// array
			String cpMapVarName = "";
			if (letNode.getRightChild() instanceof DesignatorNode)
			{
				// If the number of expressions is 0, then right child is a variable
				if (((DesignatorNode)(letNode.getRightChild())).getExpressionsSize() == 0)
				{
					// Set its SSA index of the right child
					if (letNode.getRightChild().getType().compareType(Tokens.ident) == true)
					{
						String varName = letNode.getRightChild().getType().getCharacters();
						cpMapVarName = varName;
						if (cpMap.containsKey(varName) == true)
						{
							if (cpMap.get(varName).getVariableName().compareTo("") != 0)
							{
								cpMapVarName = cpMap.get(varName).getVariableName();
							}
							
							if ((cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true) ||
								(cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true))
							{
								letNode.setRightChild(cpMap.get(varName).getNode().clone());
							}
							else
							{
								letNode.setRightChild(new TreeNode(Token.getToken(Tokens.statToken),
										cpMap.get(varName).getNode().getId()));
								
								// Check if phiIDUsageMap contains an entry for the right child of letNode
								// If yes, add the right child to phiIDUsageMap
								if (this.phiIDUsageMap.containsKey(letNode.getRightChild().getRefId()) == true)
								{
									this.phiIDUsageMap.get(letNode.getRightChild().getRefId()).add(letNode.getRightChild());
								}
								
								// Add the map to statsIDMap if a key exists
								if (this.statsIDMap.containsKey(letNode.getRightChild().getRefId()) == true)
								{
									this.statsIDMap.get(letNode.getRightChild().getRefId()).add(letNode.getRightChild());
								}
							}
							
							if (this.mightUseWOInit.containsKey(letNode.getRightChild().getRefId()) == true)
							{
								if (this.multiScopeVariable.contains(this.mightUseWOInit.get(letNode.getRightChild().getRefId()).getType().getCharacters()) == false)
								{
									this.Error("Variable '" + this.mightUseWOInit.get(letNode.getRightChild().getRefId()).getType().getCharacters() +
											"' is used without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
							}
							
							if (varsWithOldSSAIndex != null)
							{
								if (varsWithOldSSAIndex.containsKey(varName) == false)
								{
									varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
								}
								letNode.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(varName).add(letNode.getRightChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(varName) == false)
									{
										this.nodesToFixInLoops.put(varName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(varName).add(letNode);
								}
							}
						}
						else
						{
							if (this.multiScopeVariable.contains(letNode.getRightChild().getType().getCharacters()) == true)
							{
								String globalVarName = letNode.getRightChild().getType().getCharacters();
								letNode.getRightChild().setType(Token.getToken(Tokens.statToken));
								letNode.getRightChild().setRefId(this.globalVarIDMap.get(globalVarName));
								letNode.getRightChild().getType().setIsGlobalVariable(true);
							}
							else
							{
								int currentSSAIndex;
								if (this.scope.containVariable(scopeName, letNode.getRightChild().getType().getCharacters()) == true)
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(scopeName,
											letNode.getRightChild().getType().getCharacters());
								}
								else
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName,
											letNode.getRightChild().getType().getCharacters());
								}
								letNode.getRightChild().getType().setSSAIndex(currentSSAIndex);
								
								if (currentSSAIndex == 0)
								{
									this.Error("Variable '" + letNode.getRightChild().getType().getCharacters() + "' is used " +
											"without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
							}
						}
					}
				}
				// Else, right child is array
				else
				{
					// TODO
					DesignatorNode array = ((DesignatorNode)(letNode.getRightChild()));
					String key = array.getType().getCharacters();
					for (int index = 0; index < array.getExpressionsSize(); index++)
					{
						TreeNode fixedIndex = this.fixParamSSAIndex(currentBlock, array.getExpression(index),
								scopeName, statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap, false);
						if (fixedIndex != null)
						{
							if (fixedIndex.getType().compareType(Tokens.number) == true)
							{
								key += fixedIndex.getType().getCharacters();
							}
							else if (fixedIndex.getType().compareType(Tokens.statSeqToken) == true)
							{
								key += fixedIndex.getRefId();
							}
						}
						else
						{
							key += array.getExpression(index).getType().getCharacters();
						}
					}
					
					if (this.arrayCSE.containsKey(key) == true)
					{
						int arrayLoadID = this.arrayCSE.get(key);
						
						// Create a new node with the reference ID of the loadNode
						TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), arrayLoadID);
						
						// Replace statNode with loadNode
						TreeNode.Implant(statNode, letNode.getRightChild());
					}
					else
					{
						int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(letNode.getRightChild())), scopeName,
								statements, createPhiStats, varIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope,
								elimInstrs, cpMap);
						
						LoadStoreNode loadNode = new LoadStoreNode(Token.getToken(Tokens.loadSSAToken),
								letNode.getRightChild().getType().getCharacters());
						loadNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
						
						// Process CSE for load statements
						TreeNode matchingNode;
						if (this.childsInNodeToFix(letNode.getRightChild()) == false)
						{
							matchingNode = this.CSECheck(elimInstrs, loadNode.getType().getValue() - 400, letNode.getRightChild());
						}
						else
						{
							matchingNode = null;
						}
						if (matchingNode == null)
						{
							// Add letNode to the list of eliminatedInstructions
							letNode.getRightChild().setRefId(loadNode.getId());
							elimInstrs[loadNode.getType().getValue() - 400].add(letNode.getRightChild());
							
							// Add the loadNode to the statsMap so it can be referenced
							this.statsMap.put(loadNode.getId(), loadNode);
							this.statsIDMap.put(loadNode.getId(), new ArrayList<TreeNode>());
	
							// Create a new node with the reference ID of the loadNode
							TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), loadNode.getId());
							
							// Replace statNode with loadNode
							TreeNode.Implant(statNode, letNode.getRightChild());
							
							// Add loadNode to the list of statements that will eventually be the statements of the block
							statements.add(loadNode);
							
							this.arrayCSE.put(key, loadNode.getId());
						}
						else
						{
							// Create a new node with the reference ID of the loadNode
							TreeNode statNode;
							if (matchingNode.getType().compareType(Tokens.statToken) == true)
							{
								statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
							}
							else
							{
								statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
							}
							
							// Replace statNode with loadNode
							TreeNode.Implant(statNode, letNode.getRightChild());
							
							// Mark the adda statement as deleted as well
							statements.get(statements.size() - 1).setDeleteReason(DeleteReason.CSE);
							// Add loadNode to the list of statements that will eventually be the statements of the block,
							// but mark it as deleted
							statements.add(loadNode);
							loadNode.setDeleteReason(DeleteReason.CSE);
						}
					}
				}
			}
			
			// If the number of expressions is 0, then left child is a variable
			if (((DesignatorNode)(letNode.getLeftChild())).getExpressionsSize() == 0)
			{
				// Add the map between the left side variable and the expression
				// on the right side. This map will be use later
				cpMap.put(letNode.getLeftChild().getType().getCharacters(),
						new CPMapInfo(letNode.getRightChild(), cpMapVarName));
				
				// Mark this move instruction as deleted due to CP
				if (this.multiScopeVariable.contains(letNode.getLeftChild().getType().getCharacters()) == false)
				{
					letNode.setDeleteReason(DeleteReason.CP);
				}
					
				// Get the current SSA index of the assigned variable
				int currentSSAIndex;
				if (this.scope.containVariable(scopeName, letNode.getLeftChild().getType().getCharacters()) == true)
				{
					currentSSAIndex = this.scope.getVariableSSAIndex(scopeName,
						letNode.getLeftChild().getType().getCharacters());
				}
				else
				{
					currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName,
							letNode.getLeftChild().getType().getCharacters());
				}
				
				// Add the variable index to the varIndex HashMap, this index will be
				// processed by ifStatement and whileStatement functions to build the
				// phi functions
				if (createPhiStats == true)
				{
					varIndex.put(letNode.getLeftChild().getType().getCharacters(), letNode.getRightChild());
					
					if (this.loopLevel > 0)
					{
						this.varWithPhiInLoop.add(letNode.getLeftChild().getType().getCharacters());
					}
				}
				
				// Update the variable SSA index in the appropriate scope
				if (this.scope.containVariable(scopeName, letNode.getLeftChild().getType().getCharacters()) == true)
				{
					this.scope.modifyVariable(scopeName, letNode.getLeftChild().getType().getCharacters(), currentSSAIndex + 1);
				}
				else
				{
					this.scope.modifyVariable(this.mainScopeName, letNode.getLeftChild().getType().getCharacters(),
							currentSSAIndex + 1);
				}
				letNode.getLeftChild().getType().setSSAIndex(currentSSAIndex + 1);
				
				// Change the type of the letNode to movSSA
				letNode.setType(Token.getToken(Tokens.movSSAToken));
				
				// Add the letNode to the statsMap so it can be referenced
				this.statsMap.put(letNode.getId(), letNode);
				this.statsIDMap.put(letNode.getId(), new ArrayList<TreeNode>());

				// Add letNode to the list of statements that will eventually be the statements of the block
				statements.add(letNode);
				
				if (this.multiScopeVariable.contains(letNode.getLeftChild().getType().getCharacters()) == true)
				{
					String globalVarName = letNode.getLeftChild().getType().getCharacters();
					letNode.getLeftChild().setType(Token.getToken(Tokens.statToken));
					letNode.getLeftChild().setRefId(this.globalVarIDMap.get(globalVarName));
					letNode.getLeftChild().getType().setIsGlobalVariable(true);
				}
			}
			// Else, left child is array
			else
			{
				HashSet<String> keysToBeDeleted = new HashSet<String>();
				for (String key : this.arrayCSE.keySet())
				{
					if (key.startsWith(((DesignatorNode)(letNode.getLeftChild())).getType().getCharacters()) == true)
					{
						keysToBeDeleted.add(key);
					}
				}
				for (String keyToBeDeleted : keysToBeDeleted)
				{
					this.arrayCSE.remove(keyToBeDeleted);
				}
				
				this.arraysChangedInLoop.add(((DesignatorNode)(letNode.getLeftChild())).getType().getCharacters());
				
				int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(letNode.getLeftChild())), scopeName,
						statements, createPhiStats, varIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope,
						elimInstrs, cpMap);
				
				LoadStoreNode storeNode = new LoadStoreNode(Token.getToken(Tokens.storeSSAToken),
						letNode.getLeftChild().getType().getCharacters());
				storeNode.setLeftChild(letNode.getRightChild().clone());
				storeNode.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), refId));

				if (varsWithOldSSAIndex != null)
				{
					if (varsWithOldSSAIndex.containsKey(cpMapVarName) == false)
					{
						varsWithOldSSAIndex.put(cpMapVarName, new ArrayList<TreeNode>());
					}
					storeNode.setLoopLevel(this.loopLevel);
					varsWithOldSSAIndex.get(cpMapVarName).add(storeNode.getLeftChild());
					
					if (this.loopLevel > 0)
					{
						if (this.nodesToFixInLoops.containsKey(cpMapVarName) == false)
						{
							this.nodesToFixInLoops.put(cpMapVarName, new ArrayList<TreeNode>());
						}
						this.nodesToFixInLoops.get(cpMapVarName).add(storeNode);
					}
				}
				
				this.statsMap.put(storeNode.getId(), storeNode);
				this.statsIDMap.put(storeNode.getId(), new ArrayList<TreeNode>());

				statements.add(storeNode);
				
				// Process CSE for store statements, basically forget everything you know about the array
				this.forgetArray(elimInstrs, Token.getToken(Tokens.loadSSAToken).getValue() - 400, storeNode.getArrayName());
			}
		}
		else
		{
			// If both left and right children are numbers, do the math and replace the node. (Constant Folding)
			if ((letNode.getLeftChild().getType().compareType(Tokens.number) == true) &&
				(letNode.getRightChild().getType().compareType(Tokens.number) == true))
			{
				switch (letNode.getType().getType())
				{
					case plusToken:
						letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) +
								Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
						break;
						
					case minusToken:
						letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) -
								Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
						break;
						
					case timesToken:
						letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) *
								Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
						break;
						
					case divToken:
						letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) /
								Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
						break;
				}
				
				if (Integer.parseInt(letNode.getType().getCharacters()) < 0)
				{
					this.Error("Invalid number value '" + letNode.getType().getCharacters() + "'");
					this.errorFlag = true;
				}
				
				// Disconnect left and right children from the parent node
				letNode.getLeftChild().clearParents();
				letNode.getRightChild().clearParents();
				letNode.setLeftChild(null);
				letNode.setRightChild(null);
			}
			else
			{
				// Change the type of the letNode according to the operation
				switch (letNode.getType().getType())
				{
					case plusToken:
						letNode.setType(Token.getToken(Tokens.addSSAToken));
						break;
						
					case minusToken:
						letNode.setType(Token.getToken(Tokens.subSSAToken));
						break;
						
					case timesToken:
						letNode.setType(Token.getToken(Tokens.mulSSAToken));
						break;
						
					case divToken:
						letNode.setType(Token.getToken(Tokens.divSSAToken));
						break;
				}
				
				// Contains the index in the eliminatedInstructions list
				int elimInstrIndex = letNode.getType().getValue() - 400;	// the difference of index is 400
				
				if (letNode.getLeftChild().getType().compareType(Tokens.ident) == true)
				{
					// If the number of expressions is 0, the left child is a variable
					if (((DesignatorNode)(letNode.getLeftChild())).getExpressionsSize() == 0)
					{
						String varName = letNode.getLeftChild().getType().getCharacters();
//						// TODO: remove this in case of errors
//						String cpMapVarName = varName;
						
						// Check CP map and replace the left child variable with the one in the map
						if (cpMap.containsKey(varName) == true)
						{
//							if (cpMap.get(varName).getVariableName().compareTo("") != 0)
//							{
//								cpMapVarName = cpMap.get(varName).getVariableName();
//							}
							
							if ((cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true) ||
								(cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true))
							{
								letNode.setLeftChild(cpMap.get(varName).getNode().clone());
							}
							else
							{
								letNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken),
										cpMap.get(varName).getNode().getId()));
								
								// Check if phiIDUsageMap contains an entry for the left child of letNode
								// If yes, add the left child to phiIDUsageMap
								if (this.phiIDUsageMap.containsKey(letNode.getLeftChild().getRefId()) == true)
								{
									this.phiIDUsageMap.get(letNode.getLeftChild().getRefId()).add(letNode.getLeftChild());
								}
								
								// Add the map to statsIDMap if a key exists
								if (this.statsIDMap.containsKey(letNode.getLeftChild().getRefId()) == true)
								{
									this.statsIDMap.get(letNode.getLeftChild().getRefId()).add(letNode.getLeftChild());
								}
							}
							
							if (this.mightUseWOInit.containsKey(letNode.getLeftChild().getRefId()) == true)
							{
								if (this.multiScopeVariable.contains(this.mightUseWOInit.get(letNode.getLeftChild().getRefId()).getType().getCharacters()) == false)
								{
									this.Error("Variable '" + this.mightUseWOInit.get(letNode.getLeftChild().getRefId()).getType().getCharacters() +
											"' is used without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
							}
							
							if (varsWithOldSSAIndex != null)
							{
								if (varsWithOldSSAIndex.containsKey(varName) == false) // TODO varName was cpMapVarName
								{
									varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
								}
								letNode.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(varName).add(letNode.getLeftChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(varName) == false)
									{
										this.nodesToFixInLoops.put(varName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(varName).add(letNode);
								}
							}
						}
						
						if (letNode.getLeftChild().getType().compareType(Tokens.ident) == true)
						{
							if (this.multiScopeVariable.contains(letNode.getLeftChild().getType().getCharacters()) == true)
							{
								String globalVarName = letNode.getLeftChild().getType().getCharacters();
								letNode.getLeftChild().setType(Token.getToken(Tokens.statToken));
								letNode.getLeftChild().setRefId(this.globalVarIDMap.get(globalVarName));
								letNode.getLeftChild().getType().setIsGlobalVariable(true);
							}
							else
							{
								// Set the SSA index of the ident node which is the left child of the letNode
								int currentSSAIndex;
								if (this.scope.containVariable(scopeName, letNode.getLeftChild().getType().getCharacters()) == true)
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(scopeName,
										letNode.getLeftChild().getType().getCharacters());
								}
								else
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName,
											letNode.getLeftChild().getType().getCharacters());
								}
								letNode.getLeftChild().getType().setSSAIndex(currentSSAIndex);
								
								if (currentSSAIndex == 0)
								{
									this.Error("Variable '" + letNode.getLeftChild().getType().getCharacters() + "' is used " +
											"without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
								
								// Add the left child to the varWithOldSSAIndex map if its current SSA index is equal to
								// its index in oldScope
								if (varsWithOldSSAIndex != null)
								{
									int oldSSAIndex;
									if (oldCurrentScope.containsKey(letNode.getLeftChild().getType().getCharacters()) == true)
									{
										oldSSAIndex = oldCurrentScope.get(letNode.getLeftChild().getType().getCharacters());
									}
									else
									{
										oldSSAIndex = oldMainScope.get(letNode.getLeftChild().getType().getCharacters());
									}
									
									if (currentSSAIndex == oldSSAIndex)
									{
										if (varsWithOldSSAIndex.containsKey(letNode.getLeftChild().getType().getCharacters()) == false)
										{
											varsWithOldSSAIndex.put(letNode.getLeftChild().getType().getCharacters(), new ArrayList<TreeNode>());
										}
										letNode.setLoopLevel(this.loopLevel);
										varsWithOldSSAIndex.get(letNode.getLeftChild().getType().getCharacters()).add(letNode.getLeftChild());
										
										if (this.loopLevel > 0)
										{
											if (this.nodesToFixInLoops.containsKey(letNode.getLeftChild().getType().getCharacters()) == false)
											{
												this.nodesToFixInLoops.put(letNode.getLeftChild().getType().getCharacters(), new ArrayList<TreeNode>());
											}
											this.nodesToFixInLoops.get(letNode.getLeftChild().getType().getCharacters()).add(letNode);
										}
									}
								}
							}
						}
					}
					// Else, left child is array
					else
					{
						DesignatorNode array = ((DesignatorNode)(letNode.getRightChild()));
						String key = array.getType().getCharacters();
						for (int index = 0; index < array.getExpressionsSize(); index++)
						{
							TreeNode fixedIndex = this.fixParamSSAIndex(currentBlock, array.getExpression(index),
									scopeName, statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap, false);
							if (fixedIndex != null)
							{
								if (fixedIndex.getType().compareType(Tokens.number) == true)
								{
									key += fixedIndex.getType().getCharacters();
								}
								else if (fixedIndex.getType().compareType(Tokens.statSeqToken) == true)
								{
									key += fixedIndex.getRefId();
								}
							}
							else
							{
								key += array.getExpression(index).getType().getCharacters();
							}
						}
						
						if (this.arrayCSE.containsKey(key) == true)
						{
							int arrayLoadID = this.arrayCSE.get(key);
							
							// Create a new node with the reference ID of the loadNode
							TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), arrayLoadID);
							
							// Replace statNode with loadNode
							TreeNode.Implant(statNode, letNode.getLeftChild());
						}
						else
						{
							int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(letNode.getLeftChild())),
									scopeName, statements, createPhiStats, varIndex, varsWithOldSSAIndex, oldCurrentScope,
									oldMainScope, elimInstrs, cpMap);
							
							LoadStoreNode loadNode = new LoadStoreNode(Token.getToken(Tokens.loadSSAToken),
									letNode.getLeftChild().getType().getCharacters());
							loadNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
							
							// Process CSE for load statements
							TreeNode matchingNode;
							if (this.childsInNodeToFix(letNode.getLeftChild()) == false)
							{
								matchingNode = this.CSECheck(elimInstrs, loadNode.getType().getValue() - 400, letNode.getLeftChild());
							}
							else
							{
								matchingNode = null;
							}
							if (matchingNode == null)
							{
								// Add letNode to the list of eliminatedInstructions
								letNode.getLeftChild().setRefId(loadNode.getId());
								elimInstrs[loadNode.getType().getValue() - 400].add(letNode.getLeftChild());
								
								// Add the loadNode to the statsMap so it can be referenced
								this.statsMap.put(loadNode.getId(), loadNode);
								this.statsIDMap.put(loadNode.getId(), new ArrayList<TreeNode>());
	
								// Create a new node with the reference ID of the loadNode
								TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), loadNode.getId());
								
								// Replace statNode with loadNode
								TreeNode.Implant(statNode, letNode.getLeftChild());
								
								// Add loadNode to the list of statements that will eventually be the statements of the block
								statements.add(loadNode);
								
								this.arrayCSE.put(key, loadNode.getId());
							}
							else
							{
								// Create a new node with the reference ID of the loadNode
								TreeNode statNode;
								if (matchingNode.getType().compareType(Tokens.statToken) == true)
								{
									statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
								}
								else
								{
									statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
								}
								
								// Replace statNode with loadNode
								TreeNode.Implant(statNode, letNode.getLeftChild());
								
								// Mark the adda statement as deleted as well
								statements.get(statements.size() - 1).setDeleteReason(DeleteReason.CSE);
								// Add loadNode to the list of statements that will eventually be the statements of the block,
								// but mark it as deleted
								statements.add(loadNode);
								loadNode.setDeleteReason(DeleteReason.CSE);
							}
						}
					}
				}
				
				if (letNode.getRightChild().getType().compareType(Tokens.ident) == true)
				{
					// If the number of expressions is 0, the right child is a variable
					if (((DesignatorNode)(letNode.getRightChild())).getExpressionsSize() == 0)
					{
						String varName = letNode.getRightChild().getType().getCharacters();
//						// TODO: remove this in case of errors
//						String cpMapVarName = varName;
						// Check CP map and replace the right child variable with the one in the map
						if (cpMap.containsKey(varName) == true)
						{
//							if (cpMap.get(varName).getVariableName().compareTo("") != 0)
//							{
//								cpMapVarName = cpMap.get(varName).getVariableName();
//							}

							if ((cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true) ||
								(cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true))
							{
								letNode.setRightChild(cpMap.get(varName).getNode().clone());
							}
							else
							{
								letNode.setRightChild(new TreeNode(Token.getToken(Tokens.statToken),
										cpMap.get(varName).getNode().getId()));
								
								// Check if phiIDUsageMap contains an entry for the right child of letNode
								// If yes, add the right child to phiIDUsageMap
								if (this.phiIDUsageMap.containsKey(letNode.getRightChild().getRefId()) == true)
								{
									this.phiIDUsageMap.get(letNode.getRightChild().getRefId()).add(letNode.getRightChild());
								}
								
								// Add the map to statsIDMap if a key exists
								if (this.statsIDMap.containsKey(letNode.getRightChild().getRefId()) == true)
								{
									this.statsIDMap.get(letNode.getRightChild().getRefId()).add(letNode.getRightChild());
								}
							}
							
							if (this.mightUseWOInit.containsKey(letNode.getRightChild().getRefId()) == true)
							{
								if (this.multiScopeVariable.contains(this.mightUseWOInit.get(letNode.getRightChild().getRefId()).getType().getCharacters()) == false)
								{
									this.Error("Variable '" + this.mightUseWOInit.get(letNode.getRightChild().getRefId()).getType().getCharacters() +
											"' is used without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
							}
							
							if (varsWithOldSSAIndex != null)
							{
								if (varsWithOldSSAIndex.containsKey(varName) == false) //TODO varName was cpMapVarName
								{
									varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
								}
								letNode.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(varName).add(letNode.getRightChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(varName) == false)
									{
										this.nodesToFixInLoops.put(varName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(varName).add(letNode);
								}
							}
						}
						
						if (letNode.getRightChild().getType().compareType(Tokens.ident) == true)
						{
							if (this.multiScopeVariable.contains(letNode.getRightChild().getType().getCharacters()) == true)
							{
								String globalVarName = letNode.getRightChild().getType().getCharacters();
								letNode.getRightChild().setType(Token.getToken(Tokens.statToken));
								letNode.getRightChild().setRefId(this.globalVarIDMap.get(globalVarName));
								letNode.getRightChild().getType().setIsGlobalVariable(true);
							}
							else
							{
								// Set the SSA index of the ident node which is the right child of the letNode
								int currentSSAIndex;
								if (this.scope.containVariable(scopeName, letNode.getRightChild().getType().getCharacters()) == true)
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(scopeName,
										letNode.getRightChild().getType().getCharacters());
								}
								else
								{
									currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName,
											letNode.getRightChild().getType().getCharacters());
								}
								letNode.getRightChild().getType().setSSAIndex(currentSSAIndex);
								
								if (currentSSAIndex == 0)
								{
									this.Error("Variable '" + letNode.getRightChild().getType().getCharacters() + "' is used " +
											"without being initialized (in scope '" + scopeName + "')");
									this.errorFlag = true;
								}
								
								// Add the right child to the varWithOldSSAIndex map if its current SSA index is equal to
								// its index in oldScope
								if (varsWithOldSSAIndex != null)
								{
									int oldSSAIndex;
									if (oldCurrentScope.containsKey(letNode.getRightChild().getType().getCharacters()) == true)
									{
										oldSSAIndex = oldCurrentScope.get(letNode.getRightChild().getType().getCharacters());
									}
									else
									{
										oldSSAIndex = oldMainScope.get(letNode.getRightChild().getType().getCharacters());
									}
									
									if (currentSSAIndex == oldSSAIndex)
									{
										if (varsWithOldSSAIndex.containsKey(letNode.getRightChild().getType().getCharacters()) == false)
										{
											varsWithOldSSAIndex.put(letNode.getRightChild().getType().getCharacters(), new ArrayList<TreeNode>());
										}
										letNode.setLoopLevel(this.loopLevel);
										varsWithOldSSAIndex.get(letNode.getRightChild().getType().getCharacters()).add(letNode.getRightChild());
										
										if (this.loopLevel > 0)
										{
											if (this.nodesToFixInLoops.containsKey(letNode.getRightChild().getType().getCharacters()) == false)
											{
												this.nodesToFixInLoops.put(letNode.getRightChild().getType().getCharacters(), new ArrayList<TreeNode>());
											}
											this.nodesToFixInLoops.get(letNode.getRightChild().getType().getCharacters()).add(letNode);
										}
									}
								}
							}
						}
					}
					// Else, right child is array
					else
					{
						DesignatorNode array = ((DesignatorNode)(letNode.getRightChild()));
						String key = array.getType().getCharacters();
						for (int index = 0; index < array.getExpressionsSize(); index++)
						{
							TreeNode fixedIndex = this.fixParamSSAIndex(currentBlock, array.getExpression(index),
									scopeName, statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap, false);
							if (fixedIndex != null)
							{
								if (fixedIndex.getType().compareType(Tokens.number) == true)
								{
									key += fixedIndex.getType().getCharacters();
								}
								else if (fixedIndex.getType().compareType(Tokens.statSeqToken) == true)
								{
									key += fixedIndex.getRefId();
								}
							}
							else
							{
								key += array.getExpression(index).getType().getCharacters();
							}
						}
						
						if (this.arrayCSE.containsKey(key) == true)
						{
							int arrayLoadID = this.arrayCSE.get(key);
							
							// Create a new node with the reference ID of the loadNode
							TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), arrayLoadID);
							
							// Replace statNode with loadNode
							TreeNode.Implant(statNode, letNode.getRightChild());
						}
						else
						{
							int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(letNode.getRightChild())),
									scopeName, statements, createPhiStats, varIndex, varsWithOldSSAIndex, oldCurrentScope,
									oldMainScope, elimInstrs, cpMap);
							
							LoadStoreNode loadNode = new LoadStoreNode(Token.getToken(Tokens.loadSSAToken),
									letNode.getRightChild().getType().getCharacters());
							loadNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
							
							// Process CSE for load statements
							TreeNode matchingNode;
							if (this.childsInNodeToFix(letNode.getRightChild()) == false)
							{
								matchingNode = this.CSECheck(elimInstrs, loadNode.getType().getValue() - 400, letNode.getRightChild());
							}
							else
							{
								matchingNode = null;
							}
							if (matchingNode == null)
							{
								// Add letNode to the list of eliminatedInstructions
								letNode.getRightChild().setRefId(loadNode.getId());
								elimInstrs[loadNode.getType().getValue() - 400].add(letNode.getRightChild());
	
								// Add the loadNode to the statsMap so it can be referenced
								this.statsMap.put(loadNode.getId(), loadNode);
								this.statsIDMap.put(loadNode.getId(), new ArrayList<TreeNode>());
	
								// Create a new node with the reference ID of the loadNode
								TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), loadNode.getId());
								
								// Replace statNode with loadNode
								TreeNode.Implant(statNode, letNode.getRightChild());
								
								// Add loadNode to the list of statements that will eventually be the statements of the block
								statements.add(loadNode);
								
								this.arrayCSE.put(key, loadNode.getId());
							}
							else
							{
								// Create a new node with the reference ID of the loadNode
								TreeNode statNode;
								if (matchingNode.getType().compareType(Tokens.statToken) == true)
								{
									statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
								}
								else
								{
									statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
								}
								
								// Replace statNode with loadNode
								TreeNode.Implant(statNode, letNode.getRightChild());
								
								// Mark the adda statement as deleted as well
								statements.get(statements.size() - 1).setDeleteReason(DeleteReason.CSE);
								// Add loadNode to the list of statements that will eventually be the statements of the block,
								// but mark it as deleted
								statements.add(loadNode);
								loadNode.setDeleteReason(DeleteReason.CSE);
							}
						}
					}
				}
				
				if ((oldCurrentScope == null) &&
					(letNode.getLeftChild().getType().compareType(Tokens.number) == true) &&
					(letNode.getRightChild().getType().compareType(Tokens.number) == true))
				{
					switch (letNode.getType().getType())
					{
						case addSSAToken:
							letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) +
									Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
							break;
							
						case subSSAToken:
							letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) -
									Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
							break;
							
						case mulSSAToken:
							letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) *
									Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
							break;
							
						case divSSAToken:
							letNode.setType(Token.getToken(String.valueOf(Integer.parseInt(letNode.getLeftChild().getType().getCharacters()) /
									Integer.parseInt(letNode.getRightChild().getType().getCharacters()))));
							break;
					}
					
					if (Integer.parseInt(letNode.getType().getCharacters()) < 0)
					{
						this.Error("Invalid number value '" + letNode.getType().getCharacters() + "'");
						this.errorFlag = true;
					}
					
					// Disconnect left and right children from the parent node
					letNode.getLeftChild().clearParents();
					letNode.getRightChild().clearParents();
					letNode.setLeftChild(null);
					letNode.setRightChild(null);
				}
				else
				{
					TreeNode matchingNode;
					if (this.childsInNodeToFix(letNode) == false)
					{
						matchingNode = this.CSECheck(elimInstrs, elimInstrIndex, letNode);
					}
					else
					{
						matchingNode = null;
					}
					if (matchingNode == null)
					{
						// Add the letNode to the statsMap so it can be referenced
						this.statsMap.put(letNode.getId(), letNode);
						this.statsIDMap.put(letNode.getId(), new ArrayList<TreeNode>());

						// Create a new node with the reference ID of the letNode
						TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), letNode.getId());
						
						// Replace statNode with letNode
						TreeNode.Implant(statNode, letNode);
						
						// Add letNode to the list of statements that will eventually be the statements of the block
						statements.add(letNode);
						
						// Add letNode to the list of eliminatedInstructions
						elimInstrs[elimInstrIndex].add(letNode);
					}
					else
					{
						// Create a new node with the reference ID of the matchingNode
						TreeNode statNode;
						if (matchingNode.getType().compareType(Tokens.statToken) == true)
						{
							statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
						}
						else
						{
							statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
						}
						
						// Replace statNode with letNode
						TreeNode.Implant(statNode, letNode);
						
						// Add letNode to the list of statements that will eventually be the statements of the block,
						// but mark it as deleted
						statements.add(letNode);
						letNode.setDeleteReason(DeleteReason.CSE);
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "incomplete-switch"})
	private TreeNode parseIfStatement(TreeNode currentBlock, String scopeName,
			HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex, HashMap<String, Integer> oldCurrentScope,
			HashMap<String, Integer> oldMainScope, ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap)
	{
		// Change the type of the relation to cmp
		TreeNode relation = ((BlockNode)(currentBlock)).getStatement(0);
		Tokens relOp = relation.getType().getType();
		relation.setType(Token.getToken(Tokens.cmpSSAToken));
		// Split the left and right children trees if they are expressions,
		// and fix their SSA index
		TreeNode fixNode = this.fixParamSSAIndex(currentBlock, relation.getLeftChild(), scopeName,
				((BlockNode)(currentBlock)).getStatements(), varsWithOldSSAIndex, oldCurrentScope,
				oldMainScope, elimInstrs, cpMap, false);
		if (fixNode != null)
		{
			TreeNode.Implant(fixNode, relation.getLeftChild());
		}
		if (relation.getLeftChild().getType().compareType(Tokens.statToken) == true)
		{
			if (this.phiIDUsageMap.containsKey(relation.getLeftChild().getRefId()) == true)
			{
				this.phiIDUsageMap.get(relation.getLeftChild().getRefId()).add(relation.getLeftChild());
			}
			
			// Add the map to statsIDMap if a key exists
			if (this.statsIDMap.containsKey(relation.getLeftChild().getRefId()) == true)
			{
				this.statsIDMap.get(relation.getLeftChild().getRefId()).add(relation.getLeftChild());
			}
		}
		
		fixNode = this.fixParamSSAIndex(currentBlock, relation.getRightChild(), scopeName,
				((BlockNode)(currentBlock)).getStatements(), varsWithOldSSAIndex, oldCurrentScope,
				oldMainScope, elimInstrs, cpMap, false);
		if (fixNode != null)
		{
			TreeNode.Implant(fixNode, relation.getRightChild());
		}
		if (relation.getRightChild().getType().compareType(Tokens.statToken) == true)
		{
			if (this.phiIDUsageMap.containsKey(relation.getRightChild().getRefId()) == true)
			{
				this.phiIDUsageMap.get(relation.getRightChild().getRefId()).add(relation.getRightChild());
			}
			
			// Add the map to statsIDMap if a key exists
			if (this.statsIDMap.containsKey(relation.getRightChild().getRefId()) == true)
			{
				this.statsIDMap.get(relation.getRightChild().getRefId()).add(relation.getRightChild());
			}
		}
		
		// Add the relation node to the statsMap
		this.statsMap.put(relation.getId(), relation);
		this.statsIDMap.put(relation.getId(), new ArrayList<TreeNode>());
		// Remove statement 0 in the currentBlock (which contains the elation statement
		// then add the relation statement again to the end of the currentBlock statements
		// list. The reason is because fixParamSSAIndex might add some statement that should
		// proceed the relation statement
		((BlockNode)(currentBlock)).deleteStatement(0);
		((BlockNode)(currentBlock)).addStatement(relation);
		
		// Get the corresponding branching instruction based on the relation operation,
		// this instruction is the negation of the relation operation.
		Tokens relOpSSA = Tokens.bneSSAToken;		// Tokens.bneSSAToken is a dummy initial value 
		switch (relOp)
		{
			case eqlToken:
				relOpSSA = Tokens.bneSSAToken;
				break;
				
			case neqToken:
				relOpSSA = Tokens.beqSSAToken;
				break;
				
			case lssToken:
				relOpSSA = Tokens.bgeSSAToken;
				break;
				
			case geqToken:
				relOpSSA = Tokens.bltSSAToken;
				break;
				
			case leqToken:
				relOpSSA = Tokens.bgtSSAToken;
				break;
				
			case gtrToken:
				relOpSSA = Tokens.bleSSAToken;
				break;
		}
		// Create a branching statement
		TreeNode braNode = new TreeNode(Token.getToken(relOpSSA));
		// Set the left children as the result of the previous cmp instruction
		braNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken)));
		braNode.getLeftChild().setRefId(relation.getId());
		// The right child should be the address where to branch, it will be set later
		// in this function
		
		// Add the braNode node to the statsMap and the the statements of the currentBlock
		this.statsMap.put(braNode.getId(), braNode);
		this.statsIDMap.put(braNode.getId(), new ArrayList<TreeNode>());
		((BlockNode)(currentBlock)).addStatement(braNode);

		
		// Clone the eliminatedInstruction lists
		ArrayList<TreeNode>[] clonedElimInstrs = this.cloneEliminatedInstructions(elimInstrs);
		
		// Clone the cpMap
		HashMap<String, CPMapInfo> clonedCPMap = this.cloneCPMap(cpMap);
		
		// thenVarIndex is a hashMap that contains the phi instructions arguments
		HashMap<String, TreeNode> thenVarIndex = new HashMap<String, TreeNode>();
		
		// Parse then block
		TreeNode thenBlock = currentBlock.getLeftChild();
		while (thenBlock.getType().compareType(Tokens.fiToken) == false)
		{
			if ((thenBlock.getType().compareType(Tokens.statSeqToken) == true) ||
				(thenBlock.getType().compareType(Tokens.odToken) == true))
			{
				this.parseStatSequence(thenBlock, scopeName, true, thenVarIndex, varsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				thenBlock = thenBlock.getLeftChild();
			}
			else if (thenBlock.getType().compareType(Tokens.ifToken) == true)
			{
				thenBlock = this.parseIfStatement(thenBlock, scopeName, varsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				// Update the thenVarIndex map to take into considerations nested if statements
				for (int i = 0; i < ((BlockNode)(thenBlock)).getStatementsSize(); i++)
				{
					if (((BlockNode)(thenBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
					{
						thenVarIndex.put(((BlockNode)(thenBlock)).getStatement(i).getType().getCharacters(),
								((BlockNode)(thenBlock)).getStatement(i));
						
						if (this.loopLevel > 0)
						{
							this.varWithPhiInLoop.add(((BlockNode)(thenBlock)).getStatement(i).getType().getCharacters());
						}
					}
				}
				
				// Parse the thenBlock (fi block) as it was a statement sequence block
				this.parseStatSequence(thenBlock, scopeName, true, thenVarIndex, varsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				thenBlock = thenBlock.getLeftChild();
			}
			else if (thenBlock.getType().compareType(Tokens.whileToken) == true)
			{
				this.arraysChangedInLoop.clear();

				// TreeNode tempWhileNode = bodyBlock;
				thenBlock = this.parseWhileStatement(thenBlock, scopeName,
						varsWithOldSSAIndex, this.scope.cloneSSAIndices(scopeName),
						this.scope.cloneSSAIndices(this.mainScopeName), clonedElimInstrs,
						clonedCPMap);
				// Update the bodyVarIndex map to take into considerations nested if statements
				for (int i = 0; i < ((BlockNode)(thenBlock.getParent(0))).getStatementsSize(); i++)
				{
					if (((BlockNode)(thenBlock.getParent(0))).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
					{
						thenVarIndex.put(((BlockNode)(thenBlock.getParent(0))).getStatement(i).getType().getCharacters(),
								((BlockNode)(thenBlock.getParent(0))).getStatement(i));
						
						if (this.loopLevel > 0)
						{
							this.varWithPhiInLoop.add(((BlockNode)(thenBlock.getParent(0))).getStatement(i).getType().getCharacters());
						}
					}
				}
				
				// Parse the bodyBlock (od block) as it was a statement sequence block
				this.parseStatSequence(thenBlock, scopeName, true, thenVarIndex, varsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				thenBlock = thenBlock.getLeftChild();
			}
		}
		
		// Clone the eliminatedInstruction lists
		clonedElimInstrs = this.cloneEliminatedInstructions(elimInstrs);
		
		// Clone the cpMap
		clonedCPMap = this.cloneCPMap(cpMap);
		
		// thenVarIndex is a hashMap that contains the phi instructions arguments
		HashMap<String, TreeNode> elseVarIndex = new HashMap<String, TreeNode>();
				
		// Parse else block
		TreeNode elseBlock = currentBlock.getRightChild();
		if (elseBlock != null)
		{
			int elseBlockID = elseBlock.getId();
			while (elseBlock.getType().compareType(Tokens.fiToken) == false)
			{
				if ((elseBlock.getType().compareType(Tokens.statSeqToken) == true) ||
					(elseBlock.getType().compareType(Tokens.odToken) == true))
				{
					this.parseStatSequence(elseBlock, scopeName, true, elseVarIndex, varsWithOldSSAIndex, oldCurrentScope, oldMainScope,
							clonedElimInstrs, clonedCPMap);
					elseBlock = elseBlock.getLeftChild();
				}
				else if (elseBlock.getType().compareType(Tokens.ifToken) == true)
				{
					elseBlock = this.parseIfStatement(elseBlock, scopeName, varsWithOldSSAIndex, oldCurrentScope,
							oldMainScope, clonedElimInstrs, clonedCPMap);
					// Update the elseVarIndex map to take into considerations nested if statements
					for (int i = 0; i < ((BlockNode)(elseBlock)).getStatementsSize(); i++)
					{
						if (((BlockNode)(elseBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
						{
							elseVarIndex.put(((BlockNode)(elseBlock)).getStatement(i).getType().getCharacters(),
									((BlockNode)(elseBlock)).getStatement(i));
							
							if (this.loopLevel > 0)
							{
								this.varWithPhiInLoop.add(((BlockNode)(elseBlock)).getStatement(i).getType().getCharacters());
							}
						}
					}

					// Parse the thenBlock (fi block) as it was a statement sequence block
					this.parseStatSequence(elseBlock, scopeName, true, elseVarIndex, varsWithOldSSAIndex,
							oldCurrentScope, oldMainScope, clonedElimInstrs, clonedCPMap);
					elseBlock = elseBlock.getLeftChild();
				}
				else if (elseBlock.getType().compareType(Tokens.whileToken) == true)
				{
					this.arraysChangedInLoop.clear();

					// TreeNode tempWhileNode = bodyBlock;
					elseBlock = this.parseWhileStatement(elseBlock, scopeName,
							varsWithOldSSAIndex, this.scope.cloneSSAIndices(scopeName),
							this.scope.cloneSSAIndices(this.mainScopeName),
							clonedElimInstrs, clonedCPMap);
					// Update the bodyVarIndex map to take into considerations nested if statements
					for (int i = 0; i < ((BlockNode)(elseBlock.getParent(0))).getStatementsSize(); i++)
					{
						if (((BlockNode)(elseBlock.getParent(0))).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
						{
							elseVarIndex.put(((BlockNode)(elseBlock.getParent(0))).getStatement(i).getType().getCharacters(),
									((BlockNode)(elseBlock.getParent(0))).getStatement(i));
							
							if (this.loopLevel > 0)
							{
								this.varWithPhiInLoop.add(((BlockNode)(elseBlock.getParent(0))).getStatement(i).getType().getCharacters());
							}
						}
					}
					
					// Parse the bodyBlock (od block) as it was a statement sequence block
					this.parseStatSequence(elseBlock, scopeName, true, elseVarIndex, varsWithOldSSAIndex,
							oldCurrentScope, oldMainScope, clonedElimInstrs, clonedCPMap);
					elseBlock = elseBlock.getLeftChild();
				}
			}
			
			// Fix the branch address of the branch instruction in the if
			braNode.setRightChild(new TreeNode(Token.getToken(Tokens.blockToken)));
			braNode.getRightChild().setRefId(elseBlockID);
		}
		
		
		// From now on thenBlock is the fi block
		
		
		int currentSSAIndex;
		TreeNode phi;
		for (String varName : thenVarIndex.keySet())
		{
			if (this.multiScopeVariable.contains(varName) == true)
			{
				continue;
			}
			
			// Get the current SSA index of varName
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(scopeName, varName);
			}
			else
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName, varName);
			}
			
			// Create the new phi node with the appropriate variable name and SSA index
			phi = new TreeNode(Token.getToken(Tokens.phiSSAToken));
			phi.getType().setCharacters(varName);
			phi.getType().setSSAIndex(currentSSAIndex + 1);
			
			// Add the phi function to phiIDUsageMap 
			this.phiIDUsageMap.put(phi.getId(), new ArrayList<TreeNode>());
			
			// Create a left child with the appropriate reference to instruction
			if (thenVarIndex.containsKey(varName) == true)
			{
				if ((thenVarIndex.get(varName).getType().compareType(Tokens.statToken) == true) ||
					(thenVarIndex.get(varName).getType().compareType(Tokens.number) == true))
				{
					phi.setLeftChild(thenVarIndex.get(varName).clone());
					if (varsWithOldSSAIndex != null)
					{
						for (String vName : varsWithOldSSAIndex.keySet())
						{
							if (varsWithOldSSAIndex.get(vName).contains(thenVarIndex.get(varName)) == true)
							{
								phi.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(vName).add(phi.getLeftChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(vName) == false)
									{
										this.nodesToFixInLoops.put(vName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(vName).add(phi);
								}
							}
						}
					}
				}
				else
				{
					phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), thenVarIndex.get(varName).getId()));
				}
			}
			else
			{
				phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), -1));
				
				this.mightUseWOInit.put(phi.getId(), phi);
			}
			
			// Create a right child with the appropriate reference to instruction
			if (elseVarIndex.containsKey(varName) == true)
			{
				if ((elseVarIndex.get(varName).getType().compareType(Tokens.statToken) == true) ||
					(elseVarIndex.get(varName).getType().compareType(Tokens.number) == true))
				{
					phi.setRightChild(elseVarIndex.get(varName).clone());
					if (varsWithOldSSAIndex != null)
					{
						for (String vName : varsWithOldSSAIndex.keySet())
						{
							if (varsWithOldSSAIndex.get(vName).contains(elseVarIndex.get(varName)) == true)
							{
								phi.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(vName).add(phi.getRightChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(vName) == false)
									{
										this.nodesToFixInLoops.put(vName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(vName).add(phi);
								}
							}
						}
					}
				}
				else
				{
					phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), elseVarIndex.get(varName).getId()));
				}
			}
			else
			{
				if (cpMap.containsKey(varName) == true)
				{
					if ((cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true) ||
						(cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true))
					{
						phi.setRightChild(cpMap.get(varName).getNode().clone());
						
						// Add the right child to the varWithOldSSAIndex
						if (varsWithOldSSAIndex != null)
						{
							if (varsWithOldSSAIndex.containsKey(varName) == false)
							{
								varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
							}
							phi.setLoopLevel(this.loopLevel);
							varsWithOldSSAIndex.get(varName).add(phi.getRightChild());
							
							if (this.loopLevel > 0)
							{
								if (this.nodesToFixInLoops.containsKey(varName) == false)
								{
									this.nodesToFixInLoops.put(varName, new ArrayList<TreeNode>());
								}
								this.nodesToFixInLoops.get(varName).add(phi);
							}
						}
					}
					else
					{
						phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), cpMap.get(varName).getNode().getId()));
						
						// Check if phiIDUsageMap contains an entry for the right child of phi
						// If yes, add the right child to phiIDUsageMap
						if (this.phiIDUsageMap.containsKey(phi.getRightChild().getRefId()) == true)
						{
							this.phiIDUsageMap.get(phi.getRightChild().getRefId()).add(phi.getRightChild());
						}
						
						// Add the map to statsIDMap if a key exists
						if (this.statsIDMap.containsKey(phi.getRightChild().getRefId()) == true)
						{
							this.statsIDMap.get(phi.getRightChild().getRefId()).add(phi.getRightChild());
						}
					}
					
					if (this.mightUseWOInit.containsKey(phi.getRightChild().getRefId()) == true)
					{
						if (this.multiScopeVariable.contains(this.mightUseWOInit.get(phi.getRightChild().getRefId()).getType().getCharacters()) == false)
						{
							this.Error("Variable '" + this.mightUseWOInit.get(phi.getRightChild().getRefId()).getType().getCharacters() +
									"' is used without being initialized (in scope '" + scopeName + "')");
							this.errorFlag = true;
						}
					}
				}
				else
				{
					phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), -1));
					
					this.mightUseWOInit.put(phi.getId(), phi);
				}
			}
			
			if (phi.getLeftChild().getType().compareType(Tokens.statToken) == true)
			{
				// Check if phiIDUsageMap contains an entry for the left child of phi
				// If yes, add the left child to phiIDUsageMap
				if (this.phiIDUsageMap.containsKey(phi.getLeftChild().getRefId()) == true)
				{
					this.phiIDUsageMap.get(phi.getLeftChild().getRefId()).add(phi.getLeftChild());
				}
			}
			
			if (phi.getRightChild().getType().compareType(Tokens.statToken) == true)
			{
				// Check if phiIDUsageMap contains an entry for the left child of phi
				// If yes, add the left child to phiIDUsageMap
				if (this.phiIDUsageMap.containsKey(phi.getRightChild().getRefId()) == true)
				{
					this.phiIDUsageMap.get(phi.getRightChild().getRefId()).add(phi.getRightChild());
				}
			}

			// Remove the varName entry from elseVarIndex since it is processed
			elseVarIndex.remove(varName);

			// Add the phi to thenBlock (the fi block)
			((BlockNode)(thenBlock)).addStatement(phi);
			this.phiContainers.put(((BlockNode)(thenBlock)), ((BlockNode)(currentBlock)));
			
			// Modify the SSA index of the variables involved in the phi statement
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				this.scope.modifyVariable(scopeName, varName, currentSSAIndex + 1);
			}
			else
			{
				this.scope.modifyVariable(this.mainScopeName, varName, currentSSAIndex + 1);
			}
			
			// Add the new phi statement to the hash maps
			this.statsMap.put(phi.getId(), phi);
			this.statsIDMap.put(phi.getId(), new ArrayList<TreeNode>());

			cpMap.put(phi.getType().getCharacters(), new CPMapInfo(phi, ""));
		}
		
		for (String varName : elseVarIndex.keySet())
		{
			if (this.multiScopeVariable.contains(varName) == true)
			{
				continue;
			}
			
			// Get the current SSA index of varName
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(scopeName, varName);
			}
			else
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName, varName);
			}
			
			// Create the new phi node with the appropriate variable name and SSA index
			phi = new TreeNode(Token.getToken(Tokens.phiSSAToken));
			phi.getType().setCharacters(varName);
			phi.getType().setSSAIndex(currentSSAIndex + 1);
			
			// Add the phi function to phiIDUsageMap 
			this.phiIDUsageMap.put(phi.getId(), new ArrayList<TreeNode>());
			
			// Create a left child with the appropriate reference to instruction
			if (cpMap.containsKey(varName) == true)
			{
				if ((cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true) ||
					(cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true))
				{
					phi.setLeftChild(cpMap.get(varName).getNode().clone());
					
					// Add the left child to the varWithOldSSAIndex
					String cpMapVarName = varName;
					if (cpMap.get(varName).getVariableName().compareTo("") != 0)
					{
						cpMapVarName = cpMap.get(varName).getVariableName();
					}
					
					if (varsWithOldSSAIndex != null)
					{
						if (varsWithOldSSAIndex.containsKey(cpMapVarName) == false)
						{
							varsWithOldSSAIndex.put(cpMapVarName, new ArrayList<TreeNode>());
						}
						phi.setLoopLevel(this.loopLevel);
						varsWithOldSSAIndex.get(cpMapVarName).add(phi.getLeftChild());
						
						if (this.loopLevel > 0)
						{
							if (this.nodesToFixInLoops.containsKey(cpMapVarName) == false)
							{
								this.nodesToFixInLoops.put(cpMapVarName, new ArrayList<TreeNode>());
							}
							this.nodesToFixInLoops.get(cpMapVarName).add(phi);
						}
					}
				}
				else
				{
					phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), cpMap.get(varName).getNode().getId()));
					
					// Add the map to statsIDMap if a key exists
					if (this.statsIDMap.containsKey(phi.getLeftChild().getRefId()) == true)
					{
						this.statsIDMap.get(phi.getLeftChild().getRefId()).add(phi.getLeftChild());
					}
				}
				
				if (this.mightUseWOInit.containsKey(phi.getLeftChild().getRefId()) == true)
				{
					if (this.multiScopeVariable.contains(this.mightUseWOInit.get(phi.getLeftChild().getRefId()).getType().getCharacters()) == false)
					{
						this.Error("Variable '" + this.mightUseWOInit.get(phi.getLeftChild().getRefId()).getType().getCharacters() +
								"' is used without being initialized (in scope '" + scopeName + "')");
						this.errorFlag = true;
					}
				}
			}
			else
			{
				phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), -1));
				
				this.mightUseWOInit.put(phi.getId(), phi);
			}
			
			// Create a left child with the appropriate reference to instruction
			if (elseVarIndex.containsKey(varName) == true)
			{
				if ((elseVarIndex.get(varName).getType().compareType(Tokens.statToken) == true) ||
					(elseVarIndex.get(varName).getType().compareType(Tokens.number) == true))
				{
					phi.setRightChild(elseVarIndex.get(varName).clone());
					if (varsWithOldSSAIndex != null)
					{
						for (String vName : varsWithOldSSAIndex.keySet())
						{
							if (varsWithOldSSAIndex.get(vName).contains(elseVarIndex.get(varName)) == true)
							{
								phi.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(vName).add(phi.getRightChild());
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(vName) == false)
									{
										this.nodesToFixInLoops.put(vName, new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(vName).add(phi);
								}
							}
						}
					}
				}
				else
				{
					phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), elseVarIndex.get(varName).getId()));
				}
			}
			else
			{
				phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), -1));
				
				this.mightUseWOInit.put(phi.getId(), phi);
			}
			
			if (phi.getLeftChild().getType().compareType(Tokens.statToken) == true)
			{
				// Check if phiIDUsageMap contains an entry for the left child of phi
				// If yes, add the left child to phiIDUsageMap
				if (this.phiIDUsageMap.containsKey(phi.getLeftChild().getRefId()) == true)
				{
					this.phiIDUsageMap.get(phi.getLeftChild().getRefId()).add(phi.getLeftChild());
				}
			}
			
			if (phi.getRightChild().getType().compareType(Tokens.statToken) == true)
			{
				// Check if phiIDUsageMap contains an entry for the left child of phi
				// If yes, add the left child to phiIDUsageMap
				if (this.phiIDUsageMap.containsKey(phi.getRightChild().getRefId()) == true)
				{
					this.phiIDUsageMap.get(phi.getRightChild().getRefId()).add(phi.getRightChild());
				}
			}
			
			// Add the phi to thenBlock (the fi block) (add at the end)
			((BlockNode)(thenBlock)).addStatement(phi);
			this.phiContainers.put(((BlockNode)(thenBlock)), ((BlockNode)(currentBlock)));

			// Modify the SSA index of the variables involved in the phi statement
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				this.scope.modifyVariable(scopeName, varName, currentSSAIndex + 1);
			}
			else
			{
				this.scope.modifyVariable(this.mainScopeName, varName, currentSSAIndex + 1);
			}
			
			// Add the new phi statement to the hash maps
			this.statsMap.put(phi.getId(), phi);
			this.statsIDMap.put(phi.getId(), new ArrayList<TreeNode>());

			cpMap.put(phi.getType().getCharacters(), new CPMapInfo(phi, ""));
		}

		
		// If the branch address was not set (there is no else block), then set it
		if (braNode.getRightChild() == null)
		{
			braNode.setRightChild(new TreeNode(Token.getToken(Tokens.blockToken)));
			braNode.getRightChild().setRefId(thenBlock.getId());
		}
		
		// If there is an else block, add an unconditional branch instruction to the
		// end of the then block. The branch address of the fi block
		if (elseBlock != null)
		{
			braNode = new TreeNode(Token.getToken(Tokens.braSSAToken));
			braNode.setLeftChild(new TreeNode(Token.getToken(Tokens.blockToken)));
			braNode.getLeftChild().setRefId(thenBlock.getId());
			// Add the braNode node to the statsMap
			this.statsMap.put(braNode.getId(), relation);
			this.statsIDMap.put(braNode.getId(), new ArrayList<TreeNode>());

			// Parent(0) of the fi block is the last block in the then block, add the
			// branching instruction there
			((BlockNode)(thenBlock.getParent(0))).addStatement(braNode);
		}

		// Add the currentBlock to the conditionBlocks list
		this.conditionBlocks.add(((BlockNode)(currentBlock)));
		// Set the endOfBranchBlock (the fi block) of the currentBlock
		((BlockNode)(currentBlock)).setPhiBlock(((BlockNode)(thenBlock)));
		
		return thenBlock;
	}
	
	/**
	 * 
	 * @param currentBlock
	 * @param scopeNameoldCurrentScope
	 * @param varsWithOldSSAIndex contains a reference to all nodes inside the loop such that
	 * 			they have variables with SSA indices from outside the loop, defined by the oldCurrentScope
	 * 			and oldMainScope
	 * @param oldCurrentScope is a cloned copy of the current scope right before parsing the loop
	 * @param oldMainScope is a cloned copy of the main scope right before parsing the loop
	 * @return
	 */
	@SuppressWarnings({ "incomplete-switch" })
	private TreeNode parseWhileStatement(TreeNode currentBlock, String scopeName,
			HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex, HashMap<String, Integer> oldCurrentScope,
			HashMap<String, Integer> oldMainScope, ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap)
	{
		this.loopLevel++;
		
		HashMap<String, ArrayList<TreeNode>> newVarsWithOldSSAIndex = new HashMap<String, ArrayList<TreeNode>>();
		
		// Change the type of the relation to cmp
		TreeNode relation = ((BlockNode)(currentBlock)).getStatement(0);
		Tokens relOp = relation.getType().getType();
		relation.setType(Token.getToken(Tokens.cmpSSAToken));
		// Split the left and right children trees if they are expressions,
		// and fix their SSA index
		TreeNode fixNode = this.fixParamSSAIndex(currentBlock, relation.getLeftChild(), scopeName,
				((BlockNode)(currentBlock)).getStatements(), newVarsWithOldSSAIndex, oldCurrentScope,
				oldMainScope, elimInstrs, cpMap, true);
		if (fixNode != null)
		{
			TreeNode.Implant(fixNode, relation.getLeftChild());
		}
		if (relation.getLeftChild().getType().compareType(Tokens.statToken) == true)
		{
			if (this.phiIDUsageMap.containsKey(relation.getLeftChild().getRefId()) == true)
			{
				this.phiIDUsageMap.get(relation.getLeftChild().getRefId()).add(relation.getLeftChild());
			}
			
			// Add the map to statsIDMap if a key exists
			if (this.statsIDMap.containsKey(relation.getLeftChild().getRefId()) == true)
			{
				this.statsIDMap.get(relation.getLeftChild().getRefId()).add(relation.getLeftChild());
			}
		}
		
		fixNode = this.fixParamSSAIndex(currentBlock, relation.getRightChild(), scopeName,
				((BlockNode)(currentBlock)).getStatements(), newVarsWithOldSSAIndex, oldCurrentScope,
				oldMainScope, elimInstrs, cpMap, true);
		if (fixNode != null)
		{
			TreeNode.Implant(fixNode, relation.getRightChild());
		}
		if (relation.getRightChild().getType().compareType(Tokens.statToken) == true)
		{
			if (this.phiIDUsageMap.containsKey(relation.getRightChild().getRefId()) == true)
			{
				this.phiIDUsageMap.get(relation.getRightChild().getRefId()).add(relation.getRightChild());
			}
			
			// Add the map to statsIDMap if a key exists
			if (this.statsIDMap.containsKey(relation.getRightChild().getRefId()) == true)
			{
				this.statsIDMap.get(relation.getRightChild().getRefId()).add(relation.getRightChild());
			}
		}
		
		// Add the relation node to the statsMap
		this.statsMap.put(relation.getId(), relation);
		this.statsIDMap.put(relation.getId(), new ArrayList<TreeNode>());
		// Remove statement 0 in the currentBlock (which contains the elation statement
		// then add the relation statement again to the end of the currentBlock statements
		// list. The reason is because fixParamSSAIndex might add some statement that should
		// proceed the relation statement
		((BlockNode)(currentBlock)).deleteStatement(0);
		((BlockNode)(currentBlock)).addStatement(relation);
		
		// Get the corresponding branching instruction based on the relation operation,
		// this instruction is the negation of the relation operation.
		Tokens relOpSSA = Tokens.bneSSAToken;		// Tokens.bneSSAToken is a dummy initial value 
		switch (relOp)
		{
			case eqlToken:
				relOpSSA = Tokens.bneSSAToken;
				break;
				
			case neqToken:
				relOpSSA = Tokens.beqSSAToken;
				break;
				
			case lssToken:
				relOpSSA = Tokens.bgeSSAToken;
				break;
				
			case geqToken:
				relOpSSA = Tokens.bltSSAToken;
				break;
				
			case leqToken:
				relOpSSA = Tokens.bgtSSAToken;
				break;
				
			case gtrToken:
				relOpSSA = Tokens.bleSSAToken;
				break;
		}
		// Create a branching statement
		TreeNode braNode = new TreeNode(Token.getToken(relOpSSA));
		// Set the left children as the result of the previous cmp instruction
		braNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken)));
		braNode.getLeftChild().setRefId(relation.getId());
		// The right child should be the address where to branch, it will be set later
		// in this function
		
		// Add the braNode node to the statsMap and the the statements of the currentBlock
		this.statsMap.put(braNode.getId(), braNode);
		this.statsIDMap.put(braNode.getId(), new ArrayList<TreeNode>());
		((BlockNode)(currentBlock)).addStatement(braNode);

		
		// Clone the eliminatedInstruction lists
		ArrayList<TreeNode>[] clonedElimInstrs = this.cloneEliminatedInstructions(elimInstrs);
		
		// Clone the cpMap
		HashMap<String, CPMapInfo> clonedCPMap = this.cloneCPMap(cpMap);
		
		// bodyVarIndex is a hashMap that contains the phi instructions arguments
		HashMap<String, TreeNode> bodyVarIndex = new HashMap<String, TreeNode>();
		
		// Parse then block
		TreeNode bodyBlock = currentBlock.getRightChild();
		while (bodyBlock != currentBlock)
		{
			if ((bodyBlock.getType().compareType(Tokens.statSeqToken) == true) ||
				(bodyBlock.getType().compareType(Tokens.fiToken) == true))
			{
				this.parseStatSequence(bodyBlock, scopeName, true, bodyVarIndex, newVarsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				bodyBlock = bodyBlock.getLeftChild();
			}
			else if (bodyBlock.getType().compareType(Tokens.ifToken) == true)
			{
				bodyBlock = this.parseIfStatement(bodyBlock, scopeName, newVarsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				// Update the bodyVarIndex map to take into considerations nested if statements
				for (int i = 0; i < ((BlockNode)(bodyBlock)).getStatementsSize(); i++)
				{
					if (((BlockNode)(bodyBlock)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
					{
						bodyVarIndex.put(((BlockNode)(bodyBlock)).getStatement(i).getType().getCharacters(),
								((BlockNode)(bodyBlock)).getStatement(i));
						
						if (this.loopLevel > 0)
						{
							this.varWithPhiInLoop.add(((BlockNode)(bodyBlock)).getStatement(i).getType().getCharacters());
						}
					}
				}
				
				// Parse the bodyBlock (od block) as it was a statement sequence block
				this.parseStatSequence(bodyBlock, scopeName, true, bodyVarIndex, newVarsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				bodyBlock = bodyBlock.getLeftChild();
			}
			else if (bodyBlock.getType().compareType(Tokens.whileToken) == true)
			{
				TreeNode tempWhileNode = bodyBlock;
				bodyBlock = this.parseWhileStatement(bodyBlock, scopeName,
						newVarsWithOldSSAIndex, this.scope.cloneSSAIndices(scopeName),
						this.scope.cloneSSAIndices(this.mainScopeName), clonedElimInstrs,
						clonedCPMap);
				// Update the bodyVarIndex map to take into considerations nested if statements
				for (int i = 0; i < ((BlockNode)(tempWhileNode)).getStatementsSize(); i++)
				{
					if (((BlockNode)(tempWhileNode)).getStatement(i).getType().compareType(Tokens.phiSSAToken) == true)
					{
						bodyVarIndex.put(((BlockNode)(tempWhileNode)).getStatement(i).getType().getCharacters(),
								((BlockNode)(tempWhileNode)).getStatement(i));
						
						if (this.loopLevel > 0)
						{
							this.varWithPhiInLoop.add(((BlockNode)(tempWhileNode)).getStatement(i).getType().getCharacters());
						}
					}
				}
				
				// Parse the bodyBlock (od block) as it was a statement sequence block
				this.parseStatSequence(bodyBlock, scopeName, true, bodyVarIndex, newVarsWithOldSSAIndex, oldCurrentScope,
						oldMainScope, clonedElimInstrs, clonedCPMap);
				bodyBlock = bodyBlock.getLeftChild();
			}
		}
		
		// Here bodyBlock is the while block node
		BlockNode lastBodyBlock = ((BlockNode)(bodyBlock.getParent(0)));
		
		
		// From now on bodyBlock is the od block
		bodyBlock = bodyBlock.getLeftChild();
		
		
		int currentSSAIndex;
		TreeNode phi;
		for (String varName : bodyVarIndex.keySet())
		{
			if (this.multiScopeVariable.contains(varName) == true)
			{
				continue;
			}
			
			// Get the current SSA index of varName
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(scopeName, varName);
			}
			else
			{
				currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName, varName);
			}
			
			// Create the new phi node with the appropriate variable name and SSA index
			phi = new TreeNode(Token.getToken(Tokens.phiSSAToken));
			phi.getType().setCharacters(varName);
			phi.getType().setSSAIndex(currentSSAIndex + 1);
			
			// Add the phi function to phiIDUsageMap 
			this.phiIDUsageMap.put(phi.getId(), new ArrayList<TreeNode>());
			
			// Fix SSA index for variables inside the loop that are using the same SSA
			// index before the loop
			ArrayList<TreeNode> variablesToFix = newVarsWithOldSSAIndex.get(varName);
			if (variablesToFix != null)
			{
				for (int i = 0; i < variablesToFix.size(); i++)
				{
					if (cpMap.containsKey(varName) == true)
					{
						// TODO remove the second condition in case of errors
						if ((cpMap.get(varName).getNode().getType().compareToken(variablesToFix.get(i).getType()) == true) ||
							(cpMap.get(varName).getNode().getType().compareType(Tokens.ident) == true))
						{
							if (variablesToFix.get(i).getType().compareType(Tokens.ident) == true)
							{
								variablesToFix.get(i).getType().setSSAIndex(currentSSAIndex + 1);
							}
							else
							{
								variablesToFix.get(i).setType(Token.getToken(Tokens.statToken));
								variablesToFix.get(i).setRefId(phi.getId());
							}
						}
					}
					else
					{
						if (variablesToFix.get(i).getType().compareType(Tokens.ident) == true)
						{
							if (variablesToFix.get(i).getType().getSSAIndex() == 0)
							{
								variablesToFix.get(i).setType(Token.getToken(Tokens.statToken));
								variablesToFix.get(i).setRefId(phi.getId());
							}
						}
					}
				}
			}
			
			// Create a left child with the appropriate reference to instruction
			if (cpMap.containsKey(varName) == true)
			{
				if ((cpMap.get(varName).getNode().getType().compareType(Tokens.statToken) == true) ||
					(cpMap.get(varName).getNode().getType().compareType(Tokens.number) == true))
				{
					phi.setLeftChild(cpMap.get(varName).getNode().clone());
					
					// Add the left child to the varWithOldSSAIndex
					if (varsWithOldSSAIndex != null)
					{
						if (varsWithOldSSAIndex.containsKey(varName) == false)
						{
							varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
						}
						phi.setLoopLevel(this.loopLevel);
						varsWithOldSSAIndex.get(varName).add(phi.getLeftChild());
						
						if (this.loopLevel > 0)
						{
							if (this.nodesToFixInLoops.containsKey(varName) == false)
							{
								this.nodesToFixInLoops.put(varName, new ArrayList<TreeNode>());
							}
							this.nodesToFixInLoops.get(varName).add(phi);
						}
					}
				}
				else
				{
					phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), cpMap.get(varName).getNode().getId()));
					
					// Check if phiIDUsageMap contains an entry for the left child of phi
					// If yes, add the left child to phiIDUsageMap
					if (this.phiIDUsageMap.containsKey(phi.getLeftChild().getRefId()) == true)
					{
						this.phiIDUsageMap.get(phi.getLeftChild().getRefId()).add(phi.getLeftChild());
					}
					
					// Add the map to statsIDMap if a key exists
					if (this.statsIDMap.containsKey(phi.getLeftChild().getRefId()) == true)
					{
						this.statsIDMap.get(phi.getLeftChild().getRefId()).add(phi.getLeftChild());
					}
				}

				if (this.mightUseWOInit.containsKey(phi.getLeftChild().getRefId()) == true)
				{
					if (this.multiScopeVariable.contains(this.mightUseWOInit.get(phi.getLeftChild().getRefId()).getType().getCharacters()) == false)
					{
						this.Error("Variable '" + this.mightUseWOInit.get(phi.getLeftChild().getRefId()).getType().getCharacters() +
								"' is used without being initialized (in scope '" + scopeName + "')");
						this.errorFlag = true;
					}
				}
			}
			else
			{
				phi.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), -1));
				
				this.mightUseWOInit.put(phi.getId(), phi);
			}
			
			// Create a right child with the appropriate reference to instruction
			if ((bodyVarIndex.get(varName).getType().compareType(Tokens.statToken) == true) ||
				(bodyVarIndex.get(varName).getType().compareType(Tokens.number) == true))
			{
				phi.setRightChild(bodyVarIndex.get(varName).clone());
			}
			else
			{
				phi.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), bodyVarIndex.get(varName).getId()));
			}
			
			// Add the phi to thenBlock (the fi block)
			((BlockNode)(currentBlock)).addStatementAtBeginning(phi);
			this.phiContainers.put(((BlockNode)(currentBlock)), ((BlockNode)(currentBlock)));

			// Modify the SSA index of the variables involved in the phi statement
			if (this.scope.containVariable(scopeName, varName) == true)
			{
				this.scope.modifyVariable(scopeName, varName, currentSSAIndex + 1);
			}
			else
			{
				this.scope.modifyVariable(this.mainScopeName, varName, currentSSAIndex + 1);
			}
			
			// Add the new phi statement to the hash maps
			this.statsMap.put(phi.getId(), phi);
			this.statsIDMap.put(phi.getId(), new ArrayList<TreeNode>());

			// Add the phi function to the CP map list
			cpMap.put(phi.getType().getCharacters(), new CPMapInfo(phi, ""));
		}
		
		// Create a right child of braNode and set the address
		braNode.setRightChild(new TreeNode(Token.getToken(Tokens.blockToken)));
		braNode.getRightChild().setRefId(bodyBlock.getId());
		
		// Add an unconditional branch instruction to the end of the body block.
		// The branch address of the while block
		braNode = new TreeNode(Token.getToken(Tokens.braSSAToken));
		braNode.setLeftChild(new TreeNode(Token.getToken(Tokens.blockToken)));
		braNode.getLeftChild().setRefId(currentBlock.getId());
		// Add the braNode node to the statsMap
		this.statsMap.put(braNode.getId(), relation);
		this.statsIDMap.put(braNode.getId(), new ArrayList<TreeNode>());
		// Add the branching instruction at the end of the while block
		lastBodyBlock.addStatement(braNode);
		
		this.loopLevel--;
		if (this.loopLevel == 0)
		{
			this.nodesToFixInLoops.clear();
		}
		
		// Add the currentBlock to the conditionBlocks list
		this.conditionBlocks.add(((BlockNode)(currentBlock)));
		// Set the endOfBranchBlock (the fi block) of the currentBlock
		((BlockNode)(currentBlock)).setPhiBlock(((BlockNode)(currentBlock)));
		
		// Add everything in newVarsWithOldSSAIndex to varsWithOldSSAIndex
		// TODO: this is used to fix node 185 in test002.txt, this node was #0 before
		// and it should be taken from the outer phi of variable i
		if (newVarsWithOldSSAIndex != null && varsWithOldSSAIndex != null)
		{
			for (String varName : newVarsWithOldSSAIndex.keySet())
			{
				if (varsWithOldSSAIndex.containsKey(varName) == false)
				{
					varsWithOldSSAIndex.put(varName, new ArrayList<TreeNode>());
				}
				
				for (TreeNode node : newVarsWithOldSSAIndex.get(varName))
				{
					varsWithOldSSAIndex.get(varName).add(node);
				}
			}
		}
		
		for (String arrayName : this.arraysChangedInLoop)
		{
			HashSet<String> keysToBeRemoved = new HashSet<String>();
			for (String key : this.arrayCSE.keySet())
			{
				if (key.startsWith(arrayName) == true)
				{
					keysToBeRemoved.add(key);
				}
			}
			
			for (String keyToBeRemoved : keysToBeRemoved)
			{
				this.arrayCSE.remove(keyToBeRemoved);
			}
		}
		
		return bodyBlock;
	}
	
	private TreeNode parseFuncCall(TreeNode currentBlock, TreeNode callNode, String scopeName, ArrayList<TreeNode> statements,
			HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex, HashMap<String, Integer> oldCurrentScope,
			HashMap<String, Integer> oldMainScope, ArrayList<TreeNode>[] elimInsts, HashMap<String, CPMapInfo> cpMap)
	{
		// Get the name of the function
		String funcName = callNode.getLeftChild().getType().getCharacters();
		// If the function is InputNum, change the token to predefToken and remove
		// left and right children
		if (funcName.compareTo("InputNum") == 0)
		{
			callNode.setType(Token.getToken(Tokens.predefToken));
			callNode.getType().setCharacters("read");
			
			callNode.setLeftChild(null);
			callNode.setRightChild(null);
		}
		// If the function is OutputNum, change the token to predefToken and remove
		// left child
		else if (funcName.compareTo("OutputNum") == 0)
		{
			callNode.setType(Token.getToken(Tokens.predefToken));
			callNode.getType().setCharacters("write");

			callNode.setLeftChild(null);
		}
		// If the function is OutputNewLine, change the token to predefToken and remove
		// left and right children
		else if (funcName.compareTo("OutputNewLine") == 0)
		{
			callNode.setType(Token.getToken(Tokens.predefToken));
			callNode.getType().setCharacters("wln");

			callNode.setLeftChild(null);
			callNode.setRightChild(null);
		}
		// Otherwise, change the left child to hold the address of the function (where to
		// jump)
		else
		{
			callNode.setLeftChild(new TreeNode(Token.getToken(Tokens.blockToken)));
			callNode.getLeftChild().setRefId(this.funcAddrMap.get(funcName).getID());
		}
		
		// If the right child exists (there are parameters), fix them in case they are expressions
		if (callNode.getRightChild() != null)
		{
			for (int i = 0; i < ((BlockNode)(callNode.getRightChild())).getStatementsSize(); i++)
			{
				TreeNode statNode = this.fixParamSSAIndex(currentBlock, ((BlockNode)(callNode.getRightChild())).getStatement(i),
						scopeName, statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInsts, cpMap, false);
				if (statNode != null)
				{
					((BlockNode)(callNode.getRightChild())).replaceStatement(statNode, i);
					if (statNode.getType().compareType(Tokens.statToken) == true)
					{
						if (this.phiIDUsageMap.containsKey(statNode.getRefId()) == true)
						{
							this.phiIDUsageMap.get(statNode.getRefId()).add(statNode);
						}
						
						// Add the map to statsIDMap if a key exists
						if (this.statsIDMap.containsKey(statNode.getRefId()) == true)
						{
							this.statsIDMap.get(statNode.getRefId()).add(statNode);
						}
					}
				}
			}
		}
		
		return callNode;
	}
	
	// Fix the parameters of functions to be in SSA format
	private TreeNode fixParamSSAIndex(TreeNode currentBlock, TreeNode expression, String scopeName, ArrayList<TreeNode> statements,
			HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex, HashMap<String, Integer> oldCurrentScope,
			HashMap<String, Integer> oldMainScope, ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap, boolean alwaysLoad)
	{
		if (expression == null)
		{
			return null;
		}
		
		// The return value will be either null, or a TreeNode of type statToken containing
		// the address of the statements that should replace the function call
		TreeNode retNode = null;
		
		// If the expression is ident, set the SSA index
		if (expression.getType().compareType(Tokens.ident) == true)
		{
			// If the number of inner expressions is 0, then the expression is variable
			if (((DesignatorNode)(expression)).getExpressionsSize() == 0)
			{
				// If there is a mapping for the variable, parse it
				if (cpMap.containsKey(expression.getType().getCharacters()) == true)
				{
					String varName = expression.getType().getCharacters();
					
					// If the mapping maps to number, replace the number
					if (cpMap.get(expression.getType().getCharacters()).getNode().getType().compareType(Tokens.number) == true)
					{
						expression = new TreeNode(Token.getToken(cpMap.get(expression.getType().getCharacters()).getNode().getType().getCharacters()));
					}
					// If the mapping maps to reference statement, replace expression with that reference
					else if (cpMap.get(expression.getType().getCharacters()).getNode().getType().compareType(Tokens.statToken) == true)
					{
						expression = cpMap.get(expression.getType().getCharacters()).getNode().clone();
						
						// Check if phiIDUsageMap contains an entry for the left child of phi
						// If yes, add the left child to phiIDUsageMap
						if (this.phiIDUsageMap.containsKey(expression.getRefId()) == true)
						{
							this.phiIDUsageMap.get(expression.getRefId()).add(expression);
						}
						
						// Add the map to statsIDMap if a key exists
						if (this.statsIDMap.containsKey(expression.getRefId()) == true)
						{
							this.statsIDMap.get(expression.getRefId()).add(expression);
						}
					}
					// Otherwise replace with whatever the mapping maps to
					else
					{
						expression = new TreeNode(Token.getToken(Tokens.statToken), cpMap.get(expression.getType().getCharacters()).getNode().getId());
					}
					
					if (this.mightUseWOInit.containsKey(expression.getRefId()) == true)
					{
						if (this.multiScopeVariable.contains(this.mightUseWOInit.get(expression.getRefId()).getType().getCharacters()) == false)
						{
							this.Error("Variable '" + this.mightUseWOInit.get(expression.getRefId()).getType().getCharacters() +
									"' is used without being initialized (in scope '" + scopeName + "')");
							this.errorFlag = true;
						}
					}
					
					// TODO: this is added at 00:59
					String cpMapVarName = varName;
					/*if (cpMap.get(varName).getVariableName().compareTo("") != 0)
					{
						cpMapVarName = cpMap.get(varName).getVariableName();
					}*/
					
					if (varsWithOldSSAIndex != null)
					{
						if (varsWithOldSSAIndex.containsKey(cpMapVarName) == false)
						{
							varsWithOldSSAIndex.put(cpMapVarName, new ArrayList<TreeNode>());
						}
						expression.setLoopLevel(this.loopLevel);
						varsWithOldSSAIndex.get(cpMapVarName).add(expression);
						
						if (this.loopLevel > 0)
						{
							if (this.nodesToFixInLoops.containsKey(cpMapVarName) == false)
							{
								this.nodesToFixInLoops.put(cpMapVarName, new ArrayList<TreeNode>());
							}
							this.nodesToFixInLoops.get(cpMapVarName).add(expression);
						}
					}
				}
				
				if (expression.getType().compareType(Tokens.ident) == true)
				{
					if (this.multiScopeVariable.contains(expression.getType().getCharacters()) == true)
					{
						String globalVarName = expression.getType().getCharacters();
						expression.setType(Token.getToken(Tokens.statToken));
						expression.setRefId(this.globalVarIDMap.get(globalVarName));
						expression.getType().setIsGlobalVariable(true);
					}
					else
					{
						int currentSSAIndex;
						if (this.scope.containVariable(scopeName, expression.getType().getCharacters()) == true)
						{
							currentSSAIndex = this.scope.getVariableSSAIndex(scopeName, expression.getType().getCharacters());
						}
						else
						{
							currentSSAIndex = this.scope.getVariableSSAIndex(this.mainScopeName,
									expression.getType().getCharacters());
						}
						
						expression.getType().setSSAIndex(currentSSAIndex);
						
						if (currentSSAIndex == 0)
						{
							this.Error("Variable '" + expression.getType().getCharacters() + "' is used " +
									"without being initialized (in scope '" + scopeName + "')");
							this.errorFlag = true;
						}
						
						// Add the right child to the varWithOldSSAIndex map if its current SSA index is equal to
						// its index in oldScope
						if (varsWithOldSSAIndex != null)
						{
							int oldSSAIndex;
							if (oldCurrentScope.containsKey(expression.getType().getCharacters()) == true)
							{
								oldSSAIndex = oldCurrentScope.get(expression.getType().getCharacters());
							}
							else
							{
								oldSSAIndex = oldMainScope.get(expression.getType().getCharacters());
							}
							
							if (currentSSAIndex == oldSSAIndex)
							{
								if (varsWithOldSSAIndex.containsKey(expression.getType().getCharacters()) == false)
								{
									varsWithOldSSAIndex.put(expression.getType().getCharacters(), new ArrayList<TreeNode>());
								}
								expression.setLoopLevel(this.loopLevel);
								varsWithOldSSAIndex.get(expression.getType().getCharacters()).add(expression);
								
								if (this.loopLevel > 0)
								{
									if (this.nodesToFixInLoops.containsKey(expression.getType().getCharacters()) == false)
									{
										this.nodesToFixInLoops.put(expression.getType().getCharacters(), new ArrayList<TreeNode>());
									}
									this.nodesToFixInLoops.get(expression.getType().getCharacters()).add(expression);
								}
							}
						}
					}
				}
				
				retNode = expression;
			}
			// Else, the expression is array
			else
			{
				DesignatorNode array = ((DesignatorNode)(expression));
				String key = array.getType().getCharacters();
				for (int index = 0; index < array.getExpressionsSize(); index++)
				{
					TreeNode fixedIndex = this.fixParamSSAIndex(currentBlock, array.getExpression(index),
							scopeName, statements, varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap, false);
					if (fixedIndex != null)
					{
						if (fixedIndex.getType().compareType(Tokens.number) == true)
						{
							key += fixedIndex.getType().getCharacters();
						}
						else if (fixedIndex.getType().compareType(Tokens.statSeqToken) == true)
						{
							key += fixedIndex.getRefId();
						}
					}
					else
					{
						key += array.getExpression(index).getType().getCharacters();
					}
				}
				
				if (alwaysLoad == false)
				{
					if (this.arrayCSE.containsKey(key) == true)
					{
						int arrayLoadID = this.arrayCSE.get(key);
						
						// Create a new node with the reference ID of the loadNode
						TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), arrayLoadID);
						
						// Replace statNode with loadNode
						retNode = statNode;
					}
					else
					{
						int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(expression)), scopeName,
								statements, false, null, varsWithOldSSAIndex, oldCurrentScope, oldMainScope,
								elimInstrs, cpMap);
						
						LoadStoreNode loadNode = new LoadStoreNode(Token.getToken(Tokens.loadSSAToken),
								expression.getType().getCharacters());
						loadNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
						
						// Process CSE for load statements
						TreeNode matchingNode;
						if (this.childsInNodeToFix(expression) == false)
						{
							matchingNode = this.CSECheck(elimInstrs, loadNode.getType().getValue() - 400, expression);
						}
						else
						{
							matchingNode = null;
						}
						if (matchingNode == null)
						{
							// Add the loadNode to the statsMap so it can be referenced
							this.statsMap.put(loadNode.getId(), loadNode);
							this.statsIDMap.put(loadNode.getId(), new ArrayList<TreeNode>());
					
							// Add loadNode to the list of statements that will eventually be the statements of the block
							statements.add(loadNode);
							
							// Add letNode to the list of eliminatedInstructions
							expression.setRefId(loadNode.getId());
							elimInstrs[loadNode.getType().getValue() - 400].add(expression);
							
							// Create a new node with the reference ID of the loadNode
							TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), loadNode.getId());
							retNode = statNode;
							
							this.arrayCSE.put(key, loadNode.getId());
						}
						else
						{
							// Mark the adda statement as deleted as well
							statements.get(statements.size() - 1).setDeleteReason(DeleteReason.CSE);
							// Add loadNode to the list of statements that will eventually be the statements of the block,
							// but mark it as deleted
							statements.add(loadNode);
							loadNode.setDeleteReason(DeleteReason.CSE);
							
							// Create a new node with the reference ID of the matchingNode
							TreeNode statNode;
							if (matchingNode.getType().compareType(Tokens.statToken) == true)
							{
								statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
							}
							else
							{	
								statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
							}
							retNode = statNode;
						}
					}
				}
				else
				{
					int refId = this.CalculateArrayElementAddress(currentBlock, ((DesignatorNode)(expression)), scopeName,
							statements, false, null, varsWithOldSSAIndex, oldCurrentScope, oldMainScope,
							elimInstrs, cpMap);
					
					LoadStoreNode loadNode = new LoadStoreNode(Token.getToken(Tokens.loadSSAToken),
							expression.getType().getCharacters());
					loadNode.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
					
					// Process CSE for load statements
					TreeNode matchingNode;
					if (this.childsInNodeToFix(expression) == false)
					{
						matchingNode = this.CSECheck(elimInstrs, loadNode.getType().getValue() - 400, expression);
					}
					else
					{
						matchingNode = null;
					}
					if (matchingNode == null)
					{
						// Add the loadNode to the statsMap so it can be referenced
						this.statsMap.put(loadNode.getId(), loadNode);
						this.statsIDMap.put(loadNode.getId(), new ArrayList<TreeNode>());
				
						// Add loadNode to the list of statements that will eventually be the statements of the block
						statements.add(loadNode);
						
						// Add letNode to the list of eliminatedInstructions
						expression.setRefId(loadNode.getId());
						elimInstrs[loadNode.getType().getValue() - 400].add(expression);
						
						// Create a new node with the reference ID of the loadNode
						TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), loadNode.getId());
						retNode = statNode;
						
						this.arrayCSE.put(key, loadNode.getId());
					}
					else
					{
						// Mark the adda statement as deleted as well
						statements.get(statements.size() - 1).setDeleteReason(DeleteReason.CSE);
						// Add loadNode to the list of statements that will eventually be the statements of the block,
						// but mark it as deleted
						statements.add(loadNode);
						loadNode.setDeleteReason(DeleteReason.CSE);
						
						// Create a new node with the reference ID of the matchingNode
						TreeNode statNode;
						if (matchingNode.getType().compareType(Tokens.statToken) == true)
						{
							statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
						}
						else
						{	
							statNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
						}
						retNode = statNode;
					}
				}
			}
		}
		// If the expression is +, -, * or /, call the parseAssignment function to split it and
		// set all variables SSA indices
		else if ((expression.getType().compareType(Tokens.plusToken) == true) ||
				 (expression.getType().compareType(Tokens.minusToken) == true) ||
				 (expression.getType().compareType(Tokens.timesToken) == true) ||
				 (expression.getType().compareType(Tokens.divToken) == true))
		{
			this.parseAssignment(currentBlock, expression, scopeName, statements, false, null, varsWithOldSSAIndex, oldCurrentScope,
					oldMainScope, elimInstrs, cpMap);
			for (int i = 0; i < statements.size(); i++)
			{
				if (statements.get(i).getDeleteReason() == DeleteReason.CSE)
				{
					this.cseDeletedInstructions.put(statements.get(i), ((BlockNode)(currentBlock)));
				}
			}
			
			if (expression.getType().compareType(Tokens.statToken) == true)
			{
				// Get the last node of the statements list. This statement is the one that will be referenced
				// in the argument node
				TreeNode lastStat = statements.get(statements.size() - 1);
				
				// Contains the index in the eliminatedInstructions list
				int elimInstrIndex = lastStat.getType().getValue() - 400;	// the difference of index is 400
				
				TreeNode matchingNode;
				if (this.childsInNodeToFix(lastStat) == false)
				{
					matchingNode = this.CSECheck(elimInstrs, elimInstrIndex, lastStat);
				}
				else
				{
					matchingNode = null;
				}
				if (matchingNode == null)
				{
					retNode = new TreeNode(Token.getToken(Tokens.statToken), lastStat.getId());
				}
				else
				{
					if (matchingNode.getType().compareType(Tokens.statToken) == true)
					{
						retNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getRefId());
					}
					else
					{
						retNode = new TreeNode(Token.getToken(Tokens.statToken), matchingNode.getId());
					}
				}
			}
			else if (expression.getType().compareType(Tokens.number) == true)
			{
				retNode = expression;
			}
			else
			{
				retNode = new TreeNode(Token.getToken(Tokens.statToken), expression.getId());
			}
		}
		// If the expression is a function call, call parseFuncCall
		else if (expression.getType().compareType(Tokens.callToken) == true)
		{
			String functionName = expression.getLeftChild().getType().getCharacters();
			// Parse the function call statement
			TreeNode callNode = this.parseFuncCall(currentBlock, expression, scopeName, statements, varsWithOldSSAIndex,
					oldCurrentScope, oldMainScope, elimInstrs, cpMap);

			// Forget all CP maps for global variables assigned in the called function
			if (this.funcAssignedVarMap.containsKey(functionName) == true)
			{
				for (String globalVarName : this.funcAssignedVarMap.get(functionName))
				{
					cpMap.remove(globalVarName);
				}
			}
			
			// Add the callNode to the statements list and to the statement map
			statements.add(callNode);
			this.statsMap.put(callNode.getId(), callNode);
			this.statsIDMap.put(callNode.getId(), new ArrayList<TreeNode>());

			// Create a new node with the reference ID of the letNode
			TreeNode statNode = new TreeNode(Token.getToken(Tokens.statToken), callNode.getId());
			
			// Replace statNode with letNode
			TreeNode.Implant(statNode, callNode);
			
			retNode =  new TreeNode(Token.getToken(Tokens.statToken), statements.get(statements.size() - 1).getId());
		}
		
		return retNode;
	}
	
	private TreeNode parseReturnStatement(TreeNode currentBlock, TreeNode retNode, String scopeName, ArrayList<TreeNode> statements,
			HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex, HashMap<String, Integer> oldCurrentScope,
			HashMap<String, Integer> oldMainScope, ArrayList<TreeNode>[] elimInsts, HashMap<String, CPMapInfo> cpMap)
	{
		// Left child, if it exists, will be an expression, so fix it
		TreeNode fixedRefID = this.fixParamSSAIndex(currentBlock, retNode.getLeftChild(), scopeName, statements,
				varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInsts, cpMap, false);
		if (fixedRefID != null)
		{
			retNode.getLeftChild().clearParents();
			retNode.setLeftChild(fixedRefID);
		}
		return retNode;
	}
	
	private TreeNode CSECheck(ArrayList<TreeNode>[] elimInstrs, int index, TreeNode newInstr)
	{
		if ((Token.getToken(Tokens.loadSSAToken).getValue() - 400) == index)
		{
			return null;
			
//			for (int i = elimInstrs[index].size() - 1; i >= 0 ; i--)
//			{
//				String oldInstrIdent = elimInstrs[index].get(i).getType().getCharacters();
//				for (int j = 0; j < ((DesignatorNode)(elimInstrs[index].get(i))).getExpressionsSize(); j++)
//				{
//					if ((((DesignatorNode)(elimInstrs[index].get(i))).getExpression(j).getType().compareType(Tokens.number) == true) ||
//						(((DesignatorNode)(elimInstrs[index].get(i))).getExpression(j).getType().compareType(Tokens.ident) == true))
//					{
//						oldInstrIdent += ((DesignatorNode)(elimInstrs[index].get(i))).getExpression(j).getType().getCharacters();
//					}
//					else
//					{
//						oldInstrIdent += String.valueOf(((DesignatorNode)(elimInstrs[index].get(i))).getExpression(j).getRefId());
//					}
//				}
//				String newInstrIdent = newInstr.getType().getCharacters();
//				for (int j = 0; j < ((DesignatorNode)(newInstr)).getExpressionsSize(); j++)
//				{
//					if ((((DesignatorNode)(newInstr)).getExpression(j).getType().compareType(Tokens.number) == true) ||
//						(((DesignatorNode)(newInstr)).getExpression(j).getType().compareType(Tokens.ident) == true))
//					{
//						newInstrIdent += ((DesignatorNode)(newInstr)).getExpression(j).getType().getCharacters();
//					}
//					else
//					{
//						newInstrIdent += String.valueOf(((DesignatorNode)(newInstr)).getExpression(j).getRefId());
//					}
//				}
//				
//				if (oldInstrIdent.compareTo(newInstrIdent) == 0)
//				{
//					return new TreeNode(Token.getToken(Tokens.statToken), elimInstrs[index].get(i).getRefId());
//				}
//			}
		}
		else
		{
			for (int i = elimInstrs[index].size() - 1; i >= 0 ; i--)
			{
				String oldleftChildChar;
				if (elimInstrs[index].get(i).getLeftChild().getType().compareType(Tokens.statToken) == true)
				{
					oldleftChildChar = String.valueOf(elimInstrs[index].get(i).getLeftChild().getRefId());
				}
				else
				{
					oldleftChildChar = elimInstrs[index].get(i).getLeftChild().getType().getCharacters() + "_" +
							elimInstrs[index].get(i).getLeftChild().getType().getSSAIndex();
				}
				String oldrightChildChar;
				if (elimInstrs[index].get(i).getRightChild().getType().compareType(Tokens.statToken) == true)
				{
					oldrightChildChar = String.valueOf(elimInstrs[index].get(i).getRightChild().getRefId());
				}
				else
				{
					oldrightChildChar = elimInstrs[index].get(i).getRightChild().getType().getCharacters() + "_" +
							elimInstrs[index].get(i).getRightChild().getType().getSSAIndex();
				}
				
				String newleftChildChar;
				if (newInstr.getLeftChild().getType().compareType(Tokens.statToken) == true)
				{
					newleftChildChar = String.valueOf(newInstr.getLeftChild().getRefId());
				}
				else
				{
					newleftChildChar = newInstr.getLeftChild().getType().getCharacters() + "_" +
							newInstr.getLeftChild().getType().getSSAIndex();
				}
				String newrightChildChar;
				if (newInstr.getRightChild().getType().compareType(Tokens.statToken) == true)
				{
					newrightChildChar = String.valueOf(newInstr.getRightChild().getRefId());
				}
				else
				{
					newrightChildChar = newInstr.getRightChild().getType().getCharacters() + "_" +
							newInstr.getRightChild().getType().getSSAIndex();
				}
				
				
				boolean match = false;
				if ((oldleftChildChar.compareTo(newleftChildChar) == 0) &&
					(oldrightChildChar.compareTo(newrightChildChar) == 0))
				{
					match = true;
				}
				
				// Only + and * operations can be commutative
				if ((elimInstrs[index].get(i).getType().compareType(Tokens.addSSAToken) == true) ||
					(elimInstrs[index].get(i).getType().compareType(Tokens.mulSSAToken) == true))
				{
					if ((oldleftChildChar.compareTo(newrightChildChar) == 0) &&
						(oldrightChildChar.compareTo(newleftChildChar) == 0))
					{
						match = true;
					}
				}
				
				if (match == true)
				{
					boolean found = false;
					for (String varName : this.varWithPhiInLoop)
					{
						if (this.nodesToFixInLoops.containsKey(varName) == true)
						{
							if (this.nodesToFixInLoops.get(varName).contains(newInstr) == true)
							{
								found = true;
								break;
							}
						}
					}
					
					if (found == false)
					{
						if (elimInstrs[index].get(i).getLoopLevel() == newInstr.getLoopLevel())
						{
							return elimInstrs[index].get(i);
						}
						else
						{
							return null;
						}
					}
					else
					{
						return elimInstrs[index].get(i);
					}
				}
			}
		}
		
		return null;
	}
	
	private void forgetArray(ArrayList<TreeNode>[] elimInstrs, int index, String arrayName)
	{
		for (int i = elimInstrs[index].size() - 1; i >= 0; i--)
		{
			if (elimInstrs[index].get(i).getType().getCharacters().compareTo(arrayName) == 0)
			{
				elimInstrs[index].remove(i);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<TreeNode>[] cloneEliminatedInstructions(ArrayList<TreeNode>[] eliminatedInstructions)
	{
		ArrayList<TreeNode>[] clonedElimInstrs = new ArrayList[this.numOfElimInstr];
		for (int i = 0; i < this.numOfElimInstr; i++)
		{
			clonedElimInstrs[i] = new ArrayList<TreeNode>();
			for (int j = 0; j < eliminatedInstructions[i].size(); j++)
			{
				clonedElimInstrs[i].add(eliminatedInstructions[i].get(j));
			}
		}
		
		return clonedElimInstrs;
	}
	
	private HashMap<String, CPMapInfo> cloneCPMap(HashMap<String, CPMapInfo> cpMap)
	{
		HashMap<String, CPMapInfo> clonedCPMap = new HashMap<String, CPMapInfo>();
		for (String varName : cpMap.keySet())
		{
//			clonedCPMap.put(varName, cpMap.get(varName).clone());
			clonedCPMap.put(varName, new CPMapInfo(cpMap.get(varName).getNode(), cpMap.get(varName).getVariableName()));
		}
		
		return clonedCPMap;
	}
	
	private int CalculateArrayElementAddress(TreeNode currentBlock, DesignatorNode array, String scopeName, ArrayList<TreeNode> statements,
			boolean createPhiStats, HashMap<String, TreeNode> varIndex, HashMap<String, ArrayList<TreeNode>> varsWithOldSSAIndex,
			HashMap<String, Integer> oldCurrentScope, HashMap<String, Integer> oldMainScope,
			ArrayList<TreeNode>[] elimInstrs, HashMap<String, CPMapInfo> cpMap)
	{
		// This array list will contains information about the dimensions
		// (the execution results of expressions if any)
		ArrayList<TreeNode> expressions = new ArrayList<TreeNode>();
		for (int i = 0; i < array.getExpressionsSize(); i++)
		{
			TreeNode fixedExpression = this.fixParamSSAIndex(currentBlock, array.getExpression(i), scopeName, statements,
					varsWithOldSSAIndex, oldCurrentScope, oldMainScope, elimInstrs, cpMap, false);
			if (fixedExpression != null)
			{
				expressions.add(fixedExpression);
			}
			else
			{
				expressions.add(array.getExpression(i));
			}

//			// If the dimension is a number or ident, add it to the list as a number or ident token
//			if ((array.getExpression(i).getType().compareType(Tokens.number) == true) ||
//				(array.getExpression(i).getType().compareType(Tokens.ident) == true))
//			{
//				expressions.add(new TreeNode(Token.getToken(array.getExpression(i).getType().getCharacters())));
//			}
//			// Else, the dimension is an expression, parse it and add to the dimensions list as a statToken
//			else
//			{
//				this.parseAssignment(currentBlock, array.getExpression(i),
//						scopeName, statements, createPhiStats, varIndex, varsWithOldSSAIndex, oldCurrentScope,
//						oldMainScope, elimInstrs, cpMap);
//				for (int j = 0; j < statements.size(); j++)
//				{
//					if (statements.get(j).getDeleteReason() == DeleteReason.CSE)
//					{
//						this.cseDeletedInstructions.put(statements.get(j), ((BlockNode)(currentBlock)));
//					}
//				}
//				expressions.add(new TreeNode(Token.getToken(Tokens.statToken), statements.get(statements.size() - 1).getId()));
//			}
		}
		// Replace all the dimensions of the array with their expressions
		array.clearExpressions();
		for (int i = 0; i < expressions.size(); i++)
		{
			array.addExpression(expressions.get(i));
		}

		
		// Add the needed instructions to calculate the element address in the memory
		// the address is relative to the base address of the stack (the frame pointer)
		
		
		// Get the array information which contains the dimensions of the array
		Array arrayInfo;
		if (this.scope.containVariable(scopeName, array.getType().getCharacters()) == true)
		{
			arrayInfo = this.scope.getArray(scopeName, array.getType().getCharacters());
		}
		else
		{
			arrayInfo = this.scope.getArray(this.mainScopeName, array.getType().getCharacters());
		}
		
		// If the dimensions number of the array is bigger than 1, add multiple instructions
		// in the form of n4 + N4(n3 + N3(n2 + N2(n1 + N1(n0)))) where number of dimensions is 5
		// and small n is the expression and capital N is the dimension size, the indices represent
		// the index of a dimension
		TreeNode matchingNode;
		int refId = -1;
		if (expressions.size() > 1)
		{
			int mulId;
			for (int i = 0; i < expressions.size() - 1; i++)
			{
				TreeNode mul = new TreeNode(Token.getToken(Tokens.mulSSAToken));
				mul.setLeftChild(new TreeNode(Token.getToken(String.valueOf(arrayInfo.getDimension(i + 1)))));
				if (refId == -1)
				{
					mul.setRightChild(expressions.get(i));
				}
				else
				{
					mul.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
				}
				
				// Check CSE and process mul accordingly
				if (this.childsInNodeToFix(mul) == false)
				{
					matchingNode = this.CSECheck(elimInstrs, mul.getType().getValue() - 400, mul);
				}
				else
				{
					matchingNode = null;
				}
				if (matchingNode == null)
				{
					// Add the multiplication to statMap, and statements array list
					this.statsMap.put(mul.getId(), mul);
					this.statsIDMap.put(mul.getId(), new ArrayList<TreeNode>());
					statements.add(mul);
					
					mulId = mul.getId();
					
					// Add mul to the list of eliminatedInstructions
					elimInstrs[mul.getType().getValue() - 400].add(mul);
				}
				else
				{
					// Get the ID of the matching node
					mulId = matchingNode.getId();
					
					// Add mul to the list of statements that will eventually be the statements of the block,
					// but mark it as deleted, and add it to the deleted instructions list
					statements.add(mul);
					mul.setDeleteReason(DeleteReason.CSE);
				}
				
				TreeNode add = new TreeNode(Token.getToken(Tokens.addSSAToken));
				add.setLeftChild(expressions.get(i + 1));
				add.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), mulId));
				
				// Check CSE and process add accordingly
				if (this.childsInNodeToFix(add) == false)
				{
					matchingNode = this.CSECheck(elimInstrs, add.getType().getValue() - 400, add);
				}
				else
				{
					matchingNode = null;
				}
				if (matchingNode == null)
				{
					// Add the addition to statMap, and statements array list
					this.statsMap.put(add.getId(), add);
					this.statsIDMap.put(add.getId(), new ArrayList<TreeNode>());
					statements.add(add);
					
					refId = add.getId();
					
					// Add mul to the list of eliminatedInstructions
					elimInstrs[add.getType().getValue() - 400].add(add);
				}
				else
				{
					// Get the ID of the matching node
					refId = matchingNode.getId();
					
					// Add mul to the list of statements that will eventually be the statements of the block,
					// but mark it as deleted, and add it to the deleted instructions list
					statements.add(add);
					add.setDeleteReason(DeleteReason.CSE);
				}
			}
			
			// Multiply the resuting address by the unit size of the variable
			TreeNode mul = new TreeNode(Token.getToken(Tokens.mulSSAToken));
			mul.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
			mul.setRightChild(new TreeNode(Token.getToken(String.valueOf(Variable.getUnitSize()))));
			
			// Check CSE and process mul accordingly
			if (this.childsInNodeToFix(mul) == false)
			{
				matchingNode = this.CSECheck(elimInstrs, mul.getType().getValue() - 400, mul);
			}
			else
			{
				matchingNode = null;
			}
			if (matchingNode == null)
			{
				// Add the multiplication to statMap, and statements array list
				this.statsMap.put(mul.getId(), mul);
				this.statsIDMap.put(mul.getId(), new ArrayList<TreeNode>());
				statements.add(mul);
				
				refId = mul.getId();
				
				// Add mul to the list of eliminatedInstructions
				elimInstrs[mul.getType().getValue() - 400].add(mul);
			}
			else
			{
				// Get the ID of the matching node
				refId = matchingNode.getId();
				
				// Add mul to the list of statements that will eventually be the statements of the block,
				// but mark it as deleted, and add it to the deleted instructions list
				statements.add(mul);
				mul.setDeleteReason(DeleteReason.CSE);
			}
		}
		else
		{
			// Multiply the resuting address by the unit size of the variable
			TreeNode mul = new TreeNode(Token.getToken(Tokens.mulSSAToken));
			mul.setLeftChild(expressions.get(0));
			mul.setRightChild(new TreeNode(Token.getToken(String.valueOf(Variable.getUnitSize()))));
			
			// Check CSE and process mul accordingly
			if (this.childsInNodeToFix(mul) == false)
			{
				matchingNode = this.CSECheck(elimInstrs, mul.getType().getValue() - 400, mul);
			}
			else
			{
				matchingNode = null;
			}
			if (matchingNode == null)
			{
				// Add the multiplication to statMap, and statements array list
				this.statsMap.put(mul.getId(), mul);
				this.statsIDMap.put(mul.getId(), new ArrayList<TreeNode>());
				statements.add(mul);
				
				refId = mul.getId();
				
				// Add mul to the list of eliminatedInstructions
				elimInstrs[mul.getType().getValue() - 400].add(mul);
			}
			else
			{
				// Get the ID of the matching node
				refId = matchingNode.getId();
				
				// Add mul to the list of statements that will eventually be the statements of the block,
				// but mark it as deleted, and add it to the deleted instructions list
				statements.add(mul);
				mul.setDeleteReason(DeleteReason.CSE);
			}
		}
		
		// Add the base address of the array to the address of the frame
		TreeNode add = new TreeNode(Token.getToken(Tokens.addSSAToken));
		add.setLeftChild(new TreeNode(Token.getToken(Tokens.fpSSAToken)));
		add.setRightChild(new TreeNode(Token.getToken(String.valueOf(arrayInfo.getAddress()))));
		
		// Check CSE and process add accordingly
		int addId;
		if (this.childsInNodeToFix(add) == false)
		{
			matchingNode = this.CSECheck(elimInstrs, add.getType().getValue() - 400, add);
		}
		else
		{
			matchingNode = null;
		}
		if (matchingNode == null)
		{
			// Add the addition to statMap, and statements array list
			this.statsMap.put(add.getId(), add);
			this.statsIDMap.put(add.getId(), new ArrayList<TreeNode>());
			statements.add(add);
			
			addId = add.getId();
			
			// Add mul to the list of eliminatedInstructions
			elimInstrs[add.getType().getValue() - 400].add(add);
		}
		else
		{
			// Get the ID of the matching node
			addId = matchingNode.getId();
			
			// Add mul to the list of statements that will eventually be the statements of the block,
			// but mark it as deleted, and add it to the deleted instructions list
			statements.add(add);
			add.setDeleteReason(DeleteReason.CSE);
		}
		
		// Create and add the adda instruction, note than CSE cannot be part of CSE
		TreeNode adda = new TreeNode(Token.getToken(Tokens.addaSSAToken));
		adda.setLeftChild(new TreeNode(Token.getToken(Tokens.statToken), refId));
		adda.setRightChild(new TreeNode(Token.getToken(Tokens.statToken), addId));
		this.statsMap.put(adda.getId(), adda);
		this.statsIDMap.put(adda.getId(), new ArrayList<TreeNode>());
		statements.add(adda);
		
		return adda.getId();
	}
	
	private boolean childsInNodeToFix(TreeNode node)
	{
		for (String varName : this.nodesToFixInLoops.keySet())
		{
			if (node.hasChildren() == true)
			{
				if ((this.nodesToFixInLoops.get(varName).contains(node.getLeftChild()) == true) ||
					(this.nodesToFixInLoops.get(varName).contains(node.getRightChild()) == true))
				{
					return true;
				}
			}
			else
			{
				if (this.nodesToFixInLoops.get(varName).contains(node) == true)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void cleanUpCSEDeletedInstructions()
	{
		for (TreeNode deletedInstruction : this.cseDeletedInstructions.keySet())
		{
			this.cseDeletedInstructions.get(deletedInstruction).deleteStatement(deletedInstruction);
		}
	}
	
	public void cleanUpCPDeletedInstructions()
	{
		for (TreeNode deletedInstruction : this.cpDeletedInstructions.keySet())
		{
			this.cpDeletedInstructions.get(deletedInstruction).deleteStatement(deletedInstruction);
		}
	}
	
	public ArrayList<BlockNode> getConditionBlocks()
	{
		return this.conditionBlocks;
	}
	
	public HashMap<Integer, ArrayList<TreeNode>> getPhiIDUsageMap()
	{
		return this.phiIDUsageMap;
	}
	
	public HashMap<Integer, ArrayList<TreeNode>> getStatsIDMap()
	{
		return this.statsIDMap;
	}
	
	public HashMap<BlockNode, BlockNode> getPhiContainers()
	{
		return this.phiContainers;
	}
	
	public boolean getErrorFlag()
	{
		return this.errorFlag;
	}
	
	public HashSet<Integer> getGlobalVariableIDs()
	{
		HashSet<Integer> retSet = new HashSet<Integer>();
		for (String varName : this.multiScopeVariable)
		{
			if (this.scope.isArray(this.mainScopeName, varName) == false)
			{
				retSet.add(this.globalVarIDMap.get(varName));
			}
		}
		
		return retSet;
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}
