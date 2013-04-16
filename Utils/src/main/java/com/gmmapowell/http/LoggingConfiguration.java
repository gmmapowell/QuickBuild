package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LoggingConfiguration {
	
	private Logger logger;

	public LoggingConfiguration() throws SecurityException, IOException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream("/logging.properties");
		if (resourceAsStream == null)
		{
			System.out.println("Unable to find /logging.properties, setting properties programatically");
			resourceAsStream = new ByteArrayInputStream("handlers = org.slf4j.bridge.SLF4JBridgeHandler\n".getBytes());
		}
		LogManager.getLogManager().readConfiguration(resourceAsStream);
	}
}

