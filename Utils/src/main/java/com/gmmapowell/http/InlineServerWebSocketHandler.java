package com.gmmapowell.http;

import java.io.IOException;

public interface InlineServerWebSocketHandler {

    void onOpen(GPResponse response);

    void onBinaryMessage(byte[] message) throws IOException;

    void onTextMessage(String string) throws IOException;

    void onClose(int closeCode);

}
