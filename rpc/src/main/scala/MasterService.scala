package kr.ac.postech.paranode.rpc

import master.{MasterGrpc, RegisterReply, RegisterRequest}

import kr.ac.postech.paranode.core.WorkerMetadata
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future, Promise}
import Implicit._

import kr.ac.postech.paranode.utils.MutableState

class MasterService(
    executionContext: ExecutionContext,
    workers: MutableState[List[WorkerMetadata]]
) extends MasterGrpc.Master
    with Logging {
  override def register(request: RegisterRequest): Future[RegisterReply] = {
    val promise = Promise[RegisterReply]

    Future {
      logger.debug(s"[MasterServer] Register ($request)")

      val worker: WorkerMetadata = request.worker.get

      workers.update(_ :+ worker)
    }(executionContext)

    promise.future
  }
}
