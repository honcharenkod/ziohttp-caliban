package dao.repositories

import dao.models.{AuthInfo, Role, User}
import dao.{AuthInfoDAOImpl, UserDaoImpl}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._

class ProfileRepositoryImpl(ctx: Quill.Postgres[SnakeCase],
                            userDAO: UserDaoImpl,
                            authInfoDAO: AuthInfoDAOImpl) extends ProfileRepository.Service {

  import ctx._

  private val users = userDAO.quoted
  private val authInfo = authInfoDAO.quoted

  override def signUp(email: String, name: String, surname: String, password: String): Task[User] =
    for {
      user <- userDAO.create(User(0, email, name, surname, Role.User))
      _ <- authInfoDAO.create(AuthInfo(0, user.id, password))
    } yield user

  override def getUserWithAuthInfoByEmail(email: String): Task[Option[(User, AuthInfo)]] =
    for {
      password <-
        run(
          users
            .filter(_.email == lift(email))
            .join(authInfo).on(_.id == _.userId)
        ).map(_.headOption)
    } yield password
}

object ProfileRepositoryImpl {
  val live = ZLayer {
    for {
      ctx <- ZIO.service[Quill.Postgres[SnakeCase]]
      userDAO <- ZIO.service[UserDaoImpl]
      authInfoDAO <- ZIO.service[AuthInfoDAOImpl]
    } yield new ProfileRepositoryImpl(ctx, userDAO, authInfoDAO)
  }
}