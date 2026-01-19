package Frontend

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext

object Sample extends App{

  implicit val system : ActorSystem = ActorSystem("trade")
  implicit val executionContext : ExecutionContext = system.dispatcher

  val route = {
    path(""){
      get{
        complete("hi")
      }
    }
  }

  val bindingFuture = Http().newServerAt("localhost",9000).bindFlow(route)

  println("Server started at http://localhost:9000")

  bindingFuture.onComplete{
    case Success(binding) =>
      val address = binding.localAddress

      println(s"localhost is running at ${address.getAddress}")
    case Failure(exception) =>
      println(s"failed because of ${exception.getMessage}")
      system.terminate()
  }
}
