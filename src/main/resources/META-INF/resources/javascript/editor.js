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
    allClips: function() {
        if (!currentProject) {
            console.error("Loaded project not found!!!");
            return [];
        }

        if (!currentProject.clips || currentProject.clips.length == 0) {
            console.info("No clip found!!!");
            return [];
        }

        return currentProject.clips;
    },
    findClip: function(hash) {
        if (currentProject && currentProject.clips && currentProject.clips.length > 0) {
            return currentProject.clips.find(t => t.hash == hash);
        }
        return null;
    }, 
    addClipMedia: function(hash, track, position) {
        let media = Project.findMedia(hash);
        if(!media) {
            console.error('Media not found!!!', hash);
            return;
        }

        if (!currentProject.clips) {
            currentProject.clips = [];
        }
        Hash.generate(`${media.hash}-${currentProject.clips.length}`)
            .then(clipHash => {
                console.log(clipHash);
                currentProject.clips.push({
                    name: '',
                    hash: clipHash,
                    mediaHash: media.hash,
                    duration: media.duration,
                    type: track,
                    start: (currentProject.duration * position) / 100,
                    speed: 1,
                    duration: media.duration
                });
                console.log(currentProject.clips);
                UI.reconciliateClips();
            });
    }
};

const DragNDrop = {
    isMediaFile: function(e) {
        // Drag came from outside
        // just check for files
        return e.dataTransfer && 
               e.dataTransfer.items && 
               e.dataTransfer.items.length > 0 && 
               [...e.dataTransfer.items].some((item) => item.kind === 'file');
    }, 
    isMedia: function(e) {
        return DragNDrop.getDragType(e) == 'MEDIA' && e.dataTransfer && e.dataTransfer.getData('video-editor/media-hash');
    },
    setupMedia: function(hash, e) {
        let media = Project.findMedia(hash);
        console.debug("Media", media);
        if (!media) {
            console.error("Media not found!!!", hash);
        }
        e.dataTransfer.setData('video-editor/media-hash', hash);
        e.dataTransfer.setData('video-editor/media-type', media.type);
        e.dataTransfer.setData('video-editor/drag-type', 'MEDIA');
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
    },
    getDraggedElement: function(e) {
        if(e.dataTransfer && e.dataTransfer.getData('video-editor/drag-type')) {
            if (e.dataTransfer.getData('video-editor/drag-type') == 'CLIP') {
                return UI.getElementByHash(e.dataTransfer.getData('video-editor/clip-hash'));
            } else if (e.dataTransfer.getData('video-editor/drag-type') == 'MEDIA') {
                return UI.getElementByHash(e.dataTransfer.getData('video-editor/media-hash'));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }, 
    setupClip: function(hash, e) {
        let clip = Project.findClip(hash);
        console.debug("Clip", clip);
        if (!clip) {
            console.error("Clip not found!!!", hash);
        }
        e.dataTransfer.setData('video-editor/clip-hash', hash);
        e.dataTransfer.setData('video-editor/clip-type', clip.type);
        e.dataTransfer.setData('video-editor/drag-type', 'CLIP');
    }, 
    isClip: function(e) {
        return DragNDrop.getDragType(e) == 'CLIP' && e.dataTransfer && e.dataTransfer.getData('video-editor/clip-hash');
    },
    getClipType: function(e) {
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/clip-type');
        } else {
            return null;
        }
    }, 
    getClipHash: function(e) {
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/clip-hash');
        } else {
            return null;
        }
    },
    getDragType: function(e) {
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/drag-type');
        } else {
            return null;
        }
    },
    calculateDropPosition: function(e) {
        // let activeElement = DragNDrop.getDraggedElement(e);
        // if (activeElement) {
        //     const rect = activeElement.getBoundingClientRect();
            const containerRect = e.target.getBoundingClientRect();
            // console.log(`rec=${rect.left} container=${containerRect.left}`)
            let clientX = e.x - containerRect.left;
            return (clientX / containerRect.width) * 100;
        //     // console.log(newLeftPercent);
        // } else {
        //     return 0;
        // }
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
    getElementByHash: function(hash) {
        return document.querySelector(`[item-hash="${hash}"]`);
    },
    selectElement: function(type, hash) {
        console.debug("Selecting ", type, hash);
        let selectedElement = document.querySelector(`[item-hash].selected`);
        console.debug("Selected element", selectedElement);
        if (selectedElement) {
            selectedElement.classList.remove('selected');

            if (selectedElement.getAttribute('item-hash') == hash) {
                UI.clearItemProperties();
                return;
            }
        }

        let newSelectedElement = document.querySelector(`[item-hash="${hash}"]`);
        console.debug("New selected element", newSelectedElement);
        if (newSelectedElement) {
            newSelectedElement.classList.add('selected');
        }
        UI.setupItemProperties(type, hash);
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
    clearItemProperties: function() {
        let itemPropertiesEmpty = document.getElementById('item-properties-empty')
        let itemProperties = document.getElementById('item-properties')
        while(itemProperties && itemProperties.firstChild) {
            itemProperties.removeChild(itemProperties.firstChild);
        }

        if(itemPropertiesEmpty) {
            itemPropertiesEmpty.style['display'] = 'block';
        }
    },
    setupItemProperties: function(type, hash) {
        let itemPropertiesEmpty = document.getElementById('item-properties-empty')
        let itemProperties = document.getElementById('item-properties')
        while(itemProperties && itemProperties.firstChild) {
            itemProperties.removeChild(itemProperties.firstChild);
        }

        if(itemPropertiesEmpty) {
            itemPropertiesEmpty.style['display'] = 'none';
        }

        if (!itemProperties) {
            console.error("Item properties could not be found!");
        }

        if(type == 'MEDIA') {
            let media = Project.findMedia(hash);
            if (media) {
                itemProperties.insertAdjacentHTML('afterbegin', `<h3>Media</h3>
                                                                 <div class="property-group">
                                                                     <label>Clip Name</label>
                                                                     <input type="text" disabled value="${media.name}">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Duration</label>
                                                                     <input type="text" disabled value="${ UI.mediaDuration(media.duration) }">
                                                                 </div>`);
            } else {
                console.error("Media cannot be found!", hash);
            }
        } else if (type == 'CLIP') {
            let clip = Project.findClip(hash);
            if (clip) {
                itemProperties.insertAdjacentHTML('afterbegin', `<h3>Clip</h3>
                                                                 <div class="property-group">
                                                                     <label>Clip Name</label>
                                                                     <input type="text" value="${clip.name}">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Start Time <i>(s)</i></label>
                                                                     <input type="number" value="0.00" step="0.01">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Duration <i>(s)</i></label>
                                                                     <input type="number" value="5.00" step="0.01">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Speed</label>
                                                                     <input type="number" value="1.00" step="0.01" min="0.1" max="10">
                                                                 </div>`);
            } else {
                console.error("Clip cannot be found!", hash);
            }

        }
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
    reconciliateClips: function() {
        let videoTrack = document.getElementById('video-track');
        if (!videoTrack) {
            console.error("Video Clip not found!!!");
            return;
        }

        let audioTrack = document.getElementById('audio-track');
        if (!audioTrack) {
            console.error("Audio Clip not found!!!");
            return;
        }
        
        Project.allClips().forEach(clip => {
            var media = Project.findMedia(clip.mediaHash);
            var clipElm = document.querySelector(`[item-hash="${clip.hash}"]`);
            if (!clipElm) {
                switch(clip.type) {
                    case 'AUDIO':
                        audioTrack.insertAdjacentHTML('beforeend', `<div draggable="true" class="clip" item-hash="${clip.hash}" style="left: ${ (clip.start * 100) / currentProject.duration }%; width: 15%;">
                                                                    <div class="clip-content">
                                                                        <span class="clip-name">${media.name}</span>
                                                                    </div>
                                                                </div>`);
                        break;
                    case 'VIDEO':
                        videoTrack.insertAdjacentHTML('beforeend', `<div draggable="true" class="clip" item-hash="${clip.hash}" style="left: ${ (clip.start * 100) / currentProject.duration }%; width: 15%;">
                                                                    <div class="clip-content">
                                                                        <span class="clip-name">${media.name}</span>
                                                                    </div>
                                                                </div>`);
                        break;
                    default:
                        console.error('Invalid type!', clip);
                        break;
                }
                let clipElm = document.querySelector(`[item-hash="${clip.hash}"]`);
                if (!clipElm) {
                    console.error("Clip element not found!!!")
                }
                UI.setupElement(clipElm, 'CLIP');
            }
        });
    }, 
    findShadowElement: function(hash) {
        return document.querySelector(`[item-temp-hash="${hash}"]`);
    },
    removeShadowComponent: function() {
        let shadowComponent = document.querySelector(`[item-temp-hash]`);
        if (shadowComponent) {
            shadowComponent.parentElement.removeChild(shadowComponent);
        }
    },
    updateShadowElement: function(position, hash, source, destiny) {
        let destinyElm = null;
        if (destiny == 'AUDIO') {
            destinyElm = document.getElementById('audio-track');
        } else if (destiny == 'VIDEO') {
            destinyElm = document.getElementById('video-track');
        } else {
            console.error('Destiny not found!!!', arguments);
            return;
        }

        if (source == 'MEDIA') {
            let media = Project.findMedia(hash);
            if (media) {
                let previousElm = UI.findShadowElement(hash);
                if (!previousElm) {
                    destinyElm.insertAdjacentHTML('beforeend', `<div class="clip" item-temp-hash="${hash}" style="left: ${position}%; width: 15%;">
                                                                <div class="clip-content">
                                                                    <span class="clip-name">${media.name}</span>
                                                                </div>
                                                            </div>`);
                } else {
                    previousElm.style['left'] = `${position}%`;   
                }
            }
        }
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
    }, 
    'CLIP': {
        click: function() {
            let hash = this.getAttribute('item-hash');
            if (!hash) {
                console.error("Hash not defined!!!", this);
            }
            UI.selectElement('CLIP', hash);
        }, 
        dragstart: function(e) {
            let hash = this.getAttribute('item-hash');
            if (!hash) {
                console.error("Hash not defined!!!", this);
            }
            DragNDrop.setupClip(hash, e);
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
                // console.log(e)
                // Get initial positions
                let position = DragNDrop.calculateDropPosition(e);
                if (position >= 0) {
                    UI.updateShadowElement(position, DragNDrop.getMediaHash(e), 'MEDIA', 'AUDIO');
                }
                e.preventDefault();
                e.stopPropagation();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'AUDIO') {
                e.preventDefault();
                e.stopPropagation();

                let audioTrack = document.getElementById('audio-track');
                // audioTrack is the container
                // We are moving divs inside it.
                // we need to shift the divs using lef=<value>%
                if (!audioTrack) {
                    console.error("Audio track not found!!!");
                    return;
                }
                let parentXPos = audioTrack.getBoundingClientRect().left;
                console.log(`clientX=${e.clientX} layerX=${e.layerX}`);
                let elementNewXPos = e.clientX - e.layerX;
                console.log(`parent=${parentXPos} new=${elementNewXPos}`);
                // console
                // console.log(e)
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
                let position = DragNDrop.calculateDropPosition(e);

                UI.removeShadowComponent();
                Project.addClipMedia(DragNDrop.getMediaHash(e), 'AUDIO', position);
                
                e.preventDefault();
                e.stopPropagation();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'AUDIO') {
                console.log(e);
            }
        }
    }, 
    'video-track': {
        dragover: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.add('active');
                e.preventDefault();
                e.stopPropagation();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'VIDEO') {
                console.log(e);
            }
        },
        dragleave: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                e.preventDefault();
                e.stopPropagation();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'VIDEO') {
                console.log(e);
            }
        },
        drop: function(e) {
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                this.classList.remove('active');
                Project.addClipMedia(DragNDrop.getMediaHash(e), 'VIDEO');
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