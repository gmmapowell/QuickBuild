package com.gmmapowell.http;

import java.util.ArrayList;
import java.util.List;

import com.gmmapowell.exceptions.UtilException;
import com.gmmapowell.http.ws.InlineServerWebSocketHandler;
import com.gmmapowell.sync.SyncUtils;

public class FrameThread extends Thread{
	private final ConnectionHandler connHandler;
	private final InlineServerWebSocketHandler wshandler;
	private final GPResponse response;
	private final List<GPFrame> queue = new ArrayList<GPFrame>();
	private boolean isAlive = true;

	public FrameThread(ConnectionHandler connHandler, InlineServerWebSocketHandler wshandler, GPResponse response) {
		this.connHandler = connHandler;
		this.wshandler = wshandler;
		this.response = response;
	}

	@Override
	public void run() {
		try {
			wshandler.onOpen(response);
			while (true) {
				GPFrame frame = nextFrame();
				if (frame == null || (frame.opcode == 0x8 && frame.data.length == 0))
				{
					wshandler.onClose(1000);
					connHandler.closeChan();
					break;
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
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			isAlive = false;
		}
	}

	private GPFrame nextFrame() {
		synchronized (queue) {
			while (queue.isEmpty() && isAlive)
				SyncUtils.waitFor(queue, 0);
			if (!queue.isEmpty())
				return queue.remove(0);
			return null;
		}
	}

	public void queue(GPFrame frame) {
		if (frame == null)
			throw new NullPointerException();
		if (!isAlive)
			throw new UtilException("Framing thread died");
		synchronized (queue) {
			queue.add(frame);
			queue.notify();
		}
	}
	
	public void close() {
		synchronized (queue) {
			isAlive = false;
			queue.notify();
		}
	}
}
