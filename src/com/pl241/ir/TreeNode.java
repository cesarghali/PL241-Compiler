package com.pl241.ir;

import com.pl241.core.GraphvizFileCreator.GraphType;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;
import java.util.ArrayList;


/**
 * @author ekinoguz, cesarghali
 * 
 * General tree node structure
 *
 */

public class TreeNode
{
	public enum DeleteReason { NotDeleted, CSE, CP, DCE };
	
	private Token type;	// type of the Token
	private TreeNode leftChild;
	private TreeNode rightChild;
	private ArrayList<TreeNode>	parents;
	private int id;
	private int refId; // id of the statement in case of computation or id of the block when branching
	// Number of traversed passes
	private int traversedPasses;
	// Instructions rooted at deleted nodes will be considered deleted
	private DeleteReason deleteReason;
	// Contains the level of nested while loops
	private int	loopLevel;
	// Contains a reference to the BlockNode (if any) containing this TreeNode
	private BlockNode container;
	
	// Use to generate unique ID for each TreeNode
	private static int counter = 1;
		
	public TreeNode(Token type)
	{
		this.setType(type);
		this.setLeftChild(null);
		this.setRightChild(null);
		this.parents = new ArrayList<TreeNode>();
		this.setId();
		this.refId = -1;
		this.traversedPasses = 0;
		this.deleteReason = DeleteReason.NotDeleted;
		this.loopLevel = -1;
		this.container = null;
	}
	
	public TreeNode(Token type, int refId)
	{
		this.setType(type);
		this.setLeftChild(null);
		this.setRightChild(null);
		this.parents = new ArrayList<TreeNode>();
		this.setId();
		this.refId = refId;
		this.traversedPasses = 0;
		this.deleteReason = DeleteReason.NotDeleted;
		this.loopLevel = -1;
		this.container = null;
	}
	
	/**
	 * 
	 * @return all the parents
	 */
	public int getParentsSize() 
	{
		return this.parents.size();
	}
	
	public boolean hasParent()
	{
		return (this.parents.size() != 0);
	}
	
	/**
	 * 
	 * @param index 
	 * 		index of the wanted parent.
	 * 		be careful with bounds!
	 * @return
	 * 		parent at the given index
	 */
	public TreeNode getParent(int index)
	{
		return this.parents.get(index);
	}
	
	public TreeNode getLastParent()
	{
		return this.parents.get(this.parents.size() - 1);
	}
	
	public TreeNode getParentBeforeLast()
	{
		return this.parents.get(this.parents.size() - 2);
	}

	public void addParent(TreeNode parent)
	{
		this.parents.add(parent);
	}
	
	public void clearParents()
	{
		this.parents.clear();
	}
	
	public void deleteParent(TreeNode parent)
	{
		this.parents.remove(parent);
	}
	
	public void deleteLastParent()
	{
		this.parents.remove(this.parents.size() - 1);
	}

	public TreeNode getRightChild()
	{
		return rightChild;
	}

	public void setRightChild(TreeNode rightChild)
	{
		this.rightChild = rightChild;
		if (this.rightChild != null)
		{
			this.rightChild.addParent(this);
		}
	}

	public TreeNode getLeftChild()
	{
		return leftChild;
	}

	public void setLeftChild(TreeNode leftChild)
	{
		this.leftChild = leftChild;
		if (this.leftChild != null)
		{
			this.leftChild.addParent(this);
		}
	}
	
	public boolean hasChildren()
	{
		return (this.leftChild != null) || (this.rightChild != null); 
	}
	
	public int getId()
	{
		return this.id;
	}
	
	private void setId() 
	{
		this.id = counter;
		counter += 1;
	}
	
	public int getRefId()
	{
		return this.refId;
	}
	
	public void setRefId(int refId) 
	{
		this.refId = refId;
	}
	
	public int getTraversedPasses()
	{
		return this.traversedPasses;
	}
	
	public void setTraversedPasses(int travPasses)
	{
		this.traversedPasses = travPasses;
	}
	
	public DeleteReason getDeleteReason()
	{
		return this.deleteReason;
	}
	
	public void setDeleteReason(DeleteReason deleteReason)
	{
		this.deleteReason = deleteReason;
	}

	public Token getType()
	{
		return type;
	}

	/*
	 * This function does not clone the passed class. Cloning ( if needed)
	 * should be taken care before calling this function
	 */
	public void setType(Token type)
	{
		this.type = type;
	}
	
	public int getLoopLevel()
	{
		return this.loopLevel;
	}
	
	public void setLoopLevel(int loopLevel)
	{
		this.loopLevel = loopLevel;
	}
	
	public BlockNode getContainer()
	{
		return this.container;
	}
	
	protected void setContainer(BlockNode container)
	{
		this.container = container;
	}
	
	/**
	 * @return outputs the in-order traversal of tree
	 */
	public void inorderTraverse(int numOfPasses)
	{
		if (this != null)
		{
			this.inorderTraverse(this, 0, 0, numOfPasses);
		}
	}
	
	private void inorderTraverse(TreeNode head, int level, int indent, int numOfPasses)
	{
		if (head != null)
		{
			if (head.getTraversedPasses() == numOfPasses)
			{
				return;
			}
			
			this.inorderTraverse(head.getLeftChild(), level + 1, indent, numOfPasses);
			
			for (int i = 0; i < indent; i++)
			{
				System.out.print("\t");
			}
			System.out.print("Level: " + level + "\t" + head.toString());
			if (head instanceof DesignatorNode)
			{
				System.out.print(" : [Dimensions = " +
						((DesignatorNode)(head)).getExpressionsSize() + "]\n");
				
				if (((DesignatorNode)(head)).getExpressionsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((DesignatorNode)(head)).getExpressionsSize(); i++)
					{
						this.inorderTraverse(((DesignatorNode)(head)).getExpression(i), 0, indent + 1,
								numOfPasses);
						if (i < ((DesignatorNode)(head)).getExpressionsSize() - 1)
						{
							for (int j = 0; j < indent; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else if (head instanceof BlockNode)
			{
				System.out.print(" : [Statements = " +
						((BlockNode)(head)).getStatementsSize() + "]\n");
				
				if (((BlockNode)(head)).getStatementsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((BlockNode)(head)).getStatementsSize(); i++)
					{
						this.inorderTraverse(((BlockNode)(head)).getStatement(i), 0, indent + 1,
								numOfPasses);
						if (i < ((BlockNode)(head)).getStatementsSize() - 1)
						{
							for (int j = 0; j < indent + 1; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else if (head instanceof FunctionBlockNode)
			{
				System.out.print(" : [Functions = " +
						((FunctionBlockNode)(head)).getFunctionsSize() + "]\n");
				
				if (((FunctionBlockNode)(head)).getFunctionsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((FunctionBlockNode)(head)).getFunctionsSize(); i++)
					{
						this.inorderTraverse(((FunctionBlockNode)(head)).getFunction(i), 0, indent + 1,
								numOfPasses);
						if (i < ((FunctionBlockNode)(head)).getFunctionsSize() - 1)
						{
							for (int j = 0; j < indent + 1; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else
			{
				System.out.println();
			}
			
			this.inorderTraverse(head.getRightChild(), level + 1, indent, numOfPasses);
			
			head.setTraversedPasses(numOfPasses);
		}
	}
	
	/**
	 * @return outputs the pre-order traversal of tree
	 */
	public void preorderTraverse(int numOfPasses)
	{
		if (this != null)
		{
			this.preorderTraverse(this, 0, 0, numOfPasses);
		}
	}
	
	private void preorderTraverse(TreeNode head, int level, int indent, int numOfPasses)
	{
		if (head != null)
		{
			if (head.getTraversedPasses() == numOfPasses)
			{
				return;
			}
			
			for (int i = 0; i < indent; i++)
			{
				System.out.print("\t");
			}
			System.out.print("Level: " + level + "\t" + head.toString());
			if (head instanceof DesignatorNode)
			{
				System.out.print(" : [Dimensions = " +
						((DesignatorNode)(head)).getExpressionsSize() + "]\n");
				
				if (((DesignatorNode)(head)).getExpressionsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((DesignatorNode)(head)).getExpressionsSize(); i++)
					{
						this.preorderTraverse(((DesignatorNode)(head)).getExpression(i), 0, indent + 1,
								numOfPasses);
						if (i < ((DesignatorNode)(head)).getExpressionsSize() - 1)
						{
							for (int j = 0; j < indent + 1; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else if (head instanceof BlockNode)
			{
				System.out.print(" : [Statements = " +
						((BlockNode)(head)).getStatementsSize() + "]\n");
				
				if (((BlockNode)(head)).getStatementsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((BlockNode)(head)).getStatementsSize(); i++)
					{
						this.preorderTraverse(((BlockNode)(head)).getStatement(i), 0, indent + 1,
								numOfPasses);
						if (i < ((BlockNode)(head)).getStatementsSize() - 1)
						{
							for (int j = 0; j < indent + 1; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else if (head instanceof FunctionBlockNode)
			{
				System.out.print(" : [Functions = " +
						((FunctionBlockNode)(head)).getFunctionsSize() + "]\n");
				
				if (((FunctionBlockNode)(head)).getFunctionsSize() != 0)
				{
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
					for (int i = 0; i < ((FunctionBlockNode)(head)).getFunctionsSize(); i++)
					{
						this.preorderTraverse(((FunctionBlockNode)(head)).getFunction(i), 0, indent + 1,
								numOfPasses);
						if (i < ((FunctionBlockNode)(head)).getFunctionsSize() - 1)
						{
							for (int j = 0; j < indent + 1; j++)
							{
								System.out.print("\t");
							}
							System.out.println("----------------------------------------");
						}
					}
					for (int i = 0; i < indent + 1; i++)
					{
						System.out.print("\t");
					}
					System.out.println("========================================");
				}
			}
			else
			{
				System.out.println();
			}
			
			this.preorderTraverse(head.getLeftChild(), level + 1, indent, numOfPasses);
			this.preorderTraverse(head.getRightChild(), level + 1, indent, numOfPasses);
			
			head.setTraversedPasses(numOfPasses);
		}
	}
	
	/**
	 * @return string representation of TreeNode
	 */
	public String toString()
	{
		return "Node id: " + this.id + "\t" + this.type.toString();
	}
	
	public String toGraph(boolean verbose, GraphType type)
	{
		String out;
		if (verbose == true)
		{
			out = "id: " + this.id + ", " + this.getType().getName() + ", " + this.getType().getCharacters();
		}
		else
		{
			out = this.id + ": ";
			if (this.getType().compareType(Tokens.number) == true)
			{
				out += "#" + this.getType().getCharacters();
			}
			else if (this.getType().compareType(Tokens.ident) == true)
			{
				if (type == GraphType.General)
				{
					out += this.getType().getCharacters();
				}
				else
				{
					out += this.getType().getCharacters() + "_" + this.getType().getSSAIndex();
				}
			}
			else if (this.getType().compareType(Tokens.statToken) == true)
			{
				out += "(" + this.getRefId() + ")";
			}
			else if (this.getType().compareType(Tokens.blockToken) == true)
			{
				out += "-> " + this.getRefId();
			}
			else if (this.getType().compareType(Tokens.phiSSAToken) == true)
			{
				out += "phi [" + this.getType().getCharacters() + "_" + this.getType().getSSAIndex() + "]";
			}
			else if ((this.getType().compareType(Tokens.funcToken) == true) ||
					(this.getType().compareType(Tokens.procToken) == true))
			{
				out += ((FuncProcNode)(this)).getIdentifier().getCharacters();
			}
			else if ((this.getType().compareType(Tokens.geqToken) == true) ||
					 (this.getType().compareType(Tokens.gtrToken) == true) ||
					 (this.getType().compareType(Tokens.lssToken) == true) ||
					 (this.getType().compareType(Tokens.leqToken) == true))
			{
				out += "\\" + this.getType().getCharacters();
			}
			else
			{
				out += this.getType().getCharacters();
			}
		}
		return out;
	}
	
	public TreeNode clone()
	{
		TreeNode clonedNode = new TreeNode(this.type.clone());
		clonedNode.setId();
		clonedNode.setRefId(this.refId);
		return clonedNode;
	}

	/**
	 * Replace the subtree rooted at implantNode with the subtree rooted at location.
	 * Also connect <implantNode> to <location> parents
	 *  
	 * @param implantNode
	 * @param location
	 */
	public static void Implant(TreeNode implantNode, TreeNode location)
	{
		if (implantNode == location)
		{
			return;
		}
		
		TreeNode parent;
		for (int i = 0; i < location.getParentsSize(); i++)
		{
			parent = location.getParent(i);
			if (parent.getLeftChild() == location)
			{
				parent.setLeftChild(implantNode);
			}
			else if (parent.getRightChild() == location)
			{
				parent.setRightChild(implantNode);
			}
		}
		
		location.clearParents();
	}
	
	/**
	 * 
	 * @return an ArrangedChildren class. This class contains the two children
	 * 			arranged in a way that if one of them is a number, it will always
	 * 			be the second one. Also if one is a number, the hasNumber field of
	 * 			the class will be true.
	 * 
	 */
	public ArrangedChildren arrangeChildren()
	{
		ArrangedChildren children = new ArrangedChildren();
		if (this.childrenAreNumbers() == true)
		{
			children.setHasNumber(true);
			children.setFirstChild(this.leftChild);
			children.setSecondChild(this.rightChild);
		}
		else
		{
			if (this.leftChild.getType().compareType(Tokens.number) == true)
			{
				children.setHasNumber(true);
				children.setFirstChild(this.rightChild);
				children.setSecondChild(this.leftChild);
			}
			else if (this.rightChild.getType().compareType(Tokens.number) == true)
			{
				children.setHasNumber(true);
				children.setFirstChild(this.leftChild);
				children.setSecondChild(this.rightChild);
			}
			else
			{
				children.setHasNumber(false);
				children.setFirstChild(this.leftChild);
				children.setSecondChild(this.rightChild);
			}
		}
		
		return children;
	}
	
	public boolean childrenAreNumbers()
	{
		return this.leftChild.getType().compareType(Tokens.number) &&
				this.rightChild.getType().compareType(Tokens.number);
	}
}
