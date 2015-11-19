package com.insweat.hssd.lib.interop

import java.util.function.Consumer
import com.insweat.hssd.lib.tree.EntryTree
import com.insweat.hssd.lib.tree.EntryNode
import com.insweat.hssd.lib.tree.ValueNode
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.essence.EntryData

object EssenceHelper {
    def foreach(tree: EntryTree, on: Consumer[EntryNode]) {
        tree.foreach{ e =>
            on.accept(e.asInstanceOf[EntryNode])
        }
    }
    
    def foreach(tree: ValueTree, on: Consumer[ValueNode]) {
        tree.foreach{ e =>
            on.accept(e.asInstanceOf[ValueNode])
        }
    }
    
    def foreach(ed: EntryData, on: Consumer[ValueNode]) {
        foreach(ed.valueTree, on)
    }
}
