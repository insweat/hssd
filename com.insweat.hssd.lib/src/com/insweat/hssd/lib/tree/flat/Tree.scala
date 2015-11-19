package com.insweat.hssd.lib.tree.flat

import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.TreeLike
import com.insweat.hssd.lib.tree.TreeNodeLike
import com.insweat.hssd.lib.tree.TreeModifySupport
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import com.insweat.hssd.lib.tree.TreeRenameSupport

/** A flat tree implementation. 
 *  Nodes in a flat tree are stored in a flat manner. Finding 
 *  a node with full path in a flat tree is much faster than 
 *  in a structured tree.
 */
class Tree extends TreeLike 
	with TreeModifySupport with TreeRenameSupport {
    protected final class Pair(val path: TreePath, val node: TreeNode = null)
        extends Ordered[Pair] {
        def compare(that: Pair): Int = path.compare(that.path)
        override def toString = s"($path, $node)"
    }
    
    protected val nodes = new HashMap[TreePath, TreeNode]
    protected[flat] val sortedPairs = new java.util.ArrayList[Pair]
    private var updating = false

    override def root(): Option[TreeNode] = {
        if(sortedPairs.size() > 0 && sortedPairs.get(0).path.length == 1) {
            Some(sortedPairs.get(0).node)
        }
        else None
    }
    
    override def insert(parent: Option[TreeNodeLike], name: String, leaf: Boolean): TreeNode = {
        if(name == null) {
            raiseNameNull()
        }
        val path = parent match {
            case Some(parentNode) =>
                parentNode.checkOwnership(this)
                val preciseParentNode = parentNode.asInstanceOf[TreeNode]
                if(preciseParentNode.isLeaf) {
                    raiseParentIsLeaf(preciseParentNode)
                }
                val p = parentNode.path.append(name)
                if(localFind(p).isDefined) {
                    raiseChildExists(parentNode, name)
                }
                p
            case None =>
                if(hasRoot) {
                    raiseRootExists()
                }
                TreePath(name)
        }

        val rv = new TreeNode(this, path, leaf)

        doInsert(rv)

        rv
    }

    /**
     * NB after the removal, the subtree under node is no longer valid.
     */
    override def remove(node: TreeNodeLike): Boolean = {
        if(node == null) {
            raiseNodeNull()
        }
        node.checkOwnership(this)
        val preciseNode = node.asInstanceOf[TreeNode]
        
        doRemove(node.path)
    }
    
    override def clear {
        nodes.clear
        sortedPairs.clear
    }

    override def rename(node: TreeNodeLike, newName: String) {
        if(node == null) {
            raiseNodeNull()
        }
        if(newName == null) {
            raiseNameNull()
        }
        // Right now, we only allow renaming of leaf nodes in a flat tree.
        if(!node.isLeaf) {
            raiseNotLeaf(node)
        }
        node.checkOwnership(this)
        val oldPath = node.path
        node.asInstanceOf[TreeNode].onRename(newName)
        if(oldPath.last != newName) {
            doRemove(oldPath)
            doInsert(node.asInstanceOf[TreeNode])
        }
    }
    
    private def doInsert(node: TreeNode) {
        val path = node.path
        nodes += path -> node
        sortedPairs.add(new Pair(path, node))
        sort()
    }
    
    private def doRemove(path: TreePath): Boolean = {
        val rv = nodes.remove(path).isDefined
        if(rv) {
            val index = localBinarySearch(path)
            if(index >= 0) {
                sortedPairs.remove(index)
            }
        }
        rv
    }

    override def find(path: TreePath): Option[TreeNode] = localFind(path)

    def children(p: TreePath) = new Traversable[TreeNode] {
        def foreach[U](f: TreeNode=>U) {
            var index = localBinarySearch(p)
            if(index < 0) {
                index = ~index
            }
            else {
                index += 1
            }
            while(index < sortedPairs.size) {
                val pair = sortedPairs.get(index)
                if(pair.path.startsWith(p)) {
                    if(pair.path.length == p.length + 1) {
                        f(pair.node)
                    }
                    index += 1
                }
                else {
                    index = sortedPairs.size // done
                }
            }
        }
    }
    
    def beginUpdate() {
        updating = true
    }
    
    def endUpdate() {
        updating = false
        sort()
    }

    protected def binarySearch(path: TreePath): Int = localBinarySearch(path)

    final protected def sort() {
        if(!updating) {
        	java.util.Collections.sort(sortedPairs)
        }
    }

    final protected def localFind(path: TreePath): Option[TreeNode] = if(hasRoot) {
        val adjustedPath = path.rebase(TreePath(rootName.get))
        nodes.get(adjustedPath)
    }
    else None

    final protected def localBinarySearch(path: TreePath): Int = {
        var rv = java.util.Collections.binarySearch(
                sortedPairs, new Pair(path))
        if(rv < 0) {
            rv = -rv - 1
            rv = ~rv
        }
        rv
    }
}
