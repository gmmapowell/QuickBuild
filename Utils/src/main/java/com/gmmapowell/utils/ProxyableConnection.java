package com.gmmapowell.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.zinutils.exceptions.UtilException;

public class ProxyableConnection {
	private URLConnection conn;

	ProxyableConnection(Proxy proxy, String repo) {
		try {
			if (proxy == null)
				conn = new URL(repo).openConnection();
			else
				conn = new URL(repo).openConnection(proxy);
		} catch (MalformedURLException e) {
			throw UtilException.wrap(e);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public InputStream getInputStream() throws IOException {
		return conn.getInputStream();
	}

}
