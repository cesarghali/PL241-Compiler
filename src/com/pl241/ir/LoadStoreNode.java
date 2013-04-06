package com.pl241.ir;

import com.pl241.frontend.Token;

/**
 * 
 * @author cesarghali
 *
 */
public class LoadStoreNode extends TreeNode
{
	private String arrayName;
	
	public LoadStoreNode(Token type, String arrayName)
	{
		super(type);
		this.arrayName = arrayName;
	}
	
	public String getArrayName()
	{
		return this.arrayName;
	}
	
	public void setArrayName(String arrayName)
	{
		this.arrayName = arrayName;
	}
}
