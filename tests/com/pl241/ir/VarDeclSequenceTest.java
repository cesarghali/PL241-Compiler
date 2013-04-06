//package com.pl241.ir;
//
//import com.pl241.frontend.Parser;
//import com.pl241.frontend.Tokens;
//
///**
// * 
// * @author cesarghali
// *
// * Test the intermediate representation of "varDecl"
// */
//public class VarDeclSequenceTest {
//
//	public static void main(String[] args)
//	{
//		Parser par = Parser.getInstance("codes/code_varDeclSequence");
//		if (par != null)
//		{
//			while (par.getScannerSym().compareType(Tokens.eofToken) == false)
//			{
//				TreeNode cur = par.varDeclSequence();
//				if (cur != null)
//				{
//					cur.preorderTraverse();
//				}
//				else
//				{
//					System.out.println("error in parsing <varDeclSequence>");
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
