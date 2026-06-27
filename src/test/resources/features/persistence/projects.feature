Feature: Project persistence
  As an editor
  I want projects saved to the database
  So that I can resume editing later

  Scenario: A new project can be persisted and loaded
    Given a new project is persisted
    When the project is loaded by id
    Then the loaded project name should match the persisted project

  Scenario: All persisted projects can be listed
    Given a new project is persisted
    When all projects are loaded
    Then the project list should contain the persisted project
