//package com.pl241.ir;
//
//import com.pl241.frontend.Parser;
//import com.pl241.frontend.Tokens;
//
///**
// * 
// * @author ekinoguz
// *
// * Test the intermediate representation of "statSequence"
// */
//public class StatSequenceTest {
//
//	public static void main(String[] args)
//	{
//		Parser par = Parser.getInstance("codes/code_statSequence");
//		if (par != null)
//		{
//			while (par.getScannerSym().compareType(Tokens.eofToken) == false)
//			{
//				TreeNode[] cur = par.statSequence();
//				if (cur[0] != null)
//				{
//					cur[0].preorderTraverse();
//				}
//				else
//				{
//					System.out.println("error in parsing <statSequence>");
//				}
//				par.Next();
//				System.out.println("\n\n");
//				if (par.getScannerSym().compareType(Tokens.semiToken))
//				{
//					par.Next();
//				}
//			}			
//		}
//	}
//}
