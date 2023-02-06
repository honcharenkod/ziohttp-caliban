package graphql

import caliban.GraphQL.graphQL
import caliban.schema.Annotations.GQLDescription
import caliban.schema.GenericSchema
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrapper.OverallWrapper
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQL, GraphQLRequest, GraphQLResponse, RootResolver}
import dao.models.User
import graphql.ProfileApi.Mutations
import graphql.ProfileService.ProfileService
import zio.Console.{printLine, printLineError}
import zio._

import scala.language.postfixOps
object ProfileApi extends GenericSchema[ProfileService] {

  case class Queries(testQuery: UIO[String])

  case class Mutations(
                        @GQLDescription("Create a new user profile")
                        singUp: SignUp => RIO[ProfileService, User]
                      )

  case class SignUp(email: String, name: String, surname: String, password: String)

  val api: GraphQL[ProfileService] =
    graphQL(
      RootResolver(
        Queries(ZIO.succeed("OK")),
        Mutations(
          args => ProfileService.signUp(args.email, args.name, args.surname, args.password)
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
