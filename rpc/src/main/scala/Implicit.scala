package kr.ac.postech.paranode.rpc

import com.google.protobuf.ByteString
import kr.ac.postech.paranode.core.Block
import kr.ac.postech.paranode.core.Key
import kr.ac.postech.paranode.core.KeyRange
import kr.ac.postech.paranode.core.WorkerMetadata

import scala.language.implicitConversions

import common.{
  KeyRange => RpcKeyRange,
  WorkerMetadata => RpcWorkerMetadata,
  Node => RpcNode
}

object Implicit {
  implicit def toKeyRange(rpcKeyRange: RpcKeyRange): KeyRange = KeyRange(
    Key.fromByteString(rpcKeyRange.from),
    Key.fromByteString(rpcKeyRange.to)
  )

  implicit def toWorkerMetadata(
      rpcWorkerMetadata: RpcWorkerMetadata
  ): WorkerMetadata = WorkerMetadata(
    rpcWorkerMetadata.node.get.host,
    rpcWorkerMetadata.node.get.port,
    rpcWorkerMetadata.keyRange.map(toKeyRange)
  )

  implicit def toWorkerMetadata(
      rpcNode: RpcNode
  ): WorkerMetadata = WorkerMetadata(
    rpcNode.host,
    rpcNode.port,
    None
  )

  implicit def toWorkerMetadata(
      rpcWorkerMetadata: Seq[RpcWorkerMetadata]
  ): Seq[WorkerMetadata] = rpcWorkerMetadata.map(toWorkerMetadata)

  implicit def toBlock(
      rpcBlock: ByteString
  ): Block = Block.fromBytes(LazyList.from(rpcBlock.toByteArray))

  implicit def toRpcKeyRange(
      keyRange: KeyRange
  ): RpcKeyRange = RpcKeyRange(
    ByteString.copyFrom(keyRange.from.underlying),
    ByteString.copyFrom(keyRange.to.underlying)
  )

  implicit def toRpcWorkerMetadata(
      workerMetadata: WorkerMetadata
  ): RpcWorkerMetadata = RpcWorkerMetadata(
    Some(RpcNode(workerMetadata.host, workerMetadata.port)),
    workerMetadata.keyRange.map(toRpcKeyRange)
  )

  implicit def toRpcWorkerMetadata(
      workerMetadata: List[WorkerMetadata]
  ): List[RpcWorkerMetadata] = workerMetadata.map(toRpcWorkerMetadata)

  implicit def toByteString(
      block: Block
  ): ByteString = ByteString.copyFrom(block.toChars.map(_.toByte).toArray)
}
