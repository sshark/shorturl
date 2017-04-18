package com.github.ngjiunnjye.shorturl.creator

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.github.ngjiunnjye.shorturl.creator.actor.{InventoryManagerActor, InventoryManagerProxy}
import com.github.ngjiunnjye.shorturl.creator.api.{RootApi, UrlApi}
import com.github.ngjiunnjye.shorturl.utils.Config

import scala.io.StdIn


object UrlCreator extends RootApi with UrlApi with Config {
  implicit val system = ActorSystem("CreatorSystem")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher
  
  val clusterSingletonProperties = ClusterSingletonManager.props(
      singletonProps = Props(classOf[InventoryManagerActor]),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system).withRole(None))
    system.actorOf(clusterSingletonProperties, "InventoryManagerSingleton")
    
  val inventoryManager: ActorRef = InventoryManagerProxy(system).proxy
  
  
  def main(args: Array[String]) {
    
    val route = root ~ urlRoute
    val bindingFuture = Http().bindAndHandle(route,httpInterface, httpPort)

    println(s"Server online at http://${httpInterface}:${httpPort}/\nPress RETURN to stop...")
    StdIn.readLine() 
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => println("Shutdown")) // and shutdown when done
  }
}