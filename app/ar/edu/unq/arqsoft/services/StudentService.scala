package ar.edu.unq.arqsoft.services

import ar.edu.unq.arqsoft.api._
import ar.edu.unq.arqsoft.maybe.Maybe
import ar.edu.unq.arqsoft.model.Student
import ar.edu.unq.arqsoft.repository.StudentRepository
import ar.edu.unq.arqsoft.security.RoleStudent
import com.google.inject.Inject

class StudentService @Inject()(studentRepository: StudentRepository
                              ) extends UserService[Student](studentRepository, RoleStudent) {
  override protected def customClaims(user: Student): Map[String, Any] =
    super.customClaims(user) + ("fileNumber" -> user.fileNumber)


  protected def toDTO(user: Student): UserDTO = user.as[StudentDTO]

  def create(dto: CreateStudentDTO): Maybe[StudentDTO] = {
    val newModel = dto.asModel
    for {
      _ <- studentRepository.save(newModel)
    } yield newModel.as[StudentDTO]
  }

  def all: Maybe[Iterable[PartialStudentDTO]] =
    studentRepository.all().mapAs[PartialStudentDTO]

  def byFileNumber(fileNumber: Int): Maybe[StudentDTO] =
    studentRepository.byFileNumber(fileNumber).as[StudentDTO]

}
