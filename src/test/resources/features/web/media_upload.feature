Feature: Media upload
  As an editor
  I want to upload media to my project
  So that I can use it on the timeline

  Scenario: Upload image media to an existing project
    Given a new project is persisted
    When I upload the test image to the project
    Then the upload response status should be 200
    And the persisted project should contain the uploaded media
