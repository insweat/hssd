package com.insweat.hssd.lib.tree

import com.insweat.hssd.lib.util._

import scala.annotation.tailrec

/** Represents a tree structure.
 */
trait TreeLike extends Traversable[TreeNodeLike] {
    /** Tests if the root node of the tree has been inserted.
     */
    def hasRoot(): Boolean = root.isDefined

    /** Gets the root node of the tree, if root exists.
     */
    def root(): Option[TreeNodeLike]

    /** Traverses all tree nodes in this tree in the implementation-dependent
     *  most convenient way.
     */
    def foreach[U](f: TreeNodeLike=>U) {
    	root match {
    	    case Some(rootNode) => rootNode.preorder.foreach(f)
    	    case None => // pass
    	}
    }
    
    /** Gets the name of the root node, if root exists.
     */
    def rootName(): Option[String] = if(hasRoot) Some(root.get.name) else None

    /** Tests if the specific path matches the root of the tree.
     */
    def matchesRoot(path: TreePath): Boolean = {
        if(!hasRoot || path.length == 0) {
            false
        } 
        else if("*" == path(0)) {
            true
        }
        else {
            rootName.get == path(0)
        }
    }
    
    /** Finds the tree node at path.
     */
    def find(path: TreePath): Option[TreeNodeLike] = {
        @tailrec
        def doFind(node: TreeNodeLike): Option[TreeNodeLike] = {
            if(node.path.length < path.length) {
                node.findChild(path(node.path.length)) match {
                    case Some(child) => doFind(child)
                    case None => None
                }
            }
            else Some(node)
        }
        if(matchesRoot(path)) doFind(root.get) else None
    }
    
    /** Finds the tree node that best (longest) matches path.
     */
    def search(path: TreePath): Option[TreeNodeLike] = {
        if(matchesRoot(path)) {
            val matched: Option[TreeNodeLike] = 
                if(path.length > 2 && "*" == path(1)) {
                    resolve(None, path(2))
                }
                else root
    
            matched match {
                case Some(node) => Some(node.search(path))
                case None => root
            }
        }
        else None
    }

    /** Resolves a node, whose path matches <leadingNode.path>.*.<name>.
     */
    def resolve(leadingNode: Option[TreeNodeLike],
            name: String): Option[TreeNodeLike] = {
        val processedLeadingNode = leadingNode match {
            case Some(node) => node
            case None       => root.get
        }
        processedLeadingNode.children.find{
            child => {
                child.findChild(name).isDefined
            }
        }
    }
    
    def preorder = root match {
        case Some(rootNode) => rootNode.preorder
        case None => emptyTrav()
    }
    
    def preorder(cond: TreeNodeLike => Boolean) = root match {
        case Some(rootNode) => rootNode.preorder(cond)
        case None => emptyTrav()
    }

    def postorder = root match {
        case Some(rootNode) => rootNode.preorder
        case None => emptyTrav()
    }
    
    def postorder(cond: TreeNodeLike => Boolean) = root match {
        case Some(rootNode) => rootNode.preorder(cond)
        case None => emptyTrav()
    }
    
    override def toString() = {
        val typeName = getClass().getSimpleName()
        val content = if(hasRoot) s"${root.get.name}/..." else ""
        s"$typeName($content)"
    }
    
    protected def raiseNameNull() {
        throw new IllegalArgumentException("Node name cannot be null.")
    }
    
    protected def raiseParentIsLeaf(parentNode: TreeNodeLike) {
        throw new IllegalArgumentException(
                s"Leaf node $parentNode cannot become as a parent.")
    }

    protected def raiseParentIsDescendant(
            parentNode: TreeNodeLike, node: TreeNodeLike) {
        throw new IllegalArgumentException(
            s"Node $parentNode cannot be a new parent of $node, " +
            s"because $parentNode is a descendant of %node.")
    }

    protected def raiseChildExists(parentNode: TreeNodeLike, name: String) {
        throw new IllegalArgumentException(
            s"A child of $parentNode with name $name already exists.")
    }
    
    protected def raiseRootExists() {
        throw new IllegalArgumentException(s"$this already has a root $root.")
    }

    protected def raiseNodeNull() {
        throw new IllegalArgumentException("Node cannot be null.")
    }

    protected def raiseParentNull() {
    	throw new IllegalArgumentException("Parent cannot be null.")
    }

    protected def raiseNotLeaf(node: TreeNodeLike) {
    	throw new IllegalArgumentException(s"Node $node is not a leaf.")
    }
}
