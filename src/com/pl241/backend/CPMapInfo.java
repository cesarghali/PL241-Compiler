package com.pl241.backend;

import com.pl241.ir.TreeNode;

/**
 * 
 * @author cesarghali
 *
 */

public class CPMapInfo
{
	private TreeNode node;
	private String variableName;
	
	public CPMapInfo(TreeNode node, String variableName)
	{
		this.node = node;
		this.variableName = variableName;
	}
	
	public TreeNode getNode()
	{
		return this.node;
	}
	
	public String getVariableName()
	{
		return this.variableName;
	}
}