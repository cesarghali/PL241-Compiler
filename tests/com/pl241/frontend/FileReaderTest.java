package com.pl241.frontend;

/*
 * @author: ekinoguz
 */
public class FileReaderTest {

	
	public static void main(String[] args) {
		String fileName = "codes/code_01";
		
		FileReader fileReader = FileReader.getInstance(fileName);
		
		char c;
		String file = "";
		while (( c = fileReader.getSym()) != 0xff) { 
			file += c;
		}
		System.out.println(file);
	}
}
