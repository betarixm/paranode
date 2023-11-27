package kr.ac.postech.paranode.rpc

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.utils.MutableState
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import master.{MasterGrpc, RegisterReply, RegisterRequest}
import Implicit._

class MasterService(
    executionContext: ExecutionContext,
    workers: MutableState[List[WorkerMetadata]]
) extends MasterGrpc.Master
    with Logging {
  override def register(request: RegisterRequest): Future[RegisterReply] = {
    val promise = Promise[RegisterReply]

    Future {
      logger.info(s"[MasterServer] Register ($request)")

      val worker: WorkerMetadata = request.worker.get

      workers.update(_ :+ worker)
    }(executionContext)

    promise.future
  }
}
