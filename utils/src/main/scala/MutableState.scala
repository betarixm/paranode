package kr.ac.postech.paranode.utils

class MutableState[A](var underlying: A) { self =>
  def update(f: A => A): MutableState[A] = {
    synchronized {
      underlying = f(underlying)
    }
    self
  }

  def get: A = underlying
}
