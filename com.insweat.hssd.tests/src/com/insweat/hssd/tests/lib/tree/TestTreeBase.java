package com.insweat.hssd.tests.lib.tree;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scala.Option;

import com.insweat.hssd.lib.tree.TreeLike;
import com.insweat.hssd.lib.tree.TreeModifySupport;
import com.insweat.hssd.lib.tree.TreeMoveSupport;
import com.insweat.hssd.lib.tree.TreeNodeLike;
import com.insweat.hssd.lib.tree.TreePath;
import com.insweat.hssd.lib.tree.TreeRenameSupport;
import com.insweat.hssd.lib.util.Func;
import com.insweat.hssd.lib.util.Func1;
import com.insweat.hssd.util.AssertEx;

public abstract class TestTreeBase {

    public static final String RES_TREE_SAMPLE_01 = 
            "res/unittest/generic_tree.xml";

    private TreeLike tree;
    private int numInvariantTreeNodes;

    protected TreePath pathFromStr(String path) {
        return TreePath.fromStr(path);
    }

    protected TreePath pathFromComps(String ... comps) {
        return TreePath.fromComps(comps);
    }

    protected abstract TreeLike newTree();
    
    protected TreeLike getTree() {
        return tree;
    }
    
    protected int getNumInvariantTreeNodes() {
        return numInvariantTreeNodes;
    }
    
    protected void setupInvariantTree() {
        tree = newTree();
        numInvariantTreeNodes = loadTree(tree, RES_TREE_SAMPLE_01);
    }

    protected void releaseInvariantTree() {
        this.tree = null;
    }
    
    protected int loadTree(TreeLike tree, String res) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document doc = db.parse(res);
            return loadNode(tree, null, doc.getDocumentElement());
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private int loadNode(TreeLike tree, TreePath parentPath, Element elem) {
        int n = 1;
        String name = elem.getAttribute("name");
        boolean leaf = elem.getAttribute("leaf").equals("leaf");
        final Option<TreeNodeLike> parent;
        if(parentPath == null) {
            parent = Option.apply(null);
        }
        else {
            parent = tree.find(parentPath);
        }
        final TreeNodeLike treeNode = ((TreeModifySupport)tree).insert(parent, name, leaf);
        final NodeList childNodes = elem.getChildNodes();
        for(int i = 0; i < childNodes.getLength(); ++i) {
            final Node node = childNodes.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                n += loadNode(tree, treeNode.path(), (Element)node);
            }
        }
        return n;
    }
    
    //////////
    protected final void doTestConstruction() {
        final TreeNodeLike root = getTree().root().get();
        Assert.assertEquals(getTree(), root.owner());

        final int n = root.countDescendants();
        Assert.assertEquals(getNumInvariantTreeNodes(), n + 1);

        final TreePath pathSwords = pathFromStr("Root.Items.Weapons.Swords");
        final TreeNodeLike swords = getTree().find(pathSwords).get();
        int numSwordsDescendants = swords.countDescendants();
        Assert.assertEquals(7, numSwordsDescendants);

        int numSwordsChildren = swords.childCount();
        Assert.assertEquals(7, numSwordsChildren);
    }

    protected final void doTestInsertion() {
        final TreeLike tree = newTree();
        final TreeLike otherTree = newTree();
        loadTree(tree, RES_TREE_SAMPLE_01);

        final int numNodesBeforeInsertion = tree.root().get().countDescendants() + 1;
        final Option<TreeNodeLike> none = Option.apply(null);
        AssertEx.raises(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // Root already exists
                ((TreeModifySupport)tree).insert(none, "Root", false);
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // Parent is not a node of tree
                ((TreeModifySupport)otherTree).insert(tree.root(), "_", false);
            }
        });
        
        final TreePath pathSwords = pathFromStr("Root.Items.Weapons.Swords");
        final TreePath pathFirstSword = pathSwords.append("sword_sample_0");
        final Option<TreeNodeLike> firstSword = tree.find(pathFirstSword);
        Assert.assertTrue(firstSword.isDefined());
        AssertEx.raises(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // Parent is a leaf node
                ((TreeModifySupport)tree).insert(firstSword, "_", false);
            }
        });

        final Option<TreeNodeLike> swords = tree.find(pathSwords);
        Assert.assertTrue(swords.isDefined());
        AssertEx.raises(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // Node with name already exists
                ((TreeModifySupport)tree).insert(swords, pathFirstSword.last(), true);
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                // Node name is null
                ((TreeModifySupport)tree).insert(swords, null, true);
            }
        });
        
        Assert.assertEquals(numNodesBeforeInsertion, tree.root().get().countDescendants() + 1);
    }
    
    protected final void doTestRemoval() {
        final TreeLike tree = newTree();
        final TreeLike otherTree = newTree();
        loadTree(tree, RES_TREE_SAMPLE_01);
        loadTree(otherTree, RES_TREE_SAMPLE_01);
        
        final int numNodesBeforeRemoval = tree.root().get().countDescendants() + 1;
        AssertEx.raises(IllegalArgumentException.class, new Runnable(){
            @Override
            public void run() {
                // Node is null
                ((TreeModifySupport)tree).remove(null);
            }
        });
        
        final TreePath pathSwords = pathFromStr("Root.Items.Weapons.Swords");
        final TreeNodeLike swordsInOtherTree = otherTree.find(pathSwords).get();
        AssertEx.raises(IllegalArgumentException.class, new Runnable(){
            @Override
            public void run() {
                // Node is not a node of tree
                ((TreeModifySupport)tree).remove(swordsInOtherTree);
            }
        });
        
        boolean rv = ((TreeModifySupport)otherTree).remove(swordsInOtherTree);
        Assert.assertTrue(rv);

        rv = ((TreeModifySupport)otherTree).remove(swordsInOtherTree);
        Assert.assertFalse(rv);
        
        rv = ((TreeModifySupport)otherTree).remove(otherTree.root().get());
        Assert.assertTrue(rv);
        Assert.assertFalse(otherTree.root().isDefined());
        
        Assert.assertEquals(numNodesBeforeRemoval, tree.root().get().countDescendants() + 1);
    }

    protected final void doTestRenaming() {
        final TreeLike tree = newTree();
        final int numNodes = loadTree(tree, RES_TREE_SAMPLE_01);

        final TreeNodeLike root = tree.root().get();
        
        final TreeLike otherTree = newTree();
        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Renaming a node not in otherTree.
                ((TreeRenameSupport)otherTree).rename(root, "_");
            }
        });

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Renaming a null node.
                ((TreeRenameSupport)tree).rename(null, "_");
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Renaming a node with null name.
                ((TreeRenameSupport)tree).rename(root, null);
            }
        });
        
        ((TreeRenameSupport)tree).rename(root, "RenamedRoot");
        root.postorder().foreach(Func.of(new Func1<TreeNodeLike, Void>() {
            @Override
            public Void apply(TreeNodeLike a) {
                Assert.assertTrue(a.path().apply(0).equals("RenamedRoot"));
                return null;
            }
        }));

        final TreePath pathSwords = pathFromStr("*.Items.Weapons.Swords");
        Option<TreeNodeLike> node = tree.find(pathSwords);
        Assert.assertTrue(node.isDefined());

        ((TreeRenameSupport)tree).rename(node.get(), "Swords");
        node = tree.find(pathSwords);
        Assert.assertTrue(node.isDefined());

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                Option<TreeNodeLike> node = tree.find(pathSwords);
                Assert.assertTrue(node.isDefined());
                ((TreeRenameSupport)tree).rename(node.get(), "Daggers");
            }
        });

        ((TreeRenameSupport)tree).rename(node.get(), "RenamedSwords");
        node = tree.find(pathSwords);
        Assert.assertFalse(node.isDefined());
        final TreePath newPathSwords = pathFromStr("*.Items.Weapons.RenamedSwords");
        node = tree.find(newPathSwords);
        Assert.assertTrue(node.isDefined());

        final int numNodesAfterRenaming = 1 + root.countDescendants();
        Assert.assertEquals(numNodes, numNodesAfterRenaming);
    }
    
    protected final void doTestMoving() {
        final TreeLike tree = newTree();
        final int numNodes = loadTree(tree, RES_TREE_SAMPLE_01);
        final TreeLike otherTree = newTree();
        loadTree(otherTree, RES_TREE_SAMPLE_01);

        final TreeNodeLike root = tree.root().get();
        final TreeNodeLike otherRoot = otherTree.root().get();

        final TreePath pathSwords = pathFromStr("*.Items.Weapons.Swords");
        final Option<TreeNodeLike> node = tree.find(pathSwords);
        Assert.assertTrue(node.isDefined());

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a null node
                ((TreeMoveSupport)tree).move(null, root);
            }
        });

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node under null
                ((TreeMoveSupport)tree).move(root, null);
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node from tree into otherTree
                ((TreeMoveSupport)otherTree).move(node.get(), otherRoot);
            }
        });

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node out of tree
                ((TreeMoveSupport)tree).move(node.get(), otherRoot);
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node under itself.
                ((TreeMoveSupport)tree).move(node.get(), node.get());
            }
        });
        
        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node (root) under its descendant.
                ((TreeMoveSupport)tree).move(root, node.get());
            }
        });

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node under a leaf node.
                final TreePath pathFirstDagger = pathFromStr("*.Items.Weapons.Daggers.dagger_sample_0");
                final Option<TreeNodeLike> nodeFirstDagger = tree.find(pathFirstDagger);
                Assert.assertTrue(nodeFirstDagger.isDefined());
                ((TreeMoveSupport)tree).move(node.get(), nodeFirstDagger.get());
            }
        });

        AssertEx.raises(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run() {
                // Moving a node under a parent, who already has a child with the same name.
                final TreePath pathRunes = pathFromStr("*.Items.Runes");
                final TreePath pathWeapons = pathFromStr("*.Items.Weapons");
                final Option<TreeNodeLike> nodeRunes = tree.find(pathRunes);
                final Option<TreeNodeLike> nodeWeapons = tree.find(pathWeapons);
                Assert.assertTrue(nodeRunes.isDefined());
                Assert.assertTrue(nodeWeapons.isDefined());
                ((TreeMoveSupport)tree).move(nodeRunes.get(), nodeWeapons.get());
            }
        });

        final int numSwordDescendants = node.get().countDescendants();
        final TreePath pathDaggers = pathFromStr("*.Items.Weapons.Daggers");
        final Option<TreeNodeLike> nodeDaggers = tree.find(pathDaggers);
        ((TreeMoveSupport)tree).move(node.get(), nodeDaggers.get());
        
        final TreePath newPathSwords = pathFromStr("Root.Items.Weapons.Daggers.Swords");
        final Option<TreeNodeLike> newNodeSwords = tree.find(newPathSwords);
        Assert.assertTrue(newNodeSwords.isDefined());
        Assert.assertSame(node.get(), newNodeSwords.get());
        Assert.assertSame(nodeDaggers.get(), node.get().parent().get());
        
        final int numSwordDescendantsAfterMoving = newNodeSwords.get().countDescendants();
        Assert.assertEquals(numSwordDescendants, numSwordDescendantsAfterMoving);
        node.get().postorder().foreach(Func.of(new Func1<TreeNodeLike, Void>() {
            @Override
            public Void apply(TreeNodeLike a) {
                Assert.assertTrue(a.path().startsWith(newPathSwords));
                return null;
            }
        }));

        final int numNodesAfterMoving = root.countDescendants() + 1;
        Assert.assertEquals(numNodes, numNodesAfterMoving);
    }

    protected final void doTestSearching() {
        final TreePath pathSwords = pathFromStr("Root.Items.Weapons.Swords");

        Option<TreeNodeLike> node;

        final TreePath pathVoid = pathFromStr("Void.Path");
        node = getTree().find(pathVoid);
        Assert.assertFalse(node.isDefined());
        node = getTree().search(pathVoid);
        Assert.assertFalse(node.isDefined());

        final TreePath pathFirstSword = pathFromStr("Root.Items.Weapons.Swords.sword_sample_0");
        node = getTree().find(pathFirstSword);
        Assert.assertEquals(pathFirstSword, node.get().path());
        node = getTree().search(pathFirstSword);
        Assert.assertEquals(pathFirstSword, node.get().path());

        final TreePath pathVoidSword = pathFromStr("Root.Items.Weapons.Swords.sword_void_0");
        node = getTree().find(pathVoidSword);
        Assert.assertFalse(node.isDefined());
        node = getTree().search(pathVoidSword);
        Assert.assertEquals(pathSwords, node.get().path());

        final TreePath pathFirstSwordW = pathFromStr("*.Items.Weapons.Swords.sword_sample_0");
        node = getTree().find(pathFirstSwordW);
        Assert.assertEquals(pathFirstSword, node.get().path());
        node = getTree().search(pathFirstSwordW);
        Assert.assertEquals(pathFirstSword, node.get().path());

        final TreePath pathVoidSwordW = pathFromStr("*.Items.Weapons.Swords.sword_void_0");
        node = getTree().find(pathVoidSwordW);
        Assert.assertFalse(node.isDefined());
        node = getTree().search(pathVoidSwordW);
        Assert.assertEquals(pathSwords, node.get().path());

        final TreePath pathFirstSwordWW = pathFromStr("*.*.Weapons.Swords.sword_sample_0");
        node = getTree().find(pathFirstSwordWW);
        Assert.assertFalse(node.isDefined());
        node = getTree().search(pathFirstSwordWW);
        Assert.assertEquals(pathFirstSword, node.get().path());

        final TreePath pathVoidSwordWW = pathFromStr("*.*.Weapons.Swords.sword_void_0");
        node = getTree().find(pathVoidSwordWW);
        Assert.assertFalse(node.isDefined());
        node = getTree().search(pathVoidSwordWW);
        Assert.assertEquals(pathSwords, node.get().path());
        
        final TreeLike emptyTree = newTree();
        node = emptyTree.find(pathSwords);
        Assert.assertFalse(node.isDefined());
        node = emptyTree.search(pathSwords);
        Assert.assertFalse(node.isDefined());
    }
}
