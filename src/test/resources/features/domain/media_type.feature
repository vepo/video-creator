Feature: Media type detection
  As the media library
  I want to classify uploaded files by mime type
  So that clips are handled correctly on the timeline

  Scenario Outline: Mime types map to media types
    Given the mime type "<mime>"
    When the media type is resolved
    Then the media type should be "<type>"

    Examples:
      | mime        | type    |
      | video/mp4   | VIDEO   |
      | audio/mpeg  | AUDIO   |
      | image/png   | IMAGE   |
      | text/plain  | UNKNOWN |
