/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sqs.impl

import akka.stream._
import akka.stream.alpakka.sqs.{SqsAckResult, SqsBatchException}
import akka.stream.stage._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

private abstract class SqsBatchStageLogic[A](shape: FlowShape[A, Future[scala.List[SqsAckResult]]])
    extends GraphStageLogic(shape)
    with InHandler
    with OutHandler
    with StageLogging {
  import shape._

  protected var inFlight = 0

  private var completionState: Option[Try[Unit]] = None

  private def handleFailure(exception: SqsBatchException): Unit = {
    log.error(exception, "Client failure: {}", exception)
    inFlight -= exception.batchSize
    failStage(exception)
  }

  protected var failureCallback: AsyncCallback[SqsBatchException] = _

  override def preStart(): Unit = {
    super.preStart()
    failureCallback = getAsyncCallback[SqsBatchException](handleFailure)
  }

  override protected def logSource: Class[_] = classOf[SqsBatchStageLogic[A]]

  def checkForCompletion() =
    if (isClosed(in) && inFlight == 0) {
      completionState match {
        case Some(Success(_)) => completeStage()
        case Some(Failure(ex)) => failStage(ex)
        case None => failStage(new IllegalStateException("Stage completed, but there is no info about status"))
      }
    }

  override def onPull() =
    tryPull(in)

  override def onUpstreamFinish() = {
    completionState = Some(Success(()))
    checkForCompletion()
  }

  override def onUpstreamFailure(ex: Throwable) = {
    completionState = Some(Failure(ex))
    checkForCompletion()
  }

  setHandlers(in, out, this)
}
