package com.pl241.ra;

import java.util.HashMap;

import com.pl241.backend.SingleStaticAssignment;
import com.pl241.frontend.Parser;
import com.pl241.ir.TreeNode;

public class AllTestProgsRATest {

	public static void main(String[] args)
	{
		String input = "codes/testprogs/test024.txt";
		// 3 4 24 are huge ones
	
		Parser par = Parser.getInstance(input);
		System.out.println("Compiling: " + input);
		if (par != null)
		{
			TreeNode[] comp = par.computation();
			if (comp != null)
			{
				SingleStaticAssignment ssa = new SingleStaticAssignment(comp[0]);
				ssa.ParseSSA();
				if (ssa.getErrorFlag() == false)
				{
					ssa.cleanUpCSEDeletedInstructions();
					ssa.cleanUpCPDeletedInstructions();
					LiveRange lr = new LiveRange(comp[0], comp[1], ssa.getGlobalVariableIDs());
					RIG graph = lr.Calculate();
					//DeadCodeElimination dce = new DeadCodeElimination(ssa.getConditionBlocks(), ssa.getPhiIDUsageMap(),
					//		ssa.getStatsIDMap());
					//dce.DCEParser();
					RegisterAllocation ra = new RegisterAllocation(graph, 8, lr.getPhiFunctions());
					HashMap<Integer, String> colors = ra.colorify();
					ra.checkResult();
				}
			}
		}
	}
}
