#!/bin/bash
# This is script to compile all the .dot files into .png pictures
if [ -f 01_comp.dot ]
then
	echo "Compiling 01_comp.dot"
	dot -Tpng 01_comp.dot -o 01_comp.png
else
	echo "Missing file 01_comp.dot"
fi

if [ -f 02_ssa.dot ]
then
	echo "Compiling 02_ssa.dot"
	dot -Tpng 02_ssa.dot -o 02_ssa.png
else
	echo "Missing file 02_ssa.dot"
fi

if [ -f 03_cse.dot ]
then
	echo "Compiling 03_cse.dot"
	dot -Tpng 03_cse.dot -o 03_cse.png
else
	echo "Missing file 03_cse.dot"
fi

if [ -f 04_cp.dot ]
then
	echo "Compiling 04_cp.dot"
	dot -Tpng 04_cp.dot -o 04_cp.png
else
	echo "Missing file 04_cp.dot"
fi

if [ -f 05_dce.dot ]
then
	echo "Compiling 05_dce.dot"
	dot -Tpng 05_dce.dot -o 05_dce.png
else
	echo "Missing file 05_dce.dot"
fi

if [ -f 06_elm.dot ]
then
    echo "Compiling 06_elm.dot"
        dot -Tpng 06_elm.dot -o 06_elm.png
else
        echo "Missing file 06_elm.dot"
fi

if [ -f 07_rig.dot ]
then
	echo "Compiling 07_rig.dot"
	dot -Tpng 07_rig.dot -o 07_rig.png
else
	echo "Missing file 07_rig.dot"
fi





