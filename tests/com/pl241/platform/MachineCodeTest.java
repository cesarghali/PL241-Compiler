package com.pl241.platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import com.pl241.backend.DeadCodeElimination;
import com.pl241.backend.SingleStaticAssignment;
import com.pl241.core.GraphvizFileCreator;
import com.pl241.core.GraphvizFileCreator.GraphType;
import com.pl241.core.Utils;
import com.pl241.frontend.Parser;
import com.pl241.ir.TreeNode;
import com.pl241.ra.LiveRange;
import com.pl241.ra.ProcessPhiFunctions;
import com.pl241.ra.RIG;
import com.pl241.ra.RegisterAllocation;

/**
 * 
 * @author cesarghali
 *
 * Test for the code generation
 */

public class MachineCodeTest
{
	public static void main(String[] args)
	{
		boolean useDCE = false;
		
		// Delete all previous output files
		File graphvizDir = new File("graphviz/");
		for (File file : graphvizDir.listFiles())
		{
			if ((file.getName().endsWith(".dot") == true) ||
				(file.getName().endsWith(".png") == true) ||
				(file.getName().endsWith(".dlx") == true))
			{
				file.delete();
			}
		}
		
		Parser par = Parser.getInstance("codes/testprogs/pr3");
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
					
					if (useDCE == true)
					{
						DeadCodeElimination dce = new DeadCodeElimination(ssa.getConditionBlocks(), ssa.getPhiIDUsageMap(),
								ssa.getStatsIDMap());
						dce.DCEParser();
						GraphvizFileCreator.createFile("graphviz/05_dce.dot", comp[0], false, GraphType.SSA, 5);
					}
					
					ProcessPhiFunctions.eliminate(ssa.getPhiContainers()/*, colors*/);
					GraphvizFileCreator.createFile("graphviz/06_elm.dot", comp[0], false, GraphType.SSA, 6);
					
					LiveRange lr = new LiveRange(comp[0], comp[1], ssa.getGlobalVariableIDs());
					RIG graph = lr.Calculate();
					
					RegisterAllocation ra = new RegisterAllocation(graph, 6, lr.getPhiFunctions());
					HashMap<Integer, String> colors = ra.colorify();
					ra.checkResult();
					GraphvizFileCreator.createRIG("graphviz/07_rig.dot", graph, colors);
					
					System.out.println("Compiled Successfully");
					
					System.out.println("Generating machine code:");
					MachineCode machineCode = new MachineCode(comp[0], colors, ssa.getGlobalVariableIDs());
					machineCode.generate();
					
					String machineCodeOutput = "";
					for (Integer i : machineCode.getInstructions())
						machineCodeOutput += (DLX.disassemble(i));
					Utils.saveFile("graphviz/asm.dlx", machineCodeOutput);
					
					System.out.println("Executing the program:");
					DLX.load(machineCode.getInstructions());
					try
					{
						DLX.execute();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}
}