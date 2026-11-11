package eventsourcing

import scalasql.simple.{SimpleTable, SqliteDialect}

import scala.concurrent.Future

/** JVM-only [[EventStore]] implementation, backed by `scalasql`/JDBC/sqlite (see
  * `be.doeraene.services.database.DatabaseService`). The event log and checkpoint tables are mapped
  * by `be.doeraene.services.database.tables.{RawEventEnvelope, ProjectionCheckpoint}` — kept in
  * that package (rather than here, alongside the plain [[eventsourcing.RawEventEnvelope]] this
  * class deals in) so that their table names (derived from the enclosing object's name) stay
  * `raw_event_envelope` and `projection_checkpoint`, matching the existing migration, without
  * colliding with the cross-platform type of the same simple name in this package.
  *
  * Every blocking scalasql call is wrapped via `castor.Context.future`, not a plain `Future { ...
  * }`, so that `castor.Context.Test.waitForInactivity()` tracks it for its *entire* lifetime (from
  * the moment the call is made, not just from whenever a later `.map`/`.foreach` continuation
  * happens to get scheduled).
  */
class SqlEventStore(client: scalasql.DbClient.DataSource)(using ctx: castor.Context)
    extends EventStore {
  import SqliteDialect.*
  import SqlEventStore.*

  private val conn = client.getAutoCommitClientConnection

  override def appendEnvelopes(envelopes: Vector[RawEventEnvelope]): Future[Unit] =
    ctx.future {
      conn.run(
        RawEventEnvelopeRow.insert.values(envelopes.map(SqlEventStore.toRow)*).skipColumns(_.offset)
      )
      ()
    }

  override def loadEnvelopes(
      entityKind: String,
      entityId: Int,
      afterSequenceNumber: Int,
      pageSize: Int
  ): Future[Vector[RawEventEnvelope]] =
    ctx.future {
      conn
        .run(
          RawEventEnvelopeRow.select
            .filter(envelope =>
              envelope.entityId === entityId && envelope.entityKind === entityKind && envelope.sequenceNumber > afterSequenceNumber
            )
            .sortBy(_.sequenceNumber)
            .take(pageSize)
        )
        .toVector
        .map(SqlEventStore.toCommon)
    }

  override def loadEnvelopesByOffset(
      entityKind: String,
      afterOffset: Int,
      pageSize: Int
  ): Future[Vector[RawEventEnvelope]] =
    ctx.future {
      conn
        .run(
          RawEventEnvelopeRow.select
            .filter(envelope => envelope.offset > afterOffset && envelope.entityKind === entityKind)
            .sortBy(_.offset)
            .take(pageSize)
        )
        .toVector
        .map(SqlEventStore.toCommon)
    }

  override def lastEntityId(entityKind: String): Future[Option[Int]] =
    ctx.future {
      conn
        .run(
          RawEventEnvelopeRow.select
            .filter(_.entityKind === entityKind)
            .sortBy(_.entityId)
            .desc
            .take(1)
        )
        .headOption
        .map(_.entityId)
    }

  override def latestOffset(entityKind: String): Future[Option[Int]] =
    ctx.future {
      conn
        .run(
          RawEventEnvelopeRow.select
            .filter(_.entityKind === entityKind)
            .sortBy(_.offset)
            .desc
            .map(_.offset)
            .take(1)
        )
        .headOption
    }

  override def loadCheckpoint(projectionName: String): Future[Option[Int]] =
    ctx.future {
      conn
        .run(ProjectionCheckpoint.select.filter(_.projectionName === projectionName))
        .toVector
        .headOption
        .map(_.lastOffset)
    }

  override def saveCheckpoint(projectionName: String, offset: Int): Future[Unit] =
    ctx.future {
      val exists = conn
        .run(ProjectionCheckpoint.select.filter(_.projectionName === projectionName))
        .toVector
        .nonEmpty
      if exists then
        conn.run(
          ProjectionCheckpoint
            .update(_.projectionName === projectionName)
            .set(_.lastOffset := offset, _.updatedAt := System.currentTimeMillis())
        )
      else
        conn.run(
          ProjectionCheckpoint.insert.values(
            ProjectionCheckpoint(projectionName, offset, System.currentTimeMillis())
          )
        )
      ()
    }

  override def close(): Unit = conn.close()
}

object SqlEventStore {

  def toRow(envelope: RawEventEnvelope): RawEventEnvelopeRow =
    RawEventEnvelopeRow(
      offset = envelope.offset,
      entityId = envelope.entityId,
      sequenceNumber = envelope.sequenceNumber,
      eventPayload = envelope.eventPayload,
      entityKind = envelope.entityKind,
      timestamp = envelope.timestamp
    )

  def toCommon(row: RawEventEnvelopeRow): RawEventEnvelope =
    RawEventEnvelope(
      offset = row.offset,
      entityId = row.entityId,
      sequenceNumber = row.sequenceNumber,
      eventPayload = row.eventPayload,
      entityKind = row.entityKind,
      timestamp = row.timestamp
    )

  case class RawEventEnvelopeRow(
      offset: Int,
      entityId: Int,
      sequenceNumber: Int,
      eventPayload: String,
      entityKind: String,
      timestamp: Long
  )

  private object RawEventEnvelopeRow extends SimpleTable[RawEventEnvelopeRow] {
    // otherwise named after this object, `raw_event_envelope_row`
    override def tableName: String = "raw_event_envelope"
  }

  private case class ProjectionCheckpoint(projectionName: String, lastOffset: Int, updatedAt: Long)

  private object ProjectionCheckpoint extends SimpleTable[ProjectionCheckpoint]

}
