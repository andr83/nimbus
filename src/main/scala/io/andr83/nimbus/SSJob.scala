package io.andr83.nimbus

import io.andr83.nimbus.SSJob.Item
import io.andr83.nimbus.storage.Storage
import com.softwaremill.sttp._
import monix.eval.{MVar, Task}
import monix.execution.Cancelable
import monix.execution.Scheduler.{global => scheduler}
import org.jsoup.Jsoup

import scala.concurrent.duration.FiniteDuration
import collection.JavaConverters._

class SSJob(config: SSJob.Config) extends Job {
  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  override def run(storage: Storage): Cancelable = {
    val alreadyProcessed = java.util.Collections.newSetFromMap(
      new java.util.concurrent.ConcurrentHashMap[Item, java.lang.Boolean]).asScala

    scheduler.scheduleWithFixedDelay(
      0,
      config.every.length,
      config.every.unit,
      () => {
        logger.info(s"Starting task: ${config.name}")
        val doc = Jsoup.connect(config.url).get
        val items = doc.getElementById("head_line").parent().select("tr").asScala.tail.toList
        val matched = items.map { e =>
          try {
            val url = "https://ss.com" + Option(e.selectFirst(".msga2 a")).map(_.attr("href")).getOrElse("")
            val price = Option(
              e.select(".msga2-o")
                .last()
            ).map(
              _.text()
                .replace("€", "")
                .replaceAll(",", "")
                .trim
                .toDouble
            )
              .getOrElse(0.0)
            Item(url, price)
          } catch {
            case e: Throwable =>
              logger.warn(e.getMessage, e)
              Item("", 0)
          }
        }.filterNot(alreadyProcessed.apply).filter (i => i.price > 0 && i.price < config.maxPrice)
        matched foreach (i=> logger.info(i.toString))
        matched foreach (i=> {
          val text = s"[${config.name}] ${i.url} / ${i.price}€"
          val url = uri"https://api.telegram.org/bot${config.apiKey}/sendMessage?chat_id=@andr83nimbus&text=$text"
          val res = sttp.get(url).send()
          logger.info(res.unsafeBody)
        })
        matched.foreach(alreadyProcessed.add)
        logger.info(s"Finish task: ${config.name}")
      }
    )
  }
}

object SSJob {

  case class Config(name: String, url: String, maxPrice: Double, every: FiniteDuration, apiKey: String)

  case class Item(url: String, price: Double)

}
