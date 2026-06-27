Feature: Media entity identity
  As the media library
  I want media items identified by their media id
  So that duplicate references are recognized

  Scenario: Two media instances with the same id are equal
    Given media with a known id and name "clip.mp4"
    And another media instance with the same id and name "clip.mp4"
    Then the media instances should be equal

  Scenario: Media reports its video type from mime type
    Given media with mime type "video/mp4" and name "clip.mp4"
    Then the media type should be VIDEO
