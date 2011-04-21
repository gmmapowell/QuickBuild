package com.gmmapowell.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import com.gmmapowell.exceptions.UtilException;

public class SignificantWhiteSpaceFileReader {
	private LineNumberReader lnr;
	private TokenizedLine nextLine;
	private final CommandObjectFactory factory;

	private SignificantWhiteSpaceFileReader(CommandObjectFactory factory, File f) throws FileNotFoundException {
		this.factory = factory;
		lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(f)));
	}
	
	private void dispose()
	{
		try {
			if (lnr != null)
				lnr.close();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> void readBlock(Parent<T> ret, int ind) throws Exception {
		TokenizedLine s;
		T curr = null;
		while ((s = nextLine()) != null)
		{
//			System.out.println("Read line " + s.lineNo() + " ind = " + s.indent());
			if (s.blank())
				continue;
			if (s.indent < ind)
				return;
			if (s.indent > ind)
			{
				if (curr == null)
					throw new UtilException("I think actually this can only happen if the initial indent is non-zero");
				readBlock((Parent<?>) curr, s.indent);
			}
			else
			{
				T tmp = (T) createObject(s.cmd(), s);
				if (tmp == null)
					throw new UtilException("No object was created for '" + s.cmd() + "' on line " + lnr.getLineNumber());
				curr = tmp;
				ret.addChild(curr);
				accept();
			}
		}
	}

	// TODO: this requires thought and work
	private Parent<?> createObject(String cmd, TokenizedLine toks) {
		return factory.create(cmd, toks);
	}

	private TokenizedLine nextLine() throws IOException {
		if (nextLine != null)
			return nextLine;
		
		String s;
		while ((s = lnr.readLine()) != null)
		{
			nextLine = new TokenizedLine(lnr.getLineNumber(), s);
			if (nextLine.length() > 0)
				return nextLine;
		}
		return nextLine = null;
	}

	private void accept() {
		nextLine = null;
	}

	public static <U, T extends Parent<U>> void read(T parent, CommandObjectFactory factory, File f) {
		System.out.println("Reading configuration " + f);
		if (!f.exists())
			throw new UtilException("The file '" + f.getPath() + "' does not exist");
		
		SignificantWhiteSpaceFileReader fr = null;
		try {
			fr = new SignificantWhiteSpaceFileReader(factory, f);
			fr.readBlock(parent, 0);
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

}
