package dev.vepo.youtube.creator.model;

public class VideoUploadResponse {
    private String filename;
    private String message;
    private String filePath;
    private Long fileSize;
    
    public VideoUploadResponse() {}
    
    public VideoUploadResponse(String filename, String message, String filePath, Long fileSize) {
        this.filename = filename;
        this.message = message;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }
    
    // Getters and setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}