package com.pl241.ir;

import java.util.Arrays;

public class ArrangedChildren
{
	private boolean hasNumber;
	private TreeNode[] children;
	
	public ArrangedChildren()
	{
		this.children = new TreeNode[2];
	}
	
	public void setHasNumber(boolean hasNumber)
	{
		this.hasNumber = hasNumber;
	}
	
	public boolean hasNumber()
	{
		return this.hasNumber;
	}
	
	public void setFirstChild(TreeNode child)
	{
		this.children[0] = child;
	}
	
	public void setSecondChild(TreeNode child)
	{
		this.children[1] = child;
	}
	
	public TreeNode getFirstChild()
	{
		return this.children[0];
	}
	
	public TreeNode getSecondChild()
	{
		return this.children[1];
	}
	
	public String toString()
	{
		return Arrays.toString(children);
	}
}
