package io.andr83.nimbus

import java.time.Instant

import com.softwaremill.sttp._
import com.typesafe.scalalogging.LazyLogging
import io.andr83.nimbus.storage.Storage
import io.parsek._
import io.parsek.implicits._
import io.parsek.jackson.JsonSerDe
import monix.execution.Cancelable
import monix.execution.Scheduler.{global => scheduler}

import scala.concurrent.duration.FiniteDuration

/**
  * @author Andrei Tupitcyn
  */
class CoinbaseJob(config: CoinbaseJob.Config) extends Job with LazyLogging {

  import CoinbaseJob._

  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  private val serde = JsonSerDe()
  private val rateLens = root.data.rates.at(config.baseCurrency).as[Double]

  override def run(storage: Storage): Cancelable = {
    scheduler.scheduleWithFixedDelay(
      0,
      config.every.length,
      config.every.unit,
      () => {
        val rates = config.currencies.map(cur => {
          val request = sttp.get(uri"https://api.coinbase.com/v2/exchange-rates?currency=$cur")
          val response = request.send()
          for {
            body <- PResult.catchNonFatal(response.unsafeBody)
            json <- serde.parseJson(body)
            rate <- rateLens.get(json)
          } yield Rate(currency = cur, base = config.baseCurrency, rate = rate)
        })

        rates.foreach {
          case PSuccess(rate, _) =>
            storage.save(
              "rates",
              Instant.now().toEpochMilli,
              pmap('value -> PValue(rate.rate)),
              pmap('currency -> PValue(rate.currency), 'base -> PValue(rate.base))
            )
          case PError(errors) =>
            errors.toList.foreach(e => logger.error(e.getMessage, e))
            storage.errors("rates", errors)
        }
      }
    )
  }
}

object CoinbaseJob {

  case class Config(name: String, currencies: Set[String], baseCurrency: String = "EUR", every: FiniteDuration)
      extends JobConfig

  case class Rate(currency: String, base: String, rate: Double)

}
