package com.insweat.hssd.tests.lib.essense;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import scala.Tuple2;

import com.insweat.hssd.lib.essence.BuiltinSchema;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.util.Func;
import com.insweat.hssd.lib.util.Func1;

public class TestThype {
    @Test
    public final void testBuiltinThypes() {
        final BuiltinSchema sch = new BuiltinSchema();
        final HashMap<String, Thype> thypes = new HashMap<>();
        sch.compile();
        sch.thypes().foreach(Func.of(new Func1<Tuple2<String, Thype>, Void>(){
            @Override
            public Void apply(Tuple2<String, Thype> e) {
                String name = e._1;
                Thype thype = e._2;
                Assert.assertEquals(name, thype.name());
                thypes.put(name, thype);
                return null;
            }
        }));
        Assert.assertTrue(!thypes.isEmpty());
    }
}
