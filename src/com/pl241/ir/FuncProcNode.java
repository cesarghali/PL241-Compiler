package com.pl241.ir;

//import java.util.Hashtable;
import com.pl241.frontend.Token;

/**
 * 
 * @author cesarghali
 *
 */
public class FuncProcNode extends TreeNode
{
	private Token identifier;
	private BlockNode lastBlock;
	
	public FuncProcNode(Token type, Token identifier)
	{
		super(type);
		this.identifier = identifier;
		this.lastBlock = null;
	}
	
	public Token getIdentifier()
	{
		return this.identifier;
	}
	
	public void setIdentifier(Token identifier)
	{
		this.identifier = identifier;
	}
	
	public BlockNode getLastBlock()
	{
		return this.lastBlock;
	}
	
	public void setLastBlock(BlockNode lastBlock)
	{
		this.lastBlock = lastBlock;
	}
	
	/**
	 * @return String representation of ArrayNode
	 */
	public String toString()
	{
		return super.toString() + ", [" + identifier.toString() + "]";
	}
}
