package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.zinutils.exceptions.WrappedException;
import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import org.zinutils.utils.FileUtils;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;

public class FileListCommand extends NoChildCommand implements ConfigApplyCommand {
	public String fileList;
	private File execdir;
	private File file;
	
	public FileListCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "fileList", "file list"));
	}

	@Override
	public void applyTo(Config config) {
		this.file = new File(execdir, fileList);
		execdir = FileUtils.getCurrentDir();
		LinePatternParser lpp = new LinePatternParser();
		lpp.matchAll("mvncache\\s+([a-zA-Z_0-9.-]*)", "mvncache", "path");
		lpp.matchAll("([a-zA-Z_0-9./-]*)\\s+([a-zA-Z_0-9./-]*)\\s*(0[0-7][0-7][0-7])?", "path", "to", "from", "mode");
		try (FileReader fp = new FileReader(file)) {
			int cnt = 0;
			List<LinePatternMatch> matches = lpp.applyTo(fp);
			for (LinePatternMatch lpm : matches) {
				if (lpm.is("path") && lpm.get("to").equals("mvncache"))
					continue;
				System.out.println(lpm);
				cnt++;
			}
			System.out.println("file " + fileList + " cnt = " + cnt);
		} catch (IOException e) {
			throw WrappedException.wrap(e);
		}
	}

	public void addToOFL(OrderedFileList ret) {
		ret.add(file);
	}

	@Override
	public String toString() {
		return "FileList[" + fileList + "]";
	}
}
