package com.pl241.frontend;

/**
 * @author ekinoguz
 */
public class ScannerTest
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String fileName = "codes/code_03";
		Scanner scanner = Scanner.getInstance(fileName);
		
		Token current = scanner.getSym();
		while (current.compareType(Tokens.eofToken) == false)
		{
			System.out.println(current);
			current = scanner.getSym();
		}
		System.out.println(scanner.getLastSeenToken());
	}
}
