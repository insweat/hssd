package com.insweat.hssd.editor.httpd;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.insweat.hssd.editor.util.LogSupport;

class HttpFileHandler implements HttpRequestHandler  {

    private final String docRoot;
    private final LogSupport log = new LogSupport("http.file.handler");

    public HttpFileHandler(String docRoot) {
        super();
        this.docRoot = docRoot;
    }
    
    private static String getMethod(HttpRequest req) {
    	return req.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
    }

    public void handle(HttpRequest req, HttpResponse resp, HttpContext context)
    		throws HttpException, IOException {

        final String method = getMethod(req);
        if (!method.equals("GET") &&
        		!method.equals("HEAD") &&
        		!method.equals("POST")) {
            throw new MethodNotSupportedException(
            		"Method not supported: " + method);
        }
        final String target = req.getRequestLine().getUri();

        if (req instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) req).getEntity();
            byte[] entityContent = EntityUtils.toByteArray(entity);
            log.infof("Incoming entity content (bytes): %s",
            		entityContent.length);
        }

        final File file = new File(docRoot, URLDecoder.decode(target, "UTF-8"));
        if (!file.exists()) {

            resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
            StringEntity entity = new StringEntity(
                    "<html><body><h1>File" + file.getPath() +
                    " not found</h1></body></html>",
                    ContentType.create("text/html", "UTF-8"));
            resp.setEntity(entity);
            log.warnf("File not found: %s", file);

        } else if (!file.canRead() || file.isDirectory()) {

            resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
            StringEntity entity = new StringEntity(
                    "<html><body><h1>Access denied</h1></body></html>",
                    ContentType.create("text/html", "UTF-8"));
            resp.setEntity(entity);
            log.warnf("Cannot read file: %s", file);

        } else {
            String ext = file.getName().toLowerCase();
            ContentType contentType;
            if(ext.endsWith(".zip")) {
            	contentType = ContentType.create(
            			"application/zip", (Charset) null);
            }
            else if(ext.endsWith(".htm") || ext.endsWith(".html")) {
            	contentType = ContentType.TEXT_HTML;
            }
            else {
            	
            	resp.setStatusCode(HttpStatus.SC_FORBIDDEN);
                StringEntity entity = new StringEntity(
                        "<html><body><h1>Access denied</h1></body></html>",
                        ContentType.create("text/html", "UTF-8"));
                resp.setEntity(entity);
                log.warnf("File format unknown: %s", file);
                return;

            }
            resp.setStatusCode(HttpStatus.SC_OK);
            FileEntity body = new FileEntity(file, contentType);
            resp.setEntity(body);
            log.infof("Serving file: %s", file);
        }
    }

}

