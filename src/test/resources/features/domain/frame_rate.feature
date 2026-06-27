Feature: Frame rate catalog
  As a content creator
  I want standard frame rates for video output
  So that playback matches platform expectations

  Scenario: Default frame rate is 30 FPS web streaming
    Given the default frame rate
    Then the frame rate value should be 30.0
    And the frame rate category should be "Web & Streaming"

  Scenario: Frame rate can be resolved from a value string
    Given the frame rate value string "24.0"
    When I look up the frame rate by value
    Then the frame rate should be film cinema category

  Scenario: High frame rates support slow motion
    Given the frame rate "FPS_120"
    Then the frame rate should support slow motion
