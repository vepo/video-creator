Feature: Screen size resolution catalog
  As a content creator
  I want standard screen sizes for video output
  So that exports match platform requirements

  Scenario: Default screen size is Full HD landscape
    Given the default screen size
    Then the screen resolution should be "1920x1080"
    And the screen size should be landscape orientation

  Scenario: Find screen size by resolution string
    Given the resolution "1280x720"
    When I look up the screen size by resolution
    Then the screen size name should contain "720p"

  Scenario: Vertical screen sizes are taller than wide
    Given the screen size "VERTICAL_1080p"
    Then the screen size should be vertical orientation

  Scenario: Square screen sizes have equal width and height
    Given the screen size "SQUARE_720p"
    Then the screen size should be square orientation

  Scenario: Unknown resolution throws an error
    Given an unknown resolution "9999x9999"
    When I look up the screen size by resolution
    Then an illegal argument exception should be thrown
