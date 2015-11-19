package com.insweat.hssd.lib.tree.structured

import com.insweat.hssd.lib.tree.TreeNodeLike
import com.insweat.hssd.lib.tree.TreeLike
import com.insweat.hssd.lib.tree.TreeModifySupport
import com.insweat.hssd.lib.tree.TreeRenameSupport
import com.insweat.hssd.lib.tree.TreeMoveSupport

/** A standard structured tree implementation.
 */
class Tree extends TreeLike 
	with TreeModifySupport 
	with TreeRenameSupport 
	with TreeMoveSupport {
    private var rootNode: TreeNode = null

    override def root(): Option[TreeNode] = Option(rootNode)

    override def insert(
            parent: Option[TreeNodeLike], 
            name: String, 
            leaf: Boolean): TreeNode = {
        if(name == null) {
            raiseNameNull()
        }
        parent match {
            case Some(parentNode) => {
                parentNode.checkOwnership(this)
                val preciseParentNode = parentNode.asInstanceOf[TreeNode]
                if(preciseParentNode.isLeaf) {
                    raiseParentIsLeaf(preciseParentNode)
                }
                if(preciseParentNode.childNodes.contains(name)) {
                    raiseChildExists(parentNode, name)
                }
                val rv = new TreeNode(this, Some(preciseParentNode), name, leaf)
                preciseParentNode.childNodes += name -> rv
                rv
            }
            case None => {
                if(hasRoot) {
                    raiseRootExists()
                }
                rootNode = new TreeNode(this, None, name, leaf)
                rootNode
            }
        }
    }

    override def remove(node: TreeNodeLike): Boolean = {
        if(node == null) {
            raiseNodeNull()
        }
        node.checkOwnership(this)
        if(node == rootNode) {
            rootNode = null
            true
        }
        else {
            val preciseParent = node.parent.get.asInstanceOf[TreeNode]
            preciseParent.childNodes.remove(node.name).isDefined
        }
    }
    
    override def clear {
        rootNode = null
    }
    
    override def move(node: TreeNodeLike, newParent: TreeNodeLike) {
    	if(node == null) {
    	    raiseNodeNull()
    	}
    	if(newParent == null) {
    	    raiseParentNull()
    	}
        node.checkOwnership(this)
        newParent.checkOwnership(this)
        
        if(newParent.path.startsWith(node.path)) {
            raiseParentIsDescendant(newParent, node)
        }
        if(newParent.isLeaf) {
            raiseParentIsLeaf(newParent);
        }
        val extant = newParent.findChild(node.name)
        if(extant.isDefined) {
            raiseChildExists(newParent, node.name)
        }

        node.asInstanceOf[TreeNode].onMove(newParent.asInstanceOf[TreeNode])
    }

    override def rename(node: TreeNodeLike, newName: String) {
        if(node == null) {
            raiseNodeNull()
        }
        if(newName == null) {
            raiseNameNull()
        }
        node.checkOwnership(this)
        node.asInstanceOf[TreeNode].onRename(newName)
    }
}
