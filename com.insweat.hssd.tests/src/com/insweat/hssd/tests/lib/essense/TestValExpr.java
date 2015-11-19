package com.insweat.hssd.tests.lib.essense;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import scala.Enumeration.Value;

import com.insweat.hssd.lib.essence.AbsoluteExpr;
import com.insweat.hssd.lib.essence.BuiltinSchema;
import com.insweat.hssd.lib.essence.ErrorExpr;
import com.insweat.hssd.lib.essence.LambdaExpr;
import com.insweat.hssd.lib.essence.SharpExpr;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.ValExpr;
import com.insweat.hssd.lib.util.Func;
import com.insweat.hssd.lib.util.Func1;

public class TestValExpr {
    private final BuiltinSchema builtinSch = new BuiltinSchema();
    private final Thype thypeString = builtinSch.get("String").get();
    private final Thype thypeInt = builtinSch.get("Int").get();
    private final Thype thypeDouble = builtinSch.get("Double").get();
    
    private final ValExpr exprAbsStr = new AbsoluteExpr(thypeString, "str");
    private final ValExpr exprAbsInt = new AbsoluteExpr(thypeInt, 100);
    private final ValExpr exprAbsDbl = new AbsoluteExpr(thypeDouble, 20.5);
    private final ValExpr exprLambdaValid = new LambdaExpr(
            thypeDouble, "x * 1.2 + Math.sqrt(256)");
    private final ValExpr exprSharp = new SharpExpr(thypeInt, "Whatever!");
            
    private final ValExpr[] exprAll = {
            ValExpr.unknownError(),
            ValExpr.applyingError(),
            exprAbsStr, 
            exprAbsInt, 
            exprAbsDbl, 
            exprLambdaValid, 
    };
    
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    private double computeLambdaValid(double x) {
        return x * 1.2 + Math.sqrt(256);
    }

    @Test
    public final void testError() {
        for(ValExpr e : exprAll) {
            ValExpr rv = ValExpr.unknownError().apply(e);
            Assert.assertSame(ValExpr.applyingError(), rv);
        }
    }
    
    @Test
    public final void testAbsolute() {
        for(ValExpr ve : new ValExpr[]{exprAbsDbl, exprAbsInt, exprAbsStr}) {
            for(ValExpr e : exprAll) {
                ValExpr rv = ve.apply(e);
                Assert.assertSame(ve, rv);
            }
        }
    }
    
    @Test
    public final void testLambda() {
        ValExpr rv = null;

        rv = exprLambdaValid.apply(exprAbsInt);
        Assert.assertEquals(computeLambdaValid(100), rv.value());

        rv = exprLambdaValid.apply(exprAbsDbl);
        Assert.assertEquals(computeLambdaValid(20.5), rv.value());

        rv = exprLambdaValid.apply(exprLambdaValid);
        Assert.assertTrue(rv instanceof ErrorExpr);

        rv = exprLambdaValid.apply(ValExpr.unknownError());
        Assert.assertTrue(rv instanceof ErrorExpr);
    }

    @Test
    public final void testSharp() {
        ValExpr rv = null;

        rv = exprSharp.apply(exprAbsInt);
        Assert.assertTrue(rv instanceof ErrorExpr);

        rv = exprSharp.apply(exprLambdaValid);
        Assert.assertTrue(rv instanceof ErrorExpr);
        
        rv = exprSharp.apply(ValExpr.unknownError());
        Assert.assertTrue(rv instanceof ErrorExpr);
    }
    
    @Test
    public final void testValExprEnum() {
        final String[] names = {"Error", "Absolute", "Lambda", "Sharp"};
        for(String name : names) {
            ValExpr.withName(name);
        }

        ValExpr.values().foreach(Func.of(new Func1<Value, Void>()
        {
            @Override
            public Void apply(Value elem) {
                Assert.assertEquals(names[elem.id()], elem.toString());
                return null;
            }
        }));

        String errorMessage = "<error message>";
        ValExpr[] samples = null;
        
        samples = new ValExpr[] {
            ValExpr.make(ValExpr.Error(), null, errorMessage),
            ValExpr.make(ValExpr.errSym(), null, errorMessage),
        };

        for(ValExpr s : samples) {
            Assert.assertTrue(s instanceof ErrorExpr);
            Assert.assertSame(errorMessage, s.value());
        }

        samples = new ValExpr[] {
            ValExpr.make(ValExpr.Absolute(), thypeInt, 999),
            ValExpr.make(ValExpr.absSym(), thypeInt, 999),
        };

        for(ValExpr s : samples) {
            Assert.assertTrue(s instanceof AbsoluteExpr);
            Assert.assertEquals(999, s.value());
        }

        samples = new ValExpr[] {
            ValExpr.make(ValExpr.Lambda(), thypeDouble, "x + 10"),
            ValExpr.make(ValExpr.lamSym(), thypeDouble, "x + 10"),
        };

        for(ValExpr s : samples) {
            Assert.assertTrue(s instanceof LambdaExpr);
            Assert.assertEquals("x + 10", s.value());
        }
        
        samples = new ValExpr[] {
            ValExpr.make(ValExpr.Sharp(), thypeInt, "Whatever!"),
            ValExpr.make(ValExpr.shpSym(), thypeInt, "Whatever!"),
        };

        for(ValExpr s : samples) {
            Assert.assertTrue(s instanceof SharpExpr);
            Assert.assertEquals("Whatever!", s.value());
        }

    }
}
