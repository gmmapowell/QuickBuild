package com.gmmapowell.utils;

public interface PrettyPrinter {
	/** Number of spaces to indent each level
	 * 
	 * @param ind the number of spaces for each indent
	 */
	void indentLevel(int ind);
	
	/** Indent one level more */
	void indentMore();
	
	/** Indent one level less */
	void indentLess();
	
	/** Make sure that the cursor is at the start of a line, but don't insert duplicate newlines. */
	void requireNewline();
	
	/** Append an object's toString representation
	 * 
	 * @param o the object to convert and append
	 */
	void append(Object o);
	
	/** Break up the input into lines, then add each line back in again, indenting as you go.
	 * All lines will have the same initial indent, but if they are indented themselves, will vary.
	 *
	 * @param s the composite string to break up
	 */
	void appendIndented(String s);
}
