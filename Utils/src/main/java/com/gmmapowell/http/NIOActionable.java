package com.gmmapowell.http;

import java.nio.channels.SelectableChannel;

public interface NIOActionable {

	boolean ready(SelectableChannel channel) throws Exception;

}
