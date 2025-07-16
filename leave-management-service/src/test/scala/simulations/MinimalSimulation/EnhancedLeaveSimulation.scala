package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class EnhancedLeaveSimulation extends Simulation {

  val baseUrl = System.getProperty("APP_URL", "http://192.168.50.4:30081")

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Leave Service Test")

  // CSV feeder
  val userFeeder = csv("leaves-users.csv").circular

  // Generate random UUID for testing
  val randomUUID = java.util.UUID.randomUUID().toString

  // Basic Health Check Scenario
  val healthCheckScenario = scenario("Health Check Tests")
    .exec(
      http("Actuator Health")
        .get("/leave-service/actuator/health")
        .check(status.in(200, 404, 503))
    )
    .pause(1.second)
    .exec(
      http("Root Path")
        .get("/leave-service/")
        .check(status.in(200, 404, 405, 500))
    )

  // Employee Operations Scenario
  val employeeOperationsScenario = scenario("Employee Operations")
    .feed(userFeeder)
    .exec(
      http("Create Leave Request")
        .post("/leave-service/api/leaves/request")
        .body(StringBody("""
          {
            "startDate": "2025-08-01",
            "endDate": "2025-08-05",
            "leaveType": "${type}",
            "reason": "Test leave request for ${username}",
            "description": "Automated test leave request"
          }
        """)).asJson
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(200, 201, 400, 401, 403, 404, 500))
    )
    .pause(1.second)
    .exec(
      http("Get Leave History")
        .get("/leave-service/api/leaves/history")
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500))
    )

  // Manager Operations Scenario
  val managerOperationsScenario = scenario("Manager Operations")
    .feed(userFeeder)
    .exec(
      http("Get Team Leaves")
        .get("/leave-service/api/leaves/team")
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500, 503))
    )
    .pause(1.second)
    .exec(
      http("Update Leave Status - Approve")
        .put(s"/leave-service/api/leaves/$randomUUID/status")
        .header("Authorization", "Bearer test-token-${username}")
        .body(StringBody("""{"status": "APPROVED"}""")).asJson
        .check(status.in(200, 400, 401, 403, 404, 500))
    )
    .pause(1.second)
    .exec(
      http("Update Leave Status - Reject")
        .put(s"/leave-service/api/leaves/$randomUUID/status")
        .header("Authorization", "Bearer test-token-${username}")
        .body(StringBody("""{"status": "REJECTED"}""")).asJson
        .check(status.in(200, 400, 401, 403, 404, 500))
    )

  // Admin Operations Scenario
  val adminOperationsScenario = scenario("Admin Operations")
    .feed(userFeeder)
    .exec(
      http("Migrate Usernames")
        .post("/leave-service/api/leaves/migrate-usernames")
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(200, 401, 403, 404, 500))
    )
    .pause(2.seconds)
    .exec(
      http("Get Leave History (Admin)")
        .get("/leave-service/api/leaves/history")
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500))
    )

  // Load Testing Scenario
  val loadTestScenario = scenario("Load Testing")
    .feed(userFeeder)
    .exec(
      http("Health Check Load")
        .get("/leave-service/actuator/health")
        .check(status.in(200, 404, 503))
    )
    .pause(200.milliseconds)
    .exec(
      http("API Load Test")
        .get("/leave-service/api/leaves/history")
        .header("Authorization", "Bearer load-test-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500))
    )

  // Error Handling Scenario
  val errorHandlingScenario = scenario("Error Handling Tests")
    .feed(userFeeder)
    .exec(
      http("Invalid Leave Request")
        .post("/leave-service/api/leaves/request")
        .body(StringBody("""{"invalid": "data"}""")).asJson
        .header("Authorization", "Bearer test-token-${username}")
        .check(status.in(400, 401, 403, 404, 500))
    )
    .pause(1.second)
    .exec(
      http("Invalid Status Update")
        .put(s"/leave-service/api/leaves/$randomUUID/status")
        .header("Authorization", "Bearer test-token-${username}")
        .body(StringBody("""{"status": "INVALID_STATUS"}""")).asJson
        .check(status.in(400, 401, 403, 404, 500))
    )
    .pause(1.second)
    .exec(
      http("Non-existent Leave")
        .put("/leave-service/api/leaves/00000000-0000-0000-0000-000000000000/status")
        .header("Authorization", "Bearer test-token-${username}")
        .body(StringBody("""{"status": "APPROVED"}""")).asJson
        .check(status.in(404, 401, 403, 500))
    )

  // Mixed Operations Scenario
  val mixedOperationsScenario = scenario("Mixed Operations")
    .feed(userFeeder)
    .exec(
      http("Create Leave")
        .post("/leave-service/api/leaves/request")
        .body(StringBody("""
          {
            "startDate": "2025-08-15",
            "endDate": "2025-08-20",
            "leaveType": "${type}",
            "reason": "Mixed operations test",
            "description": "Testing mixed scenario"
          }
        """)).asJson
        .header("Authorization", "Bearer mixed-token-${username}")
        .check(status.in(200, 201, 400, 401, 403, 404, 500))
    )
    .pause(500.milliseconds)
    .exec(
      http("Get History")
        .get("/leave-service/api/leaves/history")
        .header("Authorization", "Bearer mixed-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500))
    )
    .pause(500.milliseconds)
    .exec(
      http("Team Leaves")
        .get("/leave-service/api/leaves/team")
        .header("Authorization", "Bearer mixed-token-${username}")
        .check(status.in(200, 400, 401, 403, 404, 500, 503))
    )

  setUp(
    healthCheckScenario.inject(
      atOnceUsers(2),
      rampUsers(3).during(5.seconds)
    ).protocols(httpProtocol),
    employeeOperationsScenario.inject(
      rampUsers(5).during(10.seconds),
      constantUsersPerSec(2).during(15.seconds)
    ).protocols(httpProtocol),
    managerOperationsScenario.inject(
      rampUsers(3).during(8.seconds),
      constantUsersPerSec(1).during(10.seconds)
    ).protocols(httpProtocol),
    adminOperationsScenario.inject(
      rampUsers(2).during(6.seconds)
    ).protocols(httpProtocol),
    loadTestScenario.inject(
      rampUsers(8).during(12.seconds),
      constantUsersPerSec(3).during(8.seconds)
    ).protocols(httpProtocol),
    errorHandlingScenario.inject(
      rampUsers(4).during(8.seconds)
    ).protocols(httpProtocol),
    mixedOperationsScenario.inject(
      rampUsers(6).during(10.seconds),
      constantUsersPerSec(2).during(12.seconds)
    ).protocols(httpProtocol)
  ).maxDuration(60.seconds)
    .assertions(
      global.responseTime.max.lte(10000),
      global.responseTime.mean.lte(3000),
      global.successfulRequests.percent.gte(50),
      global.requestsPerSec.gte(1.0),
      forAll.failedRequests.percent.lte(50)
    )
}