package com.pl241.frontend;

import java.util.regex.Pattern;


/*
 * @author: cesarghali
 */
// This class uses the FileReader class to read one character at a time,
// then it combines sequence of characters into valid tokens
public class Scanner
{
	private static Scanner instance;
	
	private FileReader fileReader;
	// inputSym contains the last read character from the FileReader
	private char inputSym;
	// Contains the last seen token
	private Token lastSeenToken;
	
	/**
	 * @return: Returns an new instance of this class in case it is not already created
	 */
	public static Scanner getInstance(String fileName)
	{
		if (instance == null)
		{
			instance = new Scanner(fileName);
			// If the fileReader object is null, then there is an error
			// while opening the file. Therefore return null
			if (instance.fileReader == null)
			{
				return null;
			}
			else
			{
				return instance;
			}
		}
		else
		{
			return instance;
		}
	}
	
	private Scanner(String fileName)
	{
		this.fileReader = FileReader.getInstance(fileName);
		// If the fileReader object is created successfully then read the first
		// character of the input file by calling the Next function
		if (this.fileReader != null)
		{
			this.Next();
		}
	}
	
	/**
	 * @return: Invoke FileReader.getSym() function to read a character. Then stores it in
	 * the inputSym variable
	 */
	private void Next()
	{
		this.inputSym = this.fileReader.getSym();
	}
	
	/**
	 * @return: Recognize the current token and returns it
	 */
	public Token getSym()
	{
		// If the last seen token is end of file, returns it and do not try to read
		// more tokens
		if (this.lastSeenToken != null)
		{
			if (this.lastSeenToken.compareType(Tokens.eofToken) == true)
			{
				return this.lastSeenToken;
			}
		}
		
		// Contains the token sequence characters
		String characters = "";
		// Contains a string version of the last read character (of inputSym)
		String sInputSym;
		// If 0 it means we are parsing the first character in a token
		//    1 it means that the token could be ==, !=, <, <=, >, >= or <-
		//    2 it means the token is a number
		//    3 otherwise, which means that the token could be a keyword or an identifier
		int continueCode = 0;
		
		// Keep reading characters until a token is recognized
		while (true)
		{
			// Convert the last seen character to string
			sInputSym = Character.toString(this.inputSym);
			
			// First letter of the token is being parsed
			if (continueCode == 0)
			{
				// Ignore comments
				if (sInputSym.compareTo("#") == 0)
				{
					while (this.inputSym != '\n')
					{
						this.Next();
					}
					this.Next();
					continue;
				}
				
				
				// Skip all spaces, tabs, new lines and carriage return
				if (sInputSym.matches("[ \t\r\n]") == true)
				{
					this.Next();
					continue;
				}
				// If the read character is end of file, then end of file token is recognize
				else if (this.inputSym == 0xff)
				{
					characters += "\0";		// \0 is the end of file characters in the Token class
					break;
				}
				// If the read character is one of the single character tokens then, recognize it
				else if (sInputSym.matches("[*/+-.,;" +
						Pattern.quote("[") + Pattern.quote("]") + Pattern.quote(")") +
						Pattern.quote("(") + Pattern.quote("}") + Pattern.quote("{") + "]") == true)
				{
					characters += sInputSym;
					this.Next();
					break;
				}
				// If the read character is =, !, <, or >, then the token could be ==, !=, <, <=, >, >= or <-
				else if (sInputSym.matches("[=!" + Pattern.quote("<") + Pattern.quote(">") + "]") == true)
				{
					characters += sInputSym;
					this.Next();
					
					continueCode = 1;
				}
				// If the read character is a digit, then the token is a number
				else if (sInputSym.matches("[0-9]") == true)
				{
					characters += sInputSym;
					this.Next();
					
					continueCode = 2;
				}
				// If the first character is a letter, then the token is either a keyword or an identifier
				else if (sInputSym.matches("[a-zA-Z]") == true)
				{
					characters += sInputSym;
					this.Next();
					
					continueCode = 3;
				}
			}
			// Means that the token could be ==, !=, <, <=, >, >= or <-
			else if (continueCode == 1)
			{
				// If the current read character is either = or -, then the token is either
				// ==, !=, <=, >= or <-, then add the current character to the characters sequence,
				// call next, and break the loop
				if (sInputSym.matches("[=-]") == true)
				{
					characters += sInputSym;
					this.Next();
				}
				// Breaking the loop here means that the token could be either < or >
				break;
			}
			// Means that the token is number, so keep reading digits until another character is read
			else if (continueCode == 2)
			{
				if (sInputSym.matches("[0-9]") == true)
				{
					characters += sInputSym;
					this.Next();
				}
				else
				{
					break;					
				}
			}
			// Means that the token is either a keyword or an identifier, so keep reading letters or
			// digits until another character is read 
			else if (continueCode == 3)
			{
				if (sInputSym.matches("[a-zA-Z0-9]") == true)
				{
					characters += sInputSym;
					this.Next();
				}
				else
				{
					break;					
				}
			}
		}
		
		// Call the getToken function to parse the token. This function will either returns the
		// corresponding token, or returns errorToken if no token is recognized
		this.lastSeenToken = Token.getToken(characters);
		if (this.lastSeenToken.compareType(Tokens.errorToken))
		{
			this.Error(characters + " is unrecognized token");
		}
		
		return this.lastSeenToken;
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
	
	/**
	 * @return Returns the last seen token
	 */
	public Token getLastSeenToken()
	{
		return this.lastSeenToken;
	}
}