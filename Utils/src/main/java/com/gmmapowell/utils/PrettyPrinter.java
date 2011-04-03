package com.gmmapowell.utils;

public class PrettyPrinter {
	private StringBuilder sb = new StringBuilder();
	private int indWidth = 4;
	private int levels = 0;
	
	/** Number of spaces to indent each level.  The default is 4.
	 * 
	 * @param ind the number of spaces for each indent
	 */
	public void indentWidth(int ind)
	{
		indWidth = ind;
	}
	
	/** Indent one level more */
	public void indentMore()
	{
		levels++;
	}
	
	/** Indent one level less */
	public void indentLess()
	{
		levels--;
	}
	
	public int getIndent() {
		return levels;
	}

	/** Test if the cursor is currently at the start of a line */
	public boolean atLineStart() {
		return sb.length() == 0 || sb.charAt(sb.length()-1) == '\n';
	}
	
	/** Make sure that the cursor is at the start of a line, but don't insert duplicate newlines. */
	public void requireNewline()
	{
		if (atLineStart())
			return;
		sb.append('\n');
	}

	/** Append an object's toString representation
	 * 
	 * @param o the object to convert and append
	 */
	public void append(Object o)
	{
		if (atLineStart())
			for (int i=0;i<levels*indWidth;i++)
				sb.append(" ");
		sb.append(o);
	}
	
	/** Break up the input into lines, then add each line back in again, indenting as you go.
	 * All lines will have the same initial indent, but if they are indented themselves, will vary.
	 *
	 * @param s the composite string to break up
	 */
	public void appendIndented(String text)
	{
		int from = 0;
		while (from < text.length())
		{
			int idx = text.indexOf('\n', from);
			if (idx == -1)
				idx = text.length();

			requireNewline();
			append(text.substring(from, idx));
			from = idx+1;
		}
		requireNewline();
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}

	public void hollerith(String text, int len) {
		hollerith(text, len, Justification.LEFT);
	}

	public void hollerith(String text, int len, Justification j) {
		String ins = j.format(text, len);
		append(ins);
	}

	public void pad(int len) {
		append(Justification.pad(len));
	}
}
