package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.Implicit._
import kr.ac.postech.paranode.rpc.master.MasterGrpc
import kr.ac.postech.paranode.rpc.master.RegisterReply
import kr.ac.postech.paranode.rpc.master.RegisterRequest
import kr.ac.postech.paranode.utils.MutableState
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

class MasterService(
    executionContext: ExecutionContext,
    mutableWorkers: MutableState[List[WorkerMetadata]]
) extends MasterGrpc.Master
    with Logging {
  override def register(request: RegisterRequest): Future[RegisterReply] = {
    val promise = Promise[RegisterReply]

    Future {
      logger.info(s"[MasterServer] Register ($request)")

      val worker: WorkerMetadata = request.worker.get

      mutableWorkers.update(_ :+ worker)
    }(executionContext)

    promise.future
  }
}
