package com.pl241.ir;

import java.util.ArrayList;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;

/**
 * 
 * @author cesarghali
 * 
 */

public class BlockNode extends TreeNode
{
	private ArrayList<TreeNode> statements;
	private BlockNode phiBlock;

	public BlockNode(Token type)
	{
		super(type);
		this.statements = new ArrayList<TreeNode>();
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be added to the end of statements ArrayList
	 */
	public void addStatement(TreeNode statement)
	{
		this.statements.add(statement);
		statement.setContainer(this);
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be inserted before the last element in statements ArrayList
	 */
	public void addStatementBeforeLast(TreeNode statement)
	{
		if (this.statements.size() == 0)
		{
			this.statements.add(statement);
		}
		else
		{
			this.statements.add(this.statements.size() - 1, statement);
		}
		statement.setContainer(this);
	}
	
	/**
	 * 
	 * @param statement
	 * 		will be added to the beginning of statements ArrayList
	 */
	public void addStatementAtBeginning(TreeNode statement)
	{
		this.statements.add(0, statement);
	}
	
	
	public void addPhiStatement(TreeNode phi)
	{
		boolean phiFound = false;
		int lastPhiIndex = 0;
		for (int i = 0; i < this.statements.size(); i++)
		{
			if (this.statements.get(i).getType().compareType(Tokens.phiSSAToken) == true)
			{
				lastPhiIndex = i;
				phiFound = true;
			}
			else
			{
				break;
			}
		}
		if (phiFound == false)
		{
			this.statements.add(0, phi);
		}
		else
		{
			
			if (this.statements.size() == lastPhiIndex)
			{
				this.statements.add(phi);
			}
			else
			{
				this.statements.add(lastPhiIndex + 1, phi);
			}
		}
	}
	
	/**
	 * 
	 * @param index
	 * 		index of the statement
	 * 		be careful with bounds!
	 * @return 	
	 * 		return the statement at given index
	 */
	public TreeNode getStatement(int index)
	{
		return this.statements.get(index);
	}
	
	public TreeNode getLastStatement()
	{
		return this.statements.get(this.statements.size() - 1);
	}
	
	/**
	 * Replace a statement with a the one in the specified index
	 * 
	 * @param statement
	 * @param index
	 */
	public void replaceStatement(TreeNode statement, int index)
	{
		this.statements.set(index, statement);
	}
	
	/**
	 * 
	 * @return size of the statements ArrayList
	 */
	public int getStatementsSize()
	{
		return this.statements.size();
	}
	
	public ArrayList<TreeNode> getStatements()
	{
		return this.statements;
	}
	
	public void clearStatements()
	{
		this.statements.clear();
	}
	
	public void deleteStatement(TreeNode statement)
	{
		this.statements.remove(statement);
	}
	
	public void deleteLastStatement()
	{
		this.statements.remove(this.statements.size() - 1);
	}
	
	public void deleteStatement(int index)
	{
		this.statements.remove(index);
	}
	
	public BlockNode getPhiBlock()
	{
		return this.phiBlock;
	}
	
	public void setPhiBlock(BlockNode phiBlock)
	{
		this.phiBlock = phiBlock;
	}
}
