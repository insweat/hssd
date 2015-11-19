package com.insweat.hssd.lib.tree

trait TreeModifySupport {
    /** Inserts a new node named name under parent.
     *  If parent is None, the new node will be attempted as the root node.
     *  @throws IllegalArgumentException if 
     *  	- parent is None but root already exists,
     *  	- parent is not a node of the tree,
     *  	- parent is a leaf node,
     *  	- parent already contains a child named name,
     *  	- name is null.
     */
    def insert(parent: Option[TreeNodeLike], 
            name: String, leaf: Boolean): TreeNodeLike
    
    /** Removes the specific node from the tree.
     *  It is implementation dependent if the subtree under node would remain
     *  valid after the removal.
     *  @return true if any node has been removed from the tree, false 
     *      otherwise.
     *  @throws IllegalArgumentException if
     *  	- node is null
     *  	- node is not a node of the tree
     */
    def remove(node: TreeNodeLike): Boolean

    def clear()
}
