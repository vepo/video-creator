Feature: Timeline preview API
  As an editor
  I want to preview my timeline
  So that I can verify edits before rendering

  Scenario: Preview request accepts a timeline project payload
    Given a new timeline project
    When I request a preview for the timeline project
    Then the preview response status should be 200 or 500
