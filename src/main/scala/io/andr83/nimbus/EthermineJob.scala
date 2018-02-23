package io.andr83.nimbus
import com.softwaremill.sttp._
import com.typesafe.scalalogging.LazyLogging
import io.andr83.nimbus.EthermineJob.WorkerStat
import io.andr83.nimbus.storage.Storage
import io.parsek._
import io.parsek.implicits._
import io.parsek.shapeless.implicits._
import io.parsek.jackson.JsonSerDe
import monix.execution.Cancelable
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration

/**
  * @author Andrei Tupitcyn
  */
class EthermineJob(config: EthermineJob.Config) extends Job with LazyLogging {
  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  private val serde = JsonSerDe()

  override def run(storage: Storage): Cancelable = {
    scheduler.scheduleWithFixedDelay(
      0,
      config.every.length,
      config.every.unit,
      () => {
        val request = sttp
          .get(uri"https://api.ethermine.org/miner/${config.miner}/workers")
          .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0) Gecko/20100101 Firefox/10.0")
        val response = request.send()
        val res = for {
          body <- PResult.catchNonFatal(response.unsafeBody)
          json <- serde.parseJson(body)
          json <- root.status.as[String](json).flatMap {
            case "OK" => PResult.valid(json)
            case error =>
              PResult.invalid(new IllegalStateException(s"Request to ethermine failed: $body"))
          }
          stats <- root.data.as[List[WorkerStat]](json)
        } yield {
          stats.foreach(stat => {
            storage.save(
              "workers",
              stat.time * 1000,
              pmap(
                'reportedHashrate -> PValue(stat.reportedHashrate),
                'currentHashrate -> PValue(stat.currentHashrate),
                'validShares -> PValue(stat.validShares),
                'invalidShares -> PValue(stat.invalidShares),
                'staleShares -> PValue(stat.staleShares),
                'averageHashrate -> PValue(stat.averageHashrate)
              ),
              pmap('pool -> PValue("ethermine"), 'worker -> PValue(stat.worker))
            )
          })
        }

        res match {
          case PError(errors) =>
            errors.toList.foreach(e => logger.error(e.getMessage, e))
          case _ =>
        }
      }
    )
  }
}

object EthermineJob {
  case class Config(name: String, miner: String, every: FiniteDuration) extends JobConfig

  case class WorkerStat(worker: String,
                        time: Long,
                        lastSeen: Long,
                        reportedHashrate: Long,
                        currentHashrate: Long,
                        validShares: Int,
                        invalidShares: Int,
                        staleShares: Int,
                        averageHashrate: Long)
}
