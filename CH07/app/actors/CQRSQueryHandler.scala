package actors

import akka.actor.Actor
import helpers.Database
import generated.Tables._
import org.jooq.impl.DSL._
import org.jooq.util.postgres.PostgresDataType
import akka.pattern.pipe

import scala.concurrent.Future
import scala.util.control.NonFatal

class CQRSQueryHandler extends Actor {

  implicit val ec = context.dispatcher

  override def receive = {
    case MentionsToday(phoneNumber) =>
      countMentions(phoneNumber).map { count =>
        DailyMentionsCount(count)
      } recover { case NonFatal(t) =>
        QueryFailed
      } pipeTo sender()
  }

  def countMentions(phoneNumber: String): Future[Int] =
    Database.query { context =>
      context.selectCount().from(MENTIONS).where(
        MENTIONS.CREATED_ON.cast(PostgresDataType.DATE).greaterOrEqual(currentDate())
        .and(MENTIONS.USER_ID.equal(
          context.select(TWITTER_USER.ID)
            .from(TWITTER_USER)
            .where(TWITTER_USER.PHONE_NUMBER.equal(phoneNumber)))
          )
      ).fetchOne().value1()
    }
}
