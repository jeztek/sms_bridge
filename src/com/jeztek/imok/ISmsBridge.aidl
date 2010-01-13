package com.jeztek.imok;

interface ISmsBridge {
	void startBridge();
	void stopBridge();
	boolean isEnabled();
}
