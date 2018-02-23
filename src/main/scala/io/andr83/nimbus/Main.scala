package io.andr83.nimbus

import java.io.File

import com.github.andr83.scalaconfig._
import com.github.andr83.scalaconfig.instances.GenericReader
import com.typesafe.config.ConfigFactory
import monix.execution.cancelables.CompositeCancelable
import io.andr83.nimbus.instances.config._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
  * @author Andrei Tupitcyn
  */
object Main extends App {
  val config: MainConfig = {
    val c = if (args.nonEmpty) {
      ConfigFactory.parseFile(new File(args(0))).withFallback(ConfigFactory.load()).resolve()
    } else ConfigFactory.load()
    c.asUnsafe[MainConfig]
  }

  import config._

  val promise    = Promise[Int]()
  val cancelable = jobs.foldLeft(CompositeCancelable())((c, j) => c += j.run(storage))

  sys.addShutdownHook {
    println("Gracefully shutdown")
    promise.success(0)
    cancelable.cancel()
  }

  Await.ready(promise.future, Duration.Inf)
}
