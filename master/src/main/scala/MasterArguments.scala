package kr.ac.postech.paranode.master

class MasterArguments(args: Array[String]) {
  def numberOfWorkers: Int = args(0).toInt

}
