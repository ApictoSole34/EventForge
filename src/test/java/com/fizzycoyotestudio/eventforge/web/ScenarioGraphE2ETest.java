package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.game.zombieshelter.ZombieShelterScenario;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-driven end-to-end tests for the scenario graph editor
 * ({@code scenario-graph.html}). Unlike the rest of the test suite
 * (unit tests + MockMvc), these tests boot the full application on a
 * random port and drive a real (headless) Chrome instance against it,
 * exercising the page's hand-written SVG/JS rather than just the
 * server-side response.
 *
 * <p>Tagged {@code e2e} so they can be excluded from the default
 * {@code mvn test} run (they need a real Chrome install and are
 * noticeably slower) and instead run separately, e.g.
 * {@code mvn test -Dgroups=e2e}.
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ScenarioGraphE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private ScenarioPersistenceService scenarioService;

    private static ChromeDriver driver;

    private UUID scenarioId;

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--window-size=1400,900");
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void seedScenario() {
        scenarioId = scenarioService.save(
                "Zombie Shelter", "desc", "zombie-attack",
                ZombieShelterScenario.initialState(),
                ZombieShelterScenario.buildRegistry().getAll()
        );
    }

    @AfterEach
    void cleanUpScenario() {
        scenarioService.deleteScenario(scenarioId);
    }

    private void openGraphPage() {
        driver.get("http://localhost:" + port + "/scenarios/" + scenarioId + "/graph");
        waitFor().until(ExpectedConditions.presenceOfElementLocated(By.id("graph-svg")));

        waitFor().until(d -> !d.findElements(By.cssSelector("g[data-event-id]")).isEmpty());
    }

    private WebDriverWait waitFor() {
        return new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @Test
    void graphPageRendersOneNodePerEvent() {
        openGraphPage();

        long nodeCount = driver.findElements(By.cssSelector("g[data-event-id]")).size();

        assertThat(nodeCount).isEqualTo(8);
    }

    @Test
    void startEventNodeIsMarkedAsStart() {
        openGraphPage();

        WebElement startNode = driver.findElement(By.cssSelector("g[data-event-id='zombie-attack'] text"));
        assertThat(startNode.getText()).startsWith("★");
    }

    @Test
    void toggleEditModeChangesButtonLabelAndShowsConnectHandles() {
        openGraphPage();

        WebElement editModeBtn = driver.findElement(By.id("btn-edit-mode"));
        assertThat(editModeBtn.getText()).contains("OFF");

        editModeBtn.click();
        waitFor().until(d -> editModeBtn.getText().contains("ON"));

        assertThat(driver.findElements(By.cssSelector("g[data-event-id] circle"))).isNotEmpty();
    }

    @Test
    void doubleClickingEmptyCanvasInEditModeOpensNewEventInspector() {
        openGraphPage();
        driver.findElement(By.id("btn-edit-mode")).click();
        waitFor().until(d -> driver.findElement(By.id("btn-edit-mode")).getText().contains("ON"));

        ((JavascriptExecutor) driver).executeScript(
                "var svg = document.getElementById('graph-svg');" +
                        "var rect = svg.getBoundingClientRect();" +
                        "var evt = new MouseEvent('dblclick', {" +
                        "  bubbles: true, cancelable: true, view: window," +
                        "  clientX: rect.left + 50, clientY: rect.top + 50" +
                        "});" +
                        "svg.dispatchEvent(evt);"
        );

        WebElement inspector = waitFor().until(ExpectedConditions.visibilityOfElementLocated(By.id("inspector")));

        assertThat(driver.findElement(By.id("inspector-title")).getText()).isEqualTo("New Event");
        assertThat(inspector.isDisplayed()).isTrue();
    }

    @Test
    void editLinkOnNodeNavigatesToEventForm() {
        openGraphPage();

        WebElement editLink = driver.findElement(By.cssSelector("g[data-event-id='zombie-attack'] a"));
        editLink.click();

        waitFor().until(ExpectedConditions.urlContains("/events/zombie-attack/edit"));
        assertThat(driver.getCurrentUrl()).endsWith("/scenarios/" + scenarioId + "/events/zombie-attack/edit");
    }

    @Test
    void inspectorCanChangeEventNameAndSave() {
        openGraphPage();
        driver.findElement(By.id("btn-edit-mode")).click();
        waitFor().until(d -> driver.findElement(By.id("btn-edit-mode")).getText().contains("ON"));

        driver.findElement(By.cssSelector("g[data-event-id='zombie-attack']")).click();
        waitFor().until(ExpectedConditions.visibilityOfElementLocated(By.id("inspector")));

        WebElement nameInput = driver.findElement(By.id("insp-name"));
        nameInput.clear();
        nameInput.sendKeys("New Name");

        driver.findElement(By.id("insp-save")).click();

        waitFor().until(ExpectedConditions.invisibilityOfElementLocated(By.id("inspector")));

        WebElement nodeTitle = driver.findElement(By.cssSelector("g[data-event-id='zombie-attack'] text"));
        assertThat(nodeTitle.getText()).contains("New Name");
    }
}