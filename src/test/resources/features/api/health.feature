Feature: Video processing health
  As an operator
  I want to check MLT availability
  So that I know whether rendering is possible

  Scenario: Health endpoint reports MLT availability
    When I request the health check
    Then the response status should be 200
    And the health response should include a status field
    And the health response should include a melt availability message
