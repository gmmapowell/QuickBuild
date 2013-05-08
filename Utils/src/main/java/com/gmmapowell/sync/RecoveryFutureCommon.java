package com.gmmapowell.sync;

public interface RecoveryFutureCommon<K> {
	public void recoverFrom(K obj);
	public void failed(Throwable t);
}
