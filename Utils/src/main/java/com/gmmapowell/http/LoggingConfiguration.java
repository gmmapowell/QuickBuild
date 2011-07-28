package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggingConfiguration {
	
	public LoggingConfiguration() throws SecurityException, IOException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream("/logging.properties");
		if (resourceAsStream == null)
			resourceAsStream = new ByteArrayInputStream("handlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter = com.gmmapowell.http.HttpFormatter\n".getBytes());
		LogManager.getLogManager().readConfiguration(resourceAsStream);
	}
}
