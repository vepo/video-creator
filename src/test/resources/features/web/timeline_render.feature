Feature: Timeline render API
  As an editor
  I want to render my timeline
  So that I can export the final video

  Scenario: Render request for a persisted project
    Given a new project is persisted
    When I request a render for the persisted project
    Then the render response status should be 200 or 500
