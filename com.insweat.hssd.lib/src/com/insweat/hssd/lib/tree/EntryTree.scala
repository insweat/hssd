package com.insweat.hssd.lib.tree

import com.insweat.hssd.lib.tree.structured.StructuredTree
import com.insweat.hssd.lib.tree.structured.StructuredTreeNode
import com.insweat.hssd.lib.essence.SchemaLike
import com.insweat.hssd.lib.essence.EntryData
import scala.collection.immutable.HashMap
import scala.util.control.Breaks

class EntryTree(val schema: SchemaLike) extends StructuredTree {
    private var _nodesByID = HashMap[Long, EntryNode]()
    
    def nodesByID = {
        if(_nodesByID.isEmpty) {
            foreach{ node =>
                val ed = EntryData.of(node.asInstanceOf[EntryNode])
                _nodesByID += ed.entryID -> node.asInstanceOf[EntryNode]
            }
        }
        _nodesByID
    }

    def flushNodesByID() {
        _nodesByID = HashMap()
    }

    def findByName(name: String): Option[EntryNode] =
        find(_.name == name).map(_.asInstanceOf[EntryNode])

    override def insert(
        parent: Option[TreeNodeLike], 
        name: String, 
        leaf: Boolean): StructuredTreeNode = {
        flushNodesByID()
        super.insert(parent, name, leaf)
    }

    override def remove(node: TreeNodeLike): Boolean = {
        flushNodesByID()
        super.remove(node)
    }
}
