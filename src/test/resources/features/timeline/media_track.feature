Feature: Media track clip management
  As an editor
  I want to manage clips on a track
  So that the timeline reflects my edit decisions

  Scenario: Adding a clip to a track assigns the track index
    Given a media track at index 0
    When a clip is added to the track
    Then the clip track index should be 0

  Scenario: Track total duration reflects clip placement
    Given a media track at index 0
    And a clip on the track from 0 to 5 seconds at timeline position 2
    When the track total duration is calculated
    Then the track total duration should be 7.0 seconds

  Scenario: An empty track has no clips
    Given a media track at index 0
    Then the track should not have clips
