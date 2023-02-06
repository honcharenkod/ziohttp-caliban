package dao.repositories

import dao.models.{AuthInfo, User}
import dao.{AuthInfoDAOImpl, UserDaoImpl}
import zio._

class ProfileRepositoryImpl(userDAO: UserDaoImpl,
                            authInfoDAO: AuthInfoDAOImpl) extends ProfileRepository.Service {

  override def signUp(email: String, name: String, surname: String, password: String): Task[User] =
    for {
      user <- userDAO.create(User(0, email, name, surname))
      _ <- authInfoDAO.create(AuthInfo(0, user.id, password))
    } yield user
}

object ProfileRepositoryImpl {
  val live = ZLayer {
    for {
      userDAO <- ZIO.service[UserDaoImpl]
      authInfoDAO <- ZIO.service[AuthInfoDAOImpl]
    } yield new ProfileRepositoryImpl(userDAO, authInfoDAO)
  }
}