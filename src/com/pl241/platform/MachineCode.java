package com.pl241.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.pl241.frontend.MemoryAllocator;
import com.pl241.frontend.Token;
import com.pl241.frontend.Tokens;
import com.pl241.frontend.Variable;
import com.pl241.ir.ArrangedChildren;
import com.pl241.ir.BlockNode;
import com.pl241.ir.FunctionBlockNode;
import com.pl241.ir.TreeNode;
import com.pl241.ra.RegisterAllocation;

/**
 * 
 * This class generates machine code for a DLX processor
 * 
 * @author cesarghali, ekinoguz
 *
 */
public class MachineCode
{
	// Register 10 is used whenever we need to put a constant in a register
	// in order to use it in the subsequent instruction
	private final int TEMP_REGISTER = 10;
	// Register 29 is the stack pointer
	private final int SP = 29;
	// Register 31 contains the return address when we use JSP
	private final int RP = 31;
	
	private TreeNode mainNode;
	// Contains a mapping between temporals and their register/memory number
	private HashMap<Integer, String> colors;
	// Contains the IDs of the global variables
	private HashSet<Integer> globalVariableIDs;
	// Contains the list of DLX instructions
	private ArrayList<Integer> instructions;
	// Contains the register number of the free proxy
	private int freeProxy;
	// Contains the base address of the area in memory where virtual registers
	// are stored
	private int virtualRegsBaseAddr;
	// Contains the indices of the instructions where the operand c needs to be be fixed
	// according to the base address of the stack
	private HashSet<Integer> addrToBeFixed;
	// Indicate whether we are writing to a proxy register and we need to store the value
	// back in the memory;
	private boolean storeBack;
	private int storeBackProxy;
	// Contains a map between the function IDs and their address in the memory
	private HashMap<Integer, Integer> functionAddresses;
	private HashMap<Integer, Integer> funcCallToBeFixed;
	
	public MachineCode(TreeNode main, HashMap<Integer, String> colors, HashSet<Integer> globalVariableIDs)
	{
		this.mainNode = main;
		this.colors = colors;
		this.globalVariableIDs = globalVariableIDs;
		this.instructions = new ArrayList<Integer>();
		this.freeProxy = RegisterAllocation.fixedColorSize - 1;
		this.virtualRegsBaseAddr = MemoryAllocator.getVirtualRegsBaseAddr();
		this.addrToBeFixed = new HashSet<Integer>();
		this.storeBack = false;
		this.functionAddresses = new HashMap<Integer, Integer>();
		this.funcCallToBeFixed = new HashMap<Integer, Integer>();
	}
	
	public void generate()
	{
		// Set the stack pointer in register R29, set it to 0 now and fix it later
		this.instructions.add(DLX.assemble(DLX.ADDI, this.SP, 0, 0));
		// Jump to the beginning of the main function
		this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, 0));
		this.instructions.add(DLX.assemble(DLX.RET, this.TEMP_REGISTER));
		
		TreeNode currentNode = this.mainNode.getLeftChild();
		boolean beginningOfMain = false;
		while (currentNode != null)
		{
			if (currentNode.getType().compareType(Tokens.varDeclSeqToken) == true)
			{
				currentNode = currentNode.getLeftChild();
			}
			else if (currentNode.getType().compareType(Tokens.funcSeqToken) == true)
			{
				for (int i = 0; i < ((FunctionBlockNode)(currentNode)).getFunctionsSize(); i++)
				{
					this.parseFuncBody(((FunctionBlockNode)(currentNode)).getFunction(i));
				}
				currentNode = currentNode.getLeftChild();
			}
			else if ((currentNode.getType().compareType(Tokens.statSeqToken) == true) ||
					 (currentNode.getType().compareType(Tokens.fiToken) == true) ||
					 (currentNode.getType().compareType(Tokens.odToken) == true))
			{
				if (beginningOfMain == false)
				{
					beginningOfMain = true;
					this.instructions.set(1, this.instructions.get(1) + this.instructions.size() * Variable.getUnitSize());
				}
				
				for (int i = 0; i < ((BlockNode)(currentNode)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(currentNode)).getStatement(i));
				}
				currentNode = currentNode.getLeftChild();
			}
			else if (currentNode.getType().compareType(Tokens.ifToken) == true)
			{
				if (beginningOfMain == false)
				{
					beginningOfMain = true;
					this.instructions.set(1, this.instructions.get(1) + this.instructions.size() * Variable.getUnitSize());
				}
				
				currentNode = this.parseIfStatement(currentNode);
			}
			else if (currentNode.getType().compareType(Tokens.whileToken) == true)
			{
				if (beginningOfMain == false)
				{
					beginningOfMain = true;
					this.instructions.set(1, this.instructions.get(1) + this.instructions.size() * Variable.getUnitSize());
				}
				
				currentNode = this.parseWhileStatement(currentNode);
			}
			else if (currentNode.getType().compareType(Tokens.endofProgToken) == true)
			{
				if (beginningOfMain == false)
				{
					beginningOfMain = true;
					this.instructions.set(1, this.instructions.get(1) + this.instructions.size() * Variable.getUnitSize());
				}
				
				this.instructions.add(DLX.assemble(DLX.RET, 0));
				currentNode = currentNode.getLeftChild();
			}
		}
		
		// Fix the stack pointer in register R29
		int virtualRegisterSize = this.getVirtualRegisterSize();
		this.instructions.set(0, this.instructions.get(0) + (this.instructions.size() * Variable.getUnitSize()) +
				this.virtualRegsBaseAddr + virtualRegisterSize);
		
		this.fixBaseAddress();
		this.fixFuncCall();
	}
	
	private void parseFuncBody(TreeNode funcHead)
	{
		// Store the address of the function in the map
		this.functionAddresses.put(funcHead.getId(), this.instructions.size() * Variable.getUnitSize());
		
		TreeNode currentNode = funcHead.getLeftChild();
		while (currentNode != null)
		{
			if ((currentNode.getType().compareType(Tokens.statSeqToken) == true) ||
				(currentNode.getType().compareType(Tokens.fiToken) == true) ||
				(currentNode.getType().compareType(Tokens.odToken) == true))
			{
				boolean doneWithArgs = false;
				for (int i = 0; i < ((BlockNode)(currentNode)).getStatementsSize(); i++)
				{
					// Pop arguments
					if (doneWithArgs == false)
					{
						if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.ident) == true)
						{
							int regNo = this.getRegisterNumber(((BlockNode)(currentNode)).getStatement(i).getId(), false);
							if (regNo == -1)
							{
								regNo = this.TEMP_REGISTER;
							}
							this.instructions.add(DLX.assemble(DLX.POP, regNo, this.SP, -1 * Variable.getUnitSize()));
							
							if (this.storeBack == true)
							{
								this.storeBack = false;
								String color;
								if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.movSSAToken) == true)
								{
									color = colors.get(((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId());
								}
								else
								{
									color = colors.get(((BlockNode)(currentNode)).getStatement(i).getId());
								}
								int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));
								this.addrToBeFixed.add(this.instructions.size());
								this.instructions.add(DLX.assemble(DLX.STW, this.storeBackProxy, 0, memory));
							}
						}
						else
						{
							doneWithArgs = true;
							i--;
						}
					}
					else
					{
						this.addInstruction(((BlockNode)(currentNode)).getStatement(i));
					}
				}
				currentNode = currentNode.getLeftChild();
			}
			else if (currentNode.getType().compareType(Tokens.ifToken) == true)
			{
				currentNode = this.parseIfStatement(currentNode);
			}
			else if (currentNode.getType().compareType(Tokens.whileToken) == true)
			{
				currentNode = this.parseWhileStatement(currentNode);
			}
		}
		
		// Always push R0 if the function ends and does not return anything
		this.instructions.add(DLX.assemble(DLX.PSH, 0, this.SP, Variable.getUnitSize()));

		// Jump back to normal execution after function call
		this.instructions.add(DLX.assemble(DLX.RET, this.RP));
	}
	
	/**
	 * Fix all addresses in the memory to be relative to the base address of the stack which
	 * is the address after all the program instructions
	 */
	private void fixBaseAddress()
	{
		// We added +1 to the instructions size because the DLX.load() sets the instruction after
		// the end of the program in the memory to -1
		int baseAddress = (this.instructions.size() + 1) * Variable.getUnitSize();
		for (int index : this.addrToBeFixed)
		{
			this.instructions.set(index, this.instructions.get(index) + baseAddress);
		}
	}
	
	private void fixFuncCall()
	{
		for (int index : this.funcCallToBeFixed.keySet())
		{
			int funcAddr = this.functionAddresses.get(this.funcCallToBeFixed.get(index));
			this.instructions.set(index, this.instructions.get(index) + funcAddr);
		}
	}
	
	// NOTE:
	// Only use DLX.assemble() function which can take opcode and integer inputs up to 3.
	// If instruction has two inputs, just give two, do not put extra 0's
	private void addInstruction(TreeNode node)
	{
		// be careful with this. It has to be be Opcode of biggest Mnemonic + 1
		int instruction = -1;
		int regNo;
		int regNo1;
		int regNo2;
		switch (node.getType().getType())
		{
			// TODO: we might need to treat adda differently
			case addaSSAToken:
			case addSSAToken:
				if (node.getLeftChild().getType().compareType(Tokens.fpSSAToken) == true)
				{
					node.getLeftChild().setType(Token.getToken("0"));
				}
				instruction = this.assembleArithmaticInstruction(node, DLX.ADD);
				break;
				
			case subSSAToken:
				instruction = this.assembleArithmaticInstruction(node, DLX.SUB);
				break;
				
			case mulSSAToken:
				instruction = this.assembleArithmaticInstruction(node, DLX.MUL);
				break;
				
			case divSSAToken:
				instruction = this.assembleArithmaticInstruction(node, DLX.DIV);
				break;
				
			case cmpSSAToken:
				instruction = this.assembleArithmaticInstruction(node, DLX.CMP);
				break;
				
			case movSSAToken:
				if (node.getRightChild().getType().compareType(Tokens.number) == true)
				{
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(node.getRightChild())));
					regNo = this.TEMP_REGISTER;
				}
				else
				{
					regNo = this.getRegisterNumber(node.getRightChild().getRefId(), true);
				}
				regNo1 = this.getRegisterNumber(node.getLeftChild().getRefId(), false);
				if (regNo != -1 && regNo1 != -1)
				{
					instruction = DLX.assemble(DLX.ADD, regNo1, regNo, 0);
				}
				break;
				
			case loadSSAToken:
				regNo1 = this.getRegisterNumber(node.getId(), false);
				regNo2 = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo1 != -1 && regNo2 != -1)
				{
					instruction = DLX.assemble(DLX.LDW, regNo1, regNo2, 0);
					this.addrToBeFixed.add(this.instructions.size());
				}
				break;
				
			case storeSSAToken:
				if (node.getLeftChild().getType().compareType(Tokens.number) == true)
				{
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(node.getLeftChild())));
					regNo = this.TEMP_REGISTER;
				}
				else
				{
					regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				}
				regNo1 = this.getRegisterNumber(node.getRightChild().getRefId(), true);
				if (regNo != -1 && regNo1 != -1)
				{
					instruction = DLX.assemble(DLX.STW, regNo, regNo1, 0);
					this.addrToBeFixed.add(this.instructions.size());
				}
				break;

			case beqSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BEQ, regNo, 0);
				}
				break;
				
			case bneSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BNE, regNo, 0);
				}
				break;
				
			case bltSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BLT, regNo, 0);
				}
				break;
				
			case bgeSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BGE, regNo, 0);
				}
				break;
				
			case bleSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BLE, regNo, 0);
				}
				break;
				
			case bgtSSAToken:
				regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
				if (regNo != -1)
				{
					instruction = DLX.assemble(DLX.BGT, regNo, 0);
				}
				break;
				
			case braSSAToken:
				// The opcode is RET because it is an unconditional jump
				this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, 0));
				instruction = DLX.assemble(DLX.RET, this.TEMP_REGISTER);
				break;
				
			// Predefined functions have preDefToken type. We only support InputNum(), OutputNum(x),
			// OutputNewLine() predefined functions
			case callToken:
			case predefToken:
				instruction = this.assembleCallInstruction(node);
				break;
				
			case returnToken:
				if (node.getLeftChild() == null)
				{
					// Always push R0 if the function ends and does not return anything
					regNo = 0;
				}
				else
				{
					if (node.getLeftChild().getType().compareType(Tokens.number) == true)
					{
						this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(node.getLeftChild())));
						regNo = this.TEMP_REGISTER;
					}
					else
					{
						regNo = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
					}
				}
				this.instructions.add(DLX.assemble(DLX.PSH, regNo, this.SP, Variable.getUnitSize()));
				this.instructions.add(DLX.assemble(DLX.RET, this.RP));
				break;
				
			default:
				this.Error("Error: unrecognized statement: " + node.toString());
				break;
		}
		
		if (instruction != -1)
		{
			this.instructions.add(instruction);
			
			if (this.storeBack == true)
			{
				this.storeBack = false;
				String color;
				if (node.getType().compareType(Tokens.movSSAToken) == true)
				{
					color = colors.get(node.getLeftChild().getRefId());
				}
				else
				{
					color = colors.get(node.getId());
				}
				int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));
				this.addrToBeFixed.add(this.instructions.size());
				this.instructions.add(DLX.assemble(DLX.STW, this.storeBackProxy, 0, memory));
			}
		}
		else
		{
			this.storeBack = false;
		}
	}
	
	private TreeNode parseIfStatement(TreeNode currentNode)
	{
		boolean doneWithArgs = false;
		int condBraIndex = 0;
		for (int i = 0; i < ((BlockNode)(currentNode)).getStatementsSize(); i++)
		{
			// Pop arguments
			if (doneWithArgs == false)
			{
				if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.ident) == true)
				{
					int regNo = this.getRegisterNumber(((BlockNode)(currentNode)).getStatement(i).getId(), false);
					this.instructions.add(DLX.assemble(DLX.POP, regNo, this.SP, -1 * Variable.getUnitSize()));
					
					if (this.storeBack == true)
					{
						this.storeBack = false;
						String color;
						if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.movSSAToken) == true)
						{
							color = colors.get(((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId());
						}
						else
						{
							color = colors.get(((BlockNode)(currentNode)).getStatement(i).getId());
						}
						int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));
						this.addrToBeFixed.add(this.instructions.size());
						this.instructions.add(DLX.assemble(DLX.STW, this.storeBackProxy, 0, memory));
					}
				}
				else
				{
					doneWithArgs = true;
					i--;
				}
			}
			else
			{
				if ((((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bneSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.beqSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bleSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bgtSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bgeSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bltSSAToken) == true))
				{
					condBraIndex = this.instructions.size();
				}
				
				this.addInstruction(((BlockNode)(currentNode)).getStatement(i));
			}
		}
		// Save the index of the branch instruction which is the last instruction of the
		// current block
//		int condBraIndex = this.instructions.size() - 1;

		TreeNode thenBlock = currentNode.getLeftChild();
		while (thenBlock.getType().compareType(Tokens.fiToken) == false)
		{
			if (thenBlock.getType().compareType(Tokens.statSeqToken) == true)
			{
				for (int i = 0; i < ((BlockNode)(thenBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(thenBlock)).getStatement(i));
				}
				thenBlock = thenBlock.getLeftChild();
			}
			else if (thenBlock.getType().compareType(Tokens.ifToken) == true)
			{
				thenBlock = this.parseIfStatement(thenBlock);
				
				// Parse the thenBlock (fiBlock) as a statement sequence and move on
				for (int i = 0; i < ((BlockNode)(thenBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(thenBlock)).getStatement(i));
				}
				thenBlock = thenBlock.getLeftChild();
			}
			else if (thenBlock.getType().compareType(Tokens.whileToken) == true)
			{
				thenBlock = this.parseWhileStatement(thenBlock);
				
				// bodyBlock now is odBlock, parse it as if it is a statement sequence block
				for (int i = 0; i < ((BlockNode)(thenBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(thenBlock)).getStatement(i));
				}
				thenBlock = thenBlock.getLeftChild();
			}
		}
		// Fix the conditional branching address
		// Note that c in the branch instruction is the number of 'instructions' to be skipped, and not bytes
		this.instructions.set(condBraIndex, this.instructions.get(condBraIndex) + (this.instructions.size() - condBraIndex));
		
		TreeNode elseBlock = currentNode.getRightChild();
		if (elseBlock != null)
		{
			// The index of the move instruction to R10, which contains the address where to jump
			int braMovIndex = this.instructions.size() - 2;
			while (elseBlock.getType().compareType(Tokens.fiToken) == false)
			{
				if (elseBlock.getType().compareType(Tokens.statSeqToken) == true)
				{
					for (int i = 0; i < ((BlockNode)(elseBlock)).getStatementsSize(); i++)
					{
						this.addInstruction(((BlockNode)(elseBlock)).getStatement(i));
					}
					elseBlock = elseBlock.getLeftChild();
				}
				else if (elseBlock.getType().compareType(Tokens.ifToken) == true)
				{
					elseBlock = this.parseIfStatement(elseBlock);
					
					// Parse the elseBlock (fiBlock) as a statement sequence and move on
					for (int i = 0; i < ((BlockNode)(elseBlock)).getStatementsSize(); i++)
					{
						this.addInstruction(((BlockNode)(elseBlock)).getStatement(i));
					}
					elseBlock = elseBlock.getLeftChild();
				}
				else if (elseBlock.getType().compareType(Tokens.whileToken) == true)
				{
					elseBlock = this.parseWhileStatement(elseBlock);
					
					// bodyBlock now is odBlock, parse it as if it is a statement sequence block
					for (int i = 0; i < ((BlockNode)(elseBlock)).getStatementsSize(); i++)
					{
						this.addInstruction(((BlockNode)(elseBlock)).getStatement(i));
					}
					elseBlock = elseBlock.getLeftChild();
				}
			}
			
			// Fix the conditional branching address
			// Note that c in the jump instruction is an absolute address given in bytes
			this.instructions.set(braMovIndex, this.instructions.get(braMovIndex) + (this.instructions.size() * Variable.getUnitSize()));
		}
		
		// From now on, thenBlock is the fiBlock
		return thenBlock;
	}
	
	private TreeNode parseWhileStatement(TreeNode currentNode)
	{
		// Contains the index of the instruction at the beginning of the while block.
		// This will be used to fix the address of the jump instruction at the end of the
		// while body block
		int beginningOfWhileIndex = this.instructions.size();
		
		boolean doneWithArgs = false;
		int condBraIndex = 0;
		for (int i = 0; i < ((BlockNode)(currentNode)).getStatementsSize(); i++)
		{
			// Pop arguments
			if (doneWithArgs == false)
			{
				if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.ident) == true)
				{
					int regNo = this.getRegisterNumber(((BlockNode)(currentNode)).getStatement(i).getId(), false);
					this.instructions.add(DLX.assemble(DLX.POP, regNo, this.SP, -1 * Variable.getUnitSize()));
					
					if (this.storeBack == true)
					{
						this.storeBack = false;
						String color;
						if (((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.movSSAToken) == true)
						{
							color = colors.get(((BlockNode)(currentNode)).getStatement(i).getLeftChild().getRefId());
						}
						else
						{
							color = colors.get(((BlockNode)(currentNode)).getStatement(i).getId());
						}
						int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));
						this.addrToBeFixed.add(this.instructions.size());
						this.instructions.add(DLX.assemble(DLX.STW, this.storeBackProxy, 0, memory));
					}
				}
				else
				{
					doneWithArgs = true;
					i--;
				}
			}
			else
			{
				if ((((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bneSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.beqSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bleSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bgtSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bgeSSAToken) == true) ||
					(((BlockNode)(currentNode)).getStatement(i).getType().compareType(Tokens.bltSSAToken) == true))
				{
					condBraIndex = this.instructions.size();
				}
				
				this.addInstruction(((BlockNode)(currentNode)).getStatement(i));
			}			
		}
		// Save the index of the branch instruction which is the last instruction of the
		// current block
//		int condBraIndex = this.instructions.size() - 1;
		
		TreeNode bodyBlock = currentNode.getRightChild();
		while (bodyBlock != currentNode)
		{
			if (bodyBlock.getType().compareType(Tokens.statSeqToken) == true)
			{
				for (int i = 0; i < ((BlockNode)(bodyBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(bodyBlock)).getStatement(i));
				}
				bodyBlock = bodyBlock.getLeftChild();
			}
			else if (bodyBlock.getType().compareType(Tokens.ifToken) == true)
			{
				bodyBlock = this.parseIfStatement(bodyBlock);
				
				// bodyBlock now is fiBlock, parse it as if it is a statement sequence block
				for (int i = 0; i < ((BlockNode)(bodyBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(bodyBlock)).getStatement(i));
				}
				
				bodyBlock = bodyBlock.getLeftChild();
			}
			else if (bodyBlock.getType().compareType(Tokens.whileToken) == true)
			{
				bodyBlock = this.parseWhileStatement(bodyBlock);

				// bodyBlock now is odBlock, parse it as if it is a statement sequence block
				for (int i = 0; i < ((BlockNode)(bodyBlock)).getStatementsSize(); i++)
				{
					this.addInstruction(((BlockNode)(bodyBlock)).getStatement(i));
				}
				
				bodyBlock = bodyBlock.getLeftChild();
			}
		}
		// Fix the conditional branching address
		// Note that c in the branch instruction is the number of 'instructions' to be skipped, and not bytes
		this.instructions.set(condBraIndex, this.instructions.get(condBraIndex) + (this.instructions.size() - condBraIndex));
		
		// Fix the unconditional branching address
		// Note that c in the jump instruction is an absolute address given in bytes
		int braMovIndex = this.instructions.size() - 2;
		this.instructions.set(braMovIndex, this.instructions.get(braMovIndex) + (beginningOfWhileIndex * Variable.getUnitSize()));
		
		
		// Return the left child of the while node which is the odBlock
		return currentNode.getLeftChild();
	}
	
	/**
	 * @return register number of the given nodeID
	 * 
	 * @param load if true add LDW instruction
	 */
	private int getRegisterNumber(int nodeID, boolean load)
	{
	    String color = colors.get(nodeID);
		// If color is null, then this node has not assigned neither a register nor a memory,
		// which means it's not used in the program
	    if (color == null)
	    {
	    	return -1;
	    }
	    
	    // if given node is not stored in memory
	    if ('m' != color.charAt(0))
	    {
	        return Integer.parseInt(color);
	    }
	       
	    int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));

	    // load the variable in memory to proxy register
	    if (load == true)
	    {
	    	this.addrToBeFixed.add(this.instructions.size());
	    	this.instructions.add(DLX.assemble(DLX.LDW, this.freeProxy, 0, memory));
	    }
	    else
	    {
		    this.storeBack = true;
		    this.storeBackProxy = this.freeProxy;
	    }
	    
	    // From now on, memory is the free proxy number
	    memory = this.freeProxy;
	    this.updateFreeProxy();	  
	    return memory;
	}
	    
	/**
	 * @param variable
	 * @return memory address of given virtual variable
	 */
	// TODO: not sure about that
	private int getVirtualRegAddrInMemory(int variable)
	{
	    return virtualRegsBaseAddr + (Variable.getUnitSize() * variable);
	}
	
	private void updateFreeProxy()
	{
		this.freeProxy = (this.freeProxy % 2) + (RegisterAllocation.fixedColorSize - 1);
	}
	
	private int getNumberFromNode(TreeNode node)
	{
		return Integer.parseInt(node.getType().getCharacters());
	}
	
	private int assembleArithmaticInstruction(TreeNode node, int operation)
	{
		ArrangedChildren children = node.arrangeChildren();
		int regNo1 = this.getRegisterNumber(node.getId(), false);
		if (regNo1 == -1)
		{
			return -1;
		}
		
		switch (operation)
		{
			case DLX.ADD:
			case DLX.MUL:
				if (node.childrenAreNumbers() == false)
				{
					int regNo2 = this.getRegisterNumber(children.getFirstChild().getRefId(), true);
					int regNo3 = this.getRegisterNumber(children.getSecondChild().getRefId(), true);
					if (children.hasNumber() == false && regNo2 != -1 && regNo3 != -1)
					{
						return DLX.assemble(operation, regNo1, regNo2, regNo3);
					}
					else if (regNo2 != -1)
					{
						return DLX.assemble(operation + 16, regNo1, regNo2, this.getNumberFromNode(children.getSecondChild()));
					}
				}
				else
				{
					// Move the first child number to register R10 and then use operationI to do the operation
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(children.getFirstChild())));
					return DLX.assemble(operation + 16, regNo1, this.TEMP_REGISTER, this.getNumberFromNode(children.getSecondChild()));
				}
				break;
				
			case DLX.CMP:
				if (node.childrenAreNumbers() == false)
				{
					if (node.getRightChild().getType().compareType(Tokens.number) == true)
					{
						int regNo2 = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
						if (regNo2 != -1)
						{
							return DLX.assemble(operation + 16, regNo1, regNo2, this.getNumberFromNode(node.getRightChild()));
						}
					}
					else if (node.getLeftChild().getType().compareType(Tokens.number) == true)
					{
						int regNo3 = this.getRegisterNumber(node.getRightChild().getRefId(), true);
						if (regNo3 != -1)
						{
							this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(node.getLeftChild())));
							return DLX.assemble(operation, regNo1, this.TEMP_REGISTER, regNo3);
						}
					}
					else
					{
						int regNo2 = this.getRegisterNumber(node.getLeftChild().getRefId(), true);
						int regNo3 = this.getRegisterNumber(node.getRightChild().getRefId(), true);
						if (regNo2 != -1 && regNo3 != -1)
						{
							return DLX.assemble(operation, regNo1, regNo2, regNo3);
						}
					}
				}
				else
				{
					// Move the first child number to register R10 and then use operationI to do the operation
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(children.getFirstChild())));
					return DLX.assemble(operation + 16, regNo1, this.TEMP_REGISTER, this.getNumberFromNode(children.getSecondChild()));
				}
				break;
				
			case DLX.SUB:
				if (node.childrenAreNumbers() == false)
				{
					int regNo2 = this.getRegisterNumber(children.getFirstChild().getRefId(), true);
					if (children.hasNumber() == false)
					{
						int regNo3 = this.getRegisterNumber(children.getSecondChild().getRefId(), true);
						if (regNo2 != -1 && regNo3 != -1)
						{
							return DLX.assemble(operation, regNo1, regNo2, regNo3);
						}
					}
					else if (regNo2 != -1)
					{
						this.instructions.add(DLX.assemble(operation + 16, regNo1, regNo2, this.getNumberFromNode(children.getSecondChild())));
						if (children.getSecondChild() == node.getLeftChild())
						{
							this.instructions.add(DLX.assemble(DLX.SUB, regNo1, 0, regNo1));
						}
					}
				}
				else
				{
					// Move the first child number to register R10 and then use operationI to do the operation
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(children.getFirstChild())));
					return DLX.assemble(operation + 16, regNo1, this.TEMP_REGISTER, this.getNumberFromNode(children.getSecondChild()));
				}
				break;
				
			case DLX.DIV:
				if (node.childrenAreNumbers() == false)
				{
					int regNo2 = this.getRegisterNumber(children.getFirstChild().getRefId(), true);
					if (children.hasNumber() == false)
					{
						int regNo3 = this.getRegisterNumber(children.getSecondChild().getRefId(), true);
						if (regNo2 != -1 && regNo3 != -1)
						{
							return DLX.assemble(operation, regNo1, regNo2, regNo3);
						}
					}
					else if (children.getSecondChild() == node.getLeftChild() && regNo2 != -1)
					{
						this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(children.getSecondChild())));
						return DLX.assemble(operation, regNo1, this.TEMP_REGISTER, regNo2);
					}
					else if (regNo2 != -1)
					{
						return DLX.assemble(operation + 16, regNo1, regNo2, this.getNumberFromNode(children.getSecondChild()));
					}
				}
				else
				{
					// Move the first child number to register R10 and then use operationI to do the operation
					this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0, this.getNumberFromNode(children.getFirstChild())));
					return DLX.assemble(operation + 16, regNo1, this.TEMP_REGISTER, this.getNumberFromNode(children.getSecondChild()));
				}
				break;
		}
		
		return -1;
	}
	

	
	private int assembleCallInstruction(TreeNode node)
	{
		int instruction = -1;
		if (node.getType().getCharacters().compareTo("read") == 0)
		{
			int regNo1 = this.getRegisterNumber(node.getId(), false);
			if (regNo1 != -1)
			{
				instruction = DLX.assemble(DLX.RDI, regNo1);
			}
			else
			{
				instruction = -1;
			}
		}
		else if (node.getType().getCharacters().compareTo("wln") == 0)
		{
			instruction = DLX.assemble(DLX.WRL);
		}
		else if (node.getType().getCharacters().compareTo("write") == 0)
		{
			TreeNode parameter = ((BlockNode)(node.getRightChild())).getStatement(0);
			int regNumber = this.TEMP_REGISTER;
			// If the parameter is a reference 
			if (parameter.getType().compareType(Tokens.statToken) == true)
			{
				regNumber = this.getRegisterNumber(parameter.getRefId(), true);
			}
			else if (parameter.getType().compareType(Tokens.number) == true)
			{
				this.instructions.add(DLX.assemble(DLX.ADDI, regNumber, 0, this.getNumberFromNode(parameter)));
			}
			if (regNumber != -1)
			{
				instruction = DLX.assemble(DLX.WRD, regNumber);
			}
			else
			{
				instruction = -1;
			}
		}
		else
		{
			int functionID = node.getLeftChild().getRefId();
			// Push all register to the end of the use memory stack
			HashSet<Integer> globalRegs = new HashSet<Integer>();
			for (int i = 1; i <= RegisterAllocation.fixedColorSize; i++)
			{
				boolean skip = false;
				for (Integer id : this.globalVariableIDs)
				{
					String color = this.colors.get(id);
					if (color == null)
					{
						continue;
					}
					else if (color.equals(i + ""))
					{
						globalRegs.add(i);
						skip = true;
						break;
					}
				}
				if (skip == false)
				{
					this.instructions.add(DLX.assemble(DLX.PSH, i, this.SP, Variable.getUnitSize()));
				}
			}
			
			// Push R31 into stack
			this.instructions.add(DLX.assemble(DLX.PSH, this.RP, this.SP, Variable.getUnitSize()));

			// Pass arguments (if any) in reversed order (Push them in stack)
			if (node.getRightChild() != null)
			{
				for (int i =  0; i < ((BlockNode)(node.getRightChild())).getStatementsSize(); i++)
				{
					int regNo;
					if (((BlockNode)(node.getRightChild())).getStatement(i).getType().compareType(Tokens.number) == true)
					{
						this.instructions.add(DLX.assemble(DLX.ADDI, this.TEMP_REGISTER, 0,
								this.getNumberFromNode(((BlockNode)(node.getRightChild())).getStatement(i))));
						regNo = this.TEMP_REGISTER;
					}
					else
					{
						regNo = this.getRegisterNumber(((BlockNode)(node.getRightChild())).getStatement(i).getRefId(), true);
					}
					this.instructions.add(DLX.assemble(DLX.PSH, regNo, this.SP, Variable.getUnitSize()));
				}
			}
			
			// Call the function
			this.funcCallToBeFixed.put(this.instructions.size(), functionID);
			this.instructions.add(DLX.assemble(DLX.JSR, 0));
			
			// Pop return value into R10
			this.instructions.add(DLX.assemble(DLX.POP, this.TEMP_REGISTER, this.SP, -1 * Variable.getUnitSize()));			

			// Pop R31
			this.instructions.add(DLX.assemble(DLX.POP, this.RP, this.SP, -1 * Variable.getUnitSize()));

			// Pop all register from the memory
			for (int i = RegisterAllocation.fixedColorSize; i >= 1; i--)
			{
				if (globalRegs.contains(i) == true)
				{
					continue;
				}
				
				this.instructions.add(DLX.assemble(DLX.POP, i, this.SP, -1 * Variable.getUnitSize()));
			}
			
			// Move the return value into the correct register which is regNo
			int regNo = this.getRegisterNumber(node.getId(), false);
			if (regNo != -1)
			{
				this.instructions.add(DLX.assemble(DLX.ADD, regNo, this.TEMP_REGISTER, 0));

				if (this.storeBack == true)
				{
					this.storeBack = false;
					String color;
					if (node.getType().compareType(Tokens.movSSAToken) == true)
					{
						color = colors.get(node.getLeftChild().getRefId());
					}
					else
					{
						color = colors.get(node.getId());
					}
					int memory = this.getVirtualRegAddrInMemory(Integer.parseInt(color.substring(1)));
					this.addrToBeFixed.add(this.instructions.size());
					this.instructions.add(DLX.assemble(DLX.STW, this.storeBackProxy, 0, memory));
				}
			}
		}
		
		return instruction;
	}
	
	private int getVirtualRegisterSize()
	{
		int count = 0;
		for (String value : this.colors.values())
		{
			if ('m' == value.charAt(0))
		    {
				count++;
		    }
		}
		
		return count * Variable.getUnitSize();
	}
	
	/**
	 * @return int[] of instructions
	 */
	public int[] getInstructions()
	{
	    int[] out = new int[this.instructions.size()];
	    for (int i = 0; i < out.length; i++)
	    {
	        out[i] = instructions.get(i);
	    }
	    return out;
	}
	
	/**
	 * 
	 * @param error message to be printed
	 */
	private void Error(String error)
	{
		System.out.println(error);
	}
}
