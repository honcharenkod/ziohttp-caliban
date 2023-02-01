package graphql

import caliban.GraphQL.graphQL
import caliban.schema.Annotations.GQLDescription
import caliban.schema.GenericSchema
import caliban.wrappers.ApolloTracing.apolloTracing
import caliban.wrappers.Wrapper.OverallWrapper
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQL, GraphQLRequest, GraphQLResponse, RootResolver}
import dao.models.User
import graphql.ProfileService.ProfileService
import zio.Console.{printLine, printLineError}
import zio._

import scala.language.postfixOps
object ProfileApi extends GenericSchema[ProfileService] {

  case class Queries(
                      @GQLDescription("Return user info by user id")
                      userInfo: Id => RIO[ProfileService, Option[User]]
                    )

  case class Id(id: Long)

  val api: GraphQL[ProfileService] =
    graphQL(
      RootResolver(
        Queries(
          args => ProfileService.getUserInfo(args.id)
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
