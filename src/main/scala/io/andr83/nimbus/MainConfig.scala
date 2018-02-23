package io.andr83.nimbus

import io.andr83.nimbus.storage.Storage

/**
  * @author Andrei Tupitcyn
  */
case class MainConfig(jobs: Seq[Job], storage: Storage)