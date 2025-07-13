package company.leave_management_service.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.time.LocalDate
import scala.util.Random

class LeaveSimulation extends Simulation {

  // --- Configuration ---
  val appUrl = System.getProperty("APP_URL", "http://localhost:8088") // Default for local runs
  val servicePrefix = "/leave-service" // API Gateway route for this service

  // NOTE: This assumes the user-service is also available via the gateway for login
  val userLoginUrl = System.getProperty("APP_URL", "http://localhost:8085")
  val userLoginPrefix = "/user-service"

  val testUser = "demo"
  val testPassword = "demo"

  // --- HTTP Protocol Setup ---
  val httpProtocol = http
    .baseUrl(appUrl) // Uses the gateway URL from CI
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // --- Automated Token Fetch Logic (via user-service on the gateway) ---
  val getJwtToken = exec(
    http("Get User Token via Gateway")
      .post(s"$userLoginUrl$userLoginPrefix/api/users/login")
      .body(StringBody(
        s"""{
           |  "username": "$testUser",
           |  "password": "$testPassword"
           |}""".stripMargin
      )).asJson
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("jwtToken"))
  )

  // --- Feeder for Dynamic Dates ---
  val dateFeeder = Iterator.continually {
    val startDate = LocalDate.now().plusDays(Random.nextInt(30) + 10)
    val endDate = startDate.plusDays(Random.nextInt(5) + 1)
    Map(
      "startDate" -> startDate.toString,
      "endDate" -> endDate.toString
    )
  }

  // --- SCENARIO DEFINITION ---
  val employeeLeaveScenario = scenario("Employee Requests Leave")
    .exec(getJwtToken) // Get a token by logging into user-service
    .feed(dateFeeder)
    .exec(
      http("Create Leave Request via Gateway")
        .post(s"$servicePrefix/api/leaves/request")
        .header("Authorization", "Bearer #{jwtToken}")
        .body(StringBody(
          """{
            |  "startDate": "#{startDate}",
            |  "endDate": "#{endDate}",
            |  "type": { "name": "VACATION" },
            |  "reason": "Gatling performance test"
            |}""".stripMargin
        )).asJson
        .check(status.is(200))
    )
    .pause(3.seconds)
    .exec(
      http("Get Leave History via Gateway")
        .get(s"$servicePrefix/api/leaves/history")
        .header("Authorization", "Bearer #{jwtToken}")
        .check(status.is(200))
    )

  // --- LOAD SETUP ---
  setUp(
    employeeLeaveScenario.inject(
      rampUsers(10).during(5.seconds)
    )
  ).protocols(httpProtocol)
}