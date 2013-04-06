package com.pl241.ir;

import com.pl241.frontend.Parser;
import com.pl241.frontend.Tokens;

/**
 * 
 * @author cesarghali
 *
 * Test the parser computation function
 */
public class ComputationTest {

	public static void main(String[] args)
	{
		Parser par = Parser.getInstance("codes/code_computation");
		if (par != null)
		{
			while (par.getScannerSym().compareType(Tokens.eofToken) == false)
			{
				TreeNode[] comp = par.computation();
				if (comp != null)
				{
					comp[0].preorderTraverse(1);
				}
				else
				{
					System.out.println("error in parsing <computation>");
				}
				par.Next();
				System.out.println("\n\n");
				if (par.getScannerSym().compareType(Tokens.semiToken))
				{
					par.Next();
				}
			}			
		}
	}
}
