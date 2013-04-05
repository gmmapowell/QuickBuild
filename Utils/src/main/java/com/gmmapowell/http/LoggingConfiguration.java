package com.gmmapowell.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.PropertyConfigurator;

public class LoggingConfiguration {
	
	public LoggingConfiguration() throws SecurityException, IOException
	{
		InputStream resourceAsStream = this.getClass().getResourceAsStream("/logging.properties");
		if (resourceAsStream == null)
			resourceAsStream = new ByteArrayInputStream("log4j.rootLogger=INFO, console\nlog4j.logger.com.gmmapowell=DEBUG\nlog4j.logger.org.ziniki=DEBUG\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.layout=com.gmmapowell.http.Log4JHttpLayout\n".getBytes());
		
		PropertyConfigurator.configure(resourceAsStream);

		//TODO: I think this would need to be done differently for slf4j. We aren't currently using this capability.
//		for (String s : new String[] { "error", "warn", "info", "debug", "trace" }) {
//			String v = System.getProperty("com.gmmapowell.logging."+ s);
//			if (v != null) {
//				for (String l : v.split(","))
//					LoggerFactory.getLogger(l). setLevel(Level.toLevel(s));
//			}
//		}
	}
}


