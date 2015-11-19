package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.essence.thypes.RootThype
import com.insweat.hssd.lib.tree.TreeDataLike
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.tree.EntryNode
import com.insweat.hssd.lib.tree.ValueNode
import com.insweat.hssd.lib.util._
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.language.implicitConversions
import scala.ref.WeakReference
import scala.collection.Set


class EntryData(sch: SchemaLike, en: EntryNode, val entryID: Long)
    extends TreeDataLike {

    private val weakSchema = WeakReference(sch)
    private val weakEntry = WeakReference(en)
    private var dirty = false
    
    val valueTree = new ValueTree(en)

    resetValueTree

    def copy(otherEN: EntryNode, newEntryID: Long) = {
        val newED = new EntryData(schema, otherEN, newEntryID)
        // Assign newED to otherEN as early as possible, so that it can be
        // accessed (indirectly) below.
        otherEN.data = newED

        // TODO flags ?

        newED.traits = traits
    }

    def schema = weakSchema()
    def owner = weakEntry()

    def entryNode = owner
    
    def isDirty(): Boolean = dirty
    def markDirty(): Unit = {
        dirty = true
    }
    def clearDirty(): Unit = {
        dirty = false
    }

    def base = entryNode.parent match {
        case Some(parentEN) => Some(EntryData.of(parentEN))
        case None => None
    }
    
    def searchValueDataAt(valuePath: TreePath): Option[ValueData] = {
        valueTree.search(valuePath).map{ vn => ValueData.of(vn) }
    }

    def valueDataAt(valuePath: TreePath): Option[ValueData] = {
        valueTree.find(valuePath).map { vn => ValueData.of(vn) }
    }

    def valueAt(valuePath: TreePath): Option[Any] = {
        valueDataAt(valuePath).map { vd => vd.value.value }
    }

    def isPublic: Boolean = {
        val path = TreePath.fromStr("*.Entry.isPublic")
        valueAt(path).map { v =>
            v != null && v.isInstanceOf[Boolean] && v.asInstanceOf[Boolean]
        }.getOrElse(false)
    }

    def tags: Set[Long] = {
        val path = TreePath.fromStr("*.Entry.tags")
        var tags: HashSet[Long] = HashSet()
        valueTree.find(path).map { vn =>
            vn.children.foreach { child =>
                val vd = ValueData.of(child)
                val value = vd.element.thype.fixed(vd.value.value)
                if(value != null) {
                    tags += value.asInstanceOf[Long]
                }
            }
        }
        tags
    }

    def caption: String = {
        val path = TreePath.fromStr("*.Entry.caption")
        valueDataAt(path).map{ vd => vd.valueText.value }.getOrElse("")
    }

    def hasImmediateTrait(tr: TraitThypeLike) = {
        val p = TreePath(ValueTree.rootName, tr.name)
        valueTree.isOverridden(p)
    }
    
    def hasInheritedTrait(tr: TraitThypeLike) = {
        var found = false
        var ed = this.base
        while(ed.isDefined) {
            if(ed.get.hasImmediateTrait(tr)) {
                found = true
                ed = None
            }
            else {
                ed = ed.get.base
            }
        }
        found
    }

    def hasTrait(tr: TraitThypeLike) = 
        hasImmediateTrait(tr) || hasInheritedTrait(tr)
    
    def traits: Traversable[TraitThypeLike] = new Traversable[TraitThypeLike] {
        def foreach[U](f: TraitThypeLike => U) {
            valueTree.root.get.children.foreach {
                vn => {
                    val vd = ValueData.of(vn)
                    f(vd.element.thype.asInstanceOf[TraitThypeLike])
                }
            }
        }
    }

    def traits_=(value: Traversable[TraitThypeLike]) {
        resetValueTree
        doInsertTraits(value, inheritedTraits)
    }

    def immediateTraits: Traversable[TraitThypeLike] = { 
        new Traversable[TraitThypeLike] {
            def foreach[U](f: TraitThypeLike=>U) {
                valueTree.overriddenNodes().foreach {
                    vn => {
                        if(vn.path.length == 2) {
                            val thype = ValueData.of(vn).element.thype
                            f(thype.asInstanceOf[TraitThypeLike])
                        }
                    }
                }
            }
        }
    }

    def inheritedTraits: HashSet[TraitThypeLike] = {
        val trav = base match {
            case Some(baseED) => baseED.traits
            case None => emptyTrav[TraitThypeLike]
        }
        HashSet(trav.toSeq: _*)
    }

    private def doInsertTraits(
            desired: Traversable[TraitThypeLike], 
            assigned: HashSet[TraitThypeLike]) {
        var updatedAssigned = assigned
        var optRootVN: Option[ValueNode] = None
        desired.foreach {
            tr => if(!updatedAssigned.contains(tr)) {
                updatedAssigned += tr
                if(optRootVN.isEmpty) {
                    optRootVN = valueTree.root
                }
                val traitNodeElement = new TraitNodeElement(tr)
                growValueTree(optRootVN.get, traitNodeElement, None)
            }
        }
    }

    def insertTraits(traits: Traversable[TraitThypeLike]) {
        val newTraits: mutable.HashSet[TraitThypeLike] = mutable.HashSet.empty
        newTraits ++= traits
        newTraits --= inheritedTraits

        if(!newTraits.isEmpty) {
            // We need to revisit all overridden values relevant to newTraits,
            // because we are unsure if complete structures of all newTraits
            // have been populated (some may be partial if an inherited trait 
            // is previously removed).

            // Backup overridden value nodes that are relevant to newTraits
            val newTraitNames = newTraits map {tr => tr.name}
            val values = backup(newTraitNames, vn => true)

            // Remove those nodes from value tree, since they would otherwise
            // cause error when populating newTraits in the valueTree.
            values.foreach { e => 
                val vn = valueTree.find(e._1).get
                valueTree.remove(vn) 
            }

            doInsertTraits(newTraits, inheritedTraits);

            // Restore values in backup. NB each vn in backup is no longer 
            // valid, but vn.path is still valid
            restore(values)
        }
    }

    def backup(): List[(TreePath, Option[ValExpr])] = 
        backup(Set.empty, vn => true)


    def backup(predicate: ValueNode => Boolean)
        : List[(TreePath, Option[ValExpr])] = backup(Set.empty, predicate)

    
    def backup(
            traitNames: Set[String],
            predicate: ValueNode => Boolean
            ): List[(TreePath, Option[ValExpr])] = {
        var rv: List[(TreePath, Option[ValExpr])] = Nil
        valueTree.overriddenNodes(traitNames).foreach { vn =>
            if(predicate(vn)) {
                rv ::= (vn.path, ValueData.of(vn).valex._1)    
            }
        }
        rv
    }

    def restore(values: List[(TreePath, Option[ValExpr])]) {
        values.foreach{ e => restore(e._1, e._2) } 
    }

    private def restore(path: TreePath, optVE: Option[ValExpr]) {
        val vn = valueTree.search(path).get
        val vd = ValueData.of(vn)
        if(vn.path.length == path.length) {
            vd.valex = optVE;
        }
        else if(vn.path.length + 1 == path.length) {
            require(vd.element.thype.isInstanceOf[CollectionThypeLike])
            val ct = vd.element.thype.asInstanceOf[CollectionThypeLike]

            val newVN = valueTree.insert(Some(vn), path.last, true)
            val elem = ct.makeElement(newVN)
            newVN.data = new ValueData(valueTree, path, elem, optVE)
        }
    }

    def removeTraits(traits: Traversable[TraitThypeLike]) {
        valueTree.root match {
            case Some(rootVN) => traits.foreach {
                tr => {
                    val traitNode = rootVN.findChild(tr.name).get
                    valueTree.remove(traitNode)
                }
            }
            case None => // pass
        }
    }
    
    def packValueTree {
        var namesAssignedTraits: Set[String] = null
        var invalidVNs: List[ValueNode] = Nil
        valueTree.overriddenNodes().foreach {
            vn => {
                if(vn.path.length >= 2) {
                    if(namesAssignedTraits == null) {
                        val names = traits.toTraversable.map{ t => t.name }
                        namesAssignedTraits = names.toSet
                    }
                    if(!namesAssignedTraits.contains(vn.path(1))) {
                        invalidVNs ::= vn
                    }
                }
            }
        }

        invalidVNs.foreach {
            vn => valueTree.remove(vn)
        }

        entryNode.children.foreach{
            en => {
                val ed = en.data.asInstanceOf[EntryData]
                ed.packValueTree
            }
        }
    }
    
    override def toString = s"EntryData($entryNode $entryID)"

    def growValueTree(
            parentVN: ValueNode,
            element: ElementLike,
            optValex: Option[ValExpr]
            ): ValueNode = {
        val simple = element.thype.isInstanceOf[SimpleThypeLike]
        val structured = element.thype.isInstanceOf[ComplexThypeLike]
        val vn = valueTree.insert(
                Some(parentVN),
                element.name,
                simple && !structured)
        vn.data = new ValueData(
                valueTree,
                parentVN.path.append(element.name),
                element,
                optValex)

        if(structured) {
            val complexThype = element.thype.asInstanceOf[ComplexThypeLike]
            complexThype.elements.values.foreach{
                e => growValueTree(vn, e, None)
            }
        }

        vn
    }
    
	private def resetValueTree {
	    if(valueTree.isAnyOverridden) {
	        valueTree.clear
	    }
        if(!entryNode.parent.isDefined) {
            val root = valueTree.insert(None, ValueTree.rootName, false)
            val rootNodeElement = new RootNodeElement(
                    RootThype.get(schema), ValueTree.rootName)
            root.data = new ValueData(
                    valueTree, root.path, rootNodeElement, None)
        }
    }
}

object EntryData {
    implicit def of(en: EntryNode): EntryData =
        en.data.asInstanceOf[EntryData]
}
