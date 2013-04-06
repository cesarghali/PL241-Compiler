package com.pl241.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.pl241.backend.DeadCodeElimination;
import com.pl241.backend.SingleStaticAssignment;
import com.pl241.core.GraphvizFileCreator.GraphType;
import com.pl241.frontend.Parser;
import com.pl241.ir.TreeNode;
import com.pl241.platform.DLX;
import com.pl241.platform.MachineCode;
import com.pl241.ra.LiveRange;
import com.pl241.ra.ProcessPhiFunctions;
import com.pl241.ra.RIG;
import com.pl241.ra.RegisterAllocation;


/**
 * 
 * @author ekinoguz
 * 
 * Command Line Interface
 * 
 */
@SuppressWarnings("static-access")
public class CLI {
	
	static Options options = new Options();
	static Option help = OptionBuilder
						.withArgName("command")
						.hasOptionalArg()
						.withDescription("print this help message.")
						.withLongOpt("help").create('h');
	static Option version = OptionBuilder
						.withArgName("command")
						.hasArg()
						.withDescription("Version")
						.withLongOpt("version").create("v");
	static Option compileFile = OptionBuilder
						.withArgName("file")
						.hasArg()
						.withDescription("Compile the given file in codes/ directory")
						.withLongOpt("compileFile").create("f");
	static Option registerNumber = OptionBuilder.withLongOpt("registerNumber")
			            .withDescription("Number of registers for machine code (default is 8)")
			            .withType(Number.class)
			            .hasArg()
			            .withArgName("registerNumber")
			            .create("rn");
	static Option deadCodeElimination = OptionBuilder.withLongOpt("deadCodeElimination")
			            .withDescription("1 if Dead Code Elimination is enabled, 0 otherwise (default is disabled)")
			            .withType(Number.class)
			            .hasArg()
			            .withArgName("1/0")
			            .create("dce");
	static {
		options.addOption(help);
		options.addOption(version);
		options.addOption(compileFile);
		options.addOption(registerNumber);
		options.addOption(deadCodeElimination);
	}

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(options, args);
		if (cmd.hasOption('h')) {
			String command = cmd.getOptionValue('h');
			if (command == null) {
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.setLeftPadding(5);
				helpFormatter.setWidth(200);
				helpFormatter.printHelp(
								"run ant cli -Dcli=\"<option>\"",
								"options:",
								options, "Copyright");
			}
		} else if (cmd.hasOption('v')) {
			System.out.println("PL241-Compiler v0.9");
		} else if (cmd.hasOption("f")) {
			String compileFile = cmd.getOptionValue("f");
			int registerNumber = 8;
			boolean DCE = false;
			if (cmd.hasOption("registerNumber"))
				registerNumber = Integer.parseInt(cmd.getOptionValue("rn"));
			if (cmd.hasOption("deadCodeElimination"))
				DCE = Integer.parseInt(cmd.getOptionValue("dce")) != 0 ? true : false;
			run(compileFile, registerNumber, DCE);
			//System.exit(0);
		} else {
			System.out.println("something is wrong, who knows? type> ant cli -Dcli=\"-h\" for help");
		}
	}
	
	public static void run(String compileFile, int registerNumber, boolean DCE)
	{
		// Delete all previous output files
		File graphvizDir = new File("../graphviz/");
		for (File file : graphvizDir.listFiles())
		{
			if ((file.getName().endsWith(".dot") == true) ||
				(file.getName().endsWith(".png") == true) ||
				(file.getName().endsWith(".dlx") == true))
			{
				file.delete();
			}
		}
		Parser par = Parser.getInstance("../codes/" + compileFile);
		if (par != null)
		{
			TreeNode[] comp = par.computation();
			if (comp != null)
			{
				GraphvizFileCreator.createFile("../graphviz/01_comp.dot", comp[0], false, GraphType.General, 1);

				SingleStaticAssignment ssa = new SingleStaticAssignment(comp[0]);
				ssa.ParseSSA();
				GraphvizFileCreator.createFile("../graphviz/02_ssa.dot", comp[0], false, GraphType.SSA, 2);
				if (ssa.getErrorFlag() == false)
				{
					ssa.cleanUpCSEDeletedInstructions();
					GraphvizFileCreator.createFile("../graphviz/03_cse.dot", comp[0], false, GraphType.SSA, 3);
					
					ssa.cleanUpCPDeletedInstructions();
					GraphvizFileCreator.createFile("../graphviz/04_cp.dot", comp[0], false, GraphType.SSA, 4);
					
					if (DCE == true)
					{
						DeadCodeElimination dce = new DeadCodeElimination(ssa.getConditionBlocks(), ssa.getPhiIDUsageMap(),
								ssa.getStatsIDMap());
						dce.DCEParser();
						GraphvizFileCreator.createFile("../graphviz/05_dce.dot", comp[0], false, GraphType.SSA, 5);
					}
					
					ProcessPhiFunctions.eliminate(ssa.getPhiContainers()/*, colors*/);
					GraphvizFileCreator.createFile("../graphviz/06_elm.dot", comp[0], false, GraphType.SSA, 6);
					
					LiveRange lr = new LiveRange(comp[0], comp[1], ssa.getGlobalVariableIDs());
					RIG graph = lr.Calculate();
					
					RegisterAllocation ra = new RegisterAllocation(graph, registerNumber, lr.getPhiFunctions());
					HashMap<Integer, String> colors = ra.colorify();
					ra.checkResult();
					GraphvizFileCreator.createRIG("../graphviz/07_rig.dot", graph, colors);
					
					System.out.println("Compiled Successfully");
					
					System.out.println("Generating machine code:");
					MachineCode machineCode = new MachineCode(comp[0], colors, ssa.getGlobalVariableIDs());
					machineCode.generate();
					
					String machineCodeOutput = "";
					for (Integer i : machineCode.getInstructions())
						machineCodeOutput += (DLX.disassemble(i));
					Utils.saveFile("../graphviz/asm.dlx", machineCodeOutput);
					
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
