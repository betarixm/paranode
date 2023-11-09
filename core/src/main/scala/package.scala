package kr.ac.postech.paranode

package object core {
  type KeyRange = (Key, Key)
  type Partition = (KeyRange, Block)
}
