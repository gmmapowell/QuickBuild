package com.gmmapowell.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Logger;

public class CLObjectInputStream extends ObjectInputStream {
	private static Logger logger = Logger.getLogger("CLOIS");
	private final ClassLoader loader;

	public CLObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
		super(in);
		this.loader = loader;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
			ClassNotFoundException {
		logger.finer("Requesting class " + desc.getName());
		Class<?> ret = loader.loadClass(desc.getName());
		if (ret != null)
		{
			logger.fine("Returning " + ret);
			return ret;
		}
		return super.resolveClass(desc);
	}

}
