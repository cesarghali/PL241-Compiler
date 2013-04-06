PL241-Compiler
==============

<<<<<<< HEAD
Course project of <a href="http://www.ics.uci.edu/~franz/Site/w12cs241.html" target="_new">CS241</a>, Advanced Compiler Construction, UC Irvine.  
This is a real compiler which compiles programs written in PL241 language to DLX machine code. We are doing:
* Constant folding
* Common subexpression elimination
* Copy propagation
* Calculation of live ranges and Register allocation
* Generation of DLX machine codes
* Graph-based representation of everything
* (Extra: Dead Code Elimination(DCE) which works for if statements. After DCE, constant folding is performed as needed. DCE might not detect all dead code in while loops.)

For more information about project descrpition and PL241 language, check <a href="https://github.com/ekinoguz/PL241-Compiler/blob/master/2012CS241Project.pdf" target="_new">2012CS241Project.pdf</a>.  
For more information about DLX, check <a href="https://github.com/ekinoguz/PL241-Compiler/blob/master/DLX.pdf" target="_new">DLX.pdf</a>

Requirements
------------
* Java version "1.7.0_11"
* Apache Ant(TM) version 1.8.2
* <a href="http://www.graphviz.org/Home.php" target="_new">Graphviz</a> 2.28.0 to visualize our compiler (most colorful part of the project!)

Compile & Run
-------------
There are two options to compile and run:  
1) From command line, go to src/ and type
> ant cli -Dcli="options"

where options are:

	-dce,--deadCodeElimination <1/0>        1 if Dead Code Elimination is enabled, 0 otherwise (default is disabled)
	-f,--compileFile <file>                 Compile the given file in codes/ directory
	-rn,--registerNumber <registerNumber>   Number of registers for machine code (default is 8)
example:
> ant cli -Dcli="-f ../codes/fibonacci"

help:
> ant cli -Dcli="-h"

2) From Eclipse, import the project and run tests/com/pl241/platform/MachineCodeTest with anything

Visualize
---------
After compilation, you can open compiler generated graphs by compiling graphviz outputs. Go to graphviz/ and type:
> ./dot.sh

Note that dot.sh file should have required permissions (chmod a+x dot.sh)  
To compile single .dot file, go to graphviz/

> dot -T[type] <input-dot-file> -o <output-file> 

where [type] is gif, pdf, ps, png. example: 
> dot -Tpng test.dot -o test.png

Packages
--------
* backend: conversion to Single Static Assigment (SSA) and Dead Code Elimination (DCE) (basically, all the optimizations)
* frontend: File reading, scanning, parsing, tokenizing
* ir: Intermediate representation of everything in our compiler
* platform: DLX implementation and machine code generation
* ra: Register allocation

Contact
-------
* <a href="https://github.com/cesarghali" target="_new">Cesar Ghali
* Ekin Oguz
=======
Course project of CS241 Advanced Compiler Construction, UC Irvine.
>>>>>>> 954319126e8ce328563324ca2e73b25a93e14688
