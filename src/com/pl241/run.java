package com.pl241;

import com.pl241.core.CLI;

public class run {

	public static void main(String[] args) {
    	
    	if (args.length == 0) {
    		System.out.println("Arguments please");
        } else {
        	System.out.println("Starting CLI");
            try {
				CLI.main(args);
			} catch (Exception e) {
				System.out.println("Command Line Failure: " + e.toString());
			}
        }
	}

}
