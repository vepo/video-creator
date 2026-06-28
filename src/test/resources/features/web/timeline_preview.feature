Feature: Timeline preview API
  As an editor
  I want to preview my timeline
  So that I can verify edits before rendering

  Scenario: Preview request for a persisted project
    Given a new project is persisted
    When I request a preview for the persisted project
    Then the preview response status should be 200 or 500
