package com.gmmapowell.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.parser.LinePatternMatch;
import com.gmmapowell.parser.LinePatternParser;
import com.gmmapowell.parser.LinePatternParser.MatchIterator;

public class ThreadedStreamReader extends Thread {
	public class LPPMatcher {

		private final LinePatternParser lpp;
		private final MatchIterator iterator;

		public LPPMatcher(LinePatternParser lpp, MatchIterator iterator) {
			this.lpp = lpp;
			this.iterator = iterator;
		}

	}

	private final OutputStream writeTo;
	private final List<LPPMatcher> matchers = new ArrayList<LPPMatcher>();
	private InputStream readFrom;
	private boolean doEcho;
	private FileOutputStream copyTo;
	private StringBuilder buffer = new StringBuilder();

	public ThreadedStreamReader() {
		this.writeTo = null;
	}

	public ThreadedStreamReader(OutputStream out) {
		this.writeTo = out;
	}

	@Override
	public void run() {
		try
		{
			byte[] bs = new byte[400];
			int cnt = 0;
			while ((cnt = readFrom.read(bs, 0, 400)) > 0) {
				if (writeTo != null) {
					writeTo.write(bs, 0, cnt);
					writeTo.flush();
				}
				if (doEcho) {
					System.out.write(bs, 0, cnt);
					System.out.flush();
				}
				if (copyTo != null) {
					copyTo.write(bs, 0, cnt);
					copyTo.flush();
				}
				for (String s : completeLines(bs, cnt)) {
					for (LPPMatcher lpm : matchers) {
						List<LinePatternMatch> ret = new ArrayList<LinePatternMatch>();
						lpm.lpp.applyTo(ret, s);
						for (LinePatternMatch m : ret)
							lpm.iterator.handleMatch(m);
					}
				}
			}
			readFrom.close();
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	private List<String> completeLines(byte[] bs, int cnt) {
		List<String> ret = new ArrayList<String>();
		for (int i=0;i<cnt;i++) {
			char c = (char)bs[i];
			if (c == '\n') {
				ret.add(buffer.toString());
				buffer.delete(0, buffer.length());
			} else if (c != '\r')
				buffer.append(c);
		}
		return ret;
	}

	public void echoStream(boolean doEcho) {
		this.doEcho = doEcho;
	}

	public void copyTo(File file) {
		try {
			this.copyTo = new FileOutputStream(file);
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}
	
	public void read(InputStream inputStream) {
		if (inputStream == null)
			return;
		readFrom = inputStream;
		start();
	}
	
	public void parseLines(LinePatternParser lpp, MatchIterator iterator) {
		matchers.add(new LPPMatcher(lpp, iterator));
	}
}
