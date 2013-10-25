package com.gmmapowell.apps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import com.gmmapowell.serialization.Endpoint;
import com.gmmapowell.utils.DateUtils;

public class LatencyTester {

	public static void main(String[] args) throws Exception {
		String server = null;
		if (args.length > 0) {
			server = args[0];
		}
		
		Socket socket = null;
		try {
			if (server == null) {
				ServerSocket lsnr = new ServerSocket(5133);
				System.out.println("Listening for a connection ...");
				socket = lsnr.accept();
				pingPong(socket.getInputStream(), socket.getOutputStream(), false);
			} else {
				socket = new Socket(server, 5133);
				pingPong(socket.getInputStream(), socket.getOutputStream(), true);
				socket.close();
			}
		} finally {
			if (socket != null)
				socket.close();
		}
			
	}

	private static void pingPong(InputStream inputStream, OutputStream outputStream, boolean doSending) throws IOException {
		Date from = new Date();
		int k = 0;
		long prevElapsed = 1000;
		while (true) {
			if (doSending) {
				outputStream.write(k++%100);
			}
			
			// Now listen
			inputStream.read();
			Date now = new Date();
			long elapsed = now.getTime()-from.getTime();
			if (elapsed >= prevElapsed) {
				System.out.println("Read " + k + " records in " + DateUtils.elapsedTime(from, now, DateUtils.Format.sss3) + "; average = " + (1000*elapsed)/k + "us");
				prevElapsed = elapsed + 1000;
			}
			doSending = true;
		}
	}
}
