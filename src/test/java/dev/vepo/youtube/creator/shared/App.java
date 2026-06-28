package dev.vepo.youtube.creator.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class App {

    private static final Pattern EDITOR_PROJECT_URL = Pattern.compile(".*/editor/[a-f0-9]{24}$");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String rootUri;

    public App(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        var uri = TestHTTPResourceManager.getUri().toString();
        this.rootUri = uri.endsWith("/") ? uri : uri + "/";
    }

    private String url(String path) {
        var normalized = path.startsWith("/") ? path.substring(1) : path;
        return rootUri + normalized;
    }

    public App access() {
        driver.get(rootUri);
        waitForReady();
        return this;
    }

    public App goTo(String path) {
        driver.get(url(path));
        waitForReady();
        return this;
    }

    public EditorPage openNewProject() {
        driver.get(url("editor/new"));
        wait.until(d -> {
            var url = d.getCurrentUrl();
            return url.contains("/editor/") && !url.endsWith("/editor/new");
        });
        waitForReady();
        wait.until(presenceOfElementLocated(id("tracks-container")));
        return new EditorPage();
    }

    public App openEditor(String projectId) {
        driver.get(url("editor/" + projectId));
        waitForReady();
        return this;
    }

    public App waitForReady() {
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        return this;
    }

    public App assertOnHomePage() {
        wait.until(visibilityOfElementLocated(cssSelector(".projects-panel")));
        return this;
    }

    public App assertDocumentTitleContains(String text) {
        wait.until(d -> d.getTitle() != null && d.getTitle().contains(text));
        return this;
    }

    public App assertPageContainsText(String text) {
        assertThat(driver.getPageSource()).contains(text);
        return this;
    }

    public App assertEditorLoaded() {
        wait.until(presenceOfElementLocated(id("tracks-container")));
        wait.until(presenceOfElementLocated(By.className("editor-header")));
        return this;
    }

    public abstract class Page<T extends Page<T>> {

        public App assertOnHomePage() {
            return App.this.assertOnHomePage();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }
    }

    public class HomePage extends Page<HomePage> {

        public HomePage assertProjectsPanelVisible() {
            wait.until(visibilityOfElementLocated(cssSelector(".projects-panel")));
            return this;
        }

        public HomePage assertContainsText(String text) {
            App.this.assertPageContainsText(text);
            return this;
        }
    }

    public class EditorPage extends Page<EditorPage> {

        public EditorPage assertEditorLoaded() {
            wait.until(presenceOfElementLocated(id("tracks-container")));
            wait.until(presenceOfElementLocated(By.className("editor-header")));
            return this;
        }

        public EditorPage assertUrlContainsProjectId() {
            assertThat(driver.getCurrentUrl()).matches(EDITOR_PROJECT_URL);
            return this;
        }
    }
}
