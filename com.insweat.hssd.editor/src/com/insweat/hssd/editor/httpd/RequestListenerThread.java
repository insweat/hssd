package com.insweat.hssd.editor.httpd;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import com.insweat.hssd.editor.util.LogSupport;

class RequestListenerThread extends Thread {

	private final LogSupport log = new LogSupport("http.request.listenerr");
    private final ServerSocket serversocket;
    private final HttpParams params;
    private final HttpService httpService;
    private final String docRoot;
    
    private boolean running;

    public RequestListenerThread(int port, String docRoot)
    		throws IOException {

    	this.docRoot = docRoot;
        this.serversocket = new ServerSocket(port);
        this.params = new SyncBasicHttpParams();
        this.params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        // Set up the HTTP protocol processor
        HttpProcessor httpproc = new ImmutableHttpProcessor(
        		new HttpResponseInterceptor[] {
                new ResponseDate(),
                new ResponseServer(),
                new ResponseContent(),
                new ResponseConnControl()
        });

        // Set up request handlers
        HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
        reqistry.register("*", new HttpFileHandler(docRoot));

        // Set up the HTTP service
        this.httpService = new HttpService(
                httpproc,
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory(),
                reqistry,
                this.params);
        
        this.serversocket.setSoTimeout(3000);
    }

    @Override
    public void run() {
    	final int port = serversocket.getLocalPort();
    	log.noticef("Serving at '%s' on port '%s'", docRoot, port);

    	running = true;
    	try {
	    	while (running) {
	            try {
	            	listen();
	            } catch (InterruptedIOException ex) {
	            } catch (IOException e) {
	            	log.errorf("I/O error initialising connection: %s", e);
	                break;
	            }
	        }
    	}
    	finally {
    		try {
				serversocket.close();
			} catch (IOException e) {
				log.errorf("While stopping server socket: %s", e);
			}
    	}
    	log.noticef("Stopped listening on port: %s", port);
    }
    
    public void markStopping() {
    	running = false;
    }
    
    private void listen() throws InterruptedIOException, IOException {
        // Set up HTTP connection
        Socket socket = this.serversocket.accept();
        log.infof("Incoming connection from %s", socket.getInetAddress());

        DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
        conn.bind(socket, this.params);

        // Start worker thread
        Thread t = new WorkerThread(this.httpService, conn);
        t.setDaemon(true);
        t.start();
    }
}