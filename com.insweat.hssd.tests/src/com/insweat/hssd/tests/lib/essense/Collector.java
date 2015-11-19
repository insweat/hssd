package com.insweat.hssd.tests.lib.essense;

import java.util.ArrayList;

import org.junit.Assert;

import scala.Option;

import com.insweat.hssd.lib.interop.Interop;
import com.insweat.hssd.lib.interop.Logging;
import com.insweat.hssd.lib.util.logging.Handler;
import com.insweat.hssd.lib.util.logging.Logger;
import com.insweat.hssd.lib.util.logging.Record;

public final class Collector {
    public final ArrayList<Record> items = new ArrayList<Record>();
    public final String name;
    public final boolean quiet;

    public Collector(String name) {
        this.name = name;
        this.quiet = true;
    }

    public Collector(String name, boolean quiet) {
        this.name = name;
        this.quiet = quiet;
    }

    public Option<Logger> getVoid() {
        return Option.apply(null);
    }

    public Option<Logger> get() {
        final Logger logger = new Logger(
                "collector", 
                Logging.LEVEL_MIN,
                Interop.none(),
                false);
        
        logger.addHandler(new Handler(Logging.LEVEL_WARNING){
            @Override
            public void emit(Record record) {
                items.add(record);
                if(!quiet) {
                    System.out.println(String.format(
                            "%s [%s] %s",
                            record.timestamp(),
                            Logging.getLevelName(record.level()),
                            record.msg()));
                }
            }
        });

        return Interop.opt(logger);
    }

    public void assertEmpty() {
        Assert.assertTrue(items.isEmpty());
    }
    
    public void assertNonEmpty() {
        assertSizeAbove(0);
    }
    
    public void assertSize(int n) {
        Assert.assertEquals(n, items.size());
    }
    
    public void assertSizeAbove(int n) {
        Assert.assertTrue(items.size() > n);
    }
    
    public void assertSizeUnder(int n) {
        Assert.assertTrue(items.size() < n);
    }
}
