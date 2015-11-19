package com.insweat.hssd.editor.httpd;

import java.io.File;
import java.io.IOException;

import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;

public class StaticHttpServer {

	private final LogSupport log = new LogSupport("static.http.server");
	public final String documentRoot;
	public final int port;
	private RequestListenerThread listenerThread;
	
	public StaticHttpServer(int port, File docRoot) {
		if(port <= 0 || port > 65535) {
			String s = S.fmt("Invalid port: %s", port);
			log.errorf(s);
			throw new IllegalArgumentException(s);
		}
		
		if(!docRoot.isDirectory()) {
			String s = "Document root must be a directory!";
			log.errorf(s);
			throw new IllegalArgumentException(s);
		}

		this.port = port;
		this.documentRoot = docRoot.getAbsolutePath();
	}

	public void start() {
		try {
			stop();

			listenerThread = new RequestListenerThread(port, documentRoot);
			listenerThread.setDaemon(false);
			listenerThread.start();
		} catch (IOException e) {
			log.errorf("When starting http server: %s", e);
			throw new RuntimeException(e);
		}
	}
	
	public void stop() {
		if(listenerThread != null) {
			listenerThread.markStopping();
			listenerThread = null;
		}
	}
	
	public boolean running() {
		return listenerThread != null;
	}
}
