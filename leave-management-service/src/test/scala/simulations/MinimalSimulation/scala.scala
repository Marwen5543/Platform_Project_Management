package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class MinimalSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://192.168.50.4:30081")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val scn = scenario("Health Check Test")
    .exec(
      http("Actuator Health")
        .get("/leave-service/actuator/health")
        .check(status.in(200, 503))
    )
    .pause(1.second)
    .exec(
      http("Service Info")
        .get("/leave-service/actuator/info")
        .check(status.in(200, 404))
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
    .maxDuration(30.seconds)
}