Feature: Media clip duration
  As an editor
  I want accurate clip duration calculations
  So that the timeline length is correct

  Scenario: Effective duration equals source range at normal speed
    Given a clip from 2.0 to 8.0 seconds at speed 1.0
    When the clip effective duration is calculated
    Then the clip effective duration should be 6.0 seconds

  Scenario: Effective duration adjusts for playback speed
    Given a clip from 0.0 to 10.0 seconds at speed 2.0
    When the clip effective duration is calculated
    Then the clip effective duration should be 5.0 seconds

  Scenario: A valid clip has required fields
    Given a clip with id, file path, file name, and type
    Then the clip should be valid
