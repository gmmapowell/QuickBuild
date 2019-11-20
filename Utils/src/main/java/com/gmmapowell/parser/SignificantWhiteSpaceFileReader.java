package com.gmmapowell.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.zinutils.exceptions.UtilException;

public class SignificantWhiteSpaceFileReader {
	private final LineCounter lnr;
	private TokenizedLine nextLine;
	private final CommandObjectFactory rootFactory;
	private final Parent<?> top;
	private String prompt;
	private final boolean interactive;
	private boolean tokenize;

	private SignificantWhiteSpaceFileReader(CommandObjectFactory factory, Reader r) {
		this.rootFactory = factory;
		this.top = null;
		lnr = new StreamLineCounter(r);
		this.interactive = false;
	}
	
	public <U, T extends Parent<U>> SignificantWhiteSpaceFileReader(CommandObjectFactory factory, T parent, InputStream in) {
		this(factory, parent, new StreamLineCounter(in));
	}

	private <U, T extends Parent<U>> SignificantWhiteSpaceFileReader(CommandObjectFactory factory, T parent, LineCounter lnr) {
		this.rootFactory = factory;
		this.top = parent;
		this.lnr = lnr;
		this.interactive = true;
	}

	public void useTokenizer(boolean b) {
		this.tokenize = b;
	}
	
	private void dispose()
	{
		if (lnr != null)
			lnr.close();
	}
	
	private <T> void readBlock(CommandObjectFactory factory, Parent<T> parent, int ind) throws Exception {
		TokenizedLine s;
		T curr = null;
		while ((s = nextLine()) != null)
		{
//			System.out.println("Read line " + s.lineNo() + " ind = " + s.indent());
			if (s.blank())
			{
				accept();
				if (interactive)
					return;
				continue;
			}
			if (s.indent < ind)
				return;
			if (s.indent > ind)
			{
				if (curr == null)
					throw new UtilException("I think actually this can only happen if the initial indent is non-zero");
				readBlock((curr instanceof CommandObjectFactory)?((CommandObjectFactory)curr):factory, (Parent<?>) curr, s.indent);
				if (nextLine() != null && nextLine().indent > ind)
					factory.handleError(s, new IndentationException());
			}
			else
			{
				curr = processCurrentLine(factory, parent, s);
			}
		}
	}

	protected <T> T processCurrentLine(CommandObjectFactory factory, Parent<T> ret, TokenizedLine s) {
		@SuppressWarnings("unchecked")
		T tmp = (T) factory.create(s.cmd(), s);
		if (tmp == null)
			throw new UtilException("No object was created for '" + s.cmd() + "' on line " + lnr.getLineNumber());
		else if (!(tmp instanceof IgnoreLine))
			ret.addChild(tmp);
		accept();
		return tmp;
	}

	private TokenizedLine nextLine() throws IOException {
		if (nextLine != null)
			return nextLine;
		
		String s;
		prompt();
		while ((s = lnr.readLine()) != null)
		{
			nextLine = new TokenizedLine(lnr.getLineNumber(), s, tokenize);
			return nextLine;
		}
		nextLine = null;
		return null;
	}

	private void prompt() {
		if (prompt != null)
		{
			System.out.print(prompt);
			System.out.flush();
		}
	}

	private void accept() {
		nextLine = null;
	}

	public static <U, T extends Parent<U>> SignificantWhiteSpaceFileReader interactive(T parent, CommandObjectFactory factory) {
		return new SignificantWhiteSpaceFileReader(factory, parent, System.in);
	}

	public static <U, T extends Parent<U>> SignificantWhiteSpaceFileReader push(T parent, CommandObjectFactory factory, PushLineCounter commsLink) {
		return new SignificantWhiteSpaceFileReader(factory, parent, commsLink);
	}
	
	public static <U, T extends Parent<U>> void read(T parent, CommandObjectFactory factory, File f) {
		read(parent, factory, f, true);
	}

	public static <U, T extends Parent<U>> void read(T parent, CommandObjectFactory factory, File f, boolean tokenize) {
		if (!f.exists())
			throw new UtilException("The file '" + f.getPath() + "' does not exist");
		try {
			read(parent, factory, new InputStreamReader(new FileInputStream(f)), tokenize);
		} catch (FileNotFoundException e) {
			throw UtilException.wrap(e);
		}
	}

	public static <U, T extends Parent<U>> void read(T parent, CommandObjectFactory factory, Reader r, boolean tokenize) {
		
		SignificantWhiteSpaceFileReader fr = null;
		try {
			fr = new SignificantWhiteSpaceFileReader(factory, r);
			fr.tokenize = tokenize;
			fr.readBlock(factory, parent, 0);
			if (fr.nextLine() != null)
				fr.inconsistentIndentation(0);
		} catch (Exception e) {
			throw UtilException.wrap(e);
		}
		finally
		{
			if (fr != null)
				fr.dispose();
		}
	}

	private void inconsistentIndentation(int i) {
		throw new UtilException("Inconsistent indentation at line " + lnr.getLineNumber() + ": found " + nextLine.indent + " when expecting " + i);
	}

	public SignificantWhiteSpaceFileReader setPrompt(String string) {
		prompt = string;
		return this;
	}

	public boolean nextBlockCommand() {
		try {
			TokenizedLine line = nextLine();
			if (line == null)
				return false;
			if (line.blank())
			{
				accept();
				return true;
			}
			if (line.tokens != null && line.tokens[line.tokens.length-1].equals("\\"))
				readBlock(rootFactory, top, 0);
			else
				processCurrentLine(rootFactory, top, line);
			return true;
		}
		catch (Exception ex)
		{
			accept();
			throw UtilException.wrap(ex);
		}
	}

}
