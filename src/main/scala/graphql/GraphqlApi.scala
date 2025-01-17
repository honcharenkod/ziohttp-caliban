package graphql

import caliban.schema.Annotations.GQLDescription
import caliban.schema.{GenericSchema, Schema}
import caliban.uploads._
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers._
import caliban.{GraphQL, RootResolver, graphQL}
import dao.models.{Message, Role, User}
import graphql.GraphqlService.GraphqlService
import graphql.auth.Auth
import graphql.auth.Auth.{Auth, authorize}
import models.Notification
import zio._
import zio.stream.ZStream
import caliban.schema.ArgBuilder.auto._

import scala.language.postfixOps

object GraphqlApi extends GenericSchema[GraphqlService with Auth with Uploads] {
  import auto._

  case class Queries(@GQLDescription("Get auth token")
                     singIn: SignIn => RIO[GraphqlService, String],
                     myProfile: RIO[Auth, User])

  case class Mutations(
                        @GQLDescription("Create a new user profile")
                        singUp: SignUp => RIO[GraphqlService, User],
                        @GQLDescription("Send message by user id")
                        sendMessage: SendMessage => RIO[GraphqlService with Auth, Message],
                        @GQLDescription("Upload profile photo")
                        uploadProfilePhoto: UploadProfilePhoto => RIO[GraphqlService with Auth with Uploads, Unit]
                      )

  case class Subscriptions(
                            subscribeNotifications: ZStream[Auth with GraphqlService, Throwable, Notification]
                          )

  case class SignUp(email: String, name: String, surname: String, password: String)

  case class SignIn(email: String, password: String)

  case class SendMessage(text: String, recipientId: Long)

  case class UploadProfilePhoto(photo: Upload)

  val api: GraphQL[GraphqlService with Auth with Uploads] =
    graphQL(
      RootResolver(
        Queries(
          args => GraphqlService.signIn(args.email, args.password),
          authorize()(Auth.user)
        ),
        Mutations(
          args => GraphqlService.signUp(args.email, args.name, args.surname, args.password),
          args => authorize()(GraphqlService.sendMessage(args.text, args.recipientId)),
          args => authorize()(GraphqlService.uploadProfilePhoto(args.photo))
        ),
        Subscriptions(
          GraphqlService.subscribeNotifications
        )
      )
    ) @@
      maxFields(200) @@ // query analyzer that limit query fields
      maxDepth(30) @@ // query analyzer that limit query depth
      timeout(3 seconds) @@ // wrapper that fails slow queries
      printSlowQueries(500 millis) @@ // wrapper that logs slow queries
      printErrors @@ // wrapper that logs errors
      apolloTracing // wrapper for https://github.com/apollographql/apollo-tracing
}