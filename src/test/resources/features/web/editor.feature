Feature: Editor page
  As an editor
  I want to open the timeline editor for a project
  So that I can arrange clips and render output

  Scenario: Opening editor for new creates a project and redirects
    When I open the editor for a new project
    Then the response status should be 303
    And the location header should point to a project editor URL

  Scenario: Opening editor for an existing project renders the editor
    Given a new project is persisted
    When I open the editor for the persisted project
    Then the response status should be 200
    And the page body should contain "editor"

  Scenario: Opening editor for unknown project redirects to home
    When I open the editor for project "000000000000000000000000"
    Then the response status should be 303
    And the location header should point to the home page
