package actors

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging}
import helpers.Database
import generated.Tables._
import org.jooq.impl.DSL

class CQRSEventHandler extends Actor with ActorLogging {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[Event])
  }

  def receive = {
    case UserRegistered(phoneNumber, userName, timestamp) =>
      Database.withTransaction { context =>
        context.insertInto(TWITTER_USER)
          .columns(TWITTER_USER.CREATED_ON, TWITTER_USER.PHONE_NUMBER, TWITTER_USER.TWITTER_USER_NAME)
          .values(new Timestamp(timestamp.getMillis), phoneNumber, userName)
          .execute()
      }
    case ClientEvent(phoneNumber, userName, MentionsSubscribed(timestamp), _) =>
      Database.withTransaction { context =>
        context.insertInto(MENTION_SUBSCRIPTIONS)
          .columns(MENTION_SUBSCRIPTIONS.USER_ID, MENTION_SUBSCRIPTIONS.CREATED_ON)
          .select(
             context.select(TWITTER_USER.ID, DSL.`val`(new Timestamp(timestamp.getMillis)))
               .from(TWITTER_USER)
               .where(
                 TWITTER_USER.PHONE_NUMBER.equal(phoneNumber)
                 .and(
                   TWITTER_USER.TWITTER_USER_NAME.equal(userName)
                 )
               )
          ).execute()
      }
    case ClientEvent(phoneNumber, userName, MentionReceived(id, created_on, from, text, timestamp), _) =>
      Database.withTransaction { context =>
        context.insertInto(MENTIONS)
          .columns(
            MENTIONS.USER_ID,
            MENTIONS.CREATED_ON,
            MENTIONS.TWEET_ID,
            MENTIONS.AUTHOR_USER_NAME,
            MENTIONS.TEXT
          )
          .select(
             context.select(
               TWITTER_USER.ID,
               DSL.`val`(new Timestamp(timestamp.getMillis)),
               DSL.`val`(id),
               DSL.`val`(from),
               DSL.`val`(text)
             )
             .from(TWITTER_USER)
             .where(
               TWITTER_USER.PHONE_NUMBER.equal(phoneNumber)
               .and(
                 TWITTER_USER.TWITTER_USER_NAME.equal(userName)
               )
             )
          ).execute()
      }
  }

}
