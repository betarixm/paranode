package kr.ac.postech.paranode.core

case class KeyRange(from: Key, to: Key) {
  def includes(key: Key): Boolean = (from <= key) && (key < to)

  def includes(record: Record): Boolean = includes(record.key)
}
