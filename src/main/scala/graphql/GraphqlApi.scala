package graphql

import caliban.GraphQL.graphQL
import caliban.schema.Annotations.GQLDescription
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrappers._
import caliban.{GraphQL, RootResolver}
import dao.models.{Message, Role, User}
import graphql.GraphqlApi.Subscriptions
import graphql.GraphqlService.ProfileService
import graphql.auth.Auth
import graphql.auth.Auth.{Auth, authorize}
import models.Notification
import zio._
import zio.stream.ZStream

import scala.language.postfixOps

object GraphqlApi extends GenericSchema[ProfileService with Auth] {

  case class Queries(@GQLDescription("Get auth token")
                     singIn: SignIn => RIO[ProfileService, String],
                     myProfile: RIO[Auth, User])

  case class Mutations(
                        @GQLDescription("Create a new user profile")
                        singUp: SignUp => RIO[ProfileService, User],
                        @GQLDescription("Send message by user id")
                        sendMessage: SendMessage => RIO[ProfileService with Auth, Message]
                      )

  case class Subscriptions(
                            subscribeNotifications: ZStream[Auth with ProfileService, Throwable, Notification]
                          )

  implicit val roleSchema: Schema[Any, Role] = Schema.gen
  case class SignUp(email: String, name: String, surname: String, password: String)
  case class SignIn(email: String, password: String)
  case class SendMessage(text: String, recipientId: Long)

  val api: GraphQL[ProfileService with Auth] =
    graphQL(
      RootResolver(
        Queries(
          args => GraphqlService.signIn(args.email, args.password),
          authorize()(Auth.user)
        ),
        Mutations(
          args => GraphqlService.signUp(args.email, args.name, args.surname, args.password),
          args => authorize()(GraphqlService.sendMessage(args.text, args.recipientId))
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