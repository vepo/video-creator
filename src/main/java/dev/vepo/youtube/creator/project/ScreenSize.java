package dev.vepo.youtube.creator.project;

/**
 * @see https://support.google.com/youtube/answer/6375112?hl=en&co=GENIE.Platform%3DDesktop
 */
public enum ScreenSize {
     // Standard Definition (SD)
     SD_360p(640, 360, "360p", "SD", "640x360", 16.0/9.0),
     SD_480p(854, 480, "480p", "SD", "854x480", 16.0/9.0),
     
     // High Definition (HD)
     HD_720p(1280, 720, "720p", "HD", "1280x720", 16.0/9.0),
     HD_1080p(1920, 1080, "1080p", "Full HD", "1920x1080", 16.0/9.0),
     
     // Ultra High Definition (UHD) - 4K
     UHD_1440p(2560, 1440, "1440p", "2K", "2560x1440", 16.0/9.0),
     UHD_2160p(3840, 2160, "2160p", "4K", "3840x2160", 16.0/9.0),
     UHD_4320p(7680, 4320, "4320p", "8K", "7680x4320", 16.0/9.0),
     
     // Vertical Video (Mobile/Social Media)
     VERTICAL_360p(360, 640, "360p Vertical", "Vertical", "360x640", 9.0/16.0),
     VERTICAL_720p(720, 1280, "720p Vertical", "Vertical HD", "720x1280", 9.0/16.0),
     VERTICAL_1080p(1080, 1920, "1080p Vertical", "Vertical Full HD", "1080x1920", 9.0/16.0),
     
     // Square Video
     SQUARE_480p(480, 480, "480p Square", "Square", "480x480", 1.0),
     SQUARE_720p(720, 720, "720p Square", "Square HD", "720x720", 1.0),
     SQUARE_1080p(1080, 1080, "1080p Square", "Square Full HD", "1080x1080", 1.0),
     
     // YouTube Shorts (Recommended)
     SHORTS_1080x1920(1080, 1920, "Shorts", "YouTube Shorts", "1080x1920", 9.0/16.0),
     
     // YouTube Recommended Upload Settings
     YT_RECOMMENDED_SD(640, 480, "Recommended SD", "SD Recommended", "640x480", 4.0/3.0),
     YT_RECOMMENDED_HD(1280, 720, "Recommended HD", "HD Recommended", "1280x720", 16.0/9.0),
     YT_RECOMMENDED_FULL_HD(1920, 1080, "Recommended Full HD", "Full HD Recommended", "1920x1080", 16.0/9.0),
     YT_RECOMMENDED_4K(3840, 2160, "Recommended 4K", "4K Recommended", "3840x2160", 16.0/9.0);
 
     private final int width;
     private final int height;
     private final String name;
     private final String category;
     private final String resolution;
     private final double aspectRatio;
 
     ScreenSize(int width, int height, String name, String category, String resolution, double aspectRatio) {
         this.width = width;
         this.height = height;
         this.name = name;
         this.category = category;
         this.resolution = resolution;
         this.aspectRatio = aspectRatio;
     }
 
     // Getters
     public int getWidth() {
         return width;
     }
 
     public int getHeight() {
         return height;
     }
 
     public String getName() {
         return name;
     }
 
     public String getCategory() {
         return category;
     }
 
     public String getResolution() {
         return resolution;
     }
 
     public double getAspectRatio() {
         return aspectRatio;
     }
 
     public String getAspectRatioString() {
         if (aspectRatio == 16.0/9.0) return "16:9";
         if (aspectRatio == 9.0/16.0) return "9:16";
         if (aspectRatio == 4.0/3.0) return "4:3";
         if (aspectRatio == 1.0) return "1:1";
         return String.format("%.2f:1", aspectRatio);
     }
 
     // Utility methods
     public boolean isVertical() {
         return height > width;
     }
 
     public boolean isSquare() {
         return width == height;
     }
 
     public boolean isLandscape() {
         return width > height;
     }
 
     public boolean isHD() {
         return height >= 720;
     }
 
     public boolean isFullHD() {
         return height >= 1080;
     }
 
     public boolean is4K() {
         return height >= 2160;
     }
 
     public boolean isRecommended() {
         return name.contains("Recommended");
     }
 
     public boolean isShorts() {
         return name.contains("Shorts");
     }
 
     // Find by resolution string
     public static ScreenSize fromResolution(String resolution) {
         for (ScreenSize size : values()) {
             if (size.getResolution().equalsIgnoreCase(resolution) || 
                 size.name().equalsIgnoreCase(resolution)) {
                 return size;
             }
         }
         throw new IllegalArgumentException("Unknown resolution: " + resolution);
     }
 
     // Find closest matching size
     public static ScreenSize findClosest(int width, int height) {
         ScreenSize closest = null;
         double minDifference = Double.MAX_VALUE;
 
         for (ScreenSize size : values()) {
             double difference = Math.abs(size.getWidth() - width) + Math.abs(size.getHeight() - height);
             if (difference < minDifference) {
                 minDifference = difference;
                 closest = size;
             }
         }
 
         return closest;
     }
 
     // Get all sizes by category
     public static ScreenSize[] getByCategory(String category) {
         return java.util.Arrays.stream(values())
                 .filter(size -> size.getCategory().equalsIgnoreCase(category))
                 .toArray(ScreenSize[]::new);
     }
 
     // Get all standard landscape sizes
     public static ScreenSize[] getLandscapeSizes() {
         return java.util.Arrays.stream(values())
                 .filter(ScreenSize::isLandscape)
                 .toArray(ScreenSize[]::new);
     }
 
     // Get all vertical sizes
     public static ScreenSize[] getVerticalSizes() {
         return java.util.Arrays.stream(values())
                 .filter(ScreenSize::isVertical)
                 .toArray(ScreenSize[]::new);
     }
 
     // Get all square sizes
     public static ScreenSize[] getSquareSizes() {
         return java.util.Arrays.stream(values())
                 .filter(ScreenSize::isSquare)
                 .toArray(ScreenSize[]::new);
     }

     public static ScreenSize getDefault() {
        return HD_1080p;
    }
 
     @Override
     public String toString() {
         return String.format("%s (%s) - %s", name, resolution, getAspectRatioString());
     }
}
