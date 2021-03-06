package ar.edu.unq.arqsoft.repository

import ar.edu.unq.arqsoft.DAOs._
import ar.edu.unq.arqsoft.maybe.{EntityNotFound, Maybe}
import ar.edu.unq.arqsoft.model.TableRow.KeyType
import ar.edu.unq.arqsoft.model._
import ar.edu.unq.arqsoft.utils.MultiResultChecker
import javax.inject.Inject
import org.squeryl.Query

class ModelRepository[T <: TableRow](dao: ModelDAO[T])
  extends Repository[T, KeyType](dao) {
  protected def multiResult[A](query: Query[A], checker: Iterable[A] => Maybe[Iterable[A]]): Maybe[Iterable[A]] =
    inTransaction(query.toList).flatMap(checker)
}

class UserRepository[T <: User with TableRow](dao: UserDAO[T]) extends ModelRepository[T](dao) {
  def notFoundByUsername(username: String): EntityNotFound =
    notFoundBy("username", username)

  def byUsername(username: String): Maybe[T] =
    singleResult(dao.byUsername(username), notFoundByUsername(username))
}

class StudentRepository @Inject()(dao: StudentDAO)
  extends UserRepository[Student](dao) {
  def notFoundByFileNumber(fileNumber: Int): EntityNotFound =
    notFoundBy("file number", fileNumber)

  def byFileNumber(fileNumber: Int): Maybe[Student] =
    singleResult(dao.byFileNumber(fileNumber), notFoundByFileNumber(fileNumber))
}

class AdminRepository @Inject()(dao: AdminDAO)
  extends UserRepository[Admin](dao) {
  def notFoundByFileNumber(fileNumber: Int): EntityNotFound =
    notFoundBy("file number", fileNumber)

  def byFileNumber(fileNumber: Int): Maybe[Admin] =
    singleResult(dao.byFileNumber(fileNumber), notFoundByFileNumber(fileNumber))
}

class CareerRepository @Inject()(dao: CareerDAO)
  extends ModelRepository[Career](dao) {
  def notFoundByShortName(shortName: String): EntityNotFound =
    notFoundBy("short name", shortName)

  def byShortName(shortName: String): Maybe[Career] =
    singleResult(dao.byShortName(shortName), notFoundByShortName(shortName))

  def getOfAdmin(admin: Admin): Maybe[Iterable[Career]] = inTransaction {
    dao.getOfAdmin(admin.id).toList
  }
}

class SubjectRepository @Inject()(dao: SubjectDAO)
  extends ModelRepository[Subject](dao) {
  def notFoundByShortNameOfCareer(shortName: String, career: Career): EntityNotFound =
    notFoundBy("(career, short name)", (career.shortName, shortName))

  def byShortNameOfCareer(shortNames: Iterable[String], career: Career): Maybe[Iterable[Subject]] =
    multiResult(dao.byShortNameOfCareer(shortNames, career.id),
      new MultiResultChecker[Subject, String](shortNames,
        subjects => shortName => subjects.find(_.shortName == shortName) match {
          case Some(_) => None
          case None => Some(notFoundByShortNameOfCareer(shortName, career))
        }
      )
    )

  def getOfPoll(poll: Poll): Maybe[Iterable[Subject]] = inTransaction {
    dao.getOfPoll(poll.id).toList
  }
}

class OfferRepository @Inject()(dao: OfferDAO)
  extends ModelRepository[OfferOptionBase](dao)

class CourseRepository @Inject()(dao: CourseDAO)
  extends ModelRepository[Course](dao) {
  def getOfPollResultBySubjectName(pollResult: PollResult, subjectShortNames: Iterable[String]): Maybe[Iterable[(Subject, Course)]] = inTransaction {
    dao.getOfPollBySubjectName(pollResult.pollId, subjectShortNames).toList
  }

  def tallyByPoll(poll: Poll): Maybe[Iterable[(Subject, Course, Student)]] = inTransaction {
    dao.tallyByPoll(poll.id).toList
  }
}

class NonCourseRepository @Inject()(dao: NonCourseDAO)
  extends ModelRepository[NonCourseOption](dao) {
  def notFoundByKey(key: String): EntityNotFound =
    notFoundBy("key", key)

  def byKey(keys: Iterable[String]): Maybe[Iterable[NonCourseOption]] = inTransaction {
    dao.byKey(keys).toList
  }

  def byKey(key: String): Maybe[NonCourseOption] =
    singleResult(dao.byKey(key), notFoundByKey(key))

  def getOfPollResultBySubjectName(pollResult: PollResult, subjectShortNames: Iterable[String]): Maybe[Iterable[(Subject, NonCourseOption)]] = inTransaction {
    dao.getOfPollBySubjectName(pollResult.pollId, subjectShortNames).toList
  }

  def tallyByPoll(poll: Poll): Maybe[Iterable[(Subject, NonCourseOption, Student)]] = inTransaction {
    dao.tallyByPoll(poll.id).toList
  }
}

class ScheduleRepository @Inject()(dao: ScheduleDAO)
  extends ModelRepository[Schedule](dao)

class PollRepository @Inject()(dao: PollDAO)
  extends ModelRepository[Poll](dao) {
  def notFoundByKeyOfCareer(career: Career, pollKey: String): EntityNotFound =
    notFoundBy("(career, key)", (career.shortName, pollKey))

  def byKeyOfCareer(pollKey: String, career: Career): Maybe[Poll] =
    singleResult(dao.byKeyOfCareer(pollKey, career.id), notFoundByKeyOfCareer(career, pollKey))

  def getOfCareer(career: Career): Maybe[Iterable[Poll]] = inTransaction {
    dao.getOfCareer(career.id).toList
  }

  def getOfStudent(student: Student): Maybe[Iterable[Poll]] = inTransaction {
    dao.getOfStudent(student.id).toList
  }

  def getOfAdmin(admin: Admin): Maybe[Iterable[Poll]] = inTransaction {
    dao.getOfAdmin(admin.id).toList
  }
}

class PollResultRepository @Inject()(dao: PollResultDAO)
  extends ModelRepository[PollResult](dao) {
  def notFoundByStudentAndPoll(student: Student, career: Career, pollKey: String): EntityNotFound =
    notFoundBy("(student, poll)", (student.email, (career.shortName, pollKey)))

  def byStudentAndPoll(student: Student, poll: Poll, career: Career): Maybe[PollResult] =
    singleResult(dao.byStudentAndPoll(student.id, poll.id), notFoundByStudentAndPoll(student, career, poll.key))
}

class PollSubjectOptionRepository @Inject()(dao: PollSubjectOptionDAO)
  extends ModelRepository[PollSubjectOption](dao)

class PollOfferOptionRepository @Inject()(dao: PollOfferOptionDAO)
  extends ModelRepository[PollOfferOption](dao)

class PollSelectedOptionRepository @Inject()(dao: PollSelectedOptionDAO)
  extends ModelRepository[PollSelectedOption](dao) {
  def getOfAlreadyPassedSubjects(pollResult: PollResult): Maybe[Iterable[PollSelectedOption]] = inTransaction {
    dao.getByPollAndOfPassedSubjectsOfStudent(pollResult.id, pollResult.studentId).toList
  }

  def getOfPollResultBySubjectName(pollResult: PollResult, subjectShortNames: Iterable[String]): Maybe[Iterable[(Subject, PollSelectedOption)]] = inTransaction {
    dao.getOfPollResult(pollResult.id, subjectShortNames).toList
  }
}

class StudentCareerRepository @Inject()(dao: StudentCareerDAO)
  extends ModelRepository[StudentCareer](dao)

class AdminCareerRepository @Inject()(dao: AdminCareerDAO)
  extends ModelRepository[AdminCareer](dao)










