Feature: Processed file download
  As an editor
  I want to download rendered videos
  So that I can use the output outside the application

  Scenario: Downloading a non-existent file returns not found
    When I download file "nonexistent-file.mp4"
    Then the response status should be 404
