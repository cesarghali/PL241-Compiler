//package com.pl241.ir;
//
//import com.pl241.frontend.Parser;
//import com.pl241.frontend.Tokens;
//
///**
// * 
// * @author ekinoguz
// *
// * Test the intermediate representation of "assignment"
// */
//public class AssignmentTest {
//
//	public static void main(String[] args)
//	{
//		Parser par = Parser.getInstance("codes/code_assignment");
//		if (par != null)
//		{
//			while (par.getScannerSym().compareType(Tokens.eofToken) == false)
//			{
//				TreeNode cur = par.assignment();
//				if (cur != null)
//				{
//					cur.preorderTraverse();
//				}
//				else
//				{
//					System.out.println("error in parsing <assignment>");
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
