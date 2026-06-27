Feature: Home page
  As a visitor
  I want to see available projects on the home page
  So that I can open an existing edit session

  Scenario: Home page renders successfully
    When I open the home page
    Then the response status should be 200
    And the page body should contain "Project"
