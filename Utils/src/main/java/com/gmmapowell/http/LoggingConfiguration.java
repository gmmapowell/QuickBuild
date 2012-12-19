package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingConfiguration {
	
	public LoggingConfiguration() throws SecurityException, IOException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream("/logging.properties");
		if (resourceAsStream == null)
			resourceAsStream = new ByteArrayInputStream("handlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter = com.gmmapowell.http.HttpFormatter\n".getBytes());
		LogManager.getLogManager().readConfiguration(resourceAsStream);
		for (String s : new String[] { "error", "severe", "info", "fine", "finer", "finest"}) {
			String v = System.getProperty("com.gmmapowell.logging."+ s);
			if (v != null) {
				for (String l : v.split(","))
					Logger.getLogger(l).setLevel(Level.parse(s.toUpperCase()));
			}
		}
	}
}
