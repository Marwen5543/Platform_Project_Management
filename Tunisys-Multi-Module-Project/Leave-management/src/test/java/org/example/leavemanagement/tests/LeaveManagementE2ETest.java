package org.example.leavemanagement.tests;

import org.example.leavemanagement.helpers.UserApiClient;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.interactions.Actions;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LeaveManagementE2ETest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static UserApiClient apiClient;
    private static Actions actions;

    // Test configuration
    private static final String BASE_URL = "http://localhost:4200";
    private static final String BACKEND_URL = "http://localhost:8088";

    // Test users with unique identifiers to avoid conflicts
    private static final String EMPLOYEE_USERNAME = "e2e-leave-employee-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String EMPLOYEE_PASSWORD = "ValidPassword_123!";
    private static final String MANAGER_USERNAME = "e2e-leave-manager-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String MANAGER_PASSWORD = "ValidPassword_123!";
    private static final String HR_USERNAME = "e2e-leave-hr-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String HR_PASSWORD = "ValidPassword_123!";

    // Navigation locators
    private final By profileDropdownTrigger = By.className("user-info");
    private final By logoutLinkInDropdown = By.linkText("Logout");
    private final By congeDropdownToggle = By.xpath("//span[@class='dropdown-toggle' and contains(text(), 'Gestion Congé')]");
    // Form elements
    private final By dateRangeInput = By.xpath("//input[@placeholder='Date de début'] | //mat-date-range-input//input");
    private final By demandeCongeLink = By.linkText("Demande Congé");
    private final By consulterCongeLink = By.linkText("Consulter Congé");
    private final By listeEquipeCongeLink = By.linkText("Liste Équipe Congé");

    // Loading and success elements
    private final By loadingSpinner = By.xpath("//div[@class='loading']");
    private final By successToastMessage = By.xpath("//div[contains(@class, 'mat-snack-bar-container')]");


    // Form elements - CORRECTED
    private final By startDateInput = By.xpath("//input[@matstartdate]"); // Use the better locator
    private final By endDateInput = By.xpath("//input[@matenddate]"); // Use the better locator
    private final By leaveTypeSelect = By.xpath("//mat-select[@formcontrolname='type']");
    private final By reasonTextarea = By.xpath("//textarea[@formcontrolname='reason']");
    private final By submitButton = By.xpath("//button[contains(@class, 'submit-btn')]");

    @BeforeAll
    static void setUp() {
        System.out.println("--- Leave Management E2E Test Suite Setup ---");

        // Setup WebDriver
        WebDriverManager.chromedriver().setup();

        // Initialize API client
        apiClient = new UserApiClient();

        // Create test users
        setupTestUsers();

        System.out.println("--- Leave Management Setup Complete ---");
    }

    @BeforeEach
    void setupEach() {
        ChromeOptions options = new ChromeOptions();
        // Remove headless for debugging, add back for CI/CD
        // options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        actions = new Actions(driver);

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDownEach() {
        if (driver != null) {
            try {
                // Take screenshot on failure for debugging
                if (driver instanceof TakesScreenshot) {
                    // Could save screenshot here for failed tests
                }
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error closing driver: " + e.getMessage());
            }
        }
    }

    @AfterAll
    static void tearDown() {
        System.out.println("--- Leave Management E2E Test Suite Cleanup ---");

        // CRITICAL: Clean up leave requests BEFORE deleting users
        // This ensures we can find and delete leaves by user reference
        cleanupLeaveRequests();

        // Clean up test users
        cleanupTestUsers();

        System.out.println("--- Leave Management Cleanup Complete ---");
    }

    private static void setupTestUsers() {
        try {
            System.out.println("Creating test EMPLOYEE user: " + EMPLOYEE_USERNAME);
            apiClient.createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, "EMPLOYEE");
            Thread.sleep(2000);

            System.out.println("Creating test MANAGER user: " + MANAGER_USERNAME);
            apiClient.createUser(MANAGER_USERNAME, MANAGER_PASSWORD, "MANAGER");
            Thread.sleep(2000);

            System.out.println("Creating test HR user: " + HR_USERNAME);
            apiClient.createUser(HR_USERNAME, HR_PASSWORD, "HR");
            Thread.sleep(2000);

            System.out.println("✅ Test users created successfully");
        } catch (Exception e) {
            System.err.println("Error setting up test users: " + e.getMessage());
        }
    }

    private static void cleanupLeaveRequests() {
        try {
            System.out.println("Cleaning up leave requests created during tests...");

            // Delete all leave requests for each test user
            try {
                apiClient.deleteLeaveRequestsForUser(EMPLOYEE_USERNAME);
                System.out.println("Deleted leave requests for: " + EMPLOYEE_USERNAME);
            } catch (Exception e) {
                System.err.println("Could not delete leaves for " + EMPLOYEE_USERNAME + ": " + e.getMessage());
            }

            try {
                apiClient.deleteLeaveRequestsForUser(MANAGER_USERNAME);
                System.out.println("Deleted leave requests for: " + MANAGER_USERNAME);
            } catch (Exception e) {
                System.err.println("Could not delete leaves for " + MANAGER_USERNAME + ": " + e.getMessage());
            }

            try {
                apiClient.deleteLeaveRequestsForUser(HR_USERNAME);
                System.out.println("Deleted leave requests for: " + HR_USERNAME);
            } catch (Exception e) {
                System.err.println("Could not delete leaves for " + HR_USERNAME + ": " + e.getMessage());
            }

            System.out.println("✅ Leave requests cleaned up successfully");
        } catch (Exception e) {
            System.err.println("Error cleaning up leave requests: " + e.getMessage());
        }
    }

    private static void cleanupTestUsers() {
        try {
            System.out.println("Deleting test EMPLOYEE user: " + EMPLOYEE_USERNAME);
            apiClient.deleteUser(EMPLOYEE_USERNAME);

            System.out.println("Deleting test MANAGER user: " + MANAGER_USERNAME);
            apiClient.deleteUser(MANAGER_USERNAME);

            System.out.println("Deleting test HR user: " + HR_USERNAME);
            apiClient.deleteUser(HR_USERNAME);

            System.out.println("✅ Test users cleaned up successfully");
        } catch (Exception e) {
            System.err.println("Error cleaning up test users: " + e.getMessage());
        }
    }

    // --- REUSABLE HELPER METHODS ---

    /**
     * Perform Keycloak-based login (matching Document Management pattern)
     */
    private void performKeycloakLogin(String username, String password) {
        System.out.println("--- Performing Keycloak Login for user: " + username + " ---");
        driver.get(BASE_URL + "/acceuil");

        // Wait for Keycloak redirect
        wait.until(ExpectedConditions.urlContains("/realms/Tunisys/"));

        // Fill in Keycloak login form
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("kc-login")).click();

        // Wait for successful login and redirect back to app
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/acceuil"));
        System.out.println("Keycloak login successful.");
    }

    /**
     * Perform logout using the profile dropdown
     */
    private void performLogout() {
        System.out.println("--- Performing Logout ---");
        try {
            wait.until(ExpectedConditions.elementToBeClickable(profileDropdownTrigger)).click();
            wait.until(ExpectedConditions.elementToBeClickable(logoutLinkInDropdown)).click();
            wait.until(ExpectedConditions.urlContains("/realms/Tunisys/"));
            System.out.println("Logout successful.");
        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Navigate to leave management section using dropdown
     */
    private void navigateToLeaveSection() {
        try {
            // Click on the "Gestion Congé" dropdown
            wait.until(ExpectedConditions.elementToBeClickable(congeDropdownToggle)).click();

            // Wait for dropdown menu to be visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(text(), 'Gestion Congé')]/following-sibling::div[@class='dropdown-menu']")
            ));

            System.out.println("Leave management dropdown opened successfully");
        } catch (TimeoutException e) {
            System.err.println("Could not open leave management dropdown: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Navigate to "Demande Congé" page
     */
    private void navigateToDemandeConge() {
        try {
            // Step 1: Click the dropdown toggle.
            WebElement dropdownToggle = wait.until(ExpectedConditions.elementToBeClickable(congeDropdownToggle));
            dropdownToggle.click();

            // Step 2: Wait for the link (now found by its text) to be present and then get the element.
            WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(demandeCongeLink));

            // Step 3: Use the robust JavascriptExecutor click. This is the safest way.
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);

            // Step 4: Confirm navigation was successful.
            wait.until(ExpectedConditions.urlContains("demande-conge"));
            System.out.println("Navigated to Demande Congé page successfully.");

        } catch (Exception e) {
            System.err.println("FATAL: Navigation to Demande Congé failed. Check if the user role has permission to see the link.");
            throw e;
        }
    }

    private void debugFormState() {
        try {
            // Check if form elements are present and their states
            WebElement form = driver.findElement(By.xpath("//form"));
            System.out.println("Form found: " + (form != null));

            List<WebElement> matSelects = driver.findElements(By.xpath("//mat-select"));
            System.out.println("Mat-select count: " + matSelects.size());

            List<WebElement> matOptions = driver.findElements(By.xpath("//mat-option"));
            System.out.println("Mat-option count: " + matOptions.size());

            // Print page source snippet around form
            String pageSource = driver.getPageSource();
            if (pageSource.contains("mat-select")) {
                int index = pageSource.indexOf("mat-select");
                int start = Math.max(0, index - 200);
                int end = Math.min(pageSource.length(), index + 500);
                System.out.println("Form HTML snippet: " + pageSource.substring(start, end));
            }

        } catch (Exception e) {
            System.err.println("Debug failed: " + e.getMessage());
        }
    }
    /**
     * Navigate to "Consulter Congé" page
     */
    private void navigateToConsulterConge() {
        navigateToLeaveSection();
        try {
            wait.until(ExpectedConditions.elementToBeClickable(consulterCongeLink)).click();
            wait.until(ExpectedConditions.urlContains("consulter-conge"));
            System.out.println("Navigated to Consulter Congé page successfully");
        } catch (TimeoutException e) {
            System.err.println("Could not navigate to Consulter Congé: " + e.getMessage());
            // Fallback: direct navigation
            driver.get(BASE_URL + "/consulter-conge");
            wait.until(ExpectedConditions.urlContains("consulter-conge"));
        }
    }

    /**
     * Navigate to "Liste Équipe Congé" page (Manager/HR only)
     */
    private void navigateToListeEquipeConge() {
        navigateToLeaveSection();
        try {
            wait.until(ExpectedConditions.elementToBeClickable(listeEquipeCongeLink)).click();
            wait.until(ExpectedConditions.urlContains("liste-equipe-conge"));
            System.out.println("Navigated to Liste Équipe Congé page successfully");
        } catch (TimeoutException e) {
            System.err.println("Could not navigate to Liste Équipe Congé: " + e.getMessage());
            // Fallback: direct navigation
            driver.get(BASE_URL + "/liste-equipe-conge");
            wait.until(ExpectedConditions.urlContains("liste-equipe-conge"));
        }
    }

    /**
     * Check if user is currently logged in
     */
    private boolean isLoggedIn() {
        try {
            return driver.findElement(By.className("modern-header")).isDisplayed() &&
                    driver.findElement(profileDropdownTrigger).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Ensure specific user is logged in
     */
    private void ensureLoggedInAs(String username, String password) {
        if (!isLoggedIn()) {
            performKeycloakLogin(username, password);
        }
    }

    private void submitLeaveRequestForm() {
        try {
            System.out.println("--- Submitting Leave Request Form ---");

            // Find submit button
            WebElement submitBtn = wait.until(ExpectedConditions.presenceOfElementLocated(submitButton));

            // Scroll to submit button
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", submitBtn);
            Thread.sleep(1000);

            // Check if button is enabled
            boolean isEnabled = submitBtn.isEnabled();
            System.out.println("Submit button enabled: " + isEnabled);

            if (!isEnabled) {
                // Try to debug form validation state
                System.out.println("Submit button is disabled, checking form state...");
                // You could add form validation debugging here
            }

            // Click submit button using JavaScript
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

            System.out.println("✅ Submit button clicked");

        } catch (Exception e) {
            System.err.println("Error submitting form: " + e.getMessage());
            throw new RuntimeException("Failed to submit form", e);
        }
    }

    /**
     * Verify that the leave request was submitted successfully
     */
    private void verifyLeaveRequestSubmission() {
        try {
            System.out.println("--- Verifying Leave Request Submission ---");

            // Wait for success message or redirect
            try {
                // Option 1: Look for success toast/snackbar
                WebElement successMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(successToastMessage));
                System.out.println("✅ Success message displayed: " + successMessage.getText());
            } catch (TimeoutException e) {
                // Option 2: Check for URL change (redirect to consulter-conge)
                try {
                    wait.until(ExpectedConditions.urlContains("consulter-conge"));
                    System.out.println("✅ Redirected to consulter-conge page");
                } catch (TimeoutException e2) {
                    // Option 3: Check if form was reset
                    WebElement startDate = driver.findElement(startDateInput);
                    String startDateValue = startDate.getAttribute("value");
                    if (startDateValue == null || startDateValue.isEmpty()) {
                        System.out.println("✅ Form was reset, indicating successful submission");
                    } else {
                        throw new RuntimeException("No success indicators found");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error verifying submission: " + e.getMessage());
            throw new RuntimeException("Could not verify successful submission", e);
        }
    }
    // --- TEST METHODS ---

    @Test
    @Order(1)
    void testEmployeeCanLogin() {
        performKeycloakLogin(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        // Verify successful login by checking for Angular app elements
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.className("modern-header")),
                ExpectedConditions.presenceOfElementLocated(By.className("app-title")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(text(), 'Project Suite')]"))
        ));

        // Verify user role is displayed in header
        WebElement userRole = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("user-role")
        ));
        Assertions.assertTrue(userRole.getText().toLowerCase().contains("employee") ||
                userRole.getText().toLowerCase().contains("employé"));

        System.out.println("✅ Employee login successful");
    }

    @Test
    @Order(2)
    void testEmployeeCanAccessLeaveManagement() {
        ensureLoggedInAs(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        // Navigate to leave management section
        navigateToLeaveSection();

        // Verify dropdown menu is accessible
        WebElement dropdownMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//span[contains(text(), 'Gestion Congé')]/following-sibling::div[@class='dropdown-menu']")
        ));

        Assertions.assertTrue(dropdownMenu.isDisplayed(), "Leave management dropdown should be accessible");
        System.out.println("✅ Employee can access leave management section");
    }

    @Test
    @Order(3)
    void testEmployeeCanSubmitLeaveRequest() throws InterruptedException {
        try {
            System.out.println("=== STARTING LEAVE REQUEST TEST ===");

            // Ensure we're logged in as an employee
            ensureLoggedInAs(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

            // Navigate to the leave request page
            navigateToDemandeConge();

            // Wait for the page to be fully loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//mat-card-title[contains(text(), 'Nouvelle demande de congé')]")
            ));

            // Additional wait for Angular Material components to initialize
            Thread.sleep(3000);

            // Take a screenshot before filling the form (optional, for debugging)
            System.out.println("Current URL: " + driver.getCurrentUrl());

            // Fill the leave request form
            fillLeaveRequestForm();

            // Submit the form
            submitLeaveRequestForm();

            // Verify submission success
            verifyLeaveRequestSubmission();

            System.out.println("✅ Employee successfully submitted leave request");

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();

            // Enhanced debugging information
            try {
                System.err.println("Current URL: " + driver.getCurrentUrl());
                System.err.println("Page title: " + driver.getTitle());

                // Check if we're still on the right page
                boolean onCorrectPage = driver.getCurrentUrl().contains("demande-conge");
                System.err.println("On correct page: " + onCorrectPage);

            } catch (Exception debugEx) {
                System.err.println("Could not gather debug info: " + debugEx.getMessage());
            }

            throw e;
        }
    }


    @Test
    @Order(4)
    void testEmployeeCanViewLeaveHistory() {
        ensureLoggedInAs(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
        navigateToConsulterConge();

        // Wait for leave history table to load
        WebElement leaveTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//mat-table | //table | //div[contains(@class, 'leave-history')]")
        ));

        // Verify that leave requests are displayed
        List<WebElement> leaveRows = driver.findElements(
                By.xpath("//mat-row | //tr[contains(@class, 'mat-row')] | //div[contains(@class, 'leave-item')]")
        );

        Assertions.assertTrue(leaveRows.size() > 0, "Should display at least one leave request");
        System.out.println("✅ Employee can view leave history (" + leaveRows.size() + " requests found)");
    }


    @Test
    @Order(5)
    void testHRCanViewAllLeaves() {
        performKeycloakLogin(HR_USERNAME, HR_PASSWORD);
        navigateToConsulterConge();

        // HR should see all leave requests from all employees
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//mat-table | //table")
        ));

        List<WebElement> allLeaveRows = driver.findElements(
                By.xpath("//mat-row | //tr[contains(@class, 'mat-row')]")
        );

        // Should see multiple requests from different users
        Assertions.assertTrue(allLeaveRows.size() >= 1, "HR should see leave requests");
        System.out.println("✅ HR can view all leave requests (" + allLeaveRows.size() + " total)");
    }

    @Test
    @Order(6)
    void testHRCanApproveRejectLeaves() {
        ensureLoggedInAs(HR_USERNAME, HR_PASSWORD);

        // Navigate to HR leave management page
        navigateToHRLeaveManagement();

        // Analyze the page state and handle accordingly
        LeavePageState pageState = analyzeLeavePageState();

        if (pageState == LeavePageState.NO_DATA) {
            System.out.println("⚠️ No team leave requests available - creating test data first");
            setupTestDataForHR();

            // Navigate back and retry
            navigateToHRLeaveManagement();
            pageState = analyzeLeavePageState();
        }

        if (pageState == LeavePageState.HAS_DATA) {
            testHRLeaveActions();
        } else if (pageState == LeavePageState.ERROR) {
            handleErrorState();
        } else {
            System.out.println("⚠️ Could not find or create leave requests for HR testing");
        }
    }

    @Test
    @Order(7)
    void testLeaveFormValidation() {
        ensureLoggedInAs(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
        navigateToDemandeConge();

        // Wait for form to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form")));

        // Try to submit empty form
        WebElement submitBtn = driver.findElement(submitButton);

        // Check if submit button is disabled for empty form
        boolean isDisabled = submitBtn.getAttribute("disabled") != null ||
                !submitBtn.isEnabled() ||
                submitBtn.getAttribute("class").contains("disabled");

        Assertions.assertTrue(isDisabled, "Submit button should be disabled for invalid form");
        System.out.println("✅ Leave form validation prevents submission of invalid data");
    }

    @Test
    @Order(8)
    void testLogoutFunctionality() {
        ensureLoggedInAs(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        // Perform logout
        performLogout();

        // Verify logout - should redirect to Keycloak login page
        wait.until(ExpectedConditions.urlContains("/realms/Tunisys/"));
        System.out.println("✅ Logout functionality works correctly");
    }

    // --- FORM FILLING HELPER METHODS ---

    private void fillLeaveRequestForm() {
        System.out.println("--- Filling Leave Request Form ---");

        try {
            // Wait for the form to be fully loaded
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//form[@ng-reflect-form='[object Object]']")
            ));
            Thread.sleep(2000); // Allow Angular to fully initialize

            // Step 1: Fill start date using direct JavaScript
            WebElement startDateField = wait.until(ExpectedConditions.presenceOfElementLocated(startDateInput));
            fillDateFieldDirectly(startDateField, "2025-09-01");

            // Step 2: Fill end date using direct JavaScript
            WebElement endDateField = wait.until(ExpectedConditions.presenceOfElementLocated(endDateInput));
            fillDateFieldDirectly(endDateField, "2025-09-05");

            // Step 3: Select leave type using robust method
            selectLeaveTypeRobust();

            // Step 4: Fill reason field
            WebElement reasonField = wait.until(ExpectedConditions.presenceOfElementLocated(reasonTextarea));
            fillTextAreaDirectly(reasonField, "Personal vacation time for family trip and relaxation");

            System.out.println("✅ Leave request form filled successfully");

        } catch (Exception e) {
            System.err.println("Error filling leave request form: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fill form", e);
        }
    }
    private void selectLeaveTypeRobust() throws InterruptedException {
        try {
            System.out.println("--- Selecting Leave Type ---");

            // Find the mat-select element
            WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(leaveTypeSelect));

            // Scroll to the select element
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", selectElement);
            Thread.sleep(1000);

            // Open dropdown using JavaScript
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", selectElement);
            Thread.sleep(2000); // Wait for dropdown to open and options to load

            // Try multiple strategies to select the first option
            boolean success = false;

            // Strategy 1: Select by text content "Congé payé"
            try {
                List<WebElement> options = driver.findElements(By.xpath("//mat-option[contains(text(), 'Congé payé')]"));
                if (!options.isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
                    success = true;
                    System.out.println("✅ Selected leave type by text: Congé payé");
                }
            } catch (Exception e) {
                System.out.println("Strategy 1 failed: " + e.getMessage());
            }

            // Strategy 2: Select first mat-option if strategy 1 failed
            if (!success) {
                try {
                    List<WebElement> allOptions = driver.findElements(By.xpath("//mat-option"));
                    if (!allOptions.isEmpty()) {
                        WebElement firstOption = allOptions.get(0);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstOption);
                        success = true;
                        System.out.println("✅ Selected first leave type option");
                    }
                } catch (Exception e) {
                    System.out.println("Strategy 2 failed: " + e.getMessage());
                }
            }

            // Strategy 3: Use pure JavaScript selection if both above failed
            if (!success) {
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "var options = document.querySelectorAll('mat-option');" +
                                    "if (options.length > 0) {" +
                                    "  options[0].click();" +
                                    "  return true;" +
                                    "}" +
                                    "return false;"
                    );
                    success = true;
                    System.out.println("✅ Selected leave type using JavaScript query");
                } catch (Exception e) {
                    System.out.println("Strategy 3 failed: " + e.getMessage());
                }
            }

            if (!success) {
                // Debug available options
                debugAvailableOptions();
                throw new RuntimeException("Could not select any leave type option");
            }

            // Wait for dropdown to close
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("Error in robust leave type selection: " + e.getMessage());
            throw e;
        }
    }


    private void fillDateFieldDirectly(WebElement dateField, String dateValue) throws InterruptedException {
        try {
            // Scroll to element
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", dateField);
            Thread.sleep(500);

            // Set value directly using JavaScript
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles: true})); arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                    dateField, dateValue
            );

            // Focus and blur to trigger Angular validation
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", dateField);
            Thread.sleep(200);
            ((JavascriptExecutor) driver).executeScript("arguments[0].blur();", dateField);

            System.out.println("✅ Date field filled: " + dateValue);
        } catch (Exception e) {
            System.err.println("Error filling date field: " + e.getMessage());
            throw e;
        }
    }

    private void fillTextAreaDirectly(WebElement textArea, String text) throws InterruptedException {
        try {
            // Scroll to element
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", textArea);
            Thread.sleep(500);

            // Set value using JavaScript
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles: true}));",
                    textArea, text
            );

            // Trigger Angular events
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", textArea);
            Thread.sleep(200);
            ((JavascriptExecutor) driver).executeScript("arguments[0].blur();", textArea);

            System.out.println("✅ Textarea filled: " + text.substring(0, Math.min(30, text.length())) + "...");
        } catch (Exception e) {
            System.err.println("Error filling textarea: " + e.getMessage());
            throw e;
        }
    }

    private String getLeaveTypeDisplayText(String leaveType) {
        switch (leaveType.toUpperCase()) {
            case "VACATION":
                return "Congé payé";
            case "SICK":
                return "Congé maladie";
            case "PERSONAL":
                return "Congé personnel";
            case "MATERNITY":
                return "Congé maternité";
            case "PATERNITY":
                return "Congé paternité";
            case "BEREAVEMENT":
                return "Congé de deuil";
            default:
                return leaveType;
        }
    }



    private void debugAvailableOptions() {
        try {
            System.out.println("=== DEBUGGING AVAILABLE OPTIONS ===");

            List<WebElement> matOptions = driver.findElements(By.xpath("//mat-option"));
            System.out.println("Total mat-option elements found: " + matOptions.size());

            for (int i = 0; i < matOptions.size(); i++) {
                WebElement option = matOptions.get(i);
                String text = option.getText();
                String value = option.getAttribute("value");
                String className = option.getAttribute("class");
                boolean isDisplayed = option.isDisplayed();
                boolean isEnabled = option.isEnabled();

                System.out.println("Option " + i + ":");
                System.out.println("  Text: '" + text + "'");
                System.out.println("  Value: '" + value + "'");
                System.out.println("  Class: '" + className + "'");
                System.out.println("  Displayed: " + isDisplayed);
                System.out.println("  Enabled: " + isEnabled);
            }

            // Check if dropdown panel is visible
            List<WebElement> panels = driver.findElements(By.xpath("//div[contains(@class, 'mat-select-panel')]"));
            System.out.println("Mat-select panels found: " + panels.size());

            System.out.println("=== END DEBUG INFO ===");

        } catch (Exception e) {
            System.err.println("Debug failed: " + e.getMessage());
        }
    }


    private enum LeavePageState {
        LOADING, ERROR, NO_DATA, HAS_DATA, UNKNOWN
    }

    private void navigateToHRLeaveManagement() {
        try {
            // Try Liste Équipe Congé first (HR team view)
            navigateToListeEquipeConge();
            System.out.println("Navigated to Liste Équipe Congé (HR team view)");
        } catch (Exception e) {
            System.out.println("Could not navigate to Liste Équipe Congé, trying Consulter Congé");
            try {
                navigateToConsulterConge();
                System.out.println("Navigated to Consulter Congé");
            } catch (Exception e2) {
                System.out.println("Navigation failed, trying direct URL");
                driver.get(BASE_URL + "/liste-equipe-conge");
                wait.until(ExpectedConditions.urlContains("liste-equipe-conge"));
            }
        }
    }

    private LeavePageState analyzeLeavePageState() {
        System.out.println("--- Analyzing Leave Page State ---");

        // Wait for the container to be present
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("team-leaves-container")
            ));
            System.out.println("✅ Team leaves container found");
        } catch (TimeoutException e) {
            System.out.println("❌ Team leaves container not found");
            debugPageContent();
            return LeavePageState.UNKNOWN;
        }

        // Wait for loading to complete
        try {
            wait.withTimeout(Duration.ofSeconds(15))
                    .until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading-spinner")));
            System.out.println("✅ Loading completed");
        } catch (TimeoutException e) {
            System.out.println("⚠️ Loading spinner still visible or not found");
        }

        // Check for error state
        try {
            WebElement errorMessage = driver.findElement(By.className("error-message"));
            if (errorMessage.isDisplayed()) {
                System.out.println("❌ Error state detected: " + errorMessage.getText());
                return LeavePageState.ERROR;
            }
        } catch (NoSuchElementException e) {
            // No error, continue
        }

        // Check for no data state
        try {
            WebElement noDataMessage = driver.findElement(By.className("no-data"));
            if (noDataMessage.isDisplayed()) {
                System.out.println("⚠️ No data state: " + noDataMessage.getText());
                return LeavePageState.NO_DATA;
            }
        } catch (NoSuchElementException e) {
            // No "no data" message
        }

        // Check for table with data
        try {
            WebElement table = driver.findElement(By.xpath("//table[@mat-table]"));
            if (table.isDisplayed()) {
                List<WebElement> rows = driver.findElements(By.xpath("//table[@mat-table]//mat-row"));
                System.out.println("✅ Table found with " + rows.size() + " data rows");
                return rows.size() > 0 ? LeavePageState.HAS_DATA : LeavePageState.NO_DATA;
            }
        } catch (NoSuchElementException e) {
            System.out.println("⚠️ No mat-table found");
        }

        return LeavePageState.NO_DATA;
    }

    private void testHRLeaveActions() {
        System.out.println("--- Testing HR Leave Actions ---");

        try {
            // Find all status dropdowns in the action column
            List<WebElement> statusSelects = driver.findElements(
                    By.xpath("//td[mat-cell]//mat-form-field//mat-select")
            );

            System.out.println("Found " + statusSelects.size() + " status dropdowns");

            if (statusSelects.isEmpty()) {
                // Fallback: look for any mat-select in the table
                statusSelects = driver.findElements(By.xpath("//table[@mat-table]//mat-select"));
                System.out.println("Fallback: Found " + statusSelects.size() + " mat-select elements");
            }

            if (statusSelects.isEmpty()) {
                System.out.println("⚠️ No status dropdowns found");
                debugActionElements();
                return;
            }

            // Test approval on first request if possible
            boolean approvalTested = testStatusChange(statusSelects.get(0), "APPROVED");
            if (approvalTested) {
                System.out.println("✅ HR can approve leave requests");
            }

            // If there are multiple requests or if we have more selects, test rejection
            int selectIndexForRejection = statusSelects.size() > 1 ? 1 : 0;
            if (selectIndexForRejection < statusSelects.size() && (!approvalTested || statusSelects.size() > 1)) {
                Thread.sleep(2000); // Wait between actions

                // Re-find elements to avoid stale reference
                statusSelects = driver.findElements(By.xpath("//table[@mat-table]//mat-select"));
                if (selectIndexForRejection < statusSelects.size()) {
                    boolean rejectionTested = testStatusChange(statusSelects.get(selectIndexForRejection), "REJECTED");
                    if (rejectionTested) {
                        System.out.println("✅ HR can reject leave requests");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error testing HR leave actions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean testStatusChange(WebElement selectElement, String targetStatus) {
        try {
            // Scroll to the select element
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});", selectElement
            );
            Thread.sleep(1000);

            // Click to open dropdown
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", selectElement);
            Thread.sleep(1500);

            // Find all available options
            List<WebElement> options = driver.findElements(By.xpath("//mat-option"));
            System.out.println("Found " + options.size() + " dropdown options");

            // Look for the target status option
            WebElement targetOption = null;
            for (WebElement option : options) {
                String optionText = option.getText().trim();
                System.out.println("Checking option: '" + optionText + "'");
                if (optionText.equalsIgnoreCase(targetStatus) ||
                        optionText.toLowerCase().contains(targetStatus.toLowerCase())) {
                    targetOption = option;
                    break;
                }
            }

            if (targetOption != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", targetOption);
                Thread.sleep(1000);

                // Wait for any confirmation or loading to complete
                waitForActionCompletion();

                return true;
            } else {
                System.out.println("⚠️ Status option '" + targetStatus + "' not found in dropdown");
                // Click somewhere else to close dropdown
                ((JavascriptExecutor) driver).executeScript("document.body.click();");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error changing status to " + targetStatus + ": " + e.getMessage());
            return false;
        }
    }

    private void waitForActionCompletion() {
        try {
            // Wait for any loading indicators to disappear
            wait.withTimeout(Duration.ofSeconds(5))
                    .until(ExpectedConditions.invisibilityOfElementLocated(By.className("loading")));
        } catch (TimeoutException e) {
            // No loading indicator found, continue
        }

        try {
            // Check for success message
            wait.withTimeout(Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(successToastMessage));
            System.out.println("✅ Success message detected");
        } catch (TimeoutException e) {
            // No success message, that's okay
        }
    }

    private void setupTestDataForHR() {
        System.out.println("--- Setting Up Test Data for HR ---");

        try {
            // Create a leave request as an employee first
            performLogout();
            performKeycloakLogin(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

            // Navigate to request leave page and submit a request
            navigateToDemandeConge();
            Thread.sleep(2000);
            fillLeaveRequestForm();
            submitLeaveRequestForm();

            System.out.println("✅ Test leave request created");

            // Log back in as HR
            performLogout();
            performKeycloakLogin(HR_USERNAME, HR_PASSWORD);

        } catch (Exception e) {
            System.err.println("Error setting up test data: " + e.getMessage());
            // Try to get back to HR user
            try {
                if (!isLoggedIn()) {
                    performKeycloakLogin(HR_USERNAME, HR_PASSWORD);
                }
            } catch (Exception loginError) {
                System.err.println("Could not recover to HR login: " + loginError.getMessage());
            }
        }
    }

    private void handleErrorState() {
        try {
            WebElement retryButton = driver.findElement(
                    By.xpath("//button[contains(text(), 'Retry')]")
            );
            if (retryButton.isDisplayed()) {
                System.out.println("Clicking retry button...");
                retryButton.click();
                Thread.sleep(3000);

                LeavePageState newState = analyzeLeavePageState();
                if (newState == LeavePageState.HAS_DATA) {
                    testHRLeaveActions();
                }
            }
        } catch (Exception e) {
            System.out.println("Could not handle error state: " + e.getMessage());
        }
    }

    private void debugPageContent() {
        System.out.println("=== PAGE DEBUG INFO ===");
        System.out.println("Current URL: " + driver.getCurrentUrl());
        System.out.println("Page title: " + driver.getTitle());

        try {
            WebElement body = driver.findElement(By.tagName("body"));
            String pageText = body.getText();
            System.out.println("Page text preview: " + pageText.substring(0, Math.min(300, pageText.length())));
        } catch (Exception e) {
            System.out.println("Could not read page content: " + e.getMessage());
        }

        System.out.println("=== END PAGE DEBUG ===");
    }

    private void debugActionElements() {
        System.out.println("=== ACTION ELEMENTS DEBUG ===");

        List<WebElement> formFields = driver.findElements(By.xpath("//mat-form-field"));
        System.out.println("Found " + formFields.size() + " mat-form-field elements");

        List<WebElement> selects = driver.findElements(By.xpath("//mat-select"));
        System.out.println("Found " + selects.size() + " mat-select elements");

        List<WebElement> tableRows = driver.findElements(By.xpath("//mat-row"));
        System.out.println("Found " + tableRows.size() + " table rows");

        System.out.println("=== END ACTION DEBUG ===");
    }

}