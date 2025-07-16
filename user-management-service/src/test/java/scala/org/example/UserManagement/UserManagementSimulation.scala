package company.user_management_service.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class UserManagementSimulation extends Simulation {

  // --- Configuration ---
  val gatewayUrl = System.getProperty("APP_URL", "http://192.168.50.4:30081")

  // --- HTTP Protocol Setup ---
  val httpProtocol = http
    .baseUrl(gatewayUrl)
    .acceptHeader("application/json,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .userAgentHeader("Gatling B2B Simulation")

  // --- Static User Data ---
  val users = Array(
    Map("username" -> "john_doe", "password" -> "password123", "role" -> "EMPLOYEE"),
    Map("username" -> "mary_jane", "password" -> "azerty12345", "role" -> "EMPLOYEE"),
    Map("username" -> "demo", "password" -> "demo", "role" -> "EMPLOYEE"),
    Map("username" -> "astra_admin007", "password" -> "Secure@Admin#2025", "role" -> "ADMIN"),
    Map("username" -> "superadmin2", "password" -> "NewSecurePass123!", "role" -> "SUPER_ADMIN")
  )

  // --- Role-Specific Feeders ---
  val employeeFeeder = users.filter(_("role") == "EMPLOYEE").circular
  val adminFeeder = users.filter(_("role") == "ADMIN").circular
  val superAdminFeeder = users.filter(_("role") == "SUPER_ADMIN").circular

  // --- Reusable "Mock" Login ---
  val mockLogin = exec(
    http("Attempt Login for #{username}")
      .post("/user-service/api/users/login")
      .body(StringBody("""{"username": "#{username}", "password": "#{password}"}""")).asJson
      .check(status.in(200, 401, 403, 404, 500))
  ).exec(session => {
    val fakeToken = "fake-jwt-for-presentation-" + Random.alphanumeric.take(20).mkString
    session.set("mockAuthToken", fakeToken)
  })

  // --- SCENARIO DEFINITIONS ---
  val employeeScenario = scenario("Employee User Journey")
    .feed(employeeFeeder)
    .exec(mockLogin)
    .pause(1.second, 3.seconds)
    .exec(
      http("Employee Fetches Own Profile")
        .get("/user-service/api/users/me")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(2.seconds)
    .exec(
      http("Employee Checks Permissions")
        .get("/user-service/api/users/myRole")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )
    .pause(1.second)
    .exec(
      http("Employee Updates Own Profile")
        .put("/user-service/api/users/me")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"firstName": "Updated", "lastName": "Name"}""")).asJson
        .check(status.in(200, 401, 403))
    )

  val adminScenario = scenario("Admin User Journey")
    .feed(adminFeeder)
    .exec(mockLogin)
    .pause(1.second)
    .exec(
      http("Admin Fetches All Users List")
        .get("/user-service/api/users")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403))
    )


  val superAdminScenario = scenario("Super Admin Full Access Journey")
    .feed(superAdminFeeder)
    .exec(mockLogin)
    .pause(1.second)
    .exec(
      http("Super Admin Deletes a User")
        .delete("/user-service/api/users/some-user-id-to-delete")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .check(status.in(200, 401, 403, 404))
    )
    .pause(1.second)
    .exec(
      http("Admin Affect User to Project")
        .put("/user-service/api/users/some-user-id/role")
        .header("Authorization", "Bearer #{mockAuthToken}")
        .body(StringBody("""{"role": "ADMIN"}""")).asJson
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
    employeeScenario.inject(rampUsers(15).during(30.seconds)),
    adminScenario.inject(rampUsers(3).during(30.seconds)),
    superAdminScenario.inject(rampUsers(1).during(30.seconds)),
    healthCheckScenario.inject(rampUsers(5).during(30.seconds))
  ).protocols(httpProtocol)
}