package com.pl241.backend;

import com.pl241.backend.SingleStaticAssignment;
import com.pl241.core.GraphvizFileCreator;
import com.pl241.core.GraphvizFileCreator.GraphType;
import com.pl241.frontend.Parser;
import com.pl241.ir.TreeNode;

/**
 * 
 * @author cesarghali
 *
 * Test SSA
 */
public class SingleStaticAssignmentTest
{
	public static void main(String[] args)
	{
		Parser par = Parser.getInstance("codes/testprogs/factorial.txt");
		if (par != null)
		{
			TreeNode[] comp = par.computation();
			if (comp != null)
			{
				GraphvizFileCreator.createFile("graphviz/01_comp.dot", comp[0], false, GraphType.General, 1);

				SingleStaticAssignment ssa = new SingleStaticAssignment(comp[0]);
				ssa.ParseSSA();
				GraphvizFileCreator.createFile("graphviz/02_ssa.dot", comp[0], false, GraphType.SSA, 2);
				if (ssa.getErrorFlag() == false)
				{
					ssa.cleanUpCSEDeletedInstructions();
					GraphvizFileCreator.createFile("graphviz/03_cse.dot", comp[0], false, GraphType.SSA, 3);
					
					ssa.cleanUpCPDeletedInstructions();
					GraphvizFileCreator.createFile("graphviz/04_cp.dot", comp[0], false, GraphType.SSA, 4);
					
					System.out.println("Compiled Successfully");
				}
			}
		}
	}
}