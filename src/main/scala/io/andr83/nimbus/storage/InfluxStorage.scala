package io.andr83.nimbus.storage

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import io.parsek.PValue.{PBoolean, PDouble, PInt, PLong}
import io.parsek.implicits._
import io.parsek.{NonEmptyList, PValue}
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.{BatchPoints, Point}

/**
  * @author Andrei Tupitcyn
  */
class InfluxStorage(config: InfluxStorage.Config) extends Storage {
  private def db =
    InfluxDBFactory
      .connect(s"http://${config.host}:${config.port}")
      .setDatabase(config.database)

  override def save(table: String, time: Long, metric: PValue.PMap, dimensions: PValue.PMap): Unit = {
    val b = Point.measurement(table).time(time, TimeUnit.MILLISECONDS)
    metric.value.foreach {
      case (k, PInt(v))     => b.addField(k.name, v)
      case (k, PLong(v))    => b.addField(k.name, v)
      case (k, PDouble(v))  => b.addField(k.name, v)
      case (k, PBoolean(v)) => b.addField(k.name, v)
      case (k, v)           => b.addField(k.name, v.asUnsafe[String])
    }

    dimensions.value.foreach {
      case (k, v) => b.tag(k.name, v.asUnsafe[String])
    }

    val p = b.build()
    db.write(p)
  }

  override def errors(measurement: String, errors: NonEmptyList[Throwable]): Unit = {
    val points = errors.toList
      .groupBy(e => e.getClass.getName)
      .flatMap {
        case (errorType, errorsList) =>
          errorsList.map(e => {
            Point
              .measurement("errors")
              .tag("source", measurement)
              .tag("type", errorType)
              .addField("count", errorsList.length)
              .build()
          })
      }
      .toList

    val batchPoints = BatchPoints
      .database(config.database)
      .points(points: _*)
      .build()
    db.write(batchPoints)
  }
}

object InfluxStorage {

  case class Config(host: String = "localhost", port: Int = 8086, database: String = "nimbus")

}
