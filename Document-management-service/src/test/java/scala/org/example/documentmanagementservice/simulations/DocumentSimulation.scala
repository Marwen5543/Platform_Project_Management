package org.example.documentmanagementservice.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class DocumentSimulation extends Simulation {

  // 1. Common HTTP Configuration
  val httpProtocol = http
    // CRITICAL FIX: Point to your application's port, not Keycloak's port
    .baseUrl("http://localhost:8090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test") // Good practice to identify test traffic

  // 2. Authentication: You MUST get a valid token
  // To get a token:
  //   - Log in to your application through the frontend (port 4200)
  //   - Use browser developer tools (Network tab) to find a request to your backend
  //   - Copy the entire "Bearer <token>" value from the Authorization header
  // Inside your DocumentSimulation class

  val jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJUQlVFWGpVcWhFVFV0aUlrcHVKNGVzcXRTaW1zYTNOMWdkOUNFMVNCODNvIn0.eyJleHAiOjE3NTE2MzgyMzEsImlhdCI6MTc1MTYzNzkzMSwiYXV0aF90aW1lIjoxNzUxNjM3OTMxLCJqdGkiOiJmMzU3NGRmZC1iNDUyLTQ3MzctYWJiYy1lYTQ1NmFkMGM2ZjQiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvcmVhbG1zL1R1bmlzeXMiLCJhdWQiOiJkZW1vLXJlc3QtYXBpIiwic3ViIjoiNTUyZGI4OTctNGMyOS00NGZlLTg2YTQtZWQ2ZGM2ZjM1OTJmIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYW5ndWxhci1hcHAiLCJub25jZSI6IjZlNDVjMGJlLTQ4OTktNGJmYS05NTE4LTYwOWUxNDU4MDQzMSIsInNlc3Npb25fc3RhdGUiOiIxZDY4ZjZhMy0xZjM0LTQyYzEtYTMxZC0xMDU2MjBhMWFlMWQiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6NDIwMCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiRU1QTE9ZRUUiXX0sInJlc291cmNlX2FjY2VzcyI6eyJkZW1vLXJlc3QtYXBpIjp7InJvbGVzIjpbImNsaWVudF9hZG1pbiJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJzaWQiOiIxZDY4ZjZhMy0xZjM0LTQyYzEtYTMxZC0xMDU2MjBhMWFlMWQiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJkZW1vIGRlbW8iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJkZW1vIiwiZ2l2ZW5fbmFtZSI6ImRlbW8iLCJmYW1pbHlfbmFtZSI6ImRlbW8iLCJlbWFpbCI6Im1hbWxvdWttYXJ3ZW4yMTlAZ21haWwuY29tIn0.TLyivLlYFPxIx1newE1dyxK7lnVJsqbYtw9RoaSz1KUAJ9esVTFNZ1eT_nWw59EWlYKCtWFrCgAiRDGAovYDRzIapPYZJPZA5yCfxAkgFhQqSRRhcPhKRnb20I5JujRp5yynqPxWHEgQ1bugwl-mwE_RjHKx_MqKRuknrijQwFuEXrHKtV77m0WolPIQyHNvP27Fh-h_Oxfzrvn6gpvOjehlTaVvj6A5kaVUK5Ado5VyLx3Pcl6sbk8Cm6HhAI-Xq1cXVXbF7cz9gMHjobnIw4tv4EGG3ZT34zQJNG1BCAvo2P-7WGH79v4izZSPBtdT3H_C53rtttcRa-dBQgMXTA"

  val authHeaders = Map(
    "Authorization" -> s"Bearer $jwtToken"
  )

  // 3. Define the Scenario (User Journey)
  val employeeScenario = scenario("Employee Document Journey")
    .exec(
      // Step 1: Request a Payslip
      http("Request Payslip")
        .post("/api/documents/request")
        .headers(authHeaders)
        .body(StringBody(
          """
            |{
            |  "documentType": "PAYSLIP",
            |  "monthYear": "2025-07"
            |}
            |""".stripMargin
        )).asJson // Tell Gatling the body is JSON
        .check(status.is(200)) // Check for a successful response
        .check(jsonPath("$.id").saveAs("documentId")) // Save the returned ID for later
    )
    .pause(2.seconds) // Simulate the user waiting for 2 seconds

    .exec(
      // Step 2: Check Document History
      http("Get Document History")
        .get("/api/documents/history")
        .headers(authHeaders)
        .check(status.is(200))
    )

  // NOTE: The download step will fail unless the document is approved first.
  // For a more realistic test, you would need a separate "HR Admin" scenario
  // that approves the document using the saved 'documentId'.
  // For now, we can comment it out or expect it to fail if not approved.
  /*
  .pause(5.seconds)
  .exec(
      // Step 3: Download the Document (after it's approved)
      // This uses the 'documentId' we saved from the first request
      http("Download Document")
          .get("/api/documents/download/${documentId}")
          .headers(authHeaders)
          .check(status.is(200))
  )
  */


  // 4. Define the Load
  setUp(
    employeeScenario.inject(
      // Start with 1 user to make sure the script works, then increase the load
      atOnceUsers(1)
      // atOnceUsers(10) // Then try with 10 users
    )
  ).protocols(httpProtocol)
}