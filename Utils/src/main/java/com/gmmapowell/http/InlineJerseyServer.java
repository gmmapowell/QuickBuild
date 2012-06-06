package com.gmmapowell.http;

import java.io.File;

public class InlineJerseyServer extends InlineServer {

	public InlineJerseyServer(int port, String appClz) {
		super(port, "com.sun.jersey.spi.container.servlet.ServletContainer");
		initParam("javax.ws.rs.Application", appClz);
	}

}
