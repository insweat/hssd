package com.insweat.hssd.lib.tree

import com.insweat.hssd.lib.tree.flat.Tree
import com.insweat.hssd.lib.tree.flat.TreeNode
import com.insweat.hssd.lib.util.Owned
import scala.ref.WeakReference
import scala.collection.mutable
import scala.collection.Set
import com.insweat.hssd.lib.essence.EntryData
import scala.util.control.BreakControl
import scala.util.control.Breaks

final class ValueTree(en: EntryNode) extends Tree with Owned[EntryNode] {

    private val weakEntry = WeakReference(en)

    def owner = weakEntry()

    def base: Option[ValueTree] = {
        if(owner.parent.isDefined) {
            val parentEN = owner.parent.get
            val ed = EntryData.of(parentEN)
            Some(ed.valueTree)
        }
        else None
    }

    private def superRoot = super.root
    override def root = obtain { self => self.superRoot}

    private def superFind(path: TreePath) = super.find(path)
    override def find(path: TreePath) = obtain { self => self.superFind(path) }

    private def superSearch(path: TreePath) = promote(super.search(path))
    override def search(path: TreePath) = obtain {
        self => self.superSearch(path) 
    }

    private def superResolve(leadingNode: Option[TreeNodeLike],
            name: String): Option[TreeNode] = 
                promote(super.resolve(leadingNode, name))

    override def resolve(leadingNode: Option[TreeNodeLike], name: String) = {
        obtain{ self => self.superResolve(leadingNode, name) }
    }

    private def superChildren(p: TreePath) = super.children(p)
    override def children(p: TreePath) = new Traversable[TreeNode] {
        def foreach[U](f: TreeNode=>U) {
            Breaks.breakable{
                val processed = mutable.HashSet[TreePath]()
                trees.foreach { t =>
                    t.superChildren(p).foreach {
                        child => if(!processed.contains(child.path)) {
                            processed += child.path
                            f(wrapped(child))
                        }
                    }
                    if(t.isOverridden(p)) {
                        Breaks.break()
                    }
                }                    
            }
        }
    }

    override def find(p: TreeNodeLike=>Boolean): Option[TreeNode] = {
        var rv: Option[TreeNode] = None
        trees.find{
            t => {
                rv = t.nodes.values.find(p)
                rv.isDefined
            }
        }
        rv
    }

    def trees: Traversable[ValueTree] = new Traversable[ValueTree] {
        def foreach[U](f: ValueTree=>U) {
            var vt = Option(ValueTree.this)
            do {
                f(vt.get)
                vt = vt.get.base
            } while(vt.isDefined)
        }
    }

    def isAnyOverridden = !nodes.isEmpty
    def isOverridden(path: TreePath) = nodes.contains(path)
    def overriddenNodes(traitNames: Set[String] = Set.empty) = {
        if(!traitNames.isEmpty) { 
            new Traversable[TreeNode] {
                def foreach[U](f: TreeNode => U) {
                    nodes.foreach{
                        e => {
                            val (p, n) = e
                            if(p.length >= 2 && traitNames.contains(p(1))) {
                                f(n)
                            }
                        }
                    }
                }
            }
        }
        else {
            new Traversable[TreeNode] {
                def foreach[U](f: TreeNode => U) {
                    nodes.foreach{
                        e => {
                            val (p, n) = e
                            f(n)
                        }
                    }
                }
            }
        }
    }

    override def toString = {
        val typeName = getClass.getSimpleName
        val content = if(hasRoot) s"${root.get.name}/..." else ""
        s"$typeName($content @ $owner)"
    }

    private def promote(optNodeLike: Option[TreeNodeLike]): Option[TreeNode] = {
        optNodeLike match {
            case Some(node) => Some(node.asInstanceOf[TreeNode])
            case None => None
        }
    }

    private def obtain(getter: ValueTree=>Option[TreeNode]) = {
        var tree: Option[ValueTree] = Some(this)
        var rv: Option[TreeNode] = None
        while(tree.isDefined && rv.isEmpty) {
            val t = tree.get
            rv = getter(t)
            tree = t.base
        }
        wrapped(rv)
    }

    private def wrap(node: TreeNode): TreeNode = {
        val rv = new TreeNode(this, node.path, node.isLeaf)
        val nodeData = node.data.asInstanceOf[TreeDataCopySupport]
        rv.data = nodeData.copy(this)
        rv
    }

    private def wrapped(optNode: Option[TreeNode]): Option[TreeNode] = 
        optNode match {
            case Some(node) => Some(wrapped(node))
            case None => None
        }

    private def wrapped(node: TreeNode): TreeNode = 
        if(node.owner != this) wrap(node) else node
}

object ValueTree {
    val rootName = "Root"
}
