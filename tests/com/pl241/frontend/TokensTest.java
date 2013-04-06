package com.pl241.frontend;

/*
 * @author: ekinoguz
 */
public class TokensTest {

	public static void main(String[] args) {

		
		System.out.println(Token.getToken("var").compareType(Tokens.varToken));
		System.out.println(Token.getToken("cesar").compareType(Tokens.ident));
		System.out.println(Token.getToken("=<").compareType(Tokens.errorToken));
		System.out.println(Token.getToken(">=").compareType(Tokens.geqToken));
		System.out.println(Token.getToken("12231osadaks").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("askdlasldal;2").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("askdlasldal21212").compareType(Tokens.ident));
		System.out.println(Token.getToken("fi").compareType(Tokens.fiToken));
		System.out.println(Token.getToken("").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("\0").compareType(Tokens.eofToken));
		System.out.println(Token.getToken("/*").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("**").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("{").compareType(Tokens.beginToken));
		System.out.println(Token.getToken("[[").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("[").compareType(Tokens.openbracketToken));
		System.out.println(Token.getToken("0").compareType(Tokens.number));
		System.out.println(Token.getToken("+0").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("0+").compareType(Tokens.errorToken));
		System.out.println(Token.getToken("<-").compareType(Tokens.becomesToken));
	}

}
