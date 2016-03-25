package ws.kotonoha.server.mongodb

import com.foursquare.rogue.{DateTimeModifyField, DateTimeQueryField, LiftRogue}
import ws.kotonoha.server.records.JodaDateField
import net.liftweb.mongodb.record.BsonRecord

import scala.language.implicitConversions

/**
 * @author eiennohito
 * @since 2013-07-24
 */
trait KotonohaLiftRogue extends LiftRogue {
  implicit def jodaDateTimeField2QueryField[M <: BsonRecord[M]](in: JodaDateField[M]): DateTimeQueryField[M] = {
    new DateTimeQueryField[M](in)
  }

  implicit def jodaDateTimeField2ModifyField[M <: BsonRecord[M]](in: JodaDateField[M]): DateTimeModifyField[M] = {
    new DateTimeModifyField[M](in)
  }
}

object KotonohaLiftRogue extends KotonohaLiftRogue
