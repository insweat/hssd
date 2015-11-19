package com.insweat.hssd.lib.tree

import scala.annotation.tailrec

import com.insweat.hssd.lib.util.Owned
import com.insweat.hssd.lib.util.Func1

trait TreeNodeLike extends Owned[TreeLike] {
    def path: TreePath
    
    def name: String

    def isLeaf: Boolean

    def hasParent: Boolean = parent.isDefined
    def parent: Option[TreeNodeLike]

    def childCount: Int
    def iterateChildren: Iterable[TreeNodeLike]
    def children: Traversable[TreeNodeLike]
    def isOrphan: Boolean = if(hasParent) {
        parent.get.findChild(name) match {
        	case Some(node) => node == this
        	case None       => false
        }
    }
    else if(owner.hasRoot) {
        owner.root.get == this
    }
    else true
    
    def countDescendants(): Int = {
        var rv: Int = 0
        postorder.foreach{ 
            _ => rv += 1
        }
        rv - 1
    }

    def findChild(name: String): Option[TreeNodeLike] = {
        children.find{
            child => child.name == name
        }
    }
    
    def search(path: TreePath): TreeNodeLike = { 
        @tailrec
        def doSearch(self: TreeNodeLike): TreeNodeLike = {
            if(self.path.length < path.length) {
                self.findChild(path(self.path.length)) match {
                    case Some(node) => doSearch(node)
                    case None => self
                }
            }
            else self
        }
        doSearch(this)
    }

    def preorder: Traversable[TreeNodeLike] = new Traversable[TreeNodeLike] {
        def foreach[U](f: TreeNodeLike => U) {
            f(TreeNodeLike.this)
            children.foreach{
                node => node.preorder.foreach(f)
            }
        }
    }
    
    def preorder(cond: TreeNodeLike => Boolean): Traversable[TreeNodeLike] =
        new Traversable[TreeNodeLike] {
            def foreach[U](f: TreeNodeLike => U) {
                if(cond(TreeNodeLike.this)) {
                    f(TreeNodeLike.this)
                    children.foreach{
                        node => node.preorder(cond).foreach(f)
                    }
                }
            }
        }   

    def postorder: Traversable[TreeNodeLike] = new Traversable[TreeNodeLike] {
        def foreach[U](f: TreeNodeLike => U) {
            children.foreach{
                node => node.postorder.foreach(f)
            }
            f(TreeNodeLike.this)
        }
    }
    
    def postorder(cond: TreeNodeLike => Boolean): Traversable[TreeNodeLike] =
        new Traversable[TreeNodeLike] {
            def foreach[U](f: TreeNodeLike => U) {
                children.foreach{
                    node => node.postorder(cond).foreach(f)
                }
                if(cond(TreeNodeLike.this)) {
                    f(TreeNodeLike.this)
                }
            }
        }

    def data: TreeDataLike
    def data_=(value: TreeDataLike): Unit

    override def toString() = {
        s"${getClass.getSimpleName}($path)"
    }
}
