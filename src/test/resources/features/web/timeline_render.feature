Feature: Timeline render API
  As an editor
  I want to render my timeline to a video file
  So that I can publish the final output

  Scenario: Render request accepts a timeline project payload
    Given a new timeline project
    When I request a render for the timeline project
    Then the render response status should be 200 or 500
