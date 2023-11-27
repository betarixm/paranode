package kr.ac.postech.paranode.utils

import scala.collection.{BuildFrom, mutable}

object GenericBuildFrom {
  def apply[A, B]: BuildFrom[List[A], B, List[B]] =
    new BuildFrom[List[A], B, List[B]] {
      override def fromSpecific(from: List[A])(
          it: IterableOnce[B]
      ): List[B] = {
        val b = newBuilder(from)
        b ++= it
        b.result()
      }

      override def newBuilder(
          from: List[A]
      ): mutable.Builder[B, List[B]] = {
        val b = List.newBuilder[B]
        b.sizeHint(from)
        b
      }
    }
}
