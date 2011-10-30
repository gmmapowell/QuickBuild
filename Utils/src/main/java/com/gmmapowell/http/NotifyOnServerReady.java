package com.gmmapowell.http;

import com.gmmapowell.serialization.Endpoint;

public interface NotifyOnServerReady {

	void serverReady(InlineServer inlineServer, Endpoint addr);

}
