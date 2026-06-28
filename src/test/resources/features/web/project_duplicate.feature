Feature: Project duplicate API
  As an editor
  I want to duplicate a project
  So that I can iterate on a copy without losing the original

  Scenario: Duplicate project returns a new project with a different id
    Given a new project is persisted
    When I duplicate the persisted project
    Then the duplicate response status should be 200
    And the duplicated project id should differ from the original
