Feature: MLT XML generation
  As the render pipeline
  I want valid MLT XML documents
  So that melt can process video edits

  Scenario: Generate MLT XML with custom dimensions and no trims
    Given video settings with width 1280 and height 720
    And an input video at "target/test-input/sample.mp4"
    When MLT XML is generated without trim operations
    Then the MLT XML should contain profile width "1280"
    And the MLT XML should contain a full-length playlist entry

  Scenario: Generate MLT XML with trim operations
    Given video settings with width 1920 and height 1080
    And an input video at "target/test-input/sample.mp4"
    And trim operations from 1.0 to 5.0 seconds
    When MLT XML is generated with trim operations
    Then the MLT XML should contain entry in="60" out="300"

  Scenario: Generate MLT XML for a timeline with clips
    Given a timeline project with a video clip on the video track
    When timeline MLT XML is generated
    Then the MLT XML should contain a producer resource
    And the MLT XML should contain a playlist entry
