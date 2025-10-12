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
                currentProject.clips.push({
                    name: '',
                    hash: clipHash,
                    mediaHash: media.hash,
                    duration: media.duration,
                    type: track,
                    start: Math.round(currentProject.duration * position / 100), // millis
                    speed: 1,
                    duration: media.duration
                });
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
        let target = e.realTarget || e.target;
        while (!target.classList.contains('track-line')) {
            target = target.parentElement;
        }
        const containerRect = target.getBoundingClientRect();
        let clientX = e.x - containerRect.left;
        return (clientX / containerRect.width) * 100;
    },
    verifyEventClass: function(e, requiredClass) {
        let currentTarget = e.realTarget || e.target;                
        if (currentTarget.classList.contains(requiredClass)) {
            return;
        }
        let closestElm = currentTarget.closest(`.${requiredClass}`);
        if (closestElm) {
            e.realTarget = closestElm;
            return;
        } 
        console.error("Not captured?!?!")
        return;
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
                                                                     <input type="number" value="${clip.start / 1000}" step="0.001">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Duration <i>(s)</i></label>
                                                                     <input type="text" value="${UI.mediaDuration(clip.duration)}" >
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Speed</label>
                                                                     <input type="number" value="${clip.speed}" step="0.01" min="0.1" max="3">
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
        let videoTrack = document.querySelector('#video-track .track-line');
        if (!videoTrack) {
            console.error("Video Clip not found!!!");
            return;
        }

        let audioTrack = document.querySelector('#audio-track .track-line');
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
                        audioTrack.insertAdjacentHTML('beforeend', `<div draggable="true" class="clip" item-hash="${clip.hash}" style="left: ${ (clip.start * 100) / currentProject.duration }%; width: ${(clip.duration * 100) / currentProject.duration}%;">
                                                                    <div class="clip-content">
                                                                        <span class="clip-name">${media.name}</span>
                                                                    </div>
                                                                </div>`);
                        break;
                    case 'VIDEO':
                        videoTrack.insertAdjacentHTML('beforeend', `<div draggable="true" class="clip" item-hash="${clip.hash}" style="left: ${ (clip.start * 100) / currentProject.duration }%; width: ${(clip.duration * 100) / currentProject.duration}%;">
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
            destinyElm = document.querySelector('#audio-track .track-line');
        } else if (destiny == 'VIDEO') {
            destinyElm = document.querySelector('#video-track .track-line');
        } else {
            console.error('Destiny not found!!!', arguments);
            return;
        }

        if (source == 'MEDIA') {
            let media = Project.findMedia(hash);
            if (media) {
                let previousElm = UI.findShadowElement(hash);
                if (!previousElm) {
                    destinyElm.insertAdjacentHTML('beforeend', `<div class="clip" item-temp-hash="${hash}" style="left: ${position}%; width: ${(media.duration * 100) / currentProject.duration}%;">
                                                                <div class="clip-content">
                                                                    <span class="clip-name">${media.name}</span>
                                                                </div>
                                                            </div>`);
                } else {
                    previousElm.style['left'] = `${position}%`;   
                }
            }
        }
    },    
    bindProjectProperties: function() {
        let txtProjectName = document.getElementById('txt-project-name');
        if (txtProjectName) {
            txtProjectName.value = currentProject.name;
            txtProjectName.onchange = function() {
                currentProject.name = txtProjectName.value;
            }
        }

        let cmbScreenSize = document.getElementById('cmb-screen-size');
        if (cmbScreenSize) {
            cmbScreenSize.value = currentProject.screenSize;
            cmbScreenSize.onchange = function() {
                currentProject.screenSize = cmbScreenSize.value;
            }
        }

        let cmbFrameRate = document.getElementById('cmb-frame-rate');
        if (cmbFrameRate) {
            cmbFrameRate.value = currentProject.frameRate;
            cmbFrameRate.onchange = function() {
                currentProject.frameRate = cmbFrameRate.value;
            }
        }
        let durationProject = document.getElementById('dur-project');
        if (durationProject) {
            UI.setupDurationControl(durationProject, 
                                    value => currentProject.duration = value, 
                                    () => currentProject.duration);
        }
    }, 
    setupDurationControl: function(root, changeListener, valueSupplier) {
        if (!root) {
            console.error("Duration control not found!!!");
            return;
        }

        root.querySelectorAll('input[type="number"]:not(.setup-done)')
            .forEach(nmbElm => {
                nmbElm.classList.add('setup-done');
                let nmbContainer = document.createElement('div');
                nmbElm.parentElement.insertBefore(nmbContainer, nmbElm);
                nmbContainer.appendChild(nmbElm);

                let btnContainer = document.createElement('div');
                btnContainer.classList.add('buttons');
                nmbContainer.appendChild(btnContainer);

                let incBtn = document.createElement('button');
                incBtn.innerText = '+'
                incBtn.onclick = function() {
                    nmbElm.stepUp();
                    var event = new Event('change');
                    nmbElm.dispatchEvent(event);
                };
                btnContainer.appendChild(incBtn);

                let decBtn = document.createElement('button');
                decBtn.innerText = '-'
                decBtn.onclick = function() {
                    nmbElm.stepDown();
                    var event = new Event('change');
                    nmbElm.dispatchEvent(event);
                };

                nmbElm.updateControlButtons = function() {
                    decBtn.disabled = nmbElm.value == 0;
                    incBtn.disabled = nmbElm.max == nmbElm.value;
                }
                btnContainer.appendChild(decBtn);
            });
        let hoursCtrl = root.querySelector('input[type="number"].hours');
        if (!hoursCtrl) {
            console.error('Hours controller not found!!!!', root);
            return;
        }

        let minutesCtrl = root.querySelector('input[type="number"].minutes');
        if (!minutesCtrl) {
            console.error('Minutes controller not found!!!!', root);
            return;
        }

        let secondsCtrl = root.querySelector('input[type="number"].seconds');
        if (!secondsCtrl) {
            console.error('Seconds controller not found!!!!', root);
            return;
        }

        let millisCtrl = root.querySelector('input[type="number"].millis');
        if (!millisCtrl) {
            console.error('Milliseconds controller not found!!!!', root);
            return;
        }

        let value = valueSupplier();
        let valueInMillis = value % 1000;
        let valueInSeconds = ((value - valueInMillis) / 1000) % 60;
        let valueInMinutes = ((value - valueInMillis - (valueInSeconds * 1000)) / (1000 * 60)) % 60;
        let valueInHours = (value - valueInMillis - (valueInSeconds * 1000) - (valueInMinutes * 1000 * 60)) / (1000 * 60 * 60);

        function durationChanged() {
            if (this.value > this.max) {
                this.value = this.max
            }
            this.updateControlButtons();
        }
        millisCtrl.onchange = durationChanged;
        secondsCtrl.onchange = durationChanged;
        minutesCtrl.onchange = durationChanged;
        hoursCtrl.onchange = durationChanged;

        millisCtrl.value = valueInMillis;
        secondsCtrl.value = valueInSeconds;
        minutesCtrl.value = valueInMinutes;
        hoursCtrl.value = valueInHours;
    }
};

const TimelineZoom = {
    zoomLevel: 1.0, // 1.0 = 100% zoom
    minZoom: 0.1,   // 10% zoom
    maxZoom: 5.0,   // 500% zoom
    zoomStep: 0.2,  // 20% zoom per click
    scrollPosition: 0, // 0-1 representing scroll position
    pixelsPerSecond: 100, // Base pixels per second at 100% zoom
    
    init: function() {
        this.setupScrollHandlers();
        this.setupZoomControls();
        this.updateTimelineDisplay();
    },
    
    setupScrollHandlers: function() {
        const scrollHandle = document.querySelector('.timeline-scroll .scroll-handle');
        const timelineScroll = document.querySelector('.timeline-scroll');
        const timelineRuler = document.querySelector('.timeline-ruler');
        
        if (!scrollHandle || !timelineScroll) return;
        
        let isDragging = false;
        let startX = 0;
        let startScrollLeft = 0;
        
        // Mouse drag handling
        scrollHandle.addEventListener('mousedown', (e) => {
            isDragging = true;
            startX = e.clientX;
            startScrollLeft = this.scrollPosition;
            scrollHandle.style.cursor = 'grabbing';
            e.preventDefault();
        });
        
        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;
    
            const startHandlePosition = scrollHandle.offsetLeft;
            const deltaX = e.clientX - startX;
            const scrollWidth = timelineScroll.clientWidth;
            const handleWidth = scrollHandle.clientWidth;
            const maxScroll = scrollWidth - handleWidth;
            if (maxScroll > 0) {
                const newHandlePosition = Math.max(0, Math.min(maxScroll, startHandlePosition + deltaX));
                scrollHandle.style.left = `${newHandlePosition}px`;
                timelineRuler.scrollLeft = deltaX + timelineRuler.scrollLeft ;
            }
        });
        
        document.addEventListener('mouseup', () => {
            isDragging = false;
            scrollHandle.style.cursor = 'pointer';
        });
        
        // Click to scroll
        timelineScroll.addEventListener('click', (e) => {
            if (e.target === timelineScroll) {
                const rect = timelineScroll.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const handleWidth = scrollHandle.clientWidth;
                
                this.scrollPosition = Math.max(0, Math.min(1, (clickX - handleWidth / 2) / (rect.width - handleWidth)));
                // this.updateTimelineDisplay();
            }
        });
    },
    
    setupZoomControls: function() {
        const zoomInBtn = document.querySelector('.timeline-controls .btn:nth-child(1)'); // 🔍+
        const zoomOutBtn = document.querySelector('.timeline-controls .btn:nth-child(2)'); // 🔍-
        const fitBtn = document.querySelector('.timeline-controls .btn:nth-child(3)'); // 📐 Fit
        
        if (zoomInBtn) {
            zoomInBtn.addEventListener('click', () => this.zoomIn());
        }
        
        if (zoomOutBtn) {
            zoomOutBtn.addEventListener('click', () => this.zoomOut());
        }
        
        if (fitBtn) {
            fitBtn.addEventListener('click', () => this.fitToTimeline());
        }
        
        // Mouse wheel zoom
        document.querySelector('.timeline-container').addEventListener('wheel', (e) => {
            if (e.ctrlKey) {
                e.preventDefault();
                if (e.deltaY < 0) {
                    this.zoomIn();
                } else {
                    this.zoomOut();
                }
            }
        });
    },
    
    zoomIn: function() {
        this.zoomLevel = Math.min(this.maxZoom, this.zoomLevel + this.zoomStep);
        this.updateTimelineDisplay();
    },
    
    zoomOut: function() {
        this.zoomLevel = Math.max(this.minZoom, this.zoomLevel - this.zoomStep);
        this.updateTimelineDisplay();
    },
    
    fitToTimeline: function() {
        // Calculate zoom level to fit entire project duration in view
        const timelineWidth = document.querySelector('.timeline-ruler').clientWidth;
        const totalSeconds = currentProject.duration / 1000;
        const requiredPixelsPerSecond = timelineWidth / totalSeconds;
        
        this.zoomLevel = Math.max(this.minZoom, Math.min(this.maxZoom, requiredPixelsPerSecond / this.pixelsPerSecond));
        this.scrollPosition = 0;
        this.updateTimelineDisplay();
    },
    
    updateTimelineDisplay: function() {
        this.updateRulerMarks();
        this.updateTrackLines();
        this.updateScrollHandle();
        this.updateClipPositions();
    },
    
    updateRulerMarks: function() {
        const rulerMarks = document.getElementById('rulerMarks');
        if (!rulerMarks) return;
        
        // Clear existing marks
        rulerMarks.innerHTML = '';
        const rule_width = 60;
        
        console.log(TimelineZoom.zoomLevel)
        rulerMarks.style.width = `${100 * TimelineZoom.zoomLevel}%`;
        const totalSeconds = currentProject.duration / 1000;
        const totalWidth = rulerMarks.offsetWidth;
        console.log('totalWidht', totalWidth);
        const totalMarks = totalWidth / rule_width; // each mark should have 30px;
        // Calculate appropriate time interval based on zoom level
        console.log(totalMarks);
        const timeInterval = currentProject.duration / totalMarks;
        console.log("Time interval", timeInterval)
        
        // Generate marks
        for (let i = 0; i < totalMarks; i++) {
            const time = i * timeInterval;
            const position = (i /totalMarks) * 100;
            
            // const mark = document.createElement('div');
            // mark.className = 'time-mark';
            // mark.style.left = `${position}%x`;
            
            // // Format time display based on zoom level
            // if (timeInterval >= 10) {
            //     mark.textContent = `${Math.floor(time / 60)}:${(time % 60).toString().padStart(2, '0')}`;
            // } else if (timeInterval >= 1) {
            //     mark.textContent = `${Math.floor(time / 60)}:${(time % 60).toString().padStart(2, '0')}`;
            // } else {
            //     mark.textContent = `${Math.floor(time / 60)}:${Math.floor(time % 60)}.${Math.floor((time % 1) * 10)}`;
            // }
                
            // rulerMarks.appendChild(mark);
            rulerMarks.insertAdjacentHTML('beforeend', `<div class="time-mark" style="left: ${ position }%">x</div>`);
        }
    },
    
    updateTrackLines: function() {
        const trackLines = document.querySelectorAll('.track-line');
        const visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        const totalWidth = visibleWidth * this.zoomLevel;
        
        trackLines.forEach(trackLine => {
            trackLine.style.background = `repeating-linear-gradient(
                90deg,
                transparent,
                transparent ${this.getGridSize() - 1}px,
                rgba(52, 73, 94, 0.3) ${this.getGridSize()}px
            )`;
            trackLine.style.width = `${totalWidth}px`;
            trackLine.style.transform = `translateX(-${this.scrollPosition * (totalWidth - visibleWidth)}px)`;
        });
    },
    
    updateScrollHandle: function() {
        const scrollHandle = document.querySelector('.timeline-scroll .scroll-handle');
        if (!scrollHandle) return;
        
        const visibleWidth = document.querySelector('.timeline-scroll').clientWidth;
        const totalWidth = visibleWidth * this.zoomLevel;
        
        if (totalWidth <= visibleWidth) {
            // Timeline fits entirely, no need for scroll
            scrollHandle.style.width = '100%';
            scrollHandle.style.cursor = 'default';
        } else {
            // Calculate handle size and position based on zoom level
            const handleWidth = (visibleWidth / totalWidth) * 100;
            scrollHandle.style.width = `${Math.max(20, handleWidth)}%`; // Minimum 20px width
            scrollHandle.style.cursor = 'pointer';
            scrollHandle.style.transform = `translateX(${this.scrollPosition * (100 - handleWidth)}%)`;
        }

        const scrollDisplay = document.querySelector('.timeline-header .timeline-zoom-level span');
        if (scrollDisplay) {
            scrollDisplay.innerText = `Zoom: ${ Math.round(TimelineZoom.zoomLevel * 100) }%`;
        }
    },
    
    updateClipPositions: function() {
        const clips = document.querySelectorAll('.clip');
        const visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        const totalWidth = visibleWidth * this.zoomLevel;
        
        clips.forEach(clip => {
            const clipHash = clip.getAttribute('item-hash');
            const clipData = Project.findClip(clipHash);
            
            if (clipData) {
                const startPercent = (clipData.start * 100) / currentProject.duration;
                const durationPercent = (clipData.duration * 100) / currentProject.duration;
                
                const startPixels = (startPercent / 100) * totalWidth;
                const durationPixels = (durationPercent / 100) * totalWidth;
                
                clip.style.left = `${startPixels - (this.scrollPosition * (totalWidth - visibleWidth))}px`;
                clip.style.width = `${durationPixels}px`;
            }
        });
    },
    
    getGridSize: function() {
        // Return grid size in pixels based on zoom level
        if (this.zoomLevel <= 0.3) return 100; // Large grid at low zoom
        if (this.zoomLevel <= 1) return 50;    // Medium grid at medium zoom
        if (this.zoomLevel <= 2) return 25;    // Small grid at high zoom
        return 10;                             // Very small grid at very high zoom
    },
    
    // Helper method to convert time to pixels
    timeToPixels: function(timeInMillis) {
        const totalSeconds = currentProject.duration / 1000;
        const visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        const totalWidth = visibleWidth * this.zoomLevel;
        return (timeInMillis / currentProject.duration) * totalWidth;
    },
    
    // Helper method to convert pixels to time
    pixelsToTime: function(pixels) {
        const totalSeconds = currentProject.duration / 1000;
        const visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        const totalWidth = visibleWidth * this.zoomLevel;
        return (pixels / totalWidth) * currentProject.duration;
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
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.add('active');                
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
                // console.log(`clientX=${e.clientX} layerX=${e.layerX}`);
                // let elementNewXPos = e.clientX - e.layerX;
                // console.log(`parent=${parentXPos} new=${elementNewXPos}`);
                // console
                // console.log(e)
            }
        },
        dragleave: function(e) {
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.remove('active');
                e.preventDefault();
                e.stopPropagation();
                UI.removeShadowComponent();
            }
        },
        drop: function(e) {
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['AUDIO', 'VIDEO'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.remove('active');
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
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.add('active');                
                let position = DragNDrop.calculateDropPosition(e);
                if (position >= 0) {
                    UI.updateShadowElement(position, DragNDrop.getMediaHash(e), 'MEDIA', 'VIDEO');
                }
                e.preventDefault();
                e.stopPropagation();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'VIDEO') {
                console.log(e);
            }
        },
        dragleave: function(e) {
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.remove('active');
                e.preventDefault();
                e.stopPropagation();
                UI.removeShadowComponent();
            } else if (DragNDrop.isClip(e) && DragNDrop.getClipType(e) == 'VIDEO') {
                console.log(e);
            }
        },
        drop: function(e) {
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e) && ['VIDEO', 'IMAGE'].indexOf(DragNDrop.getMediaType(e)) != -1) {
                let evntElement = e.realTarget || e.target;
                evntElement.classList.remove('active');
                let position = DragNDrop.calculateDropPosition(e);
                UI.removeShadowComponent();
                Project.addClipMedia(DragNDrop.getMediaHash(e), 'VIDEO', position);
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
    // Initialize timeline zoom and scroll
    TimelineZoom.init();

    UI.reconciliateMedias();
    UI.bindProjectProperties();
    TimelineZoom.updateRulerMarks();

    // Tab switching
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.classList.remove('active'));
            let evntElement = e.realTarget || e.target;
            evntElement.classList.add('active');
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

    document.addEventListener('dragover', function(e) {
        function eventIsOverElement(evnt, element) {
            let rects = element.getBoundingClientRect();
            return evnt.x >= rects.x && 
                   evnt.x <= rects.x + rects.width && 
                   evnt.y >= rects.y && 
                   evnt.y <= rects.y + rects.height;
        }
        let correctElm = ['video-track', 'audio-track'].map(id => document.getElementById(id))
                                                       .find(elm => eventIsOverElement(e, elm));
        if (correctElm) {
            let track = [...correctElm.querySelectorAll('.track-line')].find(track => eventIsOverElement(e, track));
            if (track) {
                e.realTarget = track;
                staticElementsEvents[correctElm.id]['dragover'](e);
            }
        }
    });


    document.addEventListener('dragleave', function(e) {
        function eventIsOverElement(evnt, element) {
            let rects = element.getBoundingClientRect();
            return evnt.x >= rects.x && 
                   evnt.x <= rects.x + rects.width && 
                   evnt.y >= rects.y && 
                   evnt.y <= rects.y + rects.height;
        }
        let correctElm = ['video-track', 'audio-track'].map(id => document.getElementById(id))
                                                       .find(elm => eventIsOverElement(e, elm));
        if (correctElm) {
            let track = [...correctElm.querySelectorAll('.track-line')].find(track => eventIsOverElement(e, track));
            if (track) {
                e.realTarget = track;
                staticElementsEvents[correctElm.id]['dragleave'](e);
            }
        }
    });


    document.addEventListener('drop', function(e) {
        function eventIsOverElement(evnt, element) {
            let rects = element.getBoundingClientRect();
            return evnt.x >= rects.x && 
                   evnt.x <= rects.x + rects.width && 
                   evnt.y >= rects.y && 
                   evnt.y <= rects.y + rects.height;
        }
        let correctElm = ['video-track', 'audio-track'].map(id => document.getElementById(id))
                                                       .find(elm => eventIsOverElement(e, elm));
        if (correctElm) {
            let track = [...correctElm.querySelectorAll('.track-line')].find(track => eventIsOverElement(e, track));
            if (track) {
                e.realTarget = track;
                staticElementsEvents[correctElm.id]['drop'](e);
            }
        }
    })
});