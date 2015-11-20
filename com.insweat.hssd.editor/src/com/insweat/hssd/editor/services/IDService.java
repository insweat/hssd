package com.insweat.hssd.editor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;

public class IDService {
    private final String EXEC_NAME = "exec_id_alloc";
    private final String[] EXEC_EXTS = {
            "", ".sh", ".py", ".exe", ".cmd", ".bat", ".demo"
    };

    private final LogSupport log = new LogSupport("IDService");

    public static enum Namespace {
        ENTRY_ID("tools.hssd.entry_id"),
        STRING_ID("tools.hssd.string_id");

        public final String name;

        Namespace(String name) {
            this.name = name;
        }
    }

    public long acquire(IProject project, Namespace ns) {
        return multiAcquire(project, ns, 1)[0];
    }

    public long[] multiAcquire(IProject project, Namespace ns, int n) {
        if(n <= 0) {
            String s = "n must be possitive, got %s";
            throw new IllegalArgumentException(S.fmt(s, n));
        }

        final long[] rv = new long[n];
        try {
            final long baseID = doAlloc(project, ns.name, n);
            for(int i = 0; i < n; ++i) {
                rv[i] = baseID + i;
            }
        }
        catch(Exception e) {
            die("Exception raised while allocating %s: %s", ns, e);
        }

        return rv;
    }

    private long doAlloc(IProject project, String ns, int n)
            throws IOException, InterruptedException {
        File execIDAlloc = null;
        for(String ext: EXEC_EXTS) {
            IFile file = project.getFile(EXEC_NAME + ext);
            if(file.exists()) {
                execIDAlloc = file.getLocation().toFile();
                break;
            }
        }
        
        if(execIDAlloc == null || !execIDAlloc.canExecute()) {
            throw new RuntimeException(
                    "The exec_id_alloc[.*] is missing or not executable.");
        }
        
        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(new String[]{
          execIDAlloc.getAbsolutePath(),
          ns,
          String.valueOf(n)
        });
        
        InputStream out = proc.getInputStream();
        InputStream err = proc.getErrorStream();
        
        // It does not seem possible for an exception to occur on our end,
        // just play nice.
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(err));
            StringBuilder errMessage = new StringBuilder();
            String line = null;
            while((line = br.readLine()) != null) {
                errMessage.append(line).append(S.fmt("%n"));
            }
            if(0 != proc.waitFor()) {
                throw new RuntimeException(S.fmt(
                        "%s: %s", EXEC_NAME, errMessage));
            }
        } finally {
            err.close();
        }
        
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(out));
            String startIDStr = br.readLine();
            return Long.parseLong(startIDStr);
        }
        finally {
            out.close();
        }
    }

    private long die(String fmt, Object ... args) throws RuntimeException {
        final String msg = S.fmt(fmt, args);
        log.errorf(msg);
        throw new RuntimeException(msg);
    }
}
