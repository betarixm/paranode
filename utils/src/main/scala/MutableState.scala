package kr.ac.postech.paranode.utils

class MutableState[A](var underlying: A) {
  def update(f: A => A) = new MutableState[A](f(underlying))

  def get: A = underlying
}
