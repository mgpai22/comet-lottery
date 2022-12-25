package app

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import mint.akkaFunctions


class LotteryJob() extends Actor with ActorLogging{
  override def receive: Receive = {
    case _ =>
      mint()
  }

  def mint(): Unit ={
    val akka = new akkaFunctions()
    akka.main()
  }
}

object Main extends App{

  override def main(args: Array[String]): Unit = {
    val message = if (args.contains("--chicken")) "Hello chicken" else "Hello world"
    println(message)

    val schedulerActorSystem = ActorSystem("lotteryBot")

    val jobs: ActorRef = schedulerActorSystem.actorOf(
      Props(
        new LotteryJob
      ),
      "scheduler"
    )

    schedulerActorSystem.scheduler.scheduleAtFixedRate(
      initialDelay = 2.seconds,
      interval = 60.seconds,
      receiver = jobs,
      message = ""
    )

  }

}
