package dev.vepo.youtube.creator.project;

public enum FrameRate {
     // Film & Cinema
     FPS_24(24.0, "24 FPS - Traditional Film Standard", "Film & Cinema", "🎞️"),
     FPS_23_976(23.976, "23.976 FPS - Film (NTSC)", "Film & Cinema", "🎞️"),
     
     // Broadcast Standards
     FPS_25(25.0, "25 FPS - PAL (Europe, Asia, Australia)", "Broadcast Standards", "📺"),
     FPS_30(30.0, "30 FPS - NTSC (North America, Japan)", "Broadcast Standards", "📺"),
     FPS_29_97(29.97, "29.97 FPS - NTSC Broadcast", "Broadcast Standards", "📺"),
     
     // High Frame Rate
     FPS_50(50.0, "50 FPS - PAL High", "High Frame Rate", "⚡"),
     FPS_59_94(59.94, "59.94 FPS - NTSC High", "High Frame Rate", "⚡"),
     FPS_60(60.0, "60 FPS - Smooth Action", "High Frame Rate", "⚡"),
     
     // Ultra High Frame Rate
     FPS_120(120.0, "120 FPS - Slow Motion", "Ultra High Frame Rate", "🚀"),
     FPS_240(240.0, "240 FPS - Super Slow Motion", "Ultra High Frame Rate", "🚀"),
     FPS_480(480.0, "480 FPS - Extreme Slow Motion", "Ultra High Frame Rate", "🚀"),
     
     // Web & Streaming
     FPS_24_WEB(24.0, "24 FPS - Film Style", "Web & Streaming", "🌐"),
     FPS_30_WEB(30.0, "30 FPS - Standard Streaming", "Web & Streaming", "🌐"),
     FPS_60_WEB(60.0, "60 FPS - Gaming & Sports", "Web & Streaming", "🌐");
 
     private final double value;
     private final String displayName;
     private final String category;
     private final String icon;
 
     FrameRate(double value, String displayName, String category, String icon) {
         this.value = value;
         this.displayName = displayName;
         this.category = category;
         this.icon = icon;
     }
 
     // Getters
     public double getValue() {
         return value;
     }
 
     public String getDisplayName() {
         return displayName;
     }
 
     public String getCategory() {
         return category;
     }
 
     public String getIcon() {
         return icon;
     }
 
     public String getOptionValue() {
         return String.valueOf(value);
     }
 
     public String getShortName() {
         return displayName.split(" - ")[0];
     }
 
     public String getDescription() {
         String[] parts = displayName.split(" - ");
         return parts.length > 1 ? parts[1] : "";
     }
 
     // Utility methods
     public boolean isFilmCinema() {
         return "Film & Cinema".equals(category);
     }
 
     public boolean isBroadcastStandard() {
         return "Broadcast Standards".equals(category);
     }
 
     public boolean isHighFrameRate() {
         return "High Frame Rate".equals(category);
     }
 
     public boolean isUltraHighFrameRate() {
         return "Ultra High Frame Rate".equals(category);
     }
 
     public boolean isWebStreaming() {
         return "Web & Streaming".equals(category);
     }
 
     public boolean isStandard() {
         return value == 24.0 || value == 25.0 || value == 30.0;
     }
 
     public boolean isSlowMotionCapable() {
         return value >= 60.0;
     }
 
     public boolean isNTSC() {
         return value == 23.976 || value == 29.97 || value == 59.94 || value == 30.0;
     }
 
     public boolean isPAL() {
         return value == 25.0 || value == 50.0;
     }
 
     // Find by value string (matches HTML option value)
     public static FrameRate fromValueString(String valueString) {
         for (FrameRate fps : values()) {
             if (String.valueOf(fps.getValue()).equals(valueString)) {
                 return fps;
             }
         }
         throw new IllegalArgumentException("Unknown FPS value: " + valueString);
     }
 
     // Find by display name
     public static FrameRate fromDisplayName(String displayName) {
         for (FrameRate fps : values()) {
             if (fps.getDisplayName().equals(displayName)) {
                 return fps;
             }
         }
         throw new IllegalArgumentException("Unknown FPS display name: " + displayName);
     }
 
     // Get all FPS by category
     public static FrameRate[] getByCategory(String category) {
         return java.util.Arrays.stream(values())
                 .filter(fps -> fps.getCategory().equals(category))
                 .toArray(FrameRate[]::new);
     }
 
     // Get default FPS (30 FPS for Web & Streaming)
     public static FrameRate getDefault() {
         return FPS_30_WEB;
     }
 
     // Get all categories
     public static String[] getCategories() {
         return java.util.Arrays.stream(values())
                 .map(FrameRate::getCategory)
                 .distinct()
                 .toArray(String[]::new);
     }
 
     @Override
     public String toString() {
         return displayName;
     }
}
