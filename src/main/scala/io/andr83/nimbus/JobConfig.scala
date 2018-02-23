package io.andr83.nimbus

import com.github.andr83.scalaconfig._
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

/**
  * @author Andrei Tupitcyn
  */
abstract class JobConfig {
  def name: String
}
