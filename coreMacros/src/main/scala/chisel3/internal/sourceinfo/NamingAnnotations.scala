// See LICENSE for license details.

// Transform implementations for name-propagation related annotations.
//
// Helpful references:
// http://docs.scala-lang.org/overviews/quasiquotes/syntax-summary.html#definitions
//   for quasiquote structures of various Scala structures
// http://jsuereth.com/scala/2009/02/05/leveraging-annotations-in-scala.html
//   use of Transformer
// http://www.scala-lang.org/old/sites/default/files/sids/rytz/Wed,%202010-01-27,%2015:10/annots.pdf
//   general annotations reference

package chisel3.internal.naming

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.annotation.compileTimeOnly

object NamingTransforms {
  /** Passthrough transform that prints the annottee for debugging purposes.
    */
  def dump(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    import Flag._

    annottees.foreach(tree => println(show(tree)))
    q"..$annottees"
  }

  /** Applies naming transforms to vals in the annotated module
    */
  def module(c: Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    import Flag._
    var wasNamed: Boolean = false

    val valNameTransform = new Transformer {
      override def transform(tree: Tree) = tree match {
        case q"$mods val $tname: $tpt = $expr" => {
          val TermName(tnameStr: String) = tname
          println(s"val: $tnameStr <= " + show(expr));
          val transformedExpr = super.transform(expr)
          q"$mods val $tname: $tpt = _root_.chisel3.internal.naming.Namer($transformedExpr, $tnameStr)"
        }
        case _ =>  println("stmt: " + show(tree)); super.transform(tree)
      }
    }

    val transformed = annottees.map(_ match {
      case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" => {
        val transformedStats = valNameTransform.transformTrees(stats)
        wasNamed = true
        q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$transformedStats }"
      }
      case q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$stats }" => {
        val transformedStats = valNameTransform.transformTrees(stats)
        wasNamed = true
        q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$transformedStats }"
      }
      case q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }" => {
        // Don't fail noisly when a companion object is passed in with the actuall class def
        q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }"
      }
      case other => c.abort(c.enclosingPosition, s"@module annotion may only be used on classes and traits, got ${showCode(other)}")
    })

    if (!wasNamed) {
      // Double check that something was actually transformed
      c.abort(c.enclosingPosition, s"@module annotation did not match a valid tree, got ${annottees.foreach(tree => showCode(tree).mkString(" "))}")
    }

    q"..$transformed"
  }
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class dump extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro NamingTransforms.dump
}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class module extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro NamingTransforms.module
}
