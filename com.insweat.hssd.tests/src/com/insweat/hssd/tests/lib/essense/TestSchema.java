package com.insweat.hssd.tests.lib.essense;

import com.insweat.hssd.lib.essence.Element;
import com.insweat.hssd.lib.essence.EnumValue;
import com.insweat.hssd.lib.essence.Schema;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.thypes.ComplexThype;
import com.insweat.hssd.lib.essence.thypes.EnumThype;
import com.insweat.hssd.lib.essence.thypes.TraitThype;
import com.insweat.hssd.lib.util.Func;
import com.insweat.hssd.lib.util.Func1;
import com.insweat.hssd.lib.persistence.xml.XMLSchemaLoader;

import scala.Option;
import scala.Tuple2;
import scala.collection.IndexedSeq;
import scala.collection.JavaConversions;
import scala.collection.immutable.HashMap;



import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestSchema {
    private class AttribPair
    {
        public final String name;
        public final String value;

        public AttribPair(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
    
    private class ElemTriple
    {
        public final String caption;
        public final String name;
        public final String thype;
        
        public ElemTriple(String caption, String name, String thype) {
            this.caption = caption;
            this.name = name;
            this.thype = thype;
        }
    }

    private Schema schema;
    final Collector ec = new Collector("error");

    @Before
    public final void setUp() {
        final Collector initEC = new Collector("error", false);
        final XMLSchemaLoader loader = new XMLSchemaLoader(
                "res/unittest/hssd/schemas/schema.xml");
        schema = loader.load(initEC.get()).get();
        schema.compile();
        initEC.assertEmpty();
    }

    @After
    public final void tearDown() {
        schema.clear();
        schema = null;
    }

    @Test
    public final void testEnums() {
        final EnumThype t = getTested("EGender", EnumThype.class);
        final IndexedSeq<EnumValue> valueSeq = t.values(null);
        final List<EnumValue> values = JavaConversions.seqAsJavaList(valueSeq);
        
        Assert.assertEquals("Feminine", values.get(0).name());
        Assert.assertEquals("Masculine", values.get(1).name());
        Assert.assertEquals("Neuter", values.get(2).name());
    }
    
    @Test
    public final void testComplexes() {
        ComplexThype t = null;

        t = getTested("DateYM", ComplexThype.class);
        assertElements(t
            , makeElement("Year", "year", "Int")
            , makeElement("Month", "month", "Int")
            );

        t = getTested("DateYMD", ComplexThype.class);
        assertElements(t
            , makeElement("Year", "year", "Int")
            , makeElement("Month", "month", "Int")
            , makeElement("Day", "day", "Int")
            );
    }
    
    @Test
    public final void testTraits() {
        TraitThype t = null;
        Element e = null;

        t = getTested("Entity", TraitThype.class);
        assertElements(t
            , makeElement("Gender", "gender", "EGender")
            , makeElement("Birth", "birth", "DateYM")
            , makeElement("Death", "death", "DateYM")
            );
        e = t.elements().get("gender").get();
        Assert.assertTrue(e.defaultValue().isEmpty());

        t = getTested("Human", TraitThype.class);
        assertElements(t
            , makeElement("Health", "health", "Int")
            , makeElement("Mana", "mana", "Int")
            , makeElement("Physical Defence", "physicalDefence", "Int")
            , makeElement("Magical Defence", "magicalDefence", "Int")
            , makeElement("Physical Resistence", "physicalResistence", "Float")
            , makeElement("Magical Resistence", "magicalResistence", "Float")
            );
        e = t.elements().get("health").get();
        assertAttribs(e.attribs()
            , makeAttrib("constraints",
            		"com.insweat.hssd.constraints.rangedInt,com.insweat.hssd.constraints.notNull")
            , makeAttrib("com.insweat.hssd.constraints.rangedInt.min",
            		"0")
            , makeAttrib("com.insweat.hssd.constraints.rangedInt.max",
            		"1000000")
            );
        Assert.assertFalse(e.constraints().isEmpty());
        Assert.assertTrue(e.defaultValue().isDefined());
        Assert.assertEquals(100, e.defaultValue().get());
    }
    
    //TODO incorrect schemas

    @SuppressWarnings("unchecked")
    private <T extends Thype> T getTested(String name, Class<T> clazz) {
        final Option<Thype> t = schema.get(name);
        Assert.assertTrue(
            String.format("Undefined thype: %s", name),
            t.isDefined());
        if(clazz != null) {
            Assert.assertTrue(
                String.format(
                    "Expecting instance of: %s, got %s", 
                    clazz,
                    t.get().getClass()),
                clazz.isAssignableFrom(t.get().getClass()));
        }
        return (T)t.get();
    }

    private ElemTriple makeElement(
        String caption,
        String name,
        String thype) {
        return new ElemTriple(caption, name, thype);
    }
    
    private AttribPair makeAttrib(
        String name,
        String value) {
        return new AttribPair(name, value);
    }
    
    private void assertElements(ComplexThype ct,
        ElemTriple ... expectedElems) {
        for(ElemTriple elem : expectedElems) {
            final Option<Element> e = ct.elements().get(elem.name);
            Assert.assertTrue(e.isDefined());
            Assert.assertEquals(e.get().caption(), elem.caption);
            Assert.assertEquals(e.get().thype().name(), elem.thype);
        }
    }
    
    private void assertAttribs(HashMap<String, String> attribs,
            AttribPair ... expectedAttribs) {

        final HashSet<String> missingNames = new HashSet<>();
        final HashSet<String> eqNames = new HashSet<>();
        final HashSet<String> neqNames = new HashSet<>();
        final HashSet<String> extraNames = new HashSet<>();

        for(AttribPair attrib : expectedAttribs) {
            if(!attribs.contains(attrib.name)) {
                missingNames.add(attrib.name);
            }
            else if(!attribs.get(attrib.name).get().equals(attrib.value)) {
                neqNames.add(attrib.name);
            }
            else {
                eqNames.add(attrib.name);
            }
        }

        attribs.foreach(Func.of(new Func1<Tuple2<String, String>, Void>() {
            @Override
            public Void apply(Tuple2<String, String> a) {
                if(!eqNames.contains(a._1) && !neqNames.contains(a._1)) {
                    extraNames.add(a._1);
                }
                return null;
            }
        }));
        
        if(!missingNames.isEmpty()
            || !extraNames.isEmpty()
            || !neqNames.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Unexpected attrib map: \n");
            for(String s: missingNames) {
                sb.append(String.format("\t -  %s", s));
            }
            for(String s: extraNames) {
                sb.append(String.format("\t +  %s", s));
            }
            for(String s: neqNames) {
                sb.append(String.format("\t <> %s, got %s", 
                    s, attribs.get(s).get()));
            }
            Assert.fail(sb.toString());
        }
    }
}
