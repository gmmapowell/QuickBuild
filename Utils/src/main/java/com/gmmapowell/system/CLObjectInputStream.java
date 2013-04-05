package com.gmmapowell.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

public class CLObjectInputStream extends ObjectInputStream {
	private static Logger logger = LoggerFactory.getLogger("CLOIS");
	private final ClassLoader loader;

	public CLObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
		super(in);
		this.loader = loader;
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException,
			ClassNotFoundException {
		logger.debug(MarkerFactory.getMarker("finer"), "Requesting class " + desc.getName());
		try
		{
			Class<?> ret = Class.forName(desc.getName(), false, loader);
			if (ret != null)
			{
				logger.debug("Returning " + ret);
				return ret;
			}
		}
		catch (ClassNotFoundException ex)
		{
		}
		return super.resolveClass(desc);
	}

}
