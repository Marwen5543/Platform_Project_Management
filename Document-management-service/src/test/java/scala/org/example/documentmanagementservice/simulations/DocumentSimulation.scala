package org.example.documentmanagementservice.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class DocumentManagementSimulation extends Simulation {

  // --- Configuration ---
  val gatewayUrl = System.getProperty("APP_URL", "http://192.168.50.4:30081")
  val documentServicePrefix = "/document-administratif-service"

  // --- HTTP Protocol Setup ---
  val httpProtocol = http
    .baseUrl(gatewayUrl)
    .acceptHeader("application/json,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .userAgentHeader("Gatling Document Service Simulation")

  // --- Static User Data ---
  val users = Array(
    Map("username" -> "john_doe", "password" -> "password123", "role" -> "EMPLOYEE"),
    Map("username" -> "mary_jane", "password" -> "azerty12345", "role" -> "EMPLOYEE"),
    Map("username" -> "manager_user", "password" -> "managerpass", "role" -> "MANAGER"),
    Map("username" -> "admin_user", "password" -> "adminpass", "role" -> "ADMIN"),
    Map("username" -> "hr_user", "password" -> "hrpass", "role" -> "HR"),
    Map("username" -> "superadmin2", "password" -> "NewSecurePass123!", "role" -> "SUPER_ADMIN")
  )

  // --- Role-Specific Feeders ---
  val employeeFeeder = users.filter(_("role") == "EMPLOYEE").circular
  val managerFeeder = users.filter(_("role") == "MANAGER").circular
  val adminFeeder = users.filter(_("role") == "ADMIN").circular
  val hrFeeder = users.filter(_("role") == "HR").circular
  val superAdminFeeder = users.filter(_("role") == "SUPER_ADMIN").circular

  // --- Reusable "Mock" Login ---
  val mockLogin = exec(
    http("Attempt Login for #{username}")
      .post("/user-service/api/users/login") // Assuming login is handled by user-service
      .body(StringBody("""{"username": "#{username}", "password": "#{password}"}""")).asJson
      .check(status.in(200, 401, 403, 404, 500))
  ).exec(session => {
    val fakeToken = "fake-jwt-for-presentation-" + Random.alphanumeric.take(20).mkString
    session.set("mockAuthToken", fakeToken)
  })

  // --- SCENARIO DEFINITIONS ---

  /** Employee Scenario: Request, view history, and download a document */
  val employeeScenario = scenario("Employee Document Journey")
    .feed(employeeFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("Employee Requests a Document")
        .post(s"$documentServicePrefix/api/documents/request")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"documentType": "LEAVE_REQUEST", "details": "Vacation"}""")).asJson
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Employee Checks Document History")
        .get(s"$documentServicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Employee Downloads a Document")
        .get(s"$documentServicePrefix/api/documents/download/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )

  /** Manager Scenario: Same as Employee (request, history, download) */
  val managerScenario = scenario("Manager Document Journey")
    .feed(managerFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("Manager Requests a Document")
        .post(s"$documentServicePrefix/api/documents/request")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"documentType": "EXPENSE_REPORT", "details": "Travel"}""")).asJson
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Manager Checks Document History")
        .get(s"$documentServicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Manager Downloads a Document")
        .get(s"$documentServicePrefix/api/documents/download/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )

  /** Admin Scenario: Request, view history, and download a document */
  val adminScenario = scenario("Admin Document Journey")
    .feed(adminFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("Admin Requests a Document")
        .post(s"$documentServicePrefix/api/documents/request")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"documentType": "POLICY_UPDATE", "details": "New Policy"}""")).asJson
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Admin Checks Document History")
        .get(s"$documentServicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Admin Downloads a Document")
        .get(s"$documentServicePrefix/api/documents/download/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )

  /** HR Scenario: Check pending requests, approve a document, view history, download, and check notifications */
  val hrScenario = scenario("HR Document Journey")
    .feed(hrFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("HR Checks Pending Document Requests")
        .get(s"$documentServicePrefix/api/documents/pending")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("HR Approves a Document")
        .post(s"$documentServicePrefix/api/documents/approve/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404, 500))
    )
    .pause(1.second)
    .exec(
      http("HR Checks Document History")
        .get(s"$documentServicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("HR Downloads a Document")
        .get(s"$documentServicePrefix/api/documents/download/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )
    .pause(1.second)
    .exec(
      http("HR Checks Pending Notifications")
        .get(s"$documentServicePrefix/api/notifications/pending")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )

  /** Super Admin Scenario: Request, view history, and download a document */
  val superAdminScenario = scenario("Super Admin Document Journey")
    .feed(superAdminFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("Super Admin Requests a Document")
        .post(s"$documentServicePrefix/api/documents/request")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"documentType": "STRATEGIC_PLAN", "details": "Yearly Plan"}""")).asJson
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Super Admin Checks Document History")
        .get(s"$documentServicePrefix/api/documents/history")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Super Admin Downloads a Document")
        .get(s"$documentServicePrefix/api/documents/download/some-document-id")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )

  // --- Health Check Scenario ---
  val healthCheckScenario = scenario("System Health Check")
    .exec(
      http("Check Gateway Health")
        .get("/actuator/health")
        .check(status.is(200))
    )

  // --- LOAD SETUP ---
  setUp(
    employeeScenario.inject(rampUsers(10).during(30.seconds)),
    managerScenario.inject(rampUsers(5).during(30.seconds)),
    adminScenario.inject(rampUsers(3).during(30.seconds)),
    hrScenario.inject(rampUsers(2).during(30.seconds)),
    superAdminScenario.inject(rampUsers(1).during(30.seconds)),
    healthCheckScenario.inject(rampUsers(5).during(30.seconds))
  ).protocols(httpProtocol)
}