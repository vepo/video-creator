console.log(currentProject);
function dataTransferHasFiles(dataTransfer) {
    return dataTransfer.items && 
           dataTransfer.items.length > 0 && 
           [...dataTransfer.items].some((item) => item.kind === 'file');
}
function mediaIcon(media) {
    if (media.type == 'VIDEO') {
        return '🎥';
    } else if(media.type == 'AUDIO') {
        return '🎵';
    } else {
        return '❔';
    }
}

function bindMediaDuration(duration) {
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
}

function reconciliateMediaList() {
    if (!currentProject) {
        console.error("Loaded project not found!!!");
        return;
    }

    if (!currentProject.medias || currentProject.medias.length == 0) {
        console.info("No media found!!!");
        return;
    }

    let mediaList = document.getElementById('media-list');
    if (!mediaList) {
        console.error("Media list not found!!!");
        return;
    }

    currentProject.medias.forEach(media => {
        var mediaItem = mediaList.querySelector(`[item-hash="${media.hash}"]`);
        if (!mediaItem) {
            mediaList.insertAdjacentHTML('beforeend', `<div class="file-item" item-hash="${media.hash}">
                                                           <div class="file-icon">${ mediaIcon(media) }</div>
                                                           <div class="file-info">
                                                               <div class="file-name">${ media.name }</div>
                                                               <div class="file-duration">${ bindMediaDuration(media.duration) }</div>
                                                           </div>
                                                       </div>`);
        }
    })
}
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
            reconciliateMediaList();
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error uploading file: ' + error.message);
        });
    }
}

const events = {
    'project-files-container': {
        'dragover': function(e) {
            if (e.dataTransfer && dataTransferHasFiles(e.dataTransfer)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.add('enabled');
                }
                e.preventDefault();
                e.stopPropagation();
            }
        },
        'dragleave': function(e) {
            if (e.dataTransfer && dataTransferHasFiles(e.dataTransfer)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.remove('enabled');
                }
                e.preventDefault();
                e.stopPropagation();
            }
        },
        'drop': function(e) {
            if (e.dataTransfer && dataTransferHasFiles(e.dataTransfer)) {
                let dropContainer = document.querySelector('.project-panel .drop-zone');
                if (dropContainer) {
                    dropContainer.classList.remove('enabled');
                }

                e.preventDefault();
                e.stopPropagation();
                [...e.dataTransfer.items].forEach((file) => MediaLibrary.add(file.getAsFile()));
            }
        }
    }
}
// Simple script for basic interactions
document.addEventListener('DOMContentLoaded', function() {

    // setup UI elements events handlers
    Object.entries(events).forEach(([element, events]) => {
        let elm = document.getElementById(element);
        if (elm) {
            Object.entries(events).forEach(([event, handler]) => {
                elm.addEventListener(event, handler);
            });
        }
    });

    reconciliateMediaList();

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