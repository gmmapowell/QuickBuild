package com.gmmapowell.http.ws;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gmmapowell.exceptions.UtilException;

public class WSCurl {
	private static final Logger logger = LoggerFactory.getLogger("Notification");
	private final String wsaddr;
	private Socket ws;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: WSCurl url");
			System.exit(1);
		}
		WSCurl me = new WSCurl(args[0]);
		me.run();
	}

	public WSCurl(String wsaddr) {
		this.wsaddr = wsaddr;
	}

	public void run() {
		create();
		try {
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(System.in));
			while (true) {
				String s = lnr.readLine();
				if (s == null)
					break;
				ws.fire(s);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			ws.close();
		}
	}

	@SuppressWarnings("rawtypes")
	private void create() {
		logger.debug("Connecting to " + wsaddr);
		Client client = ClientFactory.getDefault().newClient();
		RequestBuilder request = client.newRequestBuilder()
				.method(Request.METHOD.GET)
				.uri(wsaddr)
				.transport(TRANSPORT.WEBSOCKET)                        // Try WebSocket
				.transport(TRANSPORT.LONG_POLLING);                    // Fallback to Long-Polling

		ws = client.create();
		final StringBuilder msg = new StringBuilder();
		final List<String> messages = new ArrayList<String>();
		try
		{
			ws
			.on(Event.MESSAGE, new Function<String>() {
				@Override
				public void on(String s) {
					synchronized (messages) {
						System.out.println(s);
					}
				}
			})
			// Handle Errors
			.on(Event.ERROR, new Function<Throwable>() {
				@Override
				public void on(Throwable t) {
					// Some IOException occurred
					if (t instanceof ConnectException) {
						msg.append("Could not connect to Ziniki on " + wsaddr);
					} else {
						t.printStackTrace();
					}
				}
			})
			.open(request.build());
		}
		catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

}
