package com.gmmapowell.quickbuild.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.parser.LinePatternMatch;
import org.zinutils.parser.LinePatternParser;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.ZipUtils;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.quickbuild.build.bash.ScriptResource;
import com.gmmapowell.quickbuild.build.java.JarJarCommand;
import com.gmmapowell.quickbuild.core.BuildResource;
import com.gmmapowell.quickbuild.core.PendingResource;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;
import com.gmmapowell.utils.OrderedFileList;

public class FileListCommand extends NoChildCommand implements ConfigApplyCommand {
	public interface JarContentItem {
		public void addToJar(ZipArchiveOutputStream jos) throws IOException;
	}

	public class JarLibContentItem implements JarContentItem {
		private BuildResource r;
		private String as;
		private String mode;

		public JarLibContentItem(BuildResource r, String as, String mode) {
			this.r = r;
			this.as = as;
			this.mode = mode;
		}

		@Override
		public void addToJar(ZipArchiveOutputStream jos) throws IOException {
			System.out.println("add " + r.getPath() + " to jar as " + as + " with mode " + mode);
			if (mode != null) {
				ZipUtils.fileEntry(jos, as, r.getPath(), Integer.parseInt(mode, 8));
			} else {
				ZipUtils.fileEntry(jos, as, r.getPath());
			}
		}
	}

	public class MavenContentItem implements JarContentItem {
		private String name;
		private PendingResource resource;

		public MavenContentItem(String name, PendingResource r) {
			this.name = name;
			this.resource = r;
		}

		@Override
		public void addToJar(ZipArchiveOutputStream jos) throws IOException {
			String jf = "lib/" + name + ".jar";
			System.out.println("add " + resource.getPath() + " to jar as " + jf);
			ZipUtils.fileEntry(jos, jf, resource.getPath());
		}
	}

	public String fileList;
	private File execdir;
	private File file;
	private final List<PendingResource> resources = new ArrayList<>();
	private final List<JarContentItem> contents = new ArrayList<>();
	private JarJarCommand tactic;
	
	public FileListCommand(TokenizedLine toks)
	{
		toks.process(this, new ArgumentDefinition("*", Cardinality.REQUIRED, "fileList", "file list"));
	}

	@Override
	public void applyTo(Config config) {
		this.file = new File(execdir, fileList);
		execdir = FileUtils.getCurrentDir();
		LinePatternParser lpp = new LinePatternParser();
		lpp.matchAll("\\s*", "blank");
		lpp.matchAll("\\s*#.*", "comment");
		lpp.matchAll("\\s*//.*", "comment");
		lpp.matchAll("script\\s+([a-zA-Z_0-9./-]*)\\s+([a-zA-Z_0-9.:/-]+)\\s*(0[0-7][0-7][0-7])?", "script", "as", "file", "mode");
		lpp.matchAll("mvncache\\s+([a-zA-Z_0-9.-]*)\\s*([a-zA-Z_0-9.:-]+)?", "mvncache", "name", "match");
		lpp.matchAll("([a-zA-Z_0-9./-]*)\\s+([a-zA-Z_0-9./-]*)\\s*(0[0-7][0-7][0-7])?", "path", "to", "from", "mode");
		lpp.matchAll("(.*)", "unknown", "cmd");
		try (FileReader fp = new FileReader(file)) {
			int cnt = 0;
			List<LinePatternMatch> matches = lpp.applyTo(fp);
			int handledLine = 0;
			for (LinePatternMatch lpm : matches) {
				System.out.println(lpm.lineno() + " == " + lpm);

				if (lpm.is("blank") || lpm.is("comment")) {
					handledLine = lpm.lineno();
					continue;
				} else if (lpm.is("mvncache")) {
					PendingResource r;
					if (lpm.get("match") != null) {
						 r = new PendingResource(lpm.get("match"));
						resources.add(r);
					} else {
						r = new PendingResource(lpm.get("name"));
						resources.add(r);
					}
					contents.add(new MavenContentItem(lpm.get("name"), r));
					cnt++;
				} else if (lpm.is("path")) {
					if (lpm.lineno() <= handledLine)
						continue;
					else {
						PendingResource r = new PendingResource(lpm.get("from"));
						resources.add(r);
						contents.add(new JarLibContentItem(r, lpm.get("to"), lpm.get("mode")));
					}
					cnt++;
				} else if (lpm.is("script")) {
					ScriptResource r = new ScriptResource(tactic, new File(lpm.get("file")));
					config.resourceAvailable(r);
					resources.add(new PendingResource(r.compareAs()));
					contents.add(new JarLibContentItem(r, lpm.get("as"), lpm.get("mode")));
					cnt++;
				} else if (lpm.is("unknown") && lpm.lineno() > handledLine) {
					throw new CantHappenException("unknown file list command: " + lpm.get("cmd"));
				}
				handledLine = lpm.lineno();
			}
			System.out.println("file " + fileList + " cnt = " + cnt);
		} catch (IOException e) {
			throw WrappedException.wrap(e);
		}
	}
	
	public void addEntriesToJar(ZipArchiveOutputStream jos) throws IOException {
		for (JarContentItem e : contents) {
			e.addToJar(jos);
		}
	}
	
	public void tactic(JarJarCommand tactic) {
		this.tactic = tactic;
	}

	public List<PendingResource> pendingResources() {
		return resources;
	}
	
	public void addToOFL(OrderedFileList ret) {
		ret.add(file);
	}

	@Override
	public String toString() {
		return "FileList[" + fileList + "]";
	}
}
