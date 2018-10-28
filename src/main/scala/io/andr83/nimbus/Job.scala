package io.andr83.nimbus

import com.typesafe.scalalogging.LazyLogging
import io.andr83.nimbus.storage.Storage
import monix.execution.Cancelable

/**
  * @author Andrei Tupitcyn
  */
abstract class Job extends LazyLogging {
  def run(storage: Storage): Cancelable
}
