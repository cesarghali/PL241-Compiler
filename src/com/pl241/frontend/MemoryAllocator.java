package com.pl241.frontend;

/**
 * 
 * This class is responsible of allocating memory address for variables
 * and arrays. All addresses are relative to the base address
 * 
 * @author cesarghali
 *
 */
public class MemoryAllocator
{
	private static int baseAddress = 0;
	
	public static int Allocate(int size)
	{
		int retAddr = baseAddress;
		baseAddress += size;
		return retAddr;
	}
	
	public static int getVirtualRegsBaseAddr()
	{
		return baseAddress;
	}
}
