package com.pl241.frontend;

import java.util.ArrayList;
import java.util.HashMap;

import com.pl241.ir.ArrayNode;
import com.pl241.ir.BlockNode;
import com.pl241.ir.DesignatorNode;
import com.pl241.ir.FuncProcNode;
import com.pl241.ir.FunctionBlockNode;
import com.pl241.ir.MainNode;
import com.pl241.ir.ParamNode;
import com.pl241.ir.TreeNode;

/*
 * @author: cesarghali, ekinoguz
 */
public class Parser
{
	private static Parser instance;

	private Scanner scanner;
	// Contains the last seen token
	private Token scannerSym;
	// Variable scopes
	private VariablesScope scope;
	// List of predefined functions/procedures
	private String[] predefFuncProc = { "InputNum", "OutputNum", "OutputNewLine" };
	// Contains a list of functions and all the global variables that are assigned
	// in the functions
	private HashMap<String, ArrayList<String>> funcAssignedVarMap;
	// Contains a list of variables used in multiple functions
	private ArrayList<String> multiScopeVariable;

	/**
	 * @return: Returns an new instance of this class in case it is not already created
	 */
	public static Parser getInstance(String fileName)
	{
		if (instance == null)
		{
			instance = new Parser(fileName);
			// If the fileReader object is null, then there is an error
			// while opening the file. Therefore return null
			if (instance.scanner == null)
			{
				return null;
			}
		}
		return instance;
	}
	
	// Constructor
	private Parser(String fileName)
	{
		this.scanner = Scanner.getInstance(fileName);		
		// If the scanner object is created successfully then read the first token
		// from the scanner
		if (this.scanner != null)
		{
			this.scope = new VariablesScope();
			// Add predefined functions and procedures to the list of scopes
			for (int i = 0; i < this.predefFuncProc.length; i++)
			{
				this.scope.addScope(this.predefFuncProc[i]);
			}
			
			this.funcAssignedVarMap = new HashMap<String, ArrayList<String>>();
			this.multiScopeVariable = new ArrayList<String>();
			
			this.Next();
		}
	}
	
	/**
	 * @return: Invokes Scanner.getSym() to Read the next token from the scanner. Then stores
	 * it in the scannerSym variable
	 */
	public void Next()
	{
		scannerSym = this.scanner.getSym();
	}
	
	/*
	 * assignment = "let" designator "<-" expression
	 */
	private TreeNode assignment(String scopeName)
	{
		TreeNode assignNode = null;
		if (accept(Tokens.letToken))
		{
			assignNode = new TreeNode(this.scannerSym.clone());
			this.Next();
			DesignatorNode desNode = this.designator(scopeName);
			if (desNode == null)
			{
				return null;
			}
			
			if (this.accept(Tokens.becomesToken) == false)
			{
				this.Error("assignment: syntax error, expect <-");
				return null;
			}
			this.Next();	// eat the becomesToken
			TreeNode expNode = this.expression(scopeName);
			if (expNode == null)
			{
				return null;
			}

			assignNode.setLeftChild(desNode);
			assignNode.setRightChild(expNode);
			
			if (scopeName.compareTo("main") != 0)
			{
				// TODO: if multiScopeVariable is removed read the next condition from the main scope
				if (this.multiScopeVariable.contains(desNode.getType().getCharacters()) == true)
				{
					if (this.funcAssignedVarMap.containsKey(scopeName) == false)
					{
						this.funcAssignedVarMap.put(scopeName, new ArrayList<String>());
					}
					this.funcAssignedVarMap.get(scopeName).add(desNode.getType().getCharacters());
				}
			}
		}
		return assignNode;
	}
	
	/*
	 * designator = ident { "[" expression "]" }
	 */
	private DesignatorNode designator(String scopeName)
	{
		DesignatorNode desNode = null;
		if (accept(Tokens.ident)) 
		{
			if ((this.scope.containVariable(scopeName, this.scannerSym.getCharacters()) == false) &&
				(this.scope.containVariable("main", this.scannerSym.getCharacters()) == false))
			{
				this.Error("designator: undefined identifier " + this.scannerSym.getCharacters() +
						" in scope " + scopeName);
				return null;
			}
			desNode = new DesignatorNode(this.scannerSym.clone());
			this.Next();
			while (accept(Tokens.openbracketToken))
			{
				this.Next();
				TreeNode expression = this.expression(scopeName);
				if(expression == null)
				{
					return null;
				}
				
				desNode.addExpression(expression);
				if (this.accept(Tokens.closebracketToken) == false)
				{
					this.Error("designator: syntax error, expect ]");
					return null;
				}
				this.Next();	// eat the closing brackets
			}
		}
		
		// TODO: if multiScopeVariable is removed remove the next nested if statements
		// If the variable in the designator node is used in the main scope, then it is
		// a multiScopeVariable
//		if (desNode != null)
		{
			if (scopeName.compareTo("main") != 0)
			{
				if (this.scope.containVariable("main", desNode.getType().getCharacters()) == true)
				{
					if (this.multiScopeVariable.contains(desNode.getType().getCharacters()) == false)
					{
						this.multiScopeVariable.add(desNode.getType().getCharacters());
					}
				}
			}
		}
		
		return desNode;
	}
	
	/*
	 *  expression = term { ("+" |"-") term }
	 */
	private TreeNode expression(String scopeName)
	{
		TreeNode termNode = this.term(scopeName);
		if (termNode == null)
		{
			return null;
		}
		
		if (accept(Tokens.plusToken) || accept(Tokens.minusToken))
		{
			TreeNode expNode = new TreeNode(this.scannerSym);
			this.Next();
			TreeNode rightNode = this.expression(scopeName);
			if (rightNode == null)
			{
				return null;
			}
			
			expNode.setLeftChild(termNode);
			expNode.setRightChild(rightNode);
			return expNode;
		}
		else
		{
			return termNode;
		}
	}
	
	/*
	 *  term = factor { ("*" | "/") factor }
	 */
	private TreeNode term(String scopeName) 
	{
		TreeNode factorNode = this.factor(scopeName);
		if (factorNode == null)
		{
			return null;
		}
		
		if (accept(Tokens.timesToken) || accept(Tokens.divToken))
		{
			TreeNode termNode = new TreeNode(this.scannerSym);
			this.Next();
			TreeNode rightNode = this.term(scopeName);
			if (rightNode == null)
			{
				return null;
			}
			
			termNode.setLeftChild(factorNode);
			termNode.setRightChild(rightNode);
			return termNode;
		}
		else
		{
			return factorNode;
		}
	}
	
	/*
	 * factor = designator | number | "(" expression ")" | funcCall
	 */
	private TreeNode factor(String scopeName)
	{
		TreeNode factorNode = null;
		// if number
		if (accept(Tokens.number))
		{
			factorNode = new TreeNode(this.scannerSym.clone());
			this.Next();
		}
		// if expression
		else if (accept(Tokens.openparenToken))
		{
			this.Next(); // eat openparanToken
			factorNode = this.expression(scopeName);
			if (factorNode == null)
			{
				return null;
			}
			
			if (this.accept(Tokens.closeparenToken) == true)
			{
				this.Next(); // eat closeparanToken
			}
			else
			{
				this.Error("factor: syntax error, expect ]");
				return null;
			}
		}
		// if function call, do not eat the token
		else if (accept(Tokens.callToken))
		{
			factorNode = this.funcCall(scopeName);
		}
		// if designator, do not eat the token
		else if (accept(Tokens.ident))
		{
			factorNode = this.designator(scopeName);
		}
		else
		{
			this.Error("factor: syntax error");
		}
		return factorNode;
	}
	
	// relation = expression relOp expression
	private TreeNode relation(String scopeName) 
	{
		TreeNode relNode = null;
		TreeNode leftChild = this.expression(scopeName);
		if (leftChild == null)
		{
			return null;
		}
		
		// if current token is a relation operation: ==, !=, <, <=, >, >=
		if (this.scannerSym.compareType(Tokens.eqlToken) || this.scannerSym.compareType(Tokens.neqToken) ||
			this.scannerSym.compareType(Tokens.lssToken) || this.scannerSym.compareType(Tokens.geqToken) ||
			this.scannerSym.compareType(Tokens.leqToken) || this.scannerSym.compareType(Tokens.gtrToken))
		{
			relNode = new TreeNode(this.scannerSym);
			this.Next(); // eat the relation operation token
			TreeNode rightChild = this.expression(scopeName);
			if (rightChild == null)
			{
				return null;
			}
			
			relNode.setLeftChild(leftChild);
			relNode.setRightChild(rightChild);
		}
		else
		{
			this.Error("relation: syntax error");
		}
		return relNode;
	}
	
	/*
	 * returnStatement = "return" [ expression ]
	 */
	private TreeNode returnStatement(String scopeName)
	{
		TreeNode retNode = null;
		if (accept(Tokens.returnToken))
		{
			retNode = new TreeNode(this.scannerSym.clone());
			this.Next();
			if ((this.scannerSym.compareType(Tokens.semiToken) == false) &&
				(this.scannerSym.compareType(Tokens.elseToken) == false) &&
				(this.scannerSym.compareType(Tokens.fiToken) == false) &&
				(this.scannerSym.compareType(Tokens.odToken) == false) &&
				(this.scannerSym.compareType(Tokens.endToken) == false))
			{
				TreeNode expNode = this.expression(scopeName);
				if (expNode == null)
				{
					return null;
				}
				
				retNode.setLeftChild(expNode);
			}
		}
		else
		{
			this.Error("returnStatement: syntax error");
		}
		return retNode;
	}
	

	private TreeNode statement(String scopeName)
	{
		TreeNode retNode = null;
		if (this.scannerSym.compareType(Tokens.letToken) == true)
		{
			retNode = this.assignment(scopeName);
		}
		else if (this.scannerSym.compareType(Tokens.callToken) == true)
		{
			retNode = this.funcCall(scopeName);
		}		
		else if (this.scannerSym.compareType(Tokens.ifToken) == true)
		{
			retNode = this.ifStatement(scopeName);
		}
		else if (this.scannerSym.compareType(Tokens.whileToken) == true)
		{
			retNode = this.whileStatement(scopeName);
		}		
		else if (this.scannerSym.compareType(Tokens.returnToken) == true)
		{
			retNode = this.returnStatement(scopeName);
		}
		else
		{
			this.Error("statement: syntax error, expect let, call, if, while or return, instead of " +
					this.scannerSym.getType().toString());
		}
		return retNode;
	}
	
	// statSequence = statement { ";" statement }
	private TreeNode[] statSequence(String scopeName)
	{
		BlockNode firstBlock = null;
		BlockNode lastBlock = null;

		TreeNode currentStatement = this.statement(scopeName);
		if (currentStatement == null)
		{
			return null;
		}
		
		if ((currentStatement.getType().compareType(Tokens.letToken) == true) ||
			(currentStatement.getType().compareType(Tokens.callToken) == true) ||
			(currentStatement.getType().compareType(Tokens.returnToken) == true))
		{
			firstBlock = lastBlock = new BlockNode(Token.getToken(Tokens.statSeqToken));
			lastBlock.addStatement(currentStatement);
		}
		else if (currentStatement.getType().compareType(Tokens.ifToken) == true)
		{
			firstBlock = lastBlock = ((BlockNode)(currentStatement));
			while(lastBlock.getLeftChild() != null)
			{
				lastBlock = ((BlockNode)(lastBlock.getLeftChild()));
			}
		}
		else if (currentStatement.getType().compareType(Tokens.whileToken) == true)
		{
			firstBlock = ((BlockNode)(currentStatement));
			lastBlock = ((BlockNode)(currentStatement.getLeftChild()));
		}
		
		if (this.accept(Tokens.semiToken) == true)
		{
			this.Next();
			
			TreeNode[] nextNodes = this.statSequence(scopeName);
			if (nextNodes == null)
			{
				return null;
			}
			
			if ((nextNodes[0].getType().compareType(Tokens.statSeqToken) == true) &&
				((lastBlock.getType().compareType(Tokens.statSeqToken) == true) ||
				 (lastBlock.getType().compareType(Tokens.fiToken) == true) ||
				 (lastBlock.getType().compareType(Tokens.odToken) == true)))	// TODO: fiToken and odToken might be removed from the confition
			{
				for (int i = 0; i < ((BlockNode)(nextNodes[0])).getStatementsSize(); i++)
				{
					lastBlock.addStatement(((BlockNode)(nextNodes[0])).getStatement(i));
				}
				if (nextNodes[0].getLeftChild() != null)
				{
					lastBlock.setLeftChild(nextNodes[0].getLeftChild());
					lastBlock = ((BlockNode)(nextNodes[1]));
				}
			}
			else
			{
				lastBlock.setLeftChild(nextNodes[0]);
				lastBlock = ((BlockNode)(nextNodes[1]));
			}
			
			return new TreeNode[] { firstBlock, lastBlock };
		}
		else if ((this.scannerSym.compareType(Tokens.elseToken) == true) ||
				 (this.scannerSym.compareType(Tokens.fiToken) == true) ||
				 (this.scannerSym.compareType(Tokens.odToken) == true) ||
				 (this.scannerSym.compareType(Tokens.endToken) == true)||
				 (this.scannerSym.compareType(Tokens.eofToken) == true))
		{
			return new TreeNode[] { firstBlock, lastBlock };
		}
		else
		{
			this.Error("statSequence: syntax error, missing semicolon");
			return null;
		}
	}
	
	// ifStatement = "if" relation "then" statSequence [ "else" statSequence ] "fi".
	private TreeNode ifStatement(String scopeName)
	{
		BlockNode ifNode = null;
		if (this.accept(Tokens.ifToken))
		{
			ifNode = new BlockNode(Token.getToken(Tokens.ifToken));
			this.Next();
			TreeNode relation = this.relation(scopeName);
			if (relation == null)
			{
				return null;
			}
			ifNode.addStatement(relation);
			
			if (this.accept(Tokens.thenToken))
			{
				this.Next();
				
				TreeNode[] thenBlocks = this.statSequence(scopeName);
				if (thenBlocks == null)
				{
					return null;
				}
				
				TreeNode[] elseBlocks = null;
				if (this.accept(Tokens.elseToken) == true)
				{
					this.Next(); // eat the else token
					elseBlocks = this.statSequence(scopeName);
					if (elseBlocks == null)
					{
						return null;
					}
				}
				
				BlockNode fiBlock = null;
				if (this.accept(Tokens.fiToken) == true)
				{
					this.Next(); // eat the fi token
					fiBlock = new BlockNode(Token.getToken(Tokens.fiToken));
				}
				else
				{
					this.Error("ifStatement: syntax error, expect fi");
					return null;
				}
				
				// QUESTIONdoes not understand here?!
				ifNode.setLeftChild(thenBlocks[0]);
				if (elseBlocks != null)
				{
					ifNode.setRightChild(elseBlocks[0]);
				}
				thenBlocks[1].setLeftChild(fiBlock);
				if (elseBlocks != null)
				{
					elseBlocks[1].setLeftChild(fiBlock);
				}
			}
			else
			{
				this.Error("ifStatement: syntax error, expect then");
				return null;
			}
		}
		else
		{
			this.Error("ifStatement: syntax error, expect if");
		}
		
		return ifNode;
	}
	
	// whileStatement = "while" relation "do" StatSequence "od"
	private TreeNode whileStatement(String scopeName)
	{
		BlockNode whileNode = null;
		if (this.accept(Tokens.whileToken))
		{
			whileNode = new BlockNode(Token.getToken(Tokens.whileToken));
			this.Next();
			TreeNode relation = this.relation(scopeName);
			if (relation == null)
			{
				return null;
			}
			whileNode.addStatement(relation);
			
			if (this.accept(Tokens.doToken))
			{
				this.Next();
				TreeNode[] bodyBlocks = this.statSequence(scopeName);
				if (bodyBlocks == null)
				{
					return null;
				}
				
				BlockNode odBlock = null;
				if (this.accept(Tokens.odToken) == true)
				{
					this.Next();
					odBlock = new BlockNode(Token.getToken(Tokens.odToken));
				}
				else
				{
					this.Error("whileStatement: syntax error, expect od");
					return null;
				}
				
				// QUESTION: talk about here as well. while od is at left?
				whileNode.setLeftChild(odBlock);
				whileNode.setRightChild(bodyBlocks[0]);
				bodyBlocks[1].setLeftChild(whileNode);
			}
			else
			{
				this.Error("whileStatement: syntax error, expect do");
				return null;
			}
		}
		else
		{
			this.Error("whileStatement: syntax error, expect while");
		}
		
		return whileNode;
	}
	
	// funcCall = "call" ident [ "(" [expression { "," expression } ] ")" ].
	private TreeNode funcCall(String scopeName)
	{
		TreeNode retNode = null;
		if (this.accept(Tokens.callToken) == false)
		{
			this.Error("funcCall: syntax error, expect call");
			return null;
		}
		
		// get the call token and eat it
		retNode = new TreeNode(this.scannerSym.clone());
		this.Next();
		
		// check whether next token is ident or not
		if (this.accept(Tokens.ident) == false)
		{
			this.Error("funcCall: syntax error, expect identifier");
			return null;
		}
		
		// QUESTION: does this check function name?
		if (this.scope.containScope(this.scannerSym.getCharacters()) == false)
		{
			this.Error("funcCall: undefined function/procedure " + this.scannerSym.getCharacters());
			return null;
		}
		retNode.setLeftChild(new TreeNode(this.scannerSym.clone()));
		this.Next(); // eat the ident which is function name?
		
		if (this.accept(Tokens.openparenToken) == true)
		{
			this.Next(); // eat the openparenToken
			
			// QUESTION I did not understand here
			if (this.accept(Tokens.closeparenToken) == false)
			{
				BlockNode argsNode = new BlockNode(Token.getToken(Tokens.argsToken));
				TreeNode expression = this.expression(scopeName);
				if (expression == null)
				{
					return null;
				}
				
				argsNode.addStatement(expression);
				while (this.accept(Tokens.commaToken) == true)
				{
					this.Next();
					expression = this.expression(scopeName);
					if (expression == null)
					{
						return null;
					}
					
					argsNode.addStatement(expression);
				}
				
				retNode.setRightChild(argsNode);
				
				if (this.accept(Tokens.closeparenToken) == true)
				{
					this.Next();
				}
				else
				{
					this.Error("funcCall: syntax error, expect )");
					return null;
				}
			}
			else
			{
				this.Next();
			}
		}
		return retNode;
	}
	
	// varDecl = typeDecl indent { "," ident } ";"
	private TreeNode varDecl(String scopeName)
	{
		TreeNode retNode;
		// Contain the size of the variable, either scalar = unit size
		// or array size which is the result of multiplication of the
		// size of all dimensions and the unit size
		int size;
		ArrayList<Integer> dimensions = new ArrayList<Integer>();
		if (this.accept(Tokens.varToken) == true)
		{
			retNode = new TreeNode(this.scannerSym.clone());
			size = Variable.getUnitSize();
			this.Next();
		}
		else if (this.accept(Tokens.arrToken) == true)
		{
			retNode = new ArrayNode(this.scannerSym.clone());
			this.Next();
			
			size = Variable.getUnitSize();
			while (this.accept(Tokens.openbracketToken) == true)
			{
				this.Next(); // eat openbracketToken
				((ArrayNode)(retNode)).addNumber(Integer.parseInt(this.scannerSym.getCharacters()));
				size *= Integer.parseInt(this.scannerSym.getCharacters());
				dimensions.add(Integer.parseInt(this.scannerSym.getCharacters()));
				this.Next(); // eat number
				if (this.accept(Tokens.closebracketToken) == true)
				{
					this.Next();
				}
				else
				{
					this.Error("varDecl: syntax error, ] is expected in array declaration");
					return null;
				}
			}
		}
		else
		{
			this.Error("varDecl: syntax error, expect var or array");
			return null;
		}
		
		TreeNode ident = new TreeNode(this.scannerSym.clone());
		if (this.scope.containVariable(scopeName, ident.getType().getCharacters()) == false)
		{
			if (retNode instanceof ArrayNode)
			{
				this.scope.addArray(scopeName, ident.getType().getCharacters(), MemoryAllocator.Allocate(size), dimensions);
			}
			else
			{
				this.scope.addVariable(scopeName, ident.getType().getCharacters(), MemoryAllocator.Allocate(size));
			}
		}
		else
		{
			this.Error("varDecl: duplicate declaration of variable/array " +
					ident.getType().getCharacters() + " in scope " + scopeName);
			return null;
		}
		
		this.Next(); // eat the ident
		retNode.setLeftChild(ident);
		TreeNode currentNode = retNode;
		while(true)
		{
			if (this.accept(Tokens.semiToken) == true)
			{
				TreeNode semiNode = new TreeNode(this.scannerSym.clone());
				this.Next();
				currentNode.setRightChild(semiNode);
				break;
			}
			else if (this.accept(Tokens.commaToken) == true)
			{
				TreeNode commaNode = new TreeNode(this.scannerSym.clone());
				this.Next();
				currentNode.setRightChild(commaNode);
				currentNode = commaNode;
				
				ident = new TreeNode(this.scannerSym.clone());
				if (this.scope.containVariable(scopeName, ident.getType().getCharacters()) == false)
				{
					if (retNode instanceof ArrayNode)
					{
						this.scope.addArray(scopeName, ident.getType().getCharacters(), MemoryAllocator.Allocate(size), dimensions);
					}
					else
					{
						this.scope.addVariable(scopeName, ident.getType().getCharacters(), MemoryAllocator.Allocate(size));
					}	
				}
				else
				{
					this.Error("varDecl: duplicate declaration of variable/array " +
							ident.getType().getCharacters() + " in scope " + scopeName);
					return null;
				}
				
				this.Next();
				currentNode.setLeftChild(ident);
			}
			else
			{
				this.Error("varDecl: syntax error, expect ,");
				return null;
			}
		}
		
		return retNode;
	}
	
	private TreeNode varDeclSequence(String scopeName)
	{
		BlockNode varDeclSeqBlock = new BlockNode(Token.getToken(Tokens.varDeclSeqToken));
		while (	(this.accept(Tokens.varToken) == true) || 
				(this.accept(Tokens.arrToken) == true))
		{
			TreeNode varDecl = this.varDecl(scopeName);
			if (varDecl == null)
			{
				return null;
			}
			varDeclSeqBlock.addStatement(varDecl);
		}
		
		return varDeclSeqBlock;
	}
	
	// formalParam = "(" [ident { "," ident }] ")"
	private TreeNode formalParam(String functionName)
	{
		if (this.accept(Tokens.openparenToken) == false)
		{
			this.Error("formalParam: syntax error, expect (");
			return null;
		}
		ParamNode retNode = new ParamNode(Token.getToken(Tokens.paramsToken));
		this.Next(); // eat openparenToken
		while (this.accept(Tokens.closeparenToken) == false)
		{
			if (this.accept(Tokens.ident) == true)
			{
				retNode.addParameter(this.scannerSym.clone());
				if (this.scope.containVariable(functionName, this.scannerSym.getCharacters()) == false)
				{
					this.scope.addParameter(functionName, this.scannerSym.getCharacters(),
							MemoryAllocator.Allocate(Variable.getUnitSize()));
				}
				else
				{
					this.Error("formalParam: duplicate declaration of variable/array " +
							this.scannerSym.getCharacters() + " in scope " + functionName);
					return null;
				}
				this.Next(); // eat the ident
				
				if (this.accept(Tokens.commaToken) == true)
				{
					this.Next(); // eat the comma
				}
			}
			else
			{
				this.Error("formalParam: syntax error, expect , or identifier"); // note that we don't expect , initially 
				return null;
			}
		}
		this.Next(); // eat the closeparen
		return retNode;
	}
	
	// funcBody = { varDecl } "{" [ statSequence ] "}"
	private TreeNode[] funcBody(String functionName)
	{
		BlockNode retNode = null;
		BlockNode lastBlock = null;
		if ((this.accept(Tokens.varToken) == true) ||
			(this.accept(Tokens.arrToken) == true))
		{
			TreeNode varDeclSeq = this.varDeclSequence(functionName);
			if (varDeclSeq == null)
			{
				return null;
			}
			
			retNode = lastBlock = ((BlockNode)(varDeclSeq));
		}
		if (this.accept(Tokens.beginToken) == true)
		{
			this.Next(); // eat {
			TreeNode[] statSeqNodes = this.statSequence(functionName);
			if (statSeqNodes == null)
			{
				return null;
			}
			
			if (statSeqNodes != null)
			{
				// QUESTION: why we are checking return node? did not really understand rest
				if (retNode == null)
				{
					retNode = ((BlockNode)(statSeqNodes[0]));
				}
				else
				{
					retNode.setLeftChild(statSeqNodes[0]);
				}
				
				lastBlock = ((BlockNode)(statSeqNodes[1]));
			}
			if (this.accept(Tokens.endToken) == true)
			{
				this.Next(); // eat }
			}
			else
			{
				this.Error("funcBody: syntax error, expect {");
				return null;
			}
		}
		else
		{
			this.Error("funcBody: syntax error, expect {");
			return null;
		}
		
		if (retNode == null)
		{
			return null;
		}
		else
		{
			return new TreeNode[] { retNode, lastBlock };
		}
	}
	
	// funcDecl = ("function" | "procedure") ident [formalParam] ";" funcBody ";"
	private TreeNode[] funcDecl()
	{
		FuncProcNode retNode = null;
		TreeNode lastBlock = null;
		
		if ((this.accept(Tokens.funcToken) == false) &&
			(this.accept(Tokens.procToken) == false))
		{
			this.Error("funcDecl: syntax error, expect function or procedure");
			return null;			// return new TreeNode[] { retNode, lastBlock };
		}

		Token type = this.scannerSym.clone();
		this.Next();
		if (this.accept(Tokens.ident) == false)
		{
			this.Error("funcDecl: syntax error, expect identifier as a function name");
			return null;			// return new TreeNode[] { retNode, lastBlock };
		}
		retNode = new FuncProcNode(type, this.scannerSym.clone());
		String functionName = this.scannerSym.getCharacters();
		if (this.scope.containScope(functionName) == false)
		{
			this.scope.addScope(functionName);
		}
		else
		{
			this.Error("funcDecl: duplicate declaration of function/procedure " +
					this.scannerSym.getCharacters());
			return null;
		}
		this.Next(); // eats the function name
		
		if (this.accept(Tokens.openparenToken) == true)
		{
			TreeNode formalParam = this.formalParam(functionName);
			if (formalParam == null)
			{
				return null;
			}
			retNode.setRightChild(formalParam);
		}
		if (this.accept(Tokens.semiToken) == true)
		{
			this.Next();
			
			TreeNode[] body = this.funcBody(functionName);
			if (body != null)
			{
				retNode.setLeftChild(body[0]);
				lastBlock = ((BlockNode)(body[1]));
			}
			else
			{
				return null;
			}
			
			if (this.accept(Tokens.semiToken) == true)
			{
				this.Next();
			}
			else
			{
				this.Error("funcDecl: syntax error, expect ;");
				return null;
			}
			
			if (lastBlock == null)
			{
				lastBlock = retNode;
			}					
		}
		else
		{
			this.Error("funcDecl: syntax error, expect ;");
			return null;
		}
		return new TreeNode[] { retNode, lastBlock };
	}
	
	private TreeNode funcDeclSequence()
	{
		FunctionBlockNode retNode = new FunctionBlockNode(Token.getToken(Tokens.funcSeqToken));
		while (this.accept(Tokens.beginToken) == false)
		{
			TreeNode[] funcDecl = this.funcDecl();
			if (funcDecl == null)
			{
				return null;
			}
			((FuncProcNode)(funcDecl[0])).setLastBlock(((BlockNode)(funcDecl[1])));
			retNode.addFunction(((FuncProcNode)(funcDecl[0])));
		}
		
		return retNode;
	}
	
	// computation = "main" { varDecl } { funcDecl } "{" statSequence "}" "."
	public TreeNode[] computation()
	{
		MainNode mainNode = null;
		TreeNode lastBlock = null;
		if (this.accept(Tokens.mainToken) == false)
		{
			this.Error("computation: syntax error, expect main");
			return null;
		}
		
		mainNode = new MainNode(this.scannerSym.clone());
		this.scope.addScope("main");
		
		lastBlock = mainNode;
		this.Next();
		
		// if we have a variable declaration
		BlockNode varDeclSeqBlock = null;
		if ((this.accept(Tokens.varToken) == true) ||
			(this.accept(Tokens.arrToken) == true))
		{
			TreeNode varDeclSeq = this.varDeclSequence("main");
			if (varDeclSeq == null)
			{
				return null;
			}
			
			varDeclSeqBlock = ((BlockNode)(varDeclSeq));
			lastBlock.setLeftChild(varDeclSeqBlock);
			lastBlock = varDeclSeqBlock;
		}
		
		// if we have function declaration
		TreeNode funcDeclSeqBlock = null;
		if ((this.accept(Tokens.funcToken) == true) ||
			(this.accept(Tokens.procToken) == true))
		{
			funcDeclSeqBlock = this.funcDeclSequence();
			if (funcDeclSeqBlock == null)
			{
				return null;
			}
			lastBlock.setLeftChild(funcDeclSeqBlock);
			lastBlock = funcDeclSeqBlock;
		}
		
		
		// This is the last block in the program
		BlockNode periodNode = null;
		if (this.accept(Tokens.beginToken) == true)
		{
			this.Next();	// eat "{"
			TreeNode[] statSeqBlocks = this.statSequence("main");
			if (statSeqBlocks == null)
			{
				this.Error("computation: syntax error, expect statSequence or error in processing statSequence");
				return null;
			}
			else
			{
				lastBlock.setLeftChild(statSeqBlocks[0]);
				lastBlock = statSeqBlocks[1];
				
				if (this.accept(Tokens.endToken) == true)
				{
					this.Next();	// eat "}"
					
					if (this.accept(Tokens.periodToken) == true)
					{
						periodNode = new BlockNode(this.scannerSym.clone());
						this.Next();
						
						lastBlock.setLeftChild(periodNode);
					}
					else
					{
						this.Error("computation: syntax error, expect .");
						return null;
					}
				}
				else
				{
					this.Error("computation: syntax error, expect }");
					return null;
				}
			}
		}
		else
		{
			this.Error("computation: syntax error, expect {");
			return null;
		}
		
		mainNode.setScope(this.scope);
		mainNode.setFuncAssignedVarMap(this.funcAssignedVarMap);
		mainNode.setMultiScopeVariable(this.multiScopeVariable);
		return new TreeNode[]{ mainNode, periodNode };
	}
	
	private boolean accept(Tokens token)
	{
		if (this.scannerSym.compareType(token))
		{
			return true;
		}
		return false;
	}

	/**
	 * @return scannerSym
	 */
	public Token getScannerSym()
	{
		return this.scannerSym;
	}
	
	public ArrayList<String> getMultiScopeVariable()
	{
		return this.multiScopeVariable;
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}
