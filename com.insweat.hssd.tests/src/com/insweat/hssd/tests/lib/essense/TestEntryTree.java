package com.insweat.hssd.tests.lib.essense;

import com.insweat.hssd.lib.essence.Database;
import com.insweat.hssd.lib.essence.EntryData;
import com.insweat.hssd.lib.essence.EnumValue;
import com.insweat.hssd.lib.essence.Thype;
import com.insweat.hssd.lib.essence.TraitThypeLike;
import com.insweat.hssd.lib.essence.ValExpr;
import com.insweat.hssd.lib.essence.ValueData;
import com.insweat.hssd.lib.persistence.xml.XMLDatabaseLoader;
import com.insweat.hssd.lib.tree.EntryTree;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.ValueTree;
import com.insweat.hssd.lib.tree.structured.TreeNode;
import com.insweat.hssd.lib.util.Func;
import com.insweat.hssd.lib.util.Func1;
import com.insweat.hssd.util.AssertEx;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import scala.Option;
import scala.runtime.BoxedUnit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class PlainEntry {
    public final String name;
    public final long id;
    public final long parent;
    public final boolean leaf;
    public final String[] traits;
    
    public PlainEntry(
            String name, long id, long parent, boolean leaf, String ... trs) {
        this.name = name;
        this.id = id;
        this.parent = parent;
        this.leaf = leaf;
        this.traits = trs;
    }
}

public class TestEntryTree {
    private Database db;

    private void ape(
            Map<Long, PlainEntry> ents,
            String name,
            long id,
            long parent,
            boolean leaf,
            String ... trs
            ) {
        final PlainEntry pe = new PlainEntry(name, id, parent, leaf, trs);
        ents.put(id, pe);
    }

    private TreeNode fe(String path) {
        final TreePath tp = TreePath.fromStr(path);
        final EntryTree ents = db.entries();
        final Option<TreeNodeLike> optEN = ents.find(tp);
        Assert.assertTrue(
                String.format("Entry node %s is not found", path),
                optEN.isDefined());
        Assert.assertTrue(
                String.format("Node at %s turns out not an entry", path),
                optEN.get() instanceof TreeNode);
        return (TreeNode)optEN.get();
    }

    private ValueData gvd(TreeNode ent, String shortValuePath) {
        final String valuePath = "*.*." + shortValuePath;
        final EntryData ed = EntryData.of(ent);
        final TreePath vtp = TreePath.fromStr(valuePath);
        final Option<com.insweat.hssd.lib.tree.flat.TreeNode> optVN =
                ed.valueTree().search(vtp);
        Assert.assertTrue(
                String.format("Value node %s is not found", valuePath),
                optVN.isDefined());
        return ValueData.of(optVN.get());
    }
    
    private ValExpr gve(TreeNode ent, String shortValuePath) {
        final ValueData vd = gvd(ent, shortValuePath);
        return vd.value();
    }
    
    private Option<ValExpr> gvex(TreeNode ent, String shortValuePath) {
        final ValueData vd = gvd(ent, shortValuePath);
        return vd.valex()._1;
    }
    
    private void sv(
            TreeNode ent,
            String shortValuePath,
            String sym,
            String str) {
        final ValueData vd = gvd(ent, shortValuePath);
        if(sym == null) {
            vd.valex_$eq(null);
        }
        else if(":=".equals(sym)) {
            final Object value = vd.element().thype().parse(str);
            vd.valex_$eq(Option.apply(ValExpr.make(
                    sym, vd.element().thype(), value)));
        }
        else {
            vd.valex_$eq(Option.apply(ValExpr.make(
                    sym, vd.element().thype(), str)));
        }
    }

    private Object gv(TreeNode ent, String shortValuePath) {
        return gve(ent, shortValuePath).value();
    }

    @Before
    public final void setUp() {
        final Collector initEC = new Collector("error", false);
        final XMLDatabaseLoader loader = new XMLDatabaseLoader(
                "res/unittest/hssd/correct_db.xml");
        db = loader.load(initEC.get()).get();
        initEC.assertEmpty();
    }

    @After
    public final void tearDown() {
        db = null;
    }

    @Test
    public final void testEntryTree() {
        Assert.assertEquals(1, db.schemas().size());

        final Map<Long, PlainEntry> expectedEnts = 
                new HashMap<Long, PlainEntry>();
        ape(expectedEnts, "Root", 100000L, 0L, false, 
                "Entry");
        ape(expectedEnts, "Legions", 110000L, 100000L, false, 
                "Entry", "Entity", "Legion");
        ape(expectedEnts, "Troops", 110100L, 100000L, false, 
                "Entry", "Entity", "Troop");
        ape(expectedEnts, "Characters", 120000L, 100000L, false,
                "Entry", "Entity");
        ape(expectedEnts, "PCs", 121000L, 120000L, false,
                "Entry", "Entity", "PC");
        ape(expectedEnts, "Humen", 121100L, 121000L, false,
                "Entry", "Entity", "PC", "Human");
        ape(expectedEnts, "Warriors", 121110L, 121100L, false,
                "Entry", "Entity", "PC", "Human", "Warrior");
        ape(expectedEnts, "human_warrior_maleA", 121111L, 121110L, true,
                "Entry", "Entity", "PC", "Human", "Warrior");
        ape(expectedEnts, "human_warrior_femaleA", 121112L, 121110L, true,
                "Entry", "Entity", "PC", "Human", "Warrior");
        ape(expectedEnts, "BadBranch", 121120L, 121100L, false,
                "Entry", "Entity", "PC", "Human");
        ape(expectedEnts, "human_bad", 121123L, 121120L, true,
                "Entry", "Entity", "PC", "Human");
        ape(expectedEnts, "NPCs", 122000L, 120000L, false,
                "Entry", "Entity", "NPC");
        ape(expectedEnts, "Strongholds", 130000L, 100000L, false,
                "Entry", "Entity", "Stronghold");
        ape(expectedEnts, "Asia", 130100L, 130000L, false,
                "Entry", "Entity", "Stronghold");
        ape(expectedEnts, "China", 130110L, 130100L, false,
                "Entry", "Entity", "Stronghold");
        ape(expectedEnts, "Japan", 130120L, 130100L, false,
                "Entry", "Entity", "Stronghold");
        ape(expectedEnts, "Beijing", 130111L, 130110L, true,
                "Entry", "Entity", "Stronghold");
        ape(expectedEnts, "Tokyo", 130121L, 130120L, true,
                "Entry", "Entity", "Stronghold");

        final Set<Long> testedEnts = new HashSet<Long>();
        db.entries().foreach(Func.of(new Func1<TreeNodeLike, Void>(){
            @Override
            public Void apply(TreeNodeLike node) {
                final EntryData ed = EntryData.of((TreeNode)node);
                testedEnts.add(ed.entryID());
                final PlainEntry pe = expectedEnts.get(ed.entryID());
                Assert.assertNotNull(String.format("unexpected entry %s",
                        ed.entryID()), pe);
                Assert.assertEquals(pe.name, node.name());
                if(pe.parent == 0) {
                    Assert.assertFalse(node.parent().isDefined());
                }
                else{
                    Assert.assertTrue(node.parent().isDefined());
                    final EntryData ped = EntryData.of(
                            (TreeNode)node.parent().get());
                    Assert.assertEquals(pe.parent, ped.entryID());
                }
                Assert.assertEquals(pe.leaf, node.isLeaf());
                final Set<String> traitNames = 
                        new HashSet<String>(pe.traits.length);
                ed.traits().foreach(Func.of(new Func1<TraitThypeLike, Void>(){
                    @Override
                    public Void apply(TraitThypeLike a) {
                        Thype thype = (Thype)a;
                        traitNames.add(thype.name());
                        return null;
                    }
                }));
                final Set<String> expectedTraitNames = 
                        new HashSet<String>(pe.traits.length);
                for(String name: pe.traits) {
                    expectedTraitNames.add(name);
                }
                AssertEx.contentEquals(expectedTraitNames, traitNames);
                return null;
            }
        }));
        Assert.assertEquals(expectedEnts.size(), testedEnts.size());
    }

    @Test
    public final void testValueTreeCorrectValues() {
        final TreeNode humen = fe("Root.Characters.PCs.Humen");
        Assert.assertEquals(1000.0, gv(humen, "health"));

        final TreeNode humanWarriorMaleA = fe(
                "Root.Characters.PCs.Humen.Warriors.human_warrior_maleA");
        Assert.assertEquals(1000.0, gv(humanWarriorMaleA, "health"));
        Assert.assertEquals("Masculine",
                ((EnumValue)gv(humanWarriorMaleA, "gender")).name());

        final TreeNode humanWarriorFemaleA = fe(
                "Root.Characters.PCs.Humen.Warriors.human_warrior_femaleA");
        Assert.assertEquals(950.0, gv(humanWarriorFemaleA, "health"));
        Assert.assertEquals("Feminine",
                ((EnumValue)gv(humanWarriorFemaleA, "gender")).name());
    
        sv(humanWarriorFemaleA, "health", "=>", "0.99 * x");
        Assert.assertEquals(990.0, gv(humanWarriorFemaleA, "health"));
        
        final TreeNode chars = fe("Root.Characters");
        Assert.assertEquals(12.0, gv(chars, "birth.month"));

        final TreeNode pcs = fe("Root.Characters.PCs");
        Assert.assertEquals(11.0, gv(pcs, "birth.month"));
        
        Assert.assertEquals(11.0, gv(humen,"birth.month"));

        final TreeNode warriors = fe("Root.Characters.PCs.Humen.Warriors");
        Assert.assertEquals(10.0, gv(warriors,"birth.month"));

        Assert.assertEquals(10.0, gv(humanWarriorMaleA,"birth.month"));
        Assert.assertEquals(10.0, gv(humanWarriorFemaleA,"birth.month"));
    }

    @Test
    public final void testValueTreeDefaultValues() {
        final TreeNode humen = fe("Root.Characters.PCs.Humen");
        Assert.assertEquals(100, gv(humen, "mana"));
        Assert.assertNull(gv(humen, "magicalDefence"));
    }

    @Test
    public final void testValueTreeInheritedValues() {
        final TreeNode humanWarriorMaleA = fe(
                "Root.Characters.PCs.Humen.Warriors.human_warrior_maleA");
        Assert.assertEquals(100, gv(humanWarriorMaleA, "mana"));
        Assert.assertNull(gv(humanWarriorMaleA, "magicalDefence"));
        Assert.assertTrue(gvex(humanWarriorMaleA, "mana").isEmpty());
        Assert.assertTrue(gvex(humanWarriorMaleA, "magicalDefence").isEmpty());
    }

    @Test
    public final void testValueTreeChildren() {
        final TreeNode humanWarriorMaleA = fe(
                "Root.Characters.PCs.Humen.Warriors.human_warrior_maleA");
        final ValueTree vt = EntryData.of(humanWarriorMaleA).valueTree();
        final TreeNodeLike vnRoot = vt.root().get();
        final int[] counter = new int[]{ 0 };
        vnRoot.children().foreach(Func.of(new Func1<TreeNodeLike, BoxedUnit>(){
            @Override
            public BoxedUnit apply(TreeNodeLike vn) {
                Assert.assertTrue(vn.owner() == vt);
                ++counter[0];
                return null;
            }
        }));
        Assert.assertTrue(counter[0] > 0);
    }
    
    @Test
    public final void testValueTreeConstraints() {
        //TODO
    }

    @Test
    public final void testValueTreeWrongValues() {
        final TreeNode badBranch = fe("Root.Characters.PCs.Humen.BadBranch");

        Assert.assertTrue(gve(badBranch, "gender").isError());
        Assert.assertFalse(gve(badBranch, "health").isError());

        final TreeNode badHuman = fe(
                "Root.Characters.PCs.Humen.BadBranch.human_bad");
        Assert.assertTrue(gve(badHuman, "gender").isError());
        Assert.assertTrue(gve(badHuman, "health").isError());

        sv(badBranch, "gender", ":=", "1");
        Assert.assertFalse(gve(badBranch, "gender").isError());
        Assert.assertFalse(gve(badHuman, "gender").isError());

        sv(badHuman, "health", ":=", "1000");
        Assert.assertFalse(gve(badBranch, "health").isError());
        Assert.assertFalse(gve(badHuman, "health").isError());
    }
    
    @Ignore("TODO")
    @Test
    public final void testValueTreeFind(){
        
    }    
}
