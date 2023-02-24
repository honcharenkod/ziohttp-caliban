package dao.repositories

import dao.models._
import dao._
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._

class ProfileRepositoryImpl(ctx: Quill.Postgres[SnakeCase],
                            userDAO: UserDaoImpl,
                            authInfoDAO: AuthInfoDAOImpl,
                            profilePhotoDAO: ProfilePhotoDAOImpl) extends ProfileRepository.Service {

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

  override def uploadProfilePhoto(userId: Long, data: Chunk[Byte], mimeType: String): Task[ProfilePhoto] =
    profilePhotoDAO.create(
      ProfilePhoto(
        0,
        userId,
        data,
        mimeType
      )
    )
}

object ProfileRepositoryImpl {
  val live = ZLayer {
    for {
      ctx <- ZIO.service[Quill.Postgres[SnakeCase]]
      userDAO <- ZIO.service[UserDaoImpl]
      authInfoDAO <- ZIO.service[AuthInfoDAOImpl]
      profilePhotoDAO <- ZIO.service[ProfilePhotoDAOImpl]
    } yield new ProfileRepositoryImpl(ctx, userDAO, authInfoDAO, profilePhotoDAO)
  }
}