package ar.edu.unq.arqsoft.database

import java.sql.Timestamp

import ar.edu.unq.arqsoft.model._
import org.joda.time.DateTime
import org.squeryl.dsl._
import org.squeryl.{PrimitiveTypeMode, Schema, Table}

trait PrimitiveJodaTimeSupport {
  this: PrimitiveTypeMode =>
  implicit val jodaTimeTEF = new NonPrimitiveJdbcMapper[Timestamp, DateTime, TTimestamp](timestampTEF, this) {

    def convertFromJdbc(t: Timestamp) = new DateTime(t)

    def convertToJdbc(t: DateTime) = new Timestamp(t.getMillis)
  }

  implicit val optionJodaTimeTEF =
    new TypedExpressionFactory[Option[DateTime], TOptionTimestamp]
      with DeOptionizer[Timestamp, DateTime, TTimestamp, Option[DateTime], TOptionTimestamp] {

      val deOptionizer = jodaTimeTEF
    }

  implicit def jodaTimeToTE(s: DateTime): TypedExpression[DateTime, TTimestamp] = jodaTimeTEF.create(s)

  implicit def optionJodaTimeToTE(s: Option[DateTime]): TypedExpression[Option[DateTime], TOptionTimestamp] = optionJodaTimeTEF.create(s)
}

trait DSLFlavor extends PrimitiveTypeMode with PrimitiveJodaTimeSupport

object DSLFlavor extends DSLFlavor

import ar.edu.unq.arqsoft.database.DSLFlavor._ //This import provides the implicit FieldMapper for Schema.

object InscriptionPollSchema extends Schema {

  val students = table[Student]
  val careers = table[Career]
  val subjects = table[Subject]
  val offers = table[OfferOptionBase]
  val courses = table[Course]
  val nonCourses = table[NonCourseOption]
  val schedules = table[Schedule]
  val polls = table[Poll]
  val pollSubjectOption = table[PollSubjectOption]
  val pollOfferOptions = table[PollOfferOption]
  val results = table[PollResult]
  val pollSelectedOptions = table[PollSelectedOption]
  val admins = table[Admin]

  val studentResults = oneToManyRelation(students, results)
    .via((s, r) => s.id === r.studentId)
  val careerSubjects = oneToManyRelation(careers, subjects)
    .via((c, s) => c.id === s.careerId)
  val careerPolls = oneToManyRelation(careers, polls)
    .via((c, p) => c.id === p.careerId)
  val courseSchedules = oneToManyRelation(courses, schedules)
    .via((c, s) => c.id === s.courseId)
  val pollResults = oneToManyRelation(polls, results)
    .via((p, pr) => p.id === pr.pollId)
  val pollPollSubjectOptions = oneToManyRelation(polls, pollSubjectOption)
    .via((p, ps) => p.id === ps.pollId)
  val subjectPollSubjectOptions = oneToManyRelation(subjects, pollSubjectOption)
    .via((s, ps) => s.id === ps.subjectId)
  val pollPollOfferOptions = oneToManyRelation(polls, pollOfferOptions)
    .via((p, poo) => p.id === poo.pollId)
  val subjectPollOfferOptions = oneToManyRelation(subjects, pollOfferOptions)
    .via((s, poo) => s.id === poo.subjectId)
  val offerPollOfferOptions = oneToManyRelation(offers, pollOfferOptions)
    .via((o, poo) => o.id === poo.offerId)
  val resultPollSelectedOptions = oneToManyRelation(results, pollSelectedOptions)
    .via((r, pso) => r.id === pso.pollResultId)
  val subjectPollSelectedOptions = oneToManyRelation(subjects, pollSelectedOptions)
    .via((s, pso) => s.id === pso.subjectId)
  val offerPollSelectedOptions = oneToManyRelation(offers, pollSelectedOptions)
    .via((o, pso) => o.id === pso.offerId)
  val courseOffer = oneToManyRelation(offers, courses)
    .via((o, c) => o.id === c.offerId)
  val nonCourseOffer = oneToManyRelation(offers, nonCourses)
    .via((o, nc) => o.id === nc.offerId)

  val studentsCareers = manyToManyRelation(students, careers)
    .via[StudentCareer]((s, c, sc) => (s.id === sc.studentId, c.id === sc.careerId))
  val adminsCareers = manyToManyRelation(admins, careers)
    .via[AdminCareer]((a, c, ac) => (a.id === ac.adminId, c.id === ac.careerId))

  findAllTablesFor(TableRow.getClass)
    .foreach(table =>
      on(table.asInstanceOf[Table[TableRow]])(t =>
        declare(
          t.id is(primaryKey, autoIncremented)
        )
      )
    )

  on(students)(s =>
    declare(
      s.username is(unique, indexed("idxStudentUsername")),
      s.email is unique,
      s.fileNumber is(unique, indexed("idxStudentFileNumber"))
    )
  )

  on(admins)(a =>
    declare(
      a.username is(unique, indexed("idxAdminUsername")),
      a.email is unique,
      a.fileNumber is(unique, indexed("idxAdminFileNumber"))
    )
  )

  on(careers)(c =>
    declare(
      c.shortName is(unique, indexed("idxCareerShortName"))
    )
  )

  on(polls)(p =>
    declare(
      columns(p.careerId, p.key) are(unique, indexed("idxCareerKey"))
    )
  )

  on(results)(r =>
    declare(
      columns(r.pollId, r.studentId) are(unique, indexed("idxStudentPoll"))
    )
  )

  on(subjects)(s =>
    declare(
      columns(s.careerId, s.shortName) are(unique, indexed("idxCarrerSubjectShortName"))
    )
  )

  on(courses)(c =>
    declare(
      c.offerId is(unique, indexed("idxCourseOffer"))
    )
  )

  on(nonCourses)(nc =>
    declare(
      nc.offerId is(unique, indexed("idxNonCourseOffer"))
    )
  )

  on(pollSubjectOption)(ps =>
    declare(
      columns(ps.pollId, ps.subjectId) are(unique, indexed("idxPollSubjectOptionCompositeKey"))
    )
  )

  on(pollOfferOptions)(poo =>
    declare(
      columns(poo.pollId, poo.subjectId, poo.offerId) are(unique, indexed("idxPollOfferOptionCompositeKey"))
    )
  )

  on(pollSelectedOptions)(pso =>
    declare(
      columns(pso.pollResultId, pso.subjectId, pso.offerId) are(unique, indexed("idxPollSelectedOptionCompositeKey"))
    )
  )

}
