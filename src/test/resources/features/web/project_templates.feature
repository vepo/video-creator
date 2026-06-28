Feature: Project templates API
  As an editor
  I want to list project templates
  So that I can start from common presets

  Scenario: Templates endpoint returns a non-empty list
    When I request project templates
    Then the templates response status should be 200
