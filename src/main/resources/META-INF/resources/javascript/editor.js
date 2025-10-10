console.log(currentProject);
const Hash = {
    generate: async function(message) {
        const textEncoder = new TextEncoder();
        const data = textEncoder.encode(message);
        const hashBuffer = await crypto.subtle.digest('SHA-1', data);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        const hexHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        return hexHash;
    }
};
const Project = {
    findMedia: function(hash) {
        if (currentProject && currentProject.medias && currentProject.medias.length > 0) {
            return currentProject.medias.find(m => m.hash == hash);
        }
        return null;
    }, 
    allMedias: function() {
        if (!currentProject) {
            console.error("Loaded project not found!!!");
            return [];
        }

        if (!currentProject.medias || currentProject.medias.length == 0) {
            console.info("No media found!!!");
            return [];
        }

        return currentProject.medias;
    },
    allTracks: function() {
        if (!currentProject) {
            console.error("Loaded project not found!!!");
            return [];
        }

        if (!currentProject.tracks || currentProject.tracks.length == 0) {
            console.info("No track found!!!");
            return [];
        }

        return currentProject.tracks;
    },
    addTrackMedia: function(hash, track) {
        let media = Project.findMedia(hash);
        if(!media) {
            console.error('Media not found!!!', hash);
            return;
        }

        if (!currentProject.tracks) {
            currentProject.tracks = [];
        }
        Hash.generate(`${media.hash}-${currentProject.tracks.length}`)
            .then(trackHash => {
                console.log(trackHash);
                currentProject.tracks.push({
                    hash: trackHash,
                    mediaHash: media.hash,
                    duration: media.duration,
                    type: track
                });
                UI.reconciliateTracks();
            });
    }
};

const DragNDrop = {
    isMediaFile: function(e) {
        return e.dataTransfer && 
               e.dataTransfer.items && 
               e.dataTransfer.items.length > 0 && 
               [...e.dataTransfer.items].some((item) => item.kind === 'file');
    }, 
    isMedia: function(e) {
        return e.dataTransfer && e.dataTransfer.getData('video-editor/media-hash');
    },
    setupMedia: function(hash, e) {
        let media = Project.findMedia(hash);
        console.debug("Media", media);
        if (!media) {
            console.error("Media not found!!!", hash);
        }
        e.dataTransfer.setData('video-editor/media-hash', hash);
        e.dataTransfer.setData('video-editor/media-type', media.type);
    }, 
    getMediaType: function(e) {
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/media-type');
        } else {
            return null;
        }
    }, 
    getMediaHash: function(e) {
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/media-hash');
        } else {
            return null;
        }
    }
};

const UI = {
    setupElement: function(elm, type) {
        let events = dynamicElementsEvents[type];
        if (events) {
            Object.entries(events)
                  .forEach(([event, handler]) => {
                    elm.addEventListener(event, handler);
                  });
        }
    },
    selectElement: function(type, hash) {
        console.debug("Selecting ", type, hash);
        let selectedElement = document.querySelector(`[item-hash].selected`);
        console.debug("Selected element", selectedElement);
        if (selectedElement) {
            selectedElement.classList.remove('selected');

            if (selectedElement.getAttribute('item-hash') == hash) {
                return;
            }
        }

        let newSelectedElement = document.querySelector(`[item-hash="${hash}"]`);
        console.debug("New selected element", newSelectedElement);
        if (newSelectedElement) {
            newSelectedElement.classList.add('selected');
        }
    },
    mediaIcon: function(media) {
        if (media.type == 'VIDEO') {
            return '🎥';
        } else if(media.type == 'AUDIO') {
            return '🎵';
        } else if(media.type == 'IMAGE') {
            return '🖼️'
        } else {
            return '❔';
        }
    },
    mediaDuration: function(duration) {
        // Handle invalid inputs
        if (typeof duration !== 'number' || isNaN(duration) || !isFinite(duration) || duration < 0) {
            return '00:00:00.000';
        }

        // Ensure duration is an integer
        duration = Math.floor(duration);

        const hours = Math.floor(duration / 3600000); // 1000 * 60 * 60
        const minutes = Math.floor((duration % 3600000) / 60000); // 1000 * 60
        const seconds = Math.floor((duration % 60000) / 1000);
        const milliseconds = duration % 1000;

        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${milliseconds.toString().padStart(3, '0')}`;
    },
    reconciliateMedias: function() {
        let mediaList = document.getElementById('media-list');
        if (!mediaList) {
            console.error("Media list not found!!!");
            return;
        }
        Project.allMedias().forEach(media => {
            var mediaItem = mediaList.querySelector(`[item-hash="${media.hash}"]`);
            if (!mediaItem) {
                // onclick="selectElement('MEDIA', '${media.hash}')" 
                mediaList.insertAdjacentHTML('beforeend', `<div draggable="true" class="file-item" item-hash="${media.hash}">
                                                            <div class="file-icon">${ UI.mediaIcon(media) }</div>
                                                            <div class="file-info">
                                                                <div class="file-name" title="${ media.name }">${ media.name }</div>
                                                                <div class="file-duration">${ UI.mediaDuration(media.duration) }</div>
                                                            </div>
                                                        </div>`);
                let mediaElm = document.querySelector(`[item-hash="${media.hash}"]`);
                if (!mediaElm) {
                    console.error("Element not added!!!");
                }
                UI.setupElement(mediaElm, 'MEDIA');
            }
        })
    },
    reconciliateTracks: function() {
        let videoTrack = document.getElementById('video-track');
        if (!videoTrack) {
            console.error("Video Track not found!!!");
            return;
        }

        let audioTrack = document.getElementById('audio-track');
        if (!audioTrack) {
            console.error("Audio Track not found!!!");
            return;
        }
        
        Project.allTracks().forEach(track => {

            switch(track.type) {
                case 'AUDIO':
                    audioTrack.insertAdjacentHTML('beforeend', `<div class="clip" style="left: 5%; width: 15%;">
                                                                   <div class="clip-content">
                                                                       <span class="clip-name">Intro</span>
                                                                       <div class="clip-controls">
                                                                           <button class="clip-btn">✏️</button>
                                                                           <button class="clip-btn">🗑️</button>
                                                                       </div>
                                                                   </div>
                                                               </div>`);
                    break;
                case 'VIDEO':
                    videoTrack.insertAdjacentHTML('beforeend', `<div class="clip" style="left: 5%; width: 15%;">
                                                                   <div class="clip-content">
                                                                       <span class="clip-name">Intro</span>
                                                                       <div class="clip-controls">
                                                                           <button class="clip-btn">✏️</button>
                                                                           <button class="clip-btn">🗑️</button>
                                                                       </div>
                                                                   </div>
                                                               </div>`);
                    break;
                default:
                    console.error('Invalid type!', track);
                    break;
            }
        });
    }
};
const MediaLibrary = {
    add: (file) => {
        // Upload file to server
        const formData = new FormData();
        formData.append('name', file.name);
        formData.append('lastModified', file.lastModified);
        formData.append('file', file);
        
        fetch(`/api/editor/${currentProject.id}/media`, {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert('Error uploading file: ' + data.error);
                return;
            }
            currentProject.medias.push(data)
            UI.reconciliateMedias();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error uploading file: ' + error.message);
        });
    }
}

const dynamicElementsEvents = {
    'MEDIA': {
        click: function() {
            let hash = this.getAttribute('item-hash');
            if (!hash) {
                console.error("Hash not defined!!!", this);
            }
            UI.selectElement('MEDIA', hash);
        }, 
        dragstart: function(e) {
            let hash = this.getAttribute('item-hash');
            if (!hash) {
                console.error("Hash not defined!!!", this);
            }
            DragNDrop.setupMedia(hash, e);
        }
    }
};

const staticElementsEvents = {
    'project-files-container': {
        dragover: function(e) {
            if (DragNDrop.isMediaFile(e)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.add('enabled');
                }
                e.preventDefault();
                e.stopPropagation();
            }
        },
        dragleave: function(e) {
            if (DragNDrop.isMediaFile(e)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.remove('enabled');
                }
                e.preventDefault();
                e.stopPropagation();
            }
        },
        drop: function(e) {
            if (DragNDrop.isMediaFile(e)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.remove('enabled');
                }

                e.preventDefault();
                e.stopPropagation();
                [...e.dataTransfer.items].forEach((file) => MediaLibrary.add(file.getAsFile()));
            }
        }
    }, 
    'audio-track': {
        dragover: function(e) {
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.add('active');
                e.preventDefault();
                e.stopPropagation();
            }
        },
        dragleave: function(e) {
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                e.preventDefault();
                e.stopPropagation();
            }
        },
        drop: function(e) {
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                Project.addTrackMedia(DragNDrop.getMediaHash(e), 'AUDIO');
                
                e.preventDefault();
                e.stopPropagation();
            }
        }
    }, 
    'video-track': {
        dragover: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.add('active');
                e.preventDefault();
                e.stopPropagation();
            }
        },
        dragleave: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                e.preventDefault();
                e.stopPropagation();
            }
        },
        drop: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                Project.addTrackMedia(DragNDrop.getMediaHash(e), 'VIDEO');
                e.preventDefault();
                e.stopPropagation();
            }
        }
        
    }
}
// Simple script for basic interactions
document.addEventListener('DOMContentLoaded', function() {

    // setup UI elements events handlers
    Object.entries(staticElementsEvents)
          .forEach(([element, events]) => {
              let elm = document.getElementById(element);
              if (elm) {
                  Object.entries(events).forEach(([event, handler]) => {
                      elm.addEventListener(event, handler);
                  });
              }
          });

    UI.reconciliateMedias();

    // Generate time ruler marks
    const rulerMarks = document.getElementById('rulerMarks');
    for (let i = 0; i <= 60; i += 5) {
        const mark = document.createElement('div');
        mark.className = 'time-mark';
        mark.style.left = (i / 60 * 100) + '%';
        mark.textContent = i + 's';
        rulerMarks.appendChild(mark);
    }

    // Tab switching
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Clip selection
    const clips = document.querySelectorAll('.clip');
    clips.forEach(clip => {
        clip.addEventListener('click', function(e) {
            e.stopPropagation();
            clips.forEach(c => c.classList.remove('selected'));
            this.classList.add('selected');
            
            // Update properties panel
            const clipName = this.querySelector('.clip-name').textContent;
            document.getElementById('clipProperties').innerHTML = `
                <div class="property-group">
                    <label>Clip Name</label>
                    <input type="text" value="${clipName}">
                </div>
                <div class="property-group">
                    <label>Start Time</label>
                    <input type="number" value="0.00" step="0.01">
                </div>
                <div class="property-group">
                    <label>Duration</label>
                    <input type="number" value="5.00" step="0.01">
                </div>
                <div class="property-group">
                    <label>Speed</label>
                    <input type="number" value="1.00" step="0.01" min="0.1" max="10">
                </div>
            `;
        });
    });

    // Click on track area to deselect clips
    document.querySelectorAll('.track-area').forEach(area => {
        area.addEventListener('click', function() {
            clips.forEach(c => c.classList.remove('selected'));
            document.getElementById('clipProperties').innerHTML = '<p class="placeholder-text">Select a clip to edit properties</p>';
        });
    });

    // Modal functionality
    const exportBtn = document.getElementById('exportBtn');
    const exportModal = document.getElementById('exportModal');
    const closeBtn = document.querySelector('.close');

    exportBtn.addEventListener('click', function() {
        exportModal.style.display = 'flex';
    });

    closeBtn.addEventListener('click', function() {
        exportModal.style.display = 'none';
    });

    window.addEventListener('click', function(e) {
        if (e.target === exportModal) {
            exportModal.style.display = 'none';
        }
    });
});