package company.leave_management_service.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.time.LocalDate

class LeaveSimulation extends Simulation {

  // --- Configuration ---
  // 2. FIX: Correct the port to match your application.properties
  val backendBaseUrl = "http://localhost:8088"
  val keycloakUrl = "http://localhost:8080"
  val keycloakRealm = "Tunisys"
  // 3. FIX: Use the frontend client ID for user login
  val keycloakClientId = "angular-app"
  val testUser = "demo"
  val testPassword = "demo"

  // --- HTTP Protocol Setup ---
  val httpProtocol = http
    .baseUrl(backendBaseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // --- Automated Token Fetch Logic ---
  val getJwtToken = exec(
    http("Get Keycloak Token")
      .post(s"$keycloakUrl/realms/$keycloakRealm/protocol/openid-connect/token")
      .asFormUrlEncoded
      .formParam("client_id", keycloakClientId)
      .formParam("username", testUser)
      .formParam("password", testPassword)
      .formParam("grant_type", "password")
      .check(status.is(200))
      .check(jsonPath("$.access_token").saveAs("jwtToken"))
  )

  // --- Feeder for Dynamic Dates ---
  val dateFeeder = Iterator.continually {
    val startDate = LocalDate.now().plusDays(scala.util.Random.nextInt(30) + 10)
    val endDate = startDate.plusDays(scala.util.Random.nextInt(5) + 1)
    Map(
      "startDate" -> startDate.toString,
      "endDate" -> endDate.toString
    )
  }

  // --- SCENARIO DEFINITION ---
  val employeeLeaveScenario = scenario("Employee Requests Leave")
    .exec(getJwtToken) // Get a fresh token for each user
    .feed(dateFeeder) // Get unique dates for this user's request
    .exec(
      http("Create Leave Request")
        .post("/api/leaves/request")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          // Using a more robust string format for the body
          s"""
             |{
             |  "startDate": "${"$"}{startDate}",
             |  "endDate": "${"$"}{endDate}",
             |  "type": { "name": "VACATION" },
             |  "reason": "Gatling performance test"
             |}
             |""".stripMargin
        )).asJson
        .check(status.is(200))
    )
    .pause(3.seconds)
    .exec(
      http("Get Leave History")
        .get("/api/leaves/history")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )

  // --- LOAD SETUP ---
  setUp(
    employeeLeaveScenario.inject(
      rampUsers(10).during(5.seconds)
    )
  ).protocols(httpProtocol)
}