package com.pl241.frontend;

/*
 * @author: ekinoguz, cesarghali
 */
// This enum contains all possible tokens, their characters sequence and IDs
public enum Tokens {
	errorToken,			// ("", 0)
	
	timesToken,			// ("*", 1)
	divToken,			// ("/", 2)
	
	plusToken,			// ("+", 11)
	minusToken,			// ("-", 12)
	
	eqlToken,			// ("==", 20)
	neqToken,			// ("!=", 21)
	lssToken,			// ("<", 22)
	geqToken,			// (">=", 23)
	leqToken,			// ("<=", 24)
	gtrToken,			// (">", 25)
	
	periodToken,		// (".", 30)
	commaToken,			// (",", 31),
	openbracketToken,	// ("[", 32)
	closebracketToken,	// ("]", 34)
	closeparenToken,	// (")", 35)
	
	becomesToken,		// ("<-", 40)
	thenToken,			// ("then", 41)
	doToken,			// ("do", 42)
	
	openparenToken,		// ("(", 50)

	number,				// ("", 60)
	ident,				// ("", 61)

	semiToken,			// (";", 70)

	endToken,			// ("}", 80)
	odToken,			// ("od", 81)
	fiToken,			// ("fi", 82)
	
	elseToken,			// ("else", 90)
	
	letToken,			// ("let", 100)
	callToken,			// ("call", 101)
	ifToken,			// ("if", 102)
	whileToken,			// ("while", 103)
	returnToken,		// ("return", 104)
	
	varToken,			// ("var", 110)
	arrToken,			// ("array", 111)
	funcToken,			// ("function", 112)
	procToken,			// ("procedure", 113)
	
	beginToken,			// ("{", 150)
	mainToken,			// ("main", 200)
	eofToken,			// ("\0", 255)
	
	commentToken,		// ("#", 260)
	
	// Special Tokens
	statSeqToken,		// ("statSeq", 300)
	varDeclSeqToken,	// ("varDeclSeq", 301)
	paramsToken,		// ("params", 302)
	funcSeqToken,		// ("funcSeq", 303)
	argsToken,			// ("args", 304)
	tempSSAToken,			// ("tempSSA", 305)
	statToken,			// ("stat", 306)		The refID of such node is another statement (TreeNode)
	blockToken,			// ("block", 307)		The refID of such node is another BlockNode
	predefToken,		// ("predef", 308)
	
	// SSA nodes
	addSSAToken,		// ("addSSA", 400)
	subSSAToken,		// ("subSSA", 401)
	mulSSAToken,		// ("mulSSA", 402)
	divSSAToken,		// ("divSSA", 403)
	cmpSSAToken,		// ("cmpSSA", 404)
	loadSSAToken,		// ("loadSSA", 405)
	movSSAToken,		// ("movSSA", 406)
	phiSSAToken,		// ("phiSSA", 407)
	braSSAToken,		// ("braSSA", 408)
	
	bneSSAToken,		// ("bneSSA", 409)
	beqSSAToken,		// ("beqSSA", 410)
	
	bleSSAToken,		// ("bleSSA", 411)
	bgtSSAToken,		// ("bgtSSA", 412)
	
	bgeSSAToken,		// ("bgeSSA", 413)
	bltSSAToken,		// ("bltSSA", 414)
	
	addaSSAToken,		// ("addaSSA", 415)
	fpSSAToken,			// ("FPSSA", 416)
	storeSSAToken,		// ("storeSSA", 417)
	
	endofProgToken;		// ("end", 500)
	
}
