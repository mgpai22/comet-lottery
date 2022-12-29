package app

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import mint.akkaFunctions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class LotteryJob(following: Boolean, url1: String, url2: String)
    extends Actor
    with ActorLogging {
  override def receive: Receive = { case _ =>
    mint()
  }

  def mint(): Unit = {
    val akka = new akkaFunctions(following, url1, url2)
    akka.main()
  }
}

object Main extends App {

  override def main(args: Array[String]): Unit = { //main function that runs with jar

    var following = false
    var url1: String = null
    var url2: String = null

    for (arg <- args) {
      if (arg == "--follow") {
        following = true
      } else if (following) {
        if (url1 == null) {
          url1 = arg
        } else {
          url2 = arg
        }
      }
    }

    val schedulerActorSystem = ActorSystem("lotteryBot")

    val jobs: ActorRef = schedulerActorSystem.actorOf(
      Props(
        new LotteryJob(following, url1, url2)
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
