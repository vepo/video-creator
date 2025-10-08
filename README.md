# Quarkus Video Editor with Web GUI

A Quarkus-based video editing service with a web interface that uses MLT's `melt` command for video processing.

## Features

- **Web-based GUI** for easy video editing
- **Video Upload** with drag-and-drop support
- **Trim Operations** - cut specific sections of videos
- **Custom Encoding Settings** - adjust quality, resolution, codecs
- **Download Processed Videos** directly from the browser
- **Real-time Status** of MLT availability

## Prerequisites

- Java 17+
- Maven
- MLT Framework with `melt` command installed

## Installation

1. **Install MLT**:

**Ubuntu/Debian:**
```bash
sudo apt-get install melt