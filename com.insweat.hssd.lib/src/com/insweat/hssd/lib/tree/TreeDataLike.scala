package com.insweat.hssd.lib.tree

import com.insweat.hssd.lib.util.Owned

trait TreeDataLike extends Owned[TreeNodeLike] {
    def path: TreePath = owner.path
}

trait TreeDataCopySupport extends {
    self: TreeDataLike =>

    def copy(vt: TreeLike): TreeDataLike
}

trait TreeDataRenameSupport extends {
    self: TreeDataLike =>

    /** NB This must be called BEFORE the tree node is renamed, because this
     *  method may raise, and would otherwise cause the tree node and node data
     *  to be inconsistent. 
     */
    def onRename(newName: String)
}
