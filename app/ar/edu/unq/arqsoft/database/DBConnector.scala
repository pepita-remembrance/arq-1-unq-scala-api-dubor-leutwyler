package ar.edu.unq.arqsoft.database

import com.google.inject.Inject
import org.squeryl.adapters.PostgreSqlAdapter
import org.squeryl.{AbstractSession, Session}
import play.api.db.{Database => PlayDatabase}

trait DBConnector {
  def sessionCreator: Option[() => AbstractSession]
}

class PostgresConnector @Inject()(db: PlayDatabase) extends DBConnector {
  def sessionCreator: Option[() => AbstractSession] = {
    Some(() => Session.create(
      db.getConnection(),
      new PostgreSqlAdapter
    ))
  }
}