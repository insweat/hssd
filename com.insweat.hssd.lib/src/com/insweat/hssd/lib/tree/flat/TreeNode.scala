package com.insweat.hssd.lib.tree.flat

import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.TreeLike
import com.insweat.hssd.lib.tree.TreeNodeLike
import com.insweat.hssd.lib.tree.TreeDataLike
import scala.annotation.tailrec
import scala.ref.WeakReference
import com.insweat.hssd.lib.tree.TreeDataRenameSupport
import com.insweat.hssd.lib.tree.TreeRenameSupport

class TreeNode(
        ownerTree: Tree,
        private var _path: TreePath,
        val isLeaf: Boolean
        ) extends TreeNodeLike {

    private val weakOwner = new WeakReference[Tree](ownerTree)
    private var nodeData: TreeDataLike = null

    override def owner: Tree = weakOwner()

    override def name = path.last

    override def path = _path

    override def parent(): Option[TreeNode] = path.parent match {
        case Some(parentPath) => owner.find(parentPath)
        case None => None
    }
    override def childCount = children.size

    override def iterateChildren = children.toIterable
    
    override def children: Traversable[TreeNode] = owner.children(path)

    override def data: TreeDataLike = nodeData

    override def data_=(value: TreeDataLike) {
        nodeData = value
    }

    override def search(path: TreePath): TreeNode = { 
        @tailrec
        def doSearch(p: TreePath): TreeNode = {
            if(p.length > this.path.length) {
                owner.find(p) match {
                	case Some(node) => node
                	case None => doSearch(p.parent.get)
                }
            }
            else this
        }
        doSearch(path.rebase(this.path))
    }

    override def equals(other: Any): Boolean = {
        if(!other.isInstanceOf[TreeNode]) {
            false
        }
        else {
            val otherRef = other.asInstanceOf[AnyRef]
            if(this eq otherRef) {
                true
            }
            else if(otherRef eq null) {
                false
            }
            else {
                val otherNode = other.asInstanceOf[TreeNode]
                otherNode.owner == owner && otherNode.path == path
            }
        }
    }
    
    override def hashCode(): Int = path.hashCode()

    private[flat] def onRename(newName: String) {
        if(name != newName) {
            nodeData match {
                case renamable: TreeDataRenameSupport =>
                    renamable.onRename(newName)
                case _ => // pass
            }
            _path = _path.rename(newName)
        }
    }
}
