package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

public class LoggingConfiguration {
	
	public LoggingConfiguration() throws SecurityException, IOException
	{
		System.out.println("Configuring java.util.logging");
		InputStream resourceAsStream = this.getClass().getResourceAsStream("/logging.properties");
		if (resourceAsStream == null)
		{
			System.out.println("Unable to find /logging.properties, setting properties programatically");
			resourceAsStream = new ByteArrayInputStream("handlers = org.slf4j.bridge.SLF4JBridgeHandler\n".getBytes());
		}
		LogManager.getLogManager().readConfiguration(resourceAsStream);
		
		
		//commenting out some debugging/out of date code
//		Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
//		
//		System.out.println("Loggers loaded:");
//		for(; loggerNames.hasMoreElements(); ) 
//		{ 
//			String loggerName = loggerNames.nextElement();
//			System.out.println(loggerName);
//			logger = Logger.getLogger(loggerName);
//			System.out.println("Handlers: ");
//			Handler[] handlers = logger.getHandlers();
//			for(int i = 0; i < handlers.length; i++)
//				handlers[i].toString();
//		}
//		
//		for (String s : new String[] { "error", "severe", "info", "fine", "finer", "finest"}) {
//			String v = System.getProperty("com.gmmapowell.logging."+ s);
//			if (v != null) {
//				for (String l : v.split(","))
//					Logger.getLogger(l).setLevel(Level.parse(s.toUpperCase()));
//			}
//		}
	}
}

