package com.insweat.hssd.lib.tree.structured

import com.insweat.hssd.lib.tree.TreeLike
import com.insweat.hssd.lib.tree.TreeNodeLike
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.TreeDataLike
import com.insweat.hssd.lib.util
import scala.ref.WeakReference
import scala.collection.mutable.HashMap
import com.insweat.hssd.lib.tree.TreeDataRenameSupport

class TreeNode(
        ownerTree: Tree, 
        private var nodeParent: Option[TreeNode], 
        private var nodeName: String,
        val isLeaf: Boolean
    ) extends TreeNodeLike
{
    private val weakOwner = new WeakReference[Tree](ownerTree)
    private[structured] val childNodes = new HashMap[String, TreeNode];
    private var nodePath: TreePath = null
    private var nodeData: TreeDataLike = null

    override def owner: Tree = weakOwner()

    override def name: String = if(nodeName != null) nodeName else "<untitled>"

    override def path(): TreePath = {
        if(nodePath == null) {
            nodePath = parent match {
                case Some(parentNode) => parentNode.path.append(name)
                case None => TreePath(name)
            }
        }
        nodePath
    }

    override def parent: Option[TreeNode] = nodeParent

    override def childCount = childNodes.size;
    override def iterateChildren = childNodes.values
    override def children: Traversable[TreeNodeLike] = 
        new Traversable[TreeNode] {
            def foreach[U](f :TreeNode=>U) {
                childNodes.foreach{ e => f(e._2) }
            }
        } 

    override def findChild(name: String): Option[TreeNode] = childNodes.get(name)

    override def data: TreeDataLike = nodeData
    override def data_=(value: TreeDataLike) {
        nodeData = value
    }
    
    private[structured] def onRename(newName: String) {
        if(nodeName != newName) {
            nodeData match {
                case renamable: TreeDataRenameSupport =>
                    renamable.onRename(newName)
                case _ => // pass
            }

	        if(parent.isDefined) {
	            parent.get.onChildRename(this, newName)
	        }
        	nodeName = newName
        	resetPath()
        }
    }

    private[structured] def onMove(newParent: TreeNode) {
        parent.get.childNodes.remove(name)
        newParent.childNodes += name -> this
        nodeParent = Some(newParent)
        resetPath()
    }

    private def onChildRename(child: TreeNode, newName: String) {
        val extant = childNodes.get(newName)
        if(extant.isDefined) {
            throw new IllegalArgumentException(
                    s"Node ${extant.get} under parent $this already took " +
                    s"name $newName."
            )
        }
        childNodes.get(child.name) match {
            case Some(node) => {
                if(node != child) {
                    throw new RuntimeException(
                            s"Parent $this contains a node $node other " +
                            s"than $child."
                    )
                }
                childNodes.remove(child.name)
                childNodes += newName -> node
            }
            case None => // pass
        }
    }

    private def resetPath() {
        nodePath = null
        children.foreach{
            child => child.asInstanceOf[TreeNode].resetPath()
        }
    }
}
