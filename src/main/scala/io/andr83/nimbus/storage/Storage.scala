package io.andr83.nimbus.storage

import io.parsek.NonEmptyList
import io.parsek.PValue.PMap

/**
  * @author Andrei Tupitcyn
  */
trait Storage {
  def save(measurement: String, time: Long, metric: PMap, dimensions: PMap): Unit
  def errors(measurement: String, errors: NonEmptyList[Throwable]): Unit
}
