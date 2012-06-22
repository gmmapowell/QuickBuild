package com.gmmapowell.http;

import java.io.IOException;

/** This is the very simple interface we are going to provide
 * for websocket users
 *
 * <p>
 * &copy; 2012 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public interface InlineServerWebSocketHandler {

    void onOpen(GPResponse response);

    void onBinaryMessage(byte[] message) throws IOException;

    void onTextMessage(String string) throws IOException;

    void onClose(int closeCode);

}
