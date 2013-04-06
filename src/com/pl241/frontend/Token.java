package com.pl241.frontend;

public class Token
{
	// Type of the Token
	private Tokens type;
	// The characters sequence the token
	private String characters;
	// The index of the variable in SSA format
	private int ssaIndex;
	// The ID value of the token
	private int value;
	// Indicate if the token is a global variable. This is used when parsing SSA.
	// Usually the token is statToken and has a reference to a global variable
	private boolean isGlobalVariable;
	
	// Constructor of the Token class
	private Token(Tokens type, String characters, int value)
	{
		this.type = type;
		this.characters = characters;
		this.ssaIndex = 1;
		this.value = value;
		this.isGlobalVariable = false;
	}
	
	/**
	 *  Takes as input the characters sequence of the token and return an instance of
	 *  the Token class that matches the input characters sequence
	 * @param characters: is the characters sequence of the token 
	 * @return An instance of the Token class 
	 */
	public static Token getToken(String characters)
	{
		switch (characters)
		{
			case "*":
				return new Token(Tokens.timesToken, characters, 1);
				
			case "/":
				return new Token(Tokens.divToken, characters, 2);
			
			case "+":
				return new Token(Tokens.plusToken, characters, 11);
				
			case "-":
				return new Token(Tokens.minusToken, characters, 12);
			
			case "==":
				return new Token(Tokens.eqlToken, characters, 20);
			
			case "!=":
				return new Token(Tokens.neqToken, characters, 21);
			
			case "<":
				return new Token(Tokens.lssToken, characters, 22);
			
			case ">=":
				return new Token(Tokens.geqToken, characters, 23);
			
			case "<=":
				return new Token(Tokens.leqToken, characters, 24);
			
			case ">":
				return new Token(Tokens.gtrToken, characters, 25);
			
			case ".":
				return new Token(Tokens.periodToken, characters, 30);
			
			case ",":
				return new Token(Tokens.commaToken, characters, 31);
			
			case "[":
				return new Token(Tokens.openbracketToken, characters, 32);
			
			case "]":
				return new Token(Tokens.closebracketToken, characters, 34);
			
			case ")":
				return new Token(Tokens.closeparenToken, characters, 35);
			
			case "<-":
				return new Token(Tokens.becomesToken, characters, 40);
			
			case "then":
				return new Token(Tokens.thenToken, characters, 41);
			
			case "do":
				return new Token(Tokens.doToken, characters, 42);
			
			case "(":
				return new Token(Tokens.openparenToken, characters, 50);
			
			case ";":
				return new Token(Tokens.semiToken, characters, 70);
			
			case "}":
				return new Token(Tokens.endToken, characters, 80);
			
			case "od":
				return new Token(Tokens.odToken, characters, 81);
			
			case "fi":
				return new Token(Tokens.fiToken, characters, 82);
			
			case "else":
				return new Token(Tokens.elseToken, characters, 90);
			
			case "let":
				return new Token(Tokens.letToken, characters, 100);
			
			case "call":
				return new Token(Tokens.callToken, characters, 101);
			
			case "if":
				return new Token(Tokens.ifToken, characters, 102);
			
			case "while":
				return new Token(Tokens.whileToken, characters, 103);
			
			case "return":
				return new Token(Tokens.returnToken, characters, 104);
			
			case "var":
				return new Token(Tokens.varToken, characters, 110);
			
			case "array":
				return new Token(Tokens.arrToken, characters, 111);
			
			case "function":
				return new Token(Tokens.funcToken, characters, 112);
			
			case "procedure":
				return new Token(Tokens.procToken, characters, 113);
			
			case "{":
				return new Token(Tokens.beginToken, characters, 150);
			
			case "main":
				return new Token(Tokens.mainToken, characters, 200);
			
			case "\0":
				return new Token(Tokens.eofToken, characters, 255);
				
			case "#":
				return new Token(Tokens.commentToken, characters, 260);
				
			case "statSeq":
				return new Token(Tokens.statSeqToken, characters, 300);
				
			case "varDeclSeq":
				return new Token(Tokens.varDeclSeqToken, characters, 301);
				
			case "params":
				return new Token(Tokens.paramsToken, characters, 302);
				
			case "funcSeq":
				return new Token(Tokens.funcSeqToken, characters, 303);
				
			case "args":
				return new Token(Tokens.argsToken, characters, 304);
				
			case "tempSSA":
				return new Token(Tokens.tempSSAToken, characters, 305);
				
			case "stat":
				return new Token(Tokens.statToken, characters, 306);
				
			case "block":
				return new Token(Tokens.blockToken, characters, 307);
				
			case "predef":
				return new Token(Tokens.predefToken, characters, 308);
				
			case "addSAA":
				return new Token(Tokens.addSSAToken, characters, 400);
				
			case "subSAA":
				return new Token(Tokens.subSSAToken, characters, 401);
				
			case "mulSAA":
				return new Token(Tokens.mulSSAToken, characters, 402);
				
			case "divSAA":
				return new Token(Tokens.divSSAToken, characters, 403);
				
			case "cmpSSA":
				return new Token(Tokens.cmpSSAToken, characters, 404);
				
			case "loadSSA":
				return new Token(Tokens.loadSSAToken, characters, 405);
				
			case "movSAA":
				return new Token(Tokens.movSSAToken, characters, 406);
				
			case "phiSSA":
				return new Token(Tokens.phiSSAToken, characters, 407);
				
			case "braSSA":
				return new Token(Tokens.braSSAToken, characters, 408);
				
			case "bneSSA":
				return new Token(Tokens.bneSSAToken, characters, 409);
				
			case "beqSSA":
				return new Token(Tokens.beqSSAToken, characters, 410);
				
			case "bleSSA":
				return new Token(Tokens.bleSSAToken, characters, 411);
				
			case "bgtSSA":
				return new Token(Tokens.bgtSSAToken, characters, 412);
				
			case "bgeSSA":
				return new Token(Tokens.bgeSSAToken, characters, 413);
								
			case "bltSSA":
				return new Token(Tokens.bltSSAToken, characters, 414);
				
			case "addaSSA":
				return new Token(Tokens.addaSSAToken, characters, 415);
				
			case "FPSSA":
				return new Token(Tokens.fpSSAToken, characters, 416);
				
			case "storeSSA":
				return new Token(Tokens.storeSSAToken, characters, 417);
				
			case "end":
				return new Token(Tokens.endofProgToken, characters, 500);
				
			default:
				Token retToken;
				// The characters sequence could be a number
				if (characters.matches("[0-9]+") == true)		//([\\+-]?\\d+)([eE][\\+-]?\\d+)?  for more complex integers
				{
					retToken = new Token(Tokens.number, characters, 60);
				}
				// The character sequence could be an identifier
				else if (characters.matches("([a-zA-Z])([a-zA-Z0-9])*") == true)
				{
					retToken = new Token(Tokens.ident, characters, 61);
				}
				// Otherwise return the errorToken
				else
				{
					retToken = new Token(Tokens.errorToken, characters, 0);
				}
				return retToken;
		}
	}
	
	/**
	 * Takes as input the type of the token and return an instance of
	 * the Token class that matches the input type
	 * @param type: is the type of the token 
	 * @return An instance of the Token class 
	 */
	public static Token getToken(Tokens type)
	{
		switch (type)
		{
			case timesToken:
				return new Token(type, "*", 1);
				
			case divToken:
				return new Token(type, "/", 2);
			
			case plusToken:
				return new Token(type, "+", 11);
				
			case minusToken:
				return new Token(type, "-", 12);
			
			case eqlToken:
				return new Token(type, "==", 20);
			
			case neqToken:
				return new Token(type, "!=", 21);
			
			case lssToken:
				return new Token(type, "<", 22);
			
			case geqToken:
				return new Token(type, ">=", 23);
			
			case leqToken:
				return new Token(type, "<=", 24);
			
			case gtrToken:
				return new Token(type, ">", 25);
			
			case periodToken:
				return new Token(type, ".", 30);
			
			case commaToken:
				return new Token(type, ",", 31);
			
			case openbracketToken:
				return new Token(type, "[", 32);
			
			case closebracketToken:
				return new Token(type, "]", 34);
			
			case closeparenToken:
				return new Token(type, ")", 35);
			
			case becomesToken:
				return new Token(type, "<-", 40);
			
			case thenToken:
				return new Token(type, "then", 41);
			
			case doToken:
				return new Token(type, "do", 42);
			
			case openparenToken:
				return new Token(type, "(", 50);
				
			case ident:
				return new Token(type, "", 60);
				
			case number:
				return new Token(type, "", 61);
			
			case semiToken:
				return new Token(type, ";", 70);
			
			case endToken:
				return new Token(type, "}", 80);
			
			case odToken:
				return new Token(type, "od", 81);
			
			case fiToken:
				return new Token(type, "fi", 82);
			
			case elseToken:
				return new Token(type, "else", 90);
			
			case letToken:
				return new Token(type, "let", 100);
			
			case callToken:
				return new Token(type, "call", 101);
			
			case ifToken:
				return new Token(type, "if", 102);
			
			case whileToken:
				return new Token(type, "while", 103);
			
			case returnToken:
				return new Token(type, "return", 104);
			
			case varToken:
				return new Token(type, "var", 110);
			
			case arrToken:
				return new Token(type, "array", 111);
			
			case funcToken:
				return new Token(type, "function", 112);
			
			case procToken:
				return new Token(type, "procedure", 113);
			
			case beginToken:
				return new Token(type, "{", 150);
			
			case mainToken:
				return new Token(type, "main", 200);
			
			case eofToken:
				return new Token(type, "\0", 255);
				
			case commentToken:
				return new Token(type, "#", 260);
				
			case statSeqToken:
				return new Token(type, "statSeq", 300);
				
			case varDeclSeqToken:
				return new Token(type, "varDeclSeq", 301);
				
			case paramsToken:
				return new Token(type, "params", 302);
				
			case funcSeqToken:
				return new Token(type, "funcSeq", 303);
				
			case argsToken:
				return new Token(type, "args", 304);
				
			case tempSSAToken:
				return new Token(type, "tempSSA", 305);
				
			case statToken:
				return new Token(type, "stat", 306);
				
			case blockToken:
				return new Token(type, "block", 307);
				
			case predefToken:
				return new Token(type, "predef", 308);
				
			case addSSAToken:
				return new Token(type, "addSSA", 400);
				
			case subSSAToken:
				return new Token(type, "subSSA", 401);
				
			case mulSSAToken:
				return new Token(type, "mulSSA", 402);
				
			case divSSAToken:
				return new Token(type, "divSSA", 403);
				
			case cmpSSAToken:
				return new Token(type, "cmpSSA", 404);
				
			case loadSSAToken:
				return new Token(type, "loadSSA", 405);
				
			case movSSAToken:
				return new Token(type, "movSSA", 406);
				
			case phiSSAToken:
				return new Token(type, "phiSSA", 407);
				
			case braSSAToken:
				return new Token(type, "braSSA", 408);
				
			case bneSSAToken:
				return new Token(type, "bneSSA", 409);
				
			case beqSSAToken:
				return new Token(type, "beqSSA", 410);
				
			case bleSSAToken:
				return new Token(type, "bleSSA", 411);
				
			case bgtSSAToken:
				return new Token(type, "bgtSSA", 412);

			case bgeSSAToken:
				return new Token(type, "bgeSSA", 413);				
				
			case bltSSAToken:
				return new Token(type, "bltSSA", 414);
				
			case addaSSAToken:
				return new Token(type, "addaSSA", 415);
				
			case fpSSAToken:
				return new Token(type, "FPSSA", 416);
				
			case storeSSAToken:
				return new Token(type, "storeSSA", 417);
				
			case endofProgToken:
				return new Token(type, "end", 500);
				
			default:
				return new Token(Tokens.errorToken, "", 0);
		}
	}
		
	/**
	 * @return: Returns the type value of token
	 */
	public Tokens getType()
	{
		return this.type;
	}
	
	/**
	 * @return: Returns the characters sequence like ==
	 */
	public String getCharacters()
	{
		return this.characters;
	}
	
	public void setCharacters(String characters)
	{
		this.characters = characters;
	}
	
	/**
	 * @return: Returns the index of the variable in SSA format
	 */
	public int getSSAIndex()
	{
		return this.ssaIndex;
	}
	
	public void setSSAIndex(int ssaIndex)
	{
		this.ssaIndex = ssaIndex;
	}
	
	/**
	 * @return: Returns the id value of token like 20
	 */
	public int getValue()
	{
		return this.value;
	}
	
	/**
	 * @return: Returns the constant name of the current Tokens instance like eqlToken
	 */
	public String getName()
	{
		return this.type.toString();
	}
	
	public boolean isGlobalVariable()
	{
		return this.isGlobalVariable;
	}
	
	public void setIsGlobalVariable(boolean isGlobalVariable)
	{
		this.isGlobalVariable = isGlobalVariable;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Token clone()
	{
		Token clonedToken = new Token(this.type, this.characters, this.value);
		clonedToken.setSSAIndex(this.ssaIndex);
		return clonedToken;
	}
	
	/**
	 * @return: Returns a comma separated string of the character sequence, the constant name,
	 * and the ID of the current Tokens instance 
	 */
	public String toString()
	{
		return this.characters + ", " + this.getName() + ", " + this.value; 
	}
	
	public boolean compareType(Tokens type)
	{
		return (this.type == type);
	}
	
	public boolean compareToken(Token token)
	{
		return this.compareType(token.getType()) && (this.characters == token.getCharacters());
	}
}
