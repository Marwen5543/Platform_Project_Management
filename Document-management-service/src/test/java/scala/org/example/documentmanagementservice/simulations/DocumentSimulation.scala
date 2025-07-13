package org.example.documentmanagementservice.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DocumentSimulation extends Simulation {

  // --- Configuration ---
  val appUrl = System.getProperty("APP_URL", "http://localhost:8090") // Default for local runs
  val servicePrefix = "/document-service" // API Gateway route for this service

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
    .userAgentHeader("Gatling Performance Test")

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

  // --- Scenario Definition ---
  val employeeScenario = scenario("Employee Document Journey")
    .exec(getJwtToken) // Get a token by logging into user-service
    .exec(
      http("Request Payslip via Gateway")
        .post(s"$servicePrefix/api/documents/request")
        .header("Authorization", "Bearer #{jwtToken}")
        .body(StringBody(
          """{
            |  "documentType": "PAYSLIP",
            |  "monthYear": "2025-07"
            |}""".stripMargin
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("documentId"))
    )
    .pause(2.seconds)
    .exec(
      http("Get Document History via Gateway")
        .get(s"$servicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{jwtToken}")
        .check(status.is(200))
    )

  // --- Load Setup ---
  setUp(
    employeeScenario.inject(
      atOnceUsers(10)
    )
  ).protocols(httpProtocol)
}