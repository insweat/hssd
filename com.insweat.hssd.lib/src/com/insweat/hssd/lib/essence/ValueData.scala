package com.insweat.hssd.lib.essence

import com.insweat.hssd.lib.tree.EntryNode
import com.insweat.hssd.lib.tree.ValueNode
import com.insweat.hssd.lib.tree.ValueTree
import com.insweat.hssd.lib.tree.TreeDataLike
import com.insweat.hssd.lib.tree.TreeDataCopySupport
import com.insweat.hssd.lib.tree.TreePath
import com.insweat.hssd.lib.util.Covariant
import scala.language.implicitConversions
import scala.ref.WeakReference
import com.insweat.hssd.lib.tree.TreeLike
import com.insweat.hssd.lib.tree.TreeDataRenameSupport
import scala.util.control.Breaks
import com.insweat.hssd.lib.util
import com.insweat.hssd.lib.tree.structured.TreeNode
import com.insweat.hssd.lib.essence.thypes.ReferenceThype
import com.insweat.hssd.lib.tree.EntryTree
import org.apache.commons.lang3.text.StrSubstitutor
import org.apache.commons.lang3.text.StrLookup
import com.insweat.hssd.lib.essence.thypes.LStringThype
import com.insweat.hssd.lib.essence.thypes.StringThype
import scala.util.control.BreakControl

class ValueText(val value: String)

class ValueError(value: String) extends ValueText(value)

object ValueText {
    val MAX_SUB_RECURSION_DEPTH = 10
    
    def apply(value: String) = new ValueText(value)

    def error(value: String) = new ValueError(value)
    
    def sub(template: String, vt: ValueTree, depth: Int = 0): ValueText = {
        if(depth > MAX_SUB_RECURSION_DEPTH) {
            error(s"Recursion too deep while processing $template")
        }
        else {
            val subst = new StrSubstitutor(new Lookup(vt, depth))
            apply(subst.replace(template))
        }
    }

    private class Lookup(vt: ValueTree, depth: Int) extends StrLookup[String](){
        override def lookup(key: String): String = {
            val keys = key.split("#")
            var currVT = vt
            var optVN: Option[ValueNode] = None
            Breaks.breakable {
                for(i <- 0 until keys.length) {
                    val key = keys(i)
                    val k = if(key.startsWith("*")) key else "*.*." + key
                    val path = TreePath.fromStr(k)
                    val vn = currVT.search(path) match {
                        case Some(vn) if vn.path.length == path.length => vn
                        case _ => {
                            optVN = None
                            Breaks.break()
                        }
                    }

                    val vd = ValueData.of(vn)
                    currVT = vd.asRef match {
                        case Some(ed) => ed.valueTree
                        case None => {
                            if(i < keys.length - 1) {
                                optVN = None
                                Breaks.break()
                            } else {
                                currVT
                            }
                        }
                    }
                    optVN = Some(vn)
                }
            }
            optVN match {
                case Some(vn) =>
                    ValueData.of(vn).recurValueText(depth + 1).value
                case _ => null
            }
        }
    }
}

class ValueData(
        vt: ValueTree,
        private var _path: TreePath,
        val element: ElementLike,
        private var _valex: Option[ValExpr])
    extends TreeDataLike
    with TreeDataCopySupport
    with TreeDataRenameSupport {
    
    def this(vt: ValueTree, p: TreePath, e: ElementLike) {
        this(vt, p, e, None)
    }

    private val weakValueTree = WeakReference(vt)

    private val coval = Covariant(baseCoval)(computeCoval)

    override def copy(vt: TreeLike) = new ValueData(
        vt.asInstanceOf[ValueTree], path, element, _valex
    )

    override def onRename(newName: String) {
        if(!valueTree.isOverridden(path)) {
            raiseCannotRenameError(s"owner node is not overridden.")
        }
        if(!valueNode.isLeaf) {
            raiseCannotRenameError(s"owner node is not leaf.")
        }
        _path = _path.rename(newName)
    }

    override def path = _path
    override def owner = valueNode

    def base = {
        var rv: Option[ValueData] = None
        Breaks.breakable{
            valueTree.base match {
                case Some(baseValueTree) =>
                    baseValueTree.trees.foreach { t =>
                        if(t.isOverridden(path)) {
                            val vn = t.find(path).get
                            val vd = ValueData.of(vn)
                            val (valex, _) = vd.valex
                            if(valex.isDefined) {
                                rv = Some(vd)
                                Breaks.break
                            }
                        }
                    }
                case None => // pass
            }
        }
        rv
    }

    def hasBase: Boolean = base.isDefined // TODO can this be optimized?

    def entryNode = valueTree.owner
    def valueNode = valueTree.find(path).get
    def valueTree = weakValueTree()

    val defaultValex = 
        AbsoluteExpr(element.thype, element.defaultValue getOrElse null)

    def valex: (Option[ValExpr], Boolean) = {
        val isDef = !hasBase && (_valex.isEmpty || _valex.get == defaultValex)
        val rv = if(isDef) Some(defaultValex) else _valex
        (rv, isDef)
    }

    def valex_=(ve: Option[ValExpr]) {
        val changed = ve != _valex

        _valex = ve

        ensureValueNodeOverridden(false)

        // It is incorrect to assume ancestor value data to be clean, when
        // this was clean, because the valex of this data might was
        // absolute, in which case the ancestors are not guaranteed to have
        // been updated. So do not touch them clean.
        if(changed) {
            coval.touch()
        }
    }

    def value = coval.get

    def isOverridden: Boolean = {
        val (optVE, isDef) = valex
        valueTree.isOverridden(path) && !isDef && optVE.isDefined
    }

    def valexText: ValueText = {
        val elemThype = element.thype
        if(isOverridden && elemThype.isInstanceOf[SimpleThypeLike]) {
            val (optVE, _) = valex
            val ve = optVE.get // guaranteed to succeed, since it isOverridden
            if(ve.isError) {
                ValueText.error(ve.repr)
            }
            else {
                elemThype match {
                    case interp: Interpreted =>
                        interp.interpOut(valueNode, ve.value) match {
                            case Right(value) =>
                                val literal = util.literal(value)
                                ValueText(ValExpr.fmt(ve.sym, literal))
                            case Left(error) =>
                                ValueText.error(error)
                        }
                    case _ => ValueText(ve.repr)
                }
            }
        }
        else {
            ValueText("")
        }
    }

    def valueText: ValueText = recurValueText(0)

    def recurValueText(depth: Int): ValueText = {
        val elemThype = element.thype
        val value = this.value
        
        if(value.isError) {
            ValueText.error(util.literal(value.value))
        }
        else if(elemThype.isInstanceOf[SimpleThypeLike]) {
            val theValue = value.value
            elemThype match {
                case interp: Interpreted if theValue != null =>
                    interp.interpOut(valueNode, theValue) match {
                        case Right(value) =>
                            val literal = util.literal(value)
                            if(elemThype.isInstanceOf[LStringThype] ||
                                elemThype.isInstanceOf[StringThype]) {
                                ValueText.sub(literal, valueTree, depth) 
                            }
                            else {
                                ValueText(literal)
                            }
                        case Left(error) => ValueText.error(error)
                    }
                case _ => ValueText(elemThype.repr(elemThype.fixed(theValue)))
            }
        }
        else {
            ValueText("")
        }
    }

    def attempt(veOpt: Option[ValExpr]): ValExpr = {
        var result = if(veOpt.isEmpty) baseCoval match {
            case Some(coval) => coval.get match {
                case err: ErrorExpr => 
                    ErrorExpr("Base value contains an error.")
                case expr => expr
            }
            case None => defaultValex
        }
        else if(veOpt.get.isError) {
            veOpt.get
        }
        else {
            veOpt.get match {
                case ve: AbsoluteExpr => ve
                case ve => base match {
                    case None => ErrorExpr(s"No base value to apply $ve to.")
                    case Some(baseVD) => 
                        val r = ve(baseVD.value)(this)
                        r match {
                            case e: LambdaExpr => 
                                mkNonAbsResultError(baseVD.value, ve)
                            case e => e
                        }
                }
            }
        }

        result = result.validated(this, element.thype.constraint)
        
        element.constraints.foreach{ c =>
            result = result.validated(this, Some(c))
        }

        result
    }
    
    override def toString = s"ValueData($entryNode $valueNode $element)"

    private def baseCoval: Option[Covariant[ValExpr]] = base match {
        case Some(baseVD) => Some(baseVD.coval)
        case None => None
    }

    private def computeCoval: ValExpr = {
        if(valueTree.isOverridden(path)) {
            attempt(valex._1)
        }
        else {
            attempt(None)
        }
    }

    def ensureValueNodeOverridden(recursive: Boolean) {
        // Must copy children first, because once overridden, a node will
        // no longer inherit children from its base.
        if(recursive) {
            valueNode.children.foreach { childVN =>
                val childVD = ValueData.of(childVN)
                childVD.ensureValueNodeOverridden(recursive)
            }
        }

        // if path is not overridden, override it here
        if(!valueTree.isOverridden(path)) {
            var vn = valueNode
            vn = valueTree.insert(vn.parent, vn.name, vn.isLeaf)
            vn.data = this
        }
    }
    
    def asRef: Option[EntryData] = element.thype match {
        case rt: ReferenceThype =>
            if(!value.isError) {
                val entryId = rt.fixed(value.value)
                if(entryId != null) {
                    val tree = entryNode.owner.asInstanceOf[EntryTree]
                    tree.nodesByID.get(entryId.asInstanceOf[Long]).map { en =>
                        EntryData.of(en)
                    }
                }
                else None
            }
            else None
        case _ => None
    }

    private def raiseCannotRenameError(msg: String) {
        throw new IllegalArgumentException(s"Cannot rename $this: $msg")
    }

    private def mkNonAbsResultError(src: ValExpr, ve: ValExpr) = ErrorExpr(
        s"Applying $ve to $src results in a non-absolute value.")
}

object ValueData {
    implicit def of(vn: ValueNode): ValueData = vn.data.asInstanceOf[ValueData]
}
