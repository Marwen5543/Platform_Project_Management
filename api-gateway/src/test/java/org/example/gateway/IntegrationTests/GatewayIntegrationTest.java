package org.example.gateway.IntegrationTests;

import company.GatewayApplication;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = GatewayApplication.class,
        properties = {
                "spring.cloud.discovery.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureWebTestClient
public class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static MockWebServer mockUserService;
    private static MockWebServer mockDocumentService;
    private static MockWebServer mockLeaveService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Completely override the gateway routes configuration for testing
        // This approach replaces all routes with test-specific configurations

        // Route 0: User Service
        registry.add("spring.cloud.gateway.routes[0].id", () -> "user-service-test");
        registry.add("spring.cloud.gateway.routes[0].uri", () -> mockUserService.url("/").toString());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/user-service/**");
        registry.add("spring.cloud.gateway.routes[0].filters[0]", () -> "StripPrefix=1");

        // Route 1: Leave Management Service
        registry.add("spring.cloud.gateway.routes[1].id", () -> "leave-service-test");
        registry.add("spring.cloud.gateway.routes[1].uri", () -> mockLeaveService.url("/").toString());
        registry.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/leaves/**");
        registry.add("spring.cloud.gateway.routes[1].filters[0]", () -> "StripPrefix=2"); // Strip both /api/leaves
        registry.add("spring.cloud.gateway.routes[1].filters[1]", () -> "AddRequestHeader=X-Service-Name,leave-management");

        // Route 2: Document Management Service
        registry.add("spring.cloud.gateway.routes[2].id", () -> "document-service-test");
        registry.add("spring.cloud.gateway.routes[2].uri", () -> mockDocumentService.url("/").toString());
        registry.add("spring.cloud.gateway.routes[2].predicates[0]", () -> "Path=/api/documents/**");
        registry.add("spring.cloud.gateway.routes[2].filters[0]", () -> "StripPrefix=2"); // Strip both /api/documents
        registry.add("spring.cloud.gateway.routes[2].filters[1]", () -> "AddRequestHeader=X-Service-Name,document-management");

        // Global filters
        registry.add("spring.cloud.gateway.default-filters[0]", () -> "AddResponseHeader=X-Gateway-Version,1.0.0");
    }

    @BeforeAll
    static void startMockServers() throws IOException {
        mockUserService = new MockWebServer();
        mockUserService.start();
        mockDocumentService = new MockWebServer();
        mockDocumentService.start();
        mockLeaveService = new MockWebServer();
        mockLeaveService.start();
    }

    @AfterAll
    static void shutdownMockServers() throws IOException {
        if (mockUserService != null) mockUserService.shutdown();
        if (mockDocumentService != null) mockDocumentService.shutdown();
        if (mockLeaveService != null) mockLeaveService.shutdown();
    }

    @Test
    void whenRequestToDocuments_thenRouteCorrectlyAndAddHeaders() throws InterruptedException {
        // ARRANGE
        mockDocumentService.enqueue(new MockResponse()
                .setBody("{\"documentId\":\"123\",\"name\":\"test-document\"}")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/documents/123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0.0") // Default filter
                .expectBody()
                .jsonPath("$.documentId").isEqualTo("123")
                .jsonPath("$.name").isEqualTo("test-document");

        // Verify the request was properly routed and modified
        RecordedRequest recordedRequest = mockDocumentService.takeRequest();
        // After StripPrefix=2, /api/documents/123 becomes /123
        assertThat(recordedRequest.getPath()).isEqualTo("/123");
        // Check that the AddRequestHeader filter added the correct header
        assertThat(recordedRequest.getHeader("X-Service-Name")).isEqualTo("document-management");
    }

    @Test
    void whenRequestToLeaves_thenRouteCorrectlyWithHeaders() throws InterruptedException {
        // ARRANGE
        mockLeaveService.enqueue(new MockResponse()
                .setBody("{\"leaveId\":\"456\",\"status\":\"approved\"}")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/leaves/456")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0.0")
                .expectBody()
                .jsonPath("$.leaveId").isEqualTo("456")
                .jsonPath("$.status").isEqualTo("approved");

        // Verify routing
        RecordedRequest recordedRequest = mockLeaveService.takeRequest();
        // After StripPrefix=2, /api/leaves/456 becomes /456
        assertThat(recordedRequest.getPath()).isEqualTo("/456");
        assertThat(recordedRequest.getHeader("X-Service-Name")).isEqualTo("leave-management");
    }

    @Test
    void whenRequestToUserService_thenRouteCorrectly() throws InterruptedException {
        // ARRANGE
        mockUserService.enqueue(new MockResponse()
                .setBody("{\"userId\":\"789\",\"username\":\"testuser\"}")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/user-service/profile/789")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0.0")
                .expectBody()
                .jsonPath("$.userId").isEqualTo("789")
                .jsonPath("$.username").isEqualTo("testuser");

        // Verify routing
        RecordedRequest recordedRequest = mockUserService.takeRequest();
        // After StripPrefix=1, /user-service/profile/789 becomes /profile/789
        assertThat(recordedRequest.getPath()).isEqualTo("/profile/789");
    }

    @Test
    void whenRequestToUnconfiguredRoute_thenGatewayReturns404() {
        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/non-existent-service/test")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void whenPostRequestToDocuments_thenRouteCorrectlyWithBody() throws InterruptedException {
        // ARRANGE
        String requestBody = "{\"title\":\"New Document\",\"content\":\"Test content\"}";
        mockDocumentService.enqueue(new MockResponse()
                .setBody("{\"documentId\":\"999\",\"title\":\"New Document\",\"status\":\"created\"}")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(201));

        // ACT & ASSERT
        webTestClient.post()
                .uri("/api/documents")
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0.0")
                .expectBody()
                .jsonPath("$.documentId").isEqualTo("999")
                .jsonPath("$.status").isEqualTo("created");

        // Verify the POST request was properly forwarded
        RecordedRequest recordedRequest = mockDocumentService.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("X-Service-Name")).isEqualTo("document-management");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(requestBody);
    }

    @Test
    void whenRequestWithQueryParams_thenPreserveQueryParams() throws InterruptedException {
        // ARRANGE
        mockLeaveService.enqueue(new MockResponse()
                .setBody("{\"leaves\":[],\"totalCount\":0}")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // ACT & ASSERT
        webTestClient.get()
                .uri("/api/leaves?status=pending&page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Gateway-Version", "1.0.0");

        // Verify query parameters are preserved
        RecordedRequest recordedRequest = mockLeaveService.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/?status=pending&page=1&size=10");
        assertThat(recordedRequest.getHeader("X-Service-Name")).isEqualTo("leave-management");
    }

    @Test
    void whenMultipleRequests_thenAllRouteCorrectly() throws InterruptedException {
        // ARRANGE - Setup multiple mock responses
        mockDocumentService.enqueue(new MockResponse()
                .setBody("{\"documentId\":\"doc1\"}")
                .addHeader("Content-Type", "application/json"));

        mockLeaveService.enqueue(new MockResponse()
                .setBody("{\"leaveId\":\"leave1\"}")
                .addHeader("Content-Type", "application/json"));

        mockUserService.enqueue(new MockResponse()
                .setBody("{\"userId\":\"user1\"}")
                .addHeader("Content-Type", "application/json"));

        // ACT & ASSERT - Make requests to all services
        webTestClient.get().uri("/api/documents/doc1")
                .exchange().expectStatus().isOk();

        webTestClient.get().uri("/api/leaves/leave1")
                .exchange().expectStatus().isOk();

        webTestClient.get().uri("/user-service/user1")
                .exchange().expectStatus().isOk();

        // Verify all requests were routed correctly
        RecordedRequest docRequest = mockDocumentService.takeRequest();
        RecordedRequest leaveRequest = mockLeaveService.takeRequest();
        RecordedRequest userRequest = mockUserService.takeRequest();

        assertThat(docRequest.getPath()).isEqualTo("/doc1");
        assertThat(leaveRequest.getPath()).isEqualTo("/leave1");
        assertThat(userRequest.getPath()).isEqualTo("/user1");
    }
}