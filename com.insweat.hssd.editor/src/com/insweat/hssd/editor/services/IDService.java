package com.insweat.hssd.editor.services;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;

import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;
import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.util.Subprocess;

import scala.Option;
import scala.Tuple2;

public class IDService {
    private final String EXEC_NAME = "exec_id_alloc";

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
        Option<File> execIDAlloc = Subprocess.findExecutable(
                project.getLocation().toFile(), EXEC_NAME);
        
        if(!execIDAlloc.isDefined()) {
            String err = "The %s[.*] is missing or not executable.";
            throw new RuntimeException(String.format(err, EXEC_NAME));
        }
        
        Subprocess sp = Subprocess.create(new String[]{
            execIDAlloc.get().getAbsolutePath(), ns, String.valueOf(n)
        });
        
        try {
            Tuple2<String, String> rv = sp.communicate(Interop.none());
            
            if(0 != sp.proc().exitValue()) {
                throw new RuntimeException(S.fmt(
                        "%s: %s", EXEC_NAME, rv._2()));
            }

            return Long.parseLong(rv._1());
        } finally {
            if(sp.proc().isAlive()) {
                sp.proc().destroyForcibly();
            }
        }
    }

    private long die(String fmt, Object ... args) throws RuntimeException {
        final String msg = S.fmt(fmt, args);
        log.errorf(msg);
        throw new RuntimeException(msg);
    }
}
