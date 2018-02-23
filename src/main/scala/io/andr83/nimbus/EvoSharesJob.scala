package io.andr83.nimbus

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

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
class EvoSharesJob(config: EvoSharesJob.Config) extends Job with LazyLogging {
  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()
  private val serde = JsonSerDe()
  private val timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

  override def run(storage: Storage): Cancelable = {
    scheduler.scheduleWithFixedDelay(
      0,
      config.every.length,
      config.every.unit,
      () => {
        val request = sttp.get(
          uri"https://tools.eurolandir.com/tools/sharegraph/handlers/highstockdataintraday.aspx?callback=json&LanguageID=32&s=1166&isin=SE0006826046&market=7&p=OneDay&from=2018-02-23&to=2018-02-23&cur=SEK&ccode=S-EVO&ShareID=2380&currency=SEK&lang=en-US&curShow=&LanguageName=English"
        )
        val response = request.send()
        val res = for {
          body <- PResult.catchNonFatal(response.unsafeBody).map(_.replaceFirst("json\\(", "").stripSuffix(")"))
          json <- serde.parseJson(body)
          data <- root.data.as[String](json)
        } yield {
          val parts = data.split("\\|").last.split(";")
          val dateTime = LocalDateTime.parse(parts(0), timeFormatter)
          val time = dateTime.atZone(ZoneId.of("Europe/Stockholm")).toEpochSecond * 1000
          val price = parts(4).toDouble
          storage.save(
            "shares",
            time,
            pmap('price -> PValue(price)),
            pmap('symbol -> PValue("EVO"), 'currency -> PValue("SEK"))
          )
        }

        res match {
          case PError(errors) =>
            errors.toList.foreach(e => logger.error(e.getMessage, e))
            storage.errors("shares", errors)
          case _ =>
        }
      }
    )
  }
}

object EvoSharesJob {
  case class Config(every: FiniteDuration)
}
