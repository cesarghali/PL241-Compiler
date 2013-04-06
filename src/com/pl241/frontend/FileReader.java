package com.pl241.frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/*
 * @author: cesarghali
 */
// This class is responsible of reading from the input code file
public class FileReader
{
	private static FileReader instance;
	
	private BufferedReader reader;
	
	/**
	 * @return: Returns an new instance of this class in case it is not already created
	 */
	public static FileReader getInstance(String fileName)
	{
		// If there is no created instance for this class creates one
		if (instance == null)
		{
			// Call the constructor
			instance = new FileReader(fileName);
			// If the reader object is null, then there is an error while trying
			// to open the input file. Therefore, returns null as an instance of
			// this class
			if (instance.reader == null)
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
	
	// Constructor
	private FileReader(String fileName)
	{
		// Open a pointer to the input file for reading
		java.io.FileReader fReader = null;
		try
		{
			fReader = new java.io.FileReader(fileName);
			this.reader = new BufferedReader(fReader);
		}
		// If the file cannot be found, assign null to the reader object and
		// call the Error function to handle with the error
		catch (FileNotFoundException e)
		{
			this.reader = null;
			this.Error("Input file cannot be found");
			return;
		}
	}
	
	/**
	 * @return: Reads and returns the current character and proceeds to the next one
	 */
	public char getSym()
	{
		// Assign the error character value to the returned character
		char retChar = 0x00;
		try
		{
			// Read one character form the file
			int iChar = this.reader.read();
			// -1 means the end of file
			if (iChar == -1)
			{
				// 0xff is the end of file symbol that will be returned
				retChar = 0xff;
			}
			else
			{
				// Cast the read character from int to char
				retChar = (char) iChar;
			}
		}
		catch (IOException ex)
		{
			this.Error(ex.getMessage());
		}
		return retChar;
	}
	
	/**
	 * @return: Handles errors
	 */
	private void Error(String errorMsg)
	{
		System.out.println(errorMsg);
	}
}
