package io.andr83.nimbus

import io.andr83.nimbus.storage.Storage
import monix.eval.Task
import monix.execution.Cancelable

/**
  * @author Andrei Tupitcyn
  */
abstract class Job {
  def run(storage: Storage): Cancelable
}
