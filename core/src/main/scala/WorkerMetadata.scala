package kr.ac.postech.paranode.core

case class WorkerMetadata(host: String, port: Int, keyRange: Option[KeyRange])
    extends Node
