Feature: Project creation
  As an editor
  I want a new project with sensible defaults
  So that I can start editing immediately

  Scenario: A new project uses Full HD and 30 FPS defaults
    Given a newly created project
    Then the project screen size should be Full HD 1080p
    And the project frame rate should be 30 FPS web streaming
    And the project duration should be 30 minutes in milliseconds

  Scenario: A new project has an empty media library and clip list
    Given a newly created project
    Then the project should have no media attached
    And the project should have no clips attached

  Scenario: A new project receives an auto-generated name
    Given a newly created project
    Then the project name should start with "Project"

  Scenario: Two projects are equal when they share the same id
    Given a project with a known id
    And another project with the same id
    Then the projects should be equal

  Scenario: A project can be serialized to JSON
    Given a newly created project
    When the project is serialized to JSON
    Then the JSON should contain the project name
