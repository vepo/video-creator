Feature: Timeline project
  As an editor
  I want a timeline with default tracks
  So that I can place video and audio clips

  Scenario: A new timeline has default video and audio tracks
    Given a new timeline project
    Then the timeline should have 2 tracks
    And the timeline should have a video track
    And the timeline should have an audio track

  Scenario: An empty timeline has no content
    Given a new timeline project
    Then the timeline should not have content

  Scenario: Timeline duration updates from track content
    Given a new timeline project
    And the video track has a clip from 0 to 10 seconds at timeline position 0
    When the timeline duration is recalculated
    Then the timeline duration should be greater than 0
