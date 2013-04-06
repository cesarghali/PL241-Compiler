package com.pl241.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * @author ekinoguz
 *
 * General Utils functions:
 * 	saveFile
 *	appendFile
 */
public class Utils {
	
	/**
	 * 
	 * @param filePath
	 * @param data
	 * @return
	 */
	public static boolean saveFile(String filePath, String data){
		return writeToFile(filePath, data, false);
	}
	
	/**
	 * 
	 * @param filePath
	 * @param data
	 * @return
	 */
	public static boolean appendToFile(String filePath, String data){
		return writeToFile(filePath, data, true);
	}
	
	/**
	 * 
	 * @param filePath
	 * @param data
	 * @param append
	 * @return
	 */
	private static boolean writeToFile(String filePath, String data, boolean append){
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filePath, append));
			writer.write(data);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
