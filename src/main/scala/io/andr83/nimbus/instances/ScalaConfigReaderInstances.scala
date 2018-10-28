package io.andr83.nimbus.instances

import com.github.andr83.scalaconfig._
import com.typesafe.config.Config
import io.andr83.nimbus.storage.{InfluxStorage, Storage}
import io.andr83.nimbus._

/**
  * @author Andrei Tupitcyn
  */
trait ScalaConfigReaderInstances {
  implicit val jobConfigReader: Reader[Job] =
    Reader.pureV[Job]((config: Config, root: String) => {
      val c = config.getConfig(root)
      c.as[String]("type").flatMap {
        case "coinbase"  => c.as[CoinbaseJob.Config].map(new CoinbaseJob(_))
        case "ethermine" => c.as[EthermineJob.Config].map(new EthermineJob(_))
        case "evoshares" => c.as[EvoSharesJob.Config].map(new EvoSharesJob(_))
        case "ss" => c.as[SSJob.Config].map(new SSJob(_))
        case other       => Left(Seq(new IllegalStateException(s"Job type '$other' is unsupported")))
      }
    })

  implicit val storageConfigReader: Reader[Storage] =
    Reader.pureV((config: Config, root: String) => {
      val c = config.getConfig(root)
      c.as[String]("type").flatMap {
        case "influx" => c.as[InfluxStorage.Config].map(new InfluxStorage(_))
        case other    => Left(Seq(new IllegalStateException(s"Storage '$other' is unsupported")))
      }
    })
}
