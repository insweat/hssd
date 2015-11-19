package com.insweat.hssd.editor.httpd;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;

import com.insweat.hssd.editor.util.LogSupport;

class WorkerThread extends Thread {

	private final LogSupport log = new LogSupport("http.worker");
    private final HttpService httpservice;
    private final HttpServerConnection conn;

    public WorkerThread(
            final HttpService httpservice,
            final HttpServerConnection conn) {
        super();
        this.httpservice = httpservice;
        this.conn = conn;
    }

    @Override
    public void run() {
    	log.infof("New connection thread");
        HttpContext context = new BasicHttpContext(null);
        try {
            while (!Thread.interrupted() && this.conn.isOpen()) {
                this.httpservice.handleRequest(this.conn, context);
            }
        } catch (SocketTimeoutException ex) {
        	log.infof("Socket timeout.");
        } catch (ConnectionClosedException ex) {
        	log.errorf("Client closed connection");
        } catch (IOException ex) {
        	log.errorf("I/O error: %s", ex);
        } catch (HttpException ex) {
        	log.errorf("Unrecoverable HTTP protocol violation: %s", ex);
        } finally {
            try {
                this.conn.shutdown();
            } catch (IOException ignore) {}
        }
    }

}
