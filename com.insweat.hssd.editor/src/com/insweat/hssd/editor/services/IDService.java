package com.insweat.hssd.editor.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.insweat.hssd.editor.util.LogSupport;
import com.insweat.hssd.editor.util.S;

public class IDService {
    private final LogSupport log = new LogSupport("IDService");

    public static enum Namespace {
        ENTRY_ID("tools.hssd.entry_id"),
        STRING_ID("tools.hssd.string_id");

        public final String name;

        Namespace(String name) {
            this.name = name;
        }
    }

    public long acquire(Namespace ns) {
        return multiAcquire(ns, 1)[0];
    }

    public long[] multiAcquire(Namespace ns, int n) {
        if(n <= 0) {
            String s = "n must be possitive, got %s";
            throw new IllegalArgumentException(S.fmt(s, n));
        }

        final long[] rv = new long[n];
        try {
            final long baseID = doAlloc(ns.name, n);
            for(int i = 0; i < n; ++i) {
                rv[i] = baseID + i;
            }
        }
        catch(SQLException e) {
            die("An exception occurred while allocating ID from %s: %s", ns, e);
        }

        return rv;
    }
    
    private long doAlloc(String ns, int n) throws SQLException {
    	throw new UnsupportedOperationException();
    }
    
    private long die(String fmt, Object ... args) throws RuntimeException {
        final String msg = S.fmt(fmt, args);
        log.errorf(msg);
        throw new RuntimeException(msg);
    }
}
