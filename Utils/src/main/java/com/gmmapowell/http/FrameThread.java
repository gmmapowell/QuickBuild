package com.gmmapowell.http;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ws.InlineServerWebSocketHandler;

public class FrameThread extends Thread{
	private final ConnectionHandler connHandler;
	private final InlineServerWebSocketHandler wshandler;
	private final GPFrame frame;

	public FrameThread(ConnectionHandler connHandler, InlineServerWebSocketHandler wshandler, GPFrame frame) {
		this.connHandler = connHandler;
		this.wshandler = wshandler;
		this.frame = frame;
	}

	@Override
	public void run() {
		try {
			if (frame == null || (frame.opcode == 0x8 && frame.data.length == 0))
			{
				wshandler.onClose(1000);
				connHandler.closeChan();
			} else {
				InlineServer.logger.debug("Read " + frame + " telling listener");
				if (frame.opcode == 0x1)
					wshandler.onTextMessage(new String(frame.data));
				else if (frame.opcode == 0x2)
					wshandler.onBinaryMessage(frame.data);
				else if (frame.opcode == 0x8)
				{
					wshandler.onClose((frame.data[0]&0xff)<<8|(frame.data[1]&0xff));
					connHandler.closeChan();
				}
				else if (frame.opcode == 0xA)
					InlineServer.logger.info("Received pong packet from UA");
				else
					throw new UtilException("Can't handle " + frame.opcode);
			}
		} catch (Exception ex) { 
			ex.printStackTrace();
		}
	}
}
