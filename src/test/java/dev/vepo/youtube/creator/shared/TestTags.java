package dev.vepo.youtube.creator.shared;

/**
 * JUnit 5 tag names for parallel test execution ({@code mvn test -Ptest-unit}, etc.).
 */
public final class TestTags {

    public static final String UNIT = "unit";
    public static final String QUARKUS = "quarkus";
    public static final String WEB = "web";
    /** Home page and project list ({@code -Ptest-web-shard -Dweb.shard=web-platform}). */
    public static final String WEB_PLATFORM = "web-platform";
    /** Timeline editor UI ({@code -Ptest-web-shard -Dweb.shard=web-editor}). */
    public static final String WEB_EDITOR = "web-editor";

    private TestTags() {}
}
