package com.gmmapowell.bytecode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.utils.FileUtils;
import com.gmmapowell.utils.GPJarEntry;
import com.gmmapowell.utils.GPJarFile;


public class JavaRuntimeReplica {
	public abstract class JRRChecker {
		public abstract ByteCodeFile read(String clz, File clzpath);

		public abstract void close();
	}

	public class JRRFSChecker extends JRRChecker {
		private final File root;

		public JRRFSChecker(File root) {
			this.root = root;
		}

		@Override
		public ByteCodeFile read(String clz, File clzpath) {
			File from = FileUtils.combine(root, clzpath);
			if (from.canRead())
				return new ByteCodeFile(from, clz);
			else
				return null;
		}

		@Override
		public void close() {
		}
	}

	public class JRRJarChecker extends JRRChecker {
		private final GPJarFile jar;

		public JRRJarChecker(File path) {
			jar = new GPJarFile(path);
		}

		@Override
		public ByteCodeFile read(String clz, File clzpath) {
			GPJarEntry e = jar.get(clzpath.getPath());
			if (e != null)
				return new ByteCodeFile(e.asStream());
			return null;
		}

		@Override
		public void close() {
			jar.close();
		}
	}

	private final List<JRRChecker> roots = new ArrayList<JRRChecker>();
	private final Map<String, ByteCodeFile> cache = new HashMap<String, ByteCodeFile>();
	
	public void add(File path) {
		if (roots.contains(path))
			return;
		if (path.getName().endsWith(".jar") || path.getName().endsWith(".zip"))
			roots.add(new JRRJarChecker(path));
		else
			roots.add(new JRRFSChecker(path));
	}
	
	public ByteCodeFile getClass(String clz)
	{
		if (cache.containsKey(clz))
			return cache.get(clz);
		
		File clzpath = FileUtils.convertDottedToPathWithExtension(clz, ".class");
		for (JRRChecker checker : roots)
		{
			ByteCodeFile ret = checker.read(clz, clzpath);
			if (ret != null)
			{
				cache.put(clz, ret);
				return ret;
			}
		}
		throw new UtilException("JRR cannot find any version of " + clz);
	}
	
	public void close()
	{
		for (JRRChecker checker : roots)
			checker.close();
	}

}
