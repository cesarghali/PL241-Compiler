package com.pl241.frontend;

/**
 * 
 * @author ekinoguz
 *
 * Parser exception class for syntax errors.
 */

public class ParserException extends Exception{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param msg message for the exception
	 */
	public ParserException(String msg)
	{
		super("Parser exception: " + msg);
	}
	
	/**
	 * 
	 * @param methodName method name where expcetion occured
	 * @param expecting expecting Token
	 */
	public ParserException(String methodName, Token expecting)
	{
		super("Parser exception at " + methodName + ": " + "syntax error, expecting " + expecting.getCharacters());
	}
}
