package com.pl241.ra;

import java.io.File;
import java.util.HashMap;
import com.pl241.backend.DeadCodeElimination;
import com.pl241.backend.SingleStaticAssignment;
import com.pl241.core.GraphvizFileCreator;
import com.pl241.core.GraphvizFileCreator.GraphType;
import com.pl241.frontend.Parser;
import com.pl241.ir.TreeNode;

/**
 * 
 * @author cesarghali
 *
 * Test the register allocation algorithm
 */
public class RegisterAllocationTest
{
	public static void main(String[] args)
	{
		// Delete all previous output files
		File graphvizDir = new File("graphviz/");
		for (File file : graphvizDir.listFiles())
		{
			if ((file.getName().endsWith(".dot") == true) ||
				(file.getName().endsWith(".png") == true))
			{
				file.delete();
			}
		}
		
		Parser par = Parser.getInstance("codes/testprogs/test024.txt");
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
					
					LiveRange lr = new LiveRange(comp[0], comp[1], ssa.getGlobalVariableIDs());
					RIG graph = lr.Calculate();

					DeadCodeElimination dce = new DeadCodeElimination(ssa.getConditionBlocks(), ssa.getPhiIDUsageMap(),
							ssa.getStatsIDMap());
					dce.DCEParser();
					GraphvizFileCreator.createFile("graphviz/05_dce.dot", comp[0], false, GraphType.SSA, 5);
					
					RegisterAllocation ra = new RegisterAllocation(graph, 8, lr.getPhiFunctions());
					HashMap<Integer, String> colors = ra.colorify();
					GraphvizFileCreator.createRIG("graphviz/06_rig.dot", graph, colors);
					
					System.out.println("Compiled Successfully");
					
					ra.checkResult();
				}
			}
		}
	}
}