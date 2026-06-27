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
    ensureStructure: function() {
        if (!currentProject) {
            return;
        }
        if (!currentProject.medias) {
            currentProject.medias = [];
        }
        if (!currentProject.clips) {
            currentProject.clips = [];
        }
        if (!currentProject.tracks || currentProject.tracks.length === 0) {
            currentProject.tracks = [
                { index: 0, name: 'Video Track 1', type: 'VIDEO', muted: false, locked: false },
                { index: 1, name: 'Audio Track 1', type: 'AUDIO', muted: false, locked: false }
            ];
        }
        currentProject.tracks.forEach(function(track, index) {
            track.index = index;
            if (typeof track.muted !== 'boolean') {
                track.muted = false;
            }
            if (typeof track.locked !== 'boolean') {
                track.locked = false;
            }
        });
        Project.allClips().forEach(function(clip) {
            if (typeof clip.trackIndex !== 'number') {
                clip.trackIndex = Project.defaultTrackForType(clip.type);
            }
            if (typeof clip.sourceIn !== 'number') {
                clip.sourceIn = 0;
            }
            if (typeof clip.sourceOut !== 'number') {
                clip.sourceOut = 0;
            }
        });
    },
    allTracks: function() {
        Project.ensureStructure();
        return currentProject.tracks;
    },
    findTrack: function(index) {
        return Project.allTracks().find(function(track) {
            return track.index === index;
        }) || null;
    },
    defaultTrackForType: function(type) {
        var track = Project.allTracks().find(function(t) {
            return t.type === type;
        });
        return track ? track.index : 0;
    },
    addTrack: function(type) {
        Project.ensureStructure();
        var count = Project.allTracks().filter(function(t) {
            return t.type === type;
        }).length + 1;
        var name = (type === 'AUDIO' ? 'Audio Track ' : 'Video Track ') + count;
        currentProject.tracks.push({
            index: currentProject.tracks.length,
            name: name,
            type: type,
            muted: false,
            locked: false
        });
        UI.reconciliateTracks();
        UI.reconciliateClips();
        ProjectSave.schedule();
    },
    removeTrack: function(trackIndex) {
        var track = Project.findTrack(trackIndex);
        if (!track) {
            return;
        }
        var sameTypeCount = Project.allTracks().filter(function(t) {
            return t.type === track.type;
        }).length;
        if (sameTypeCount <= 1) {
            UI.notify('Cannot remove the last ' + track.type.toLowerCase() + ' track.', 'error');
            return;
        }
        var hasClips = Project.allClips().some(function(clip) {
            return clip.trackIndex === trackIndex;
        });
        if (hasClips) {
            UI.notify('Remove or move clips before deleting this track.', 'error');
            return;
        }
        if (!confirm('Remove track "' + track.name + '"?')) {
            return;
        }
        currentProject.tracks.splice(trackIndex, 1);
        currentProject.tracks.forEach(function(t, i) {
            t.index = i;
        });
        Project.allClips().forEach(function(clip) {
            if (clip.trackIndex > trackIndex) {
                clip.trackIndex -= 1;
            }
        });
        UI.reconciliateTracks();
        UI.reconciliateClips();
        ProjectSave.schedule();
        UI.notify('Track removed.', 'info');
    },
    resolveAvTracksForDrop: function(targetTrackIndex) {
        var target = Project.findTrack(targetTrackIndex);
        if (!target) {
            return {
                video: Project.defaultTrackForType('VIDEO'),
                audio: Project.defaultTrackForType('AUDIO')
            };
        }
        if (target.type === 'VIDEO') {
            return { video: target.index, audio: Project.defaultTrackForType('AUDIO') };
        }
        return { video: Project.defaultTrackForType('VIDEO'), audio: target.index };
    },
    trackAcceptsMedia: function(trackIndex, mediaType) {
        var track = Project.findTrack(trackIndex);
        if (!track || track.locked) {
            return false;
        }
        if (track.type === 'VIDEO') {
            return ['VIDEO', 'IMAGE'].indexOf(mediaType) !== -1;
        }
        if (track.type === 'AUDIO') {
            return ['AUDIO', 'VIDEO'].indexOf(mediaType) !== -1;
        }
        return false;
    },
    findMedia: function(hash) {
        if (currentProject && currentProject.medias && currentProject.medias.length > 0) {
            return currentProject.medias.find(m => m.hash == hash);
        }
        return null;
    },
    resolveMediaType: function(media) {
        if (!media) {
            return null;
        }
        if (media.type && media.type !== 'UNKNOWN') {
            return media.type;
        }
        var mime = media.mimeType || '';
        if (mime.indexOf('video/') === 0) {
            return 'VIDEO';
        }
        if (mime.indexOf('audio/') === 0) {
            return 'AUDIO';
        }
        if (mime.indexOf('image/') === 0) {
            return 'IMAGE';
        }
        return media.type || null;
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
    findSyncedClips: function(syncGroup) {
        if (!syncGroup || !currentProject || !currentProject.clips) {
            return [];
        }
        return currentProject.clips.filter(function(clip) {
            return clip.syncGroup === syncGroup;
        });
    },
    clipDuration: function(media) {
        if (media && typeof media.duration === 'number' && media.duration > 0) {
            return media.duration;
        }
        return 5000;
    },
    buildClip: function(media, type, startMs, duration, hash, syncGroup, trackIndex) {
        return {
            name: media.name || '',
            hash: hash,
            mediaHash: media.hash,
            duration: duration,
            type: type,
            start: startMs,
            speed: 1,
            syncGroup: syncGroup || null,
            trackIndex: typeof trackIndex === 'number' ? trackIndex : Project.defaultTrackForType(type),
            sourceIn: 0,
            sourceOut: 0
        };
    },
    addMediaToTimeline: function(mediaHash, positionPercent, targetTrackIndex) {
        var media = Project.findMedia(mediaHash);
        if (!media) {
            console.error('Media not found!!!', mediaHash);
            return;
        }
        if (!currentProject.clips) {
            currentProject.clips = [];
        }

        var startMs = Math.round(currentProject.duration * positionPercent / 100);
        var duration = Project.clipDuration(media);
        var baseIndex = currentProject.clips.length;
        var tracks = Project.resolveAvTracksForDrop(targetTrackIndex);

        var mediaType = Project.resolveMediaType(media);
        if (mediaType === 'VIDEO') {
            var syncGroup = 'sync-' + media.hash + '-' + startMs + '-' + baseIndex;
            Hash.generate(media.hash + '-' + baseIndex + '-video').then(function(videoHash) {
                Hash.generate(media.hash + '-' + baseIndex + '-audio').then(function(audioHash) {
                    currentProject.clips.push(
                        Project.buildClip(media, 'VIDEO', startMs, duration, videoHash, syncGroup, tracks.video),
                        Project.buildClip(media, 'AUDIO', startMs, duration, audioHash, syncGroup, tracks.audio)
                    );
                    UI.reconciliateClips();
                    ProjectSave.schedule();
                });
            });
        } else if (mediaType === 'AUDIO') {
            var audioTrack = typeof targetTrackIndex === 'number' ? targetTrackIndex : tracks.audio;
            Hash.generate(media.hash + '-' + baseIndex + '-audio').then(function(audioHash) {
                currentProject.clips.push(
                    Project.buildClip(media, 'AUDIO', startMs, duration, audioHash, null, audioTrack)
                );
                UI.reconciliateClips();
                ProjectSave.schedule();
            });
        } else if (mediaType === 'IMAGE') {
            var videoTrack = typeof targetTrackIndex === 'number' ? targetTrackIndex : tracks.video;
            Hash.generate(media.hash + '-' + baseIndex + '-video').then(function(videoHash) {
                currentProject.clips.push(
                    Project.buildClip(media, 'VIDEO', startMs, duration, videoHash, null, videoTrack)
                );
                UI.reconciliateClips();
                ProjectSave.schedule();
            });
        } else {
            console.error('Unsupported media type for timeline:', mediaType, media);
        }
    },
    clampClipStart: function(startMs, duration) {
        var maxStart = Math.max(0, currentProject.duration - duration);
        return Math.max(0, Math.min(maxStart, Math.round(startMs)));
    },
    acceptsClipOnTrack: function(clipHash, trackIndex) {
        var clip = Project.findClip(clipHash);
        var track = Project.findTrack(trackIndex);
        if (!clip || !track || track.locked) {
            return false;
        }
        if (clip.trackIndex === trackIndex || clip.type === track.type) {
            return true;
        }
        if (clip.syncGroup) {
            return Project.findSyncedClips(clip.syncGroup).some(function(c) {
                return c.trackIndex === trackIndex || c.type === track.type;
            });
        }
        return false;
    },
    moveClipToStart: function(clipHash, startMs, trackIndex) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            console.error('Clip not found for move', clipHash);
            return;
        }
        var clips = clip.syncGroup ? Project.findSyncedClips(clip.syncGroup) : [clip];
        var media = Project.findMedia(clip.mediaHash);
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var clampedStart = Project.clampClipStart(startMs, duration);
        var avTracks = typeof trackIndex === 'number' ? Project.resolveAvTracksForDrop(trackIndex) : null;
        clips.forEach(function(c) {
            c.start = clampedStart;
            if (avTracks) {
                c.trackIndex = c.type === 'AUDIO' ? avTracks.audio : avTracks.video;
            }
        });
        UI.reconciliateClips();
        UI.setClipDraggingState(null, false);
        ProjectSave.schedule();
    },
    requestDeleteClip: function(clipHash) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var media = Project.findMedia(clip.mediaHash);
        var name = clip.name || (media && media.name) || 'this clip';
        if (!confirm('Delete "' + name + '" from the timeline?')) {
            return;
        }
        Project.deleteClip(clipHash);
        UI.notify('Clip deleted.', 'info');
    },
    deleteClip: function(clipHash) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var toRemove = clip.syncGroup ? Project.findSyncedClips(clip.syncGroup) : [clip];
        var removeHashes = toRemove.map(function(c) { return c.hash; });
        currentProject.clips = currentProject.clips.filter(function(c) {
            return removeHashes.indexOf(c.hash) === -1;
        });
        removeHashes.forEach(function(hash) {
            var el = document.querySelector('[item-hash="' + hash + '"]');
            if (el && el.parentElement) {
                el.parentElement.removeChild(el);
            }
        });
        UI.clearItemProperties();
        UI.reconciliateClips();
        ProjectSave.schedule();
        UI.updateTimelineActions();
    },
    splitClip: function(clipHash, splitAtMs) {
        var clip = Project.findClip(clipHash);
        if (!clip || splitAtMs <= 0 || splitAtMs >= clip.duration) {
            return Promise.resolve();
        }
        var media = Project.findMedia(clip.mediaHash);
        var sourceIn = clip.sourceIn || 0;
        var rightDuration = clip.duration - splitAtMs;
        clip.duration = splitAtMs;
        if (clip.sourceOut > 0) {
            clip.sourceOut = sourceIn + splitAtMs;
        }
        return Hash.generate(clip.hash + '-split-' + Date.now() + '-' + Math.random()).then(function(newHash) {
            currentProject.clips.push({
                name: clip.name,
                hash: newHash,
                mediaHash: clip.mediaHash,
                duration: rightDuration,
                type: clip.type,
                start: clip.start + splitAtMs,
                speed: clip.speed || 1,
                syncGroup: null,
                trackIndex: clip.trackIndex,
                sourceIn: sourceIn + splitAtMs,
                sourceOut: media && media.duration > 0 ? media.duration : 0
            });
        });
    },
    cutSelectedClipAtPlayhead: function() {
        var hash = UI.getSelectedClipHash();
        if (!hash) {
            UI.notify('Select a clip to cut.', 'error');
            return;
        }
        var clip = Project.findClip(hash);
        var playheadMs = Playhead.positionMs();
        if (!clip) {
            return;
        }
        var cutPoint = playheadMs - clip.start;
        if (cutPoint <= 0 || cutPoint >= clip.duration) {
            UI.notify('Move the playhead inside the selected clip to cut.', 'error');
            return;
        }
        var group = clip.syncGroup ? Project.findSyncedClips(clip.syncGroup) : [clip];
        Promise.all(group.map(function(c) {
            return Project.splitClip(c.hash, cutPoint);
        })).then(function() {
            UI.reconciliateClips();
            ProjectSave.schedule();
            UI.notify('Clip cut at playhead.', 'success');
        });
    }
};

const DragNDrop = {
    activeDrag: null,
    clearActiveDrag: function() {
        this.activeDrag = null;
    },
    hasDragKind: function(e, kind) {
        if (this.activeDrag && this.activeDrag.kind === kind) {
            return true;
        }
        if (!e || !e.dataTransfer) {
            return false;
        }
        var dragType = e.dataTransfer.getData('video-editor/drag-type');
        if (dragType === kind) {
            return true;
        }
        var types = e.dataTransfer.types ? Array.from(e.dataTransfer.types) : [];
        if (kind === 'MEDIA') {
            return types.indexOf('video-editor/media-hash') !== -1;
        }
        if (kind === 'CLIP') {
            return types.indexOf('video-editor/clip-hash') !== -1;
        }
        return false;
    },
    isMediaFile: function(e) {
        // Drag came from outside
        // just check for files
        return e.dataTransfer && 
               e.dataTransfer.items && 
               e.dataTransfer.items.length > 0 && 
               [...e.dataTransfer.items].some((item) => item.kind === 'file');
    }, 
    isMedia: function(e) {
        return DragNDrop.hasDragKind(e, 'MEDIA');
    },
    setupMedia: function(hash, e) {
        let media = Project.findMedia(hash);
        if (!media) {
            console.error("Media not found!!!", hash);
            return;
        }
        var mediaType = Project.resolveMediaType(media);
        e.dataTransfer.setData('video-editor/media-hash', hash);
        e.dataTransfer.setData('video-editor/media-type', mediaType);
        e.dataTransfer.setData('video-editor/drag-type', 'MEDIA');
        this.activeDrag = { kind: 'MEDIA', hash: hash, mediaType: mediaType };
    },
    getMediaType: function(e) {
        if (this.activeDrag && this.activeDrag.kind === 'MEDIA') {
            return this.activeDrag.mediaType;
        }
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/media-type');
        }
        return null;
    },
    getMediaHash: function(e) {
        if (this.activeDrag && this.activeDrag.kind === 'MEDIA') {
            return this.activeDrag.hash;
        }
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/media-hash');
        }
        return null;
    },
    getDraggedElement: function(e) {
        if (this.activeDrag) {
            if (this.activeDrag.kind === 'CLIP') {
                return UI.getElementByHash(this.activeDrag.hash);
            }
            if (this.activeDrag.kind === 'MEDIA') {
                return UI.getElementByHash(this.activeDrag.hash);
            }
        }
        if (e.dataTransfer && e.dataTransfer.getData('video-editor/drag-type')) {
            if (e.dataTransfer.getData('video-editor/drag-type') == 'CLIP') {
                return UI.getElementByHash(e.dataTransfer.getData('video-editor/clip-hash'));
            } else if (e.dataTransfer.getData('video-editor/drag-type') == 'MEDIA') {
                return UI.getElementByHash(e.dataTransfer.getData('video-editor/media-hash'));
            }
        }
        return null;
    },
    setupClip: function(hash, e) {
        let clip = Project.findClip(hash);
        console.debug("Clip", clip);
        if (!clip) {
            console.error("Clip not found!!!", hash);
            return;
        }
        e.dataTransfer.setData('video-editor/clip-hash', hash);
        e.dataTransfer.setData('video-editor/clip-type', clip.type);
        e.dataTransfer.setData('video-editor/drag-type', 'CLIP');
        var clipElm = UI.getElementByHash(hash);
        var grabOffsetMs = 0;
        if (clipElm && typeof e.clientX === 'number') {
            var clipRect = clipElm.getBoundingClientRect();
            var trackLine = clipElm.closest('.track-line');
            var totalWidth = trackLine ? trackLine.offsetWidth : clipRect.width;
            if (totalWidth > 0) {
                grabOffsetMs = Math.round(((e.clientX - clipRect.left) / totalWidth) * currentProject.duration);
            }
        }
        var media = Project.findMedia(clip.mediaHash);
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        this.activeDrag = {
            kind: 'CLIP',
            hash: hash,
            clipType: clip.type,
            syncGroup: clip.syncGroup || null,
            grabOffsetMs: grabOffsetMs,
            duration: duration
        };
        UI.setClipDraggingState(hash, true);
    },
    isClip: function(e) {
        return DragNDrop.hasDragKind(e, 'CLIP');
    },
    getClipType: function(e) {
        if (this.activeDrag && this.activeDrag.kind === 'CLIP') {
            return this.activeDrag.clipType;
        }
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/clip-type');
        }
        return null;
    },
    getClipHash: function(e) {
        if (this.activeDrag && this.activeDrag.kind === 'CLIP') {
            return this.activeDrag.hash;
        }
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/clip-hash');
        }
        return null;
    },
    getDragType: function(e) {
        if (this.activeDrag) {
            return this.activeDrag.kind;
        }
        if (e && e.dataTransfer) {
            return e.dataTransfer.getData('video-editor/drag-type');
        }
        return null;
    },
    calculateDropPosition: function(e) {
        let target = e.realTarget || e.target;
        while (target && !target.classList.contains('track-line')) {
            target = target.parentElement;
        }
        if (!target) {
            return 0;
        }
        const containerRect = target.getBoundingClientRect();
        const totalWidth = target.offsetWidth || containerRect.width;
        const visibleWidth = containerRect.width;
        const scrollOffset = TimelineZoom.scrollPosition * Math.max(0, totalWidth - visibleWidth);
        const pointerX = typeof e.clientX === 'number' ? e.clientX : e.x;
        const clientX = pointerX - containerRect.left + scrollOffset;
        return Math.max(0, Math.min(100, (clientX / totalWidth) * 100));
    },
    clipStartFromPosition: function(positionPercent) {
        var startMs = Math.round(currentProject.duration * positionPercent / 100);
        if (this.activeDrag && this.activeDrag.kind === 'CLIP') {
            startMs -= this.activeDrag.grabOffsetMs || 0;
            startMs = Project.clampClipStart(startMs, this.activeDrag.duration);
        }
        return startMs;
    },
    verifyEventClass: function(e, requiredClass) {
        let currentTarget = e.realTarget || e.target;
        if (!currentTarget) {
            console.error('Drop target not found');
            return;
        }
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
    notifyTimer: null,
    notify: function(message, type) {
        type = type || 'info';
        var bar = document.getElementById('editorStatus');
        if (!bar) {
            console.error(message);
            return;
        }
        bar.textContent = message;
        bar.className = 'editor-status editor-status--' + type;
        bar.classList.remove('visually-hidden');
        if (this.notifyTimer) {
            clearTimeout(this.notifyTimer);
            this.notifyTimer = null;
        }
        if (type !== 'error') {
            var self = this;
            this.notifyTimer = setTimeout(function() {
                bar.classList.add('visually-hidden');
            }, 6000);
        }
    },
    setButtonBusy: function(btn, busy) {
        if (!btn) {
            return;
        }
        btn.disabled = !!busy;
        if (busy) {
            btn.setAttribute('aria-busy', 'true');
        } else {
            btn.removeAttribute('aria-busy');
        }
    },
    deselectAll: function() {
        var selected = document.querySelector('[item-hash].selected');
        if (selected) {
            selected.classList.remove('selected');
            UI.clearItemProperties();
        }
        UI.updateTimelineActions();
    },
    updateTimelineActions: function() {
        var hash = UI.getSelectedClipHash();
        var cutBtn = document.getElementById('cutClipBtn');
        var deleteBtn = document.getElementById('deleteClipBtn');
        if (cutBtn) {
            cutBtn.disabled = !hash;
        }
        if (deleteBtn) {
            deleteBtn.disabled = !hash;
        }
    },
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
        UI.updateTimelineActions();
    },
    mediaIcon: function(media) {
        var iconId = 'unknown';
        var mediaType = Project.resolveMediaType(media);
        if (mediaType == 'VIDEO') {
            iconId = 'video';
        } else if (mediaType == 'AUDIO') {
            iconId = 'audio';
        } else if (mediaType == 'IMAGE') {
            iconId = 'image';
        }
        return '<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#' + iconId + '"/></svg>';
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
                                                                     <label>Media Name</label>
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
    clipLayoutPercent: function(clip, media) {
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        return {
            left: (clip.start * 100) / currentProject.duration,
            width: (duration * 100) / currentProject.duration
        };
    },
    applyClipLayout: function(clipElm, clip, media) {
        var layout = UI.clipLayoutPercent(clip, media);
        clipElm.style.left = layout.left + '%';
        clipElm.style.width = layout.width + '%';
    },
    getTrackLine: function(trackIndex) {
        var trackArea = document.querySelector('.track-area[data-track-index="' + trackIndex + '"]');
        return trackArea ? trackArea.querySelector('.track-line') : null;
    },
    getTrackIndexFromEvent: function(e) {
        var target = e.realTarget || e.target;
        if (!target || !target.closest) {
            return null;
        }
        var trackArea = target.closest('.track-area');
        if (!trackArea) {
            return null;
        }
        return parseInt(trackArea.getAttribute('data-track-index'), 10);
    },
    getSelectedClipHash: function() {
        var selected = document.querySelector('.clip[item-hash].selected');
        return selected ? selected.getAttribute('item-hash') : null;
    },
    reconciliateTracks: function() {
        var container = document.getElementById('tracks-container');
        if (!container) {
            return;
        }
        container.innerHTML = '';
        Project.allTracks().forEach(function(track) {
            var icon = track.type === 'AUDIO' ? 'audio' : 'video';
            container.insertAdjacentHTML('beforeend',
                '<div class="track-row" data-track-row="' + track.index + '">' +
                '<div class="track-header">' +
                '<div class="track-info">' +
                '<svg class="icon track-icon" aria-hidden="true"><use href="/icons/icons.svg#' + icon + '"/></svg>' +
                '<span class="track-name">' + track.name + '</span>' +
                '</div>' +
                '<div class="track-controls">' +
                '<button class="track-btn mute-btn' + (track.muted ? ' track-btn--active' : '') + '" type="button" data-track-index="' + track.index + '"' +
                ' aria-label="Mute track" aria-pressed="' + track.muted + '" title="Mute track">' +
                '<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#mute"/></svg></button>' +
                '<button class="track-btn lock-btn' + (track.locked ? ' track-btn--active' : '') + '" type="button" data-track-index="' + track.index + '"' +
                ' aria-label="Lock track" aria-pressed="' + track.locked + '" title="Lock track">' +
                '<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#lock"/></svg></button>' +
                '<button class="track-btn remove-track-btn" type="button" data-track-index="' + track.index + '" aria-label="Remove track" title="Remove track">&times;</button>' +
                '</div></div>' +
                '<div class="track-area' + (track.locked ? ' track-area--locked' : '') + '" data-track-index="' + track.index + '" data-track-type="' + track.type + '">' +
                '<div class="track-line"></div></div></div>');
        });
        container.querySelectorAll('.mute-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                var track = Project.findTrack(parseInt(btn.getAttribute('data-track-index'), 10));
                if (track) {
                    track.muted = !track.muted;
                    btn.setAttribute('aria-pressed', track.muted);
                    btn.classList.toggle('track-btn--active', track.muted);
                    ProjectSave.schedule();
                }
            });
        });
        container.querySelectorAll('.lock-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                var track = Project.findTrack(parseInt(btn.getAttribute('data-track-index'), 10));
                if (track) {
                    track.locked = !track.locked;
                    UI.reconciliateTracks();
                    ProjectSave.schedule();
                }
            });
        });
        container.querySelectorAll('.remove-track-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                Project.removeTrack(parseInt(btn.getAttribute('data-track-index'), 10));
            });
        });
        TimelineZoom.updateTrackLines();
    },
    reconciliateClips: function() {
        document.querySelectorAll('.clip[item-hash]').forEach(function(elm) {
            elm.parentElement.removeChild(elm);
        });

        Project.allClips().forEach(function(clip) {
            var media = Project.findMedia(clip.mediaHash);
            if (!media) {
                console.error('Media missing for clip', clip.hash);
                return;
            }
            var trackLine = UI.getTrackLine(clip.trackIndex);
            if (!trackLine) {
                console.error('Track missing for clip', clip.hash, clip.trackIndex);
                return;
            }
            var layout = UI.clipLayoutPercent(clip, media);
            var syncAttr = clip.syncGroup ? ' data-sync-group="' + clip.syncGroup + '"' : '';
            var clipClass = clip.type === 'AUDIO' ? 'clip clip--audio' : 'clip';
            trackLine.insertAdjacentHTML('beforeend',
                '<div draggable="true" class="' + clipClass + '" item-hash="' + clip.hash + '"' + syncAttr +
                ' style="left: ' + layout.left + '%; width: ' + layout.width + '%;">' +
                '<div class="clip-content"><span class="clip-name" title="' + media.name + '">' + media.name + '</span></div></div>');
            var clipElm = document.querySelector('[item-hash="' + clip.hash + '"]');
            if (clipElm) {
                UI.setupElement(clipElm, 'CLIP');
            }
        });
        TimelineZoom.updateClipPositions();
    },
    setClipDraggingState: function(clipHash, dragging) {
        document.querySelectorAll('.clip[item-hash].clip--dragging').forEach(function(elm) {
            elm.classList.remove('clip--dragging');
        });
        if (!dragging || !clipHash) {
            return;
        }
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var hashes = clip.syncGroup
            ? Project.findSyncedClips(clip.syncGroup).map(function(c) { return c.hash; })
            : [clipHash];
        hashes.forEach(function(hash) {
            var elm = UI.getElementByHash(hash);
            if (elm) {
                elm.classList.add('clip--dragging');
            }
        });
    },
    updateShadowElementForClip: function(startMs, clip, extraClass) {
        var media = Project.findMedia(clip.mediaHash);
        if (!media) {
            return;
        }
        var layout = UI.clipLayoutPercent({ start: startMs, duration: clip.duration }, media);
        var trackLine = UI.getTrackLine(clip.trackIndex);
        if (!trackLine) {
            return;
        }
        var tempId = 'move-' + clip.hash;
        var clipClass = 'clip' + (extraClass ? ' ' + extraClass : '');
        var previousElm = UI.findShadowElement(tempId);
        if (!previousElm) {
            trackLine.insertAdjacentHTML('beforeend',
                '<div class="' + clipClass + '" item-temp-hash="' + tempId + '" style="left: ' + layout.left +
                '%; width: ' + layout.width + '%; opacity: 0.6;">' +
                '<div class="clip-content"><span class="clip-name" title="' + media.name + '">' + media.name + '</span></div></div>');
        } else {
            previousElm.style.left = layout.left + '%';
            previousElm.style.width = layout.width + '%';
        }
    },
    updateShadowElementsForClipMove: function(startMs, clipHash) {
        UI.removeShadowComponent();
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var clips = clip.syncGroup ? Project.findSyncedClips(clip.syncGroup) : [clip];
        clips.forEach(function(c) {
            UI.updateShadowElementForClip(startMs, c, c.type === 'AUDIO' ? 'clip--audio' : '');
        });
    },
    highlightTrack: function(e, active) {
        var evntElement = e.realTarget || e.target;
        if (!evntElement) {
            return;
        }
        evntElement.classList.toggle('active', active);
        var trackArea = evntElement.closest('.track-area');
        if (trackArea) {
            trackArea.classList.toggle('active', active);
        }
    },
    handleClipDragover: function(e, trackIndex) {
        DragNDrop.verifyEventClass(e, 'track-line');
        if (!DragNDrop.isClip(e)) {
            return false;
        }
        var clipHash = DragNDrop.getClipHash(e);
        if (!Project.acceptsClipOnTrack(clipHash, trackIndex)) {
            return false;
        }
        var startMs = DragNDrop.clipStartFromPosition(DragNDrop.calculateDropPosition(e));
        UI.updateShadowElementsForClipMove(startMs, clipHash);
        UI.highlightTrack(e, true);
        e.preventDefault();
        e.stopPropagation();
        return true;
    },
    handleClipDrop: function(e, trackIndex) {
        DragNDrop.verifyEventClass(e, 'track-line');
        if (!DragNDrop.isClip(e)) {
            return false;
        }
        var clipHash = DragNDrop.getClipHash(e);
        if (!Project.acceptsClipOnTrack(clipHash, trackIndex)) {
            return false;
        }
        var startMs = DragNDrop.clipStartFromPosition(DragNDrop.calculateDropPosition(e));
        UI.highlightTrack(e, false);
        UI.removeShadowComponent();
        Project.moveClipToStart(clipHash, startMs, trackIndex);
        e.preventDefault();
        e.stopPropagation();
        return true;
    },
    findShadowElement: function(tempId) {
        return document.querySelector('[item-temp-hash="' + tempId + '"]');
    },
    removeShadowComponent: function() {
        document.querySelectorAll('[item-temp-hash]').forEach(function(shadow) {
            shadow.parentElement.removeChild(shadow);
        });
    },
    updateShadowElementOnTrack: function(position, tempId, trackIndex, widthPercent, extraClass) {
        var trackLine = UI.getTrackLine(trackIndex);
        if (!trackLine) {
            return;
        }

        var mediaHash = tempId.replace(/-(video|audio)$/, '');
        var media = Project.findMedia(mediaHash);
        if (!media) {
            return;
        }

        var clipClass = 'clip' + (extraClass ? ' ' + extraClass : '');
        var previousElm = UI.findShadowElement(tempId);
        if (!previousElm) {
            trackLine.insertAdjacentHTML('beforeend',
                '<div class="' + clipClass + '" item-temp-hash="' + tempId + '" style="left: ' + position +
                '%; width: ' + widthPercent + '%; opacity: 0.6;">' +
                '<div class="clip-content"><span class="clip-name" title="' + media.name + '">' + media.name + '</span></div></div>');
        } else {
            previousElm.style.left = position + '%';
            previousElm.style.width = widthPercent + '%';
        }
    },
    updateShadowElementsForMedia: function(position, mediaHash, targetTrackIndex) {
        var media = Project.findMedia(mediaHash);
        if (!media) {
            return;
        }
        var widthPercent = (Project.clipDuration(media) * 100) / currentProject.duration;
        var tracks = Project.resolveAvTracksForDrop(targetTrackIndex);
        var mediaType = Project.resolveMediaType(media);

        if (mediaType === 'VIDEO') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-video', tracks.video, widthPercent, '');
            UI.updateShadowElementOnTrack(position, mediaHash + '-audio', tracks.audio, widthPercent, 'clip--audio');
        } else if (mediaType === 'IMAGE') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-video', tracks.video, widthPercent, '');
        } else if (mediaType === 'AUDIO') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-audio', tracks.audio, widthPercent, 'clip--audio');
        }
    },
    bindProjectProperties: function() {
        let txtProjectName = document.getElementById('txt-project-name');
        if (txtProjectName) {
            txtProjectName.value = currentProject.name;
            txtProjectName.onchange = function() {
                currentProject.name = txtProjectName.value;
                ProjectSave.schedule();
            }
        }

        let cmbScreenSize = document.getElementById('cmb-screen-size');
        if (cmbScreenSize) {
            cmbScreenSize.value = currentProject.screenSize;
            cmbScreenSize.onchange = function() {
                currentProject.screenSize = cmbScreenSize.value;
                ProjectSave.schedule();
            }
        }

        let cmbFrameRate = document.getElementById('cmb-frame-rate');
        if (cmbFrameRate) {
            cmbFrameRate.value = currentProject.frameRate;
            cmbFrameRate.onchange = function() {
                currentProject.frameRate = cmbFrameRate.value;
                ProjectSave.schedule();
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
                this.value = this.max;
            }
            this.updateControlButtons();
            var totalMs = (parseInt(hoursCtrl.value, 10) * 3600000) +
                (parseInt(minutesCtrl.value, 10) * 60000) +
                (parseInt(secondsCtrl.value, 10) * 1000) +
                parseInt(millisCtrl.value, 10);
            changeListener(totalMs);
            ProjectSave.schedule();
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
        let startMovementX = 0;
        let startComponentX = 0;
        // let startScrollLeft = 0;
        
        // Mouse drag handling
        scrollHandle.addEventListener('mousedown', (e) => {
            isDragging = true;
            startMovementX = e.clientX;
            startComponentX = scrollHandle.offsetLeft;
            // console.log(startMovementX);
            // startScrollLeft = this.scrollPosition;
            scrollHandle.style.cursor = 'grabbing';
            e.preventDefault();
        });
        
        document.addEventListener('mousemove', (e) => {
            if (!isDragging) return;
    
            const startHandlePosition = scrollHandle.offsetLeft;
            const deltaX = e.clientX - startMovementX;
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
        // timelineScroll.addEventListener('click', (e) => {
        //     if (e.target === timelineScroll) {
        //         const rect = timelineScroll.getBoundingClientRect();
        //         const clickX = e.clientX - rect.left;
        //         const handleWidth = scrollHandle.clientWidth;
                
        //         this.scrollPosition = Math.max(0, Math.min(1, (clickX - handleWidth / 2) / (rect.width - handleWidth)));
        //         // this.updateTimelineDisplay();
        //     }
        // });
    },
    
    setupZoomControls: function() {
        const zoomInBtn = document.getElementById('timelineZoomInBtn');
        const zoomOutBtn = document.getElementById('timelineZoomOutBtn');
        const fitBtn = document.getElementById('timelineFitBtn');
        
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
    
    formatRulerTime: function(timeMs) {
        var totalSeconds = Math.floor(timeMs / 1000);
        var hours = Math.floor(totalSeconds / 3600);
        var minutes = Math.floor((totalSeconds % 3600) / 60);
        var seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + ':' + minutes.toString().padStart(2, '0') + ':' + seconds.toString().padStart(2, '0');
        }
        return minutes + ':' + seconds.toString().padStart(2, '0');
    },

    updateRulerMarks: function() {
        const rulerMarks = document.getElementById('rulerMarks');
        if (!rulerMarks || !currentProject.duration) {
            return;
        }

        rulerMarks.innerHTML = '';
        const markWidthPx = 60;
        rulerMarks.style.width = `${100 * this.zoomLevel}%`;
        const totalWidth = rulerMarks.offsetWidth || document.querySelector('.timeline-ruler').clientWidth;
        const totalMarks = Math.max(1, Math.floor(totalWidth / markWidthPx));
        const timeIntervalMs = currentProject.duration / totalMarks;

        for (let i = 0; i <= totalMarks; i++) {
            const timeMs = Math.min(currentProject.duration, Math.round(i * timeIntervalMs));
            const position = (timeMs * 100) / currentProject.duration;
            const label = TimelineZoom.formatRulerTime(timeMs);
            rulerMarks.insertAdjacentHTML('beforeend',
                '<div class="time-mark" style="left: ' + position + '%">' + label + '</div>');
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

        const scrollDisplay = document.getElementById('zoomLevelLabel');
        if (scrollDisplay) {
            scrollDisplay.textContent = 'Zoom: ' + Math.round(TimelineZoom.zoomLevel * 100) + '%';
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
                const media = Project.findMedia(clipData.mediaHash);
                const duration = clipData.duration > 0 ? clipData.duration : Project.clipDuration(media);
                const startPercent = (clipData.start * 100) / currentProject.duration;
                const durationPercent = (duration * 100) / currentProject.duration;
                
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
    uploadsInProgress: 0,
    addFiles: function(fileList) {
        Array.from(fileList).forEach(function(file) {
            if (file) {
                MediaLibrary.add(file);
            }
        });
    },
    add: function(file) {
        if (!file) {
            console.error('No file provided for upload');
            return;
        }
        var projectId = currentProject.id;
        if (!projectId) {
            console.error('Project id not available');
            UI.notify('Cannot upload: project not loaded.', 'error');
            return;
        }
        var uploadBtn = document.getElementById('uploadBtn');
        MediaLibrary.uploadsInProgress += 1;
        UI.setButtonBusy(uploadBtn, true);
        UI.notify('Uploading ' + file.name + '…', 'info');
        var formData = new FormData();
        formData.append('name', file.name);
        formData.append('lastModified', file.lastModified);
        formData.append('file', file);

        fetch('/api/editor/' + projectId + '/media', {
            method: 'POST',
            body: formData
        })
        .then(function(response) { return response.json(); })
        .then(function(data) {
            if (data.error) {
                UI.notify('Upload failed: ' + data.error + ' — try again.', 'error');
                return;
            }
            if (!currentProject.medias) {
                currentProject.medias = [];
            }
            currentProject.medias.push(data);
            UI.reconciliateMedias();
            UI.notify('Added ' + file.name + ' to project.', 'success');
        })
        .catch(function(error) {
            console.error('Error:', error);
            UI.notify('Upload failed: ' + error.message + ' — check your connection and try again.', 'error');
        })
        .finally(function() {
            MediaLibrary.uploadsInProgress -= 1;
            if (MediaLibrary.uploadsInProgress <= 0) {
                UI.setButtonBusy(document.getElementById('uploadBtn'), false);
            }
        });
    }
};

const Playhead = {
    positionMs_: 0,
    positionMs: function() {
        return this.positionMs_;
    },
    init: function() {
        var ruler = document.querySelector('.timeline-ruler');
        if (ruler) {
            ruler.addEventListener('click', function(e) {
                var rect = ruler.getBoundingClientRect();
                var percent = (e.clientX - rect.left) / rect.width;
                Playhead.setPositionPercent(Math.max(0, Math.min(1, percent)));
            });
        }
        this.updateVisual();
    },
    setPositionPercent: function(percent) {
        this.positionMs_ = Math.round(currentProject.duration * percent);
        this.updateVisual();
    },
    setPositionMs: function(ms) {
        if (!currentProject || !currentProject.duration) {
            return;
        }
        this.positionMs_ = Math.max(0, Math.min(Math.round(ms), currentProject.duration));
        this.updateVisual();
    },
    updateVisual: function() {
        var playhead = document.getElementById('playhead');
        if (!playhead || !currentProject.duration) {
            return;
        }
        var percent = (this.positionMs_ * 100) / currentProject.duration;
        playhead.style.left = percent + '%';
        playhead.setAttribute('aria-valuenow', this.positionMs_);
        if (currentProject.duration) {
            playhead.setAttribute('aria-valuemax', currentProject.duration);
        }
    }
};

const ProjectSave = {
    timer: null,
    saving: false,
    schedule: function() {
        var self = this;
        if (this.timer) {
            clearTimeout(this.timer);
        }
        this.timer = setTimeout(function() {
            self.save({ silent: true });
        }, 800);
    },
    save: function(options) {
        options = options || {};
        var silent = !!options.silent;
        if (!currentProject || !currentProject.id) {
            return Promise.resolve();
        }
        if (this.saving) {
            return Promise.resolve();
        }
        this.saving = true;
        var saveBtn = document.getElementById('saveBtn');
        if (!silent) {
            UI.setButtonBusy(saveBtn, true);
            UI.notify('Saving project…', 'info');
        }
        return fetch('/api/editor/' + currentProject.id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(currentProject)
        }).then(function(response) {
            if (!response.ok) {
                return response.text().then(function(body) {
                    throw new Error('Save failed: ' + response.status + ' ' + body);
                });
            }
            return response.json();
        }).then(function() {
            if (!silent) {
                UI.notify('Project saved.', 'success');
            }
        }).catch(function(err) {
            console.error('Save error', err);
            if (silent) {
                UI.notify('Auto-save failed — click Save to retry.', 'error');
            } else {
                UI.notify('Save failed — try again or check your connection.', 'error');
            }
            throw err;
        }).finally(function() {
            ProjectSave.saving = false;
            if (!silent) {
                UI.setButtonBusy(document.getElementById('saveBtn'), false);
            }
        });
    },
    init: function() {
        var saveBtn = document.getElementById('saveBtn');
        if (saveBtn) {
            saveBtn.addEventListener('click', function() {
                ProjectSave.save();
            });
        }
    }
};

const Preview = {
    running: false,
    statusTimer: null,
    init: function() {
        this.container = document.querySelector('.preview-container');
        this.controls = document.getElementById('previewControls');
        this.video = document.getElementById('previewVideo');
        this.placeholder = document.getElementById('previewPlaceholder');
        this.status = document.getElementById('previewStatus');
        this.bindTransport(document.getElementById('playBtn'), 'play');
        this.bindTransport(document.getElementById('stopBtn'), 'stop');
        this.bindTransport(document.getElementById('previewPlayBtn'), 'play');
        this.bindTransport(document.getElementById('previewRewindBtn'), 'rewind');
        this.bindTransport(document.getElementById('previewForwardBtn'), 'forward');
        this.bindTransport(document.getElementById('previewPrevFrameBtn'), 'prevFrame');
        this.bindTransport(document.getElementById('previewNextFrameBtn'), 'nextFrame');
    },
    bindTransport: function(button, action) {
        if (!button) {
            return;
        }
        button.addEventListener('click', function() {
            Preview.handleTransport(action);
        });
    },
    handleTransport: function(action) {
        switch (action) {
            case 'play':
                this.play();
                break;
            case 'stop':
                this.stop();
                break;
            case 'rewind':
                this.seek(-2);
                break;
            case 'forward':
                this.seek(2);
                break;
            case 'prevFrame':
                this.stepFrame(-1);
                break;
            case 'nextFrame':
                this.stepFrame(1);
                break;
        }
    },
    hasLoadedVideo: function() {
        return this.video
            && this.video.src
            && this.container
            && this.container.classList.contains('preview-container--active');
    },
    showPlaceholder: function() {
        if (this.container) {
            this.container.classList.remove('preview-container--active');
        }
        if (this.video) {
            this.video.classList.add('visually-hidden');
            this.video.pause();
            this.video.removeAttribute('src');
            this.video.load();
        }
        if (this.placeholder) {
            this.placeholder.classList.remove('visually-hidden');
        }
        if (this.controls) {
            this.controls.classList.remove('visually-hidden');
        }
    },
    showVideo: function() {
        if (this.container) {
            this.container.classList.add('preview-container--active');
        }
        if (this.placeholder) {
            this.placeholder.classList.add('visually-hidden');
        }
        if (this.video) {
            this.video.classList.remove('visually-hidden');
        }
        if (this.controls) {
            this.controls.classList.add('visually-hidden');
        }
    },
    play: function() {
        if (this.hasLoadedVideo()) {
            if (this.video.paused) {
                this.video.play();
            } else {
                this.video.pause();
            }
            return;
        }
        if (!Project.allClips().length) {
            UI.notify('Add clips to the timeline before previewing.', 'error');
            return;
        }
        this.generate();
    },
    seek: function(deltaSeconds) {
        if (this.hasLoadedVideo()) {
            this.video.currentTime = Math.max(0, this.video.currentTime + deltaSeconds);
            return;
        }
        Playhead.setPositionMs(Playhead.positionMs() + deltaSeconds * 1000);
    },
    stepFrame: function(direction) {
        var fps = (currentProject && currentProject.frameRate) || 30;
        var stepMs = Math.round(1000 / fps) * direction;
        if (this.hasLoadedVideo()) {
            this.video.pause();
            this.video.currentTime = Math.max(0, this.video.currentTime + stepMs / 1000);
            return;
        }
        Playhead.setPositionMs(Playhead.positionMs() + stepMs);
    },
    setStatus: function(message, autoHide) {
        if (!this.status) {
            return;
        }
        this.status.textContent = message;
        this.status.classList.remove('visually-hidden');
        if (this.statusTimer) {
            clearTimeout(this.statusTimer);
            this.statusTimer = null;
        }
        if (autoHide) {
            var self = this;
            this.statusTimer = setTimeout(function() {
                if (self.status) {
                    self.status.classList.add('visually-hidden');
                }
            }, 3000);
        }
    },
    generate: function() {
        if (!currentProject.id || this.running) {
            return;
        }
        this.running = true;
        var playBtn = document.getElementById('playBtn');
        var previewPlayBtn = document.getElementById('previewPlayBtn');
        UI.setButtonBusy(playBtn, true);
        UI.setButtonBusy(previewPlayBtn, true);
        this.setStatus('Generating preview…');
        UI.notify('Generating preview…', 'info');
        ProjectSave.save({ silent: true })
            .then(function() {
                return fetch('/api/editor/' + currentProject.id + '/preview', { method: 'POST' });
            })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().catch(function() {
                        return {};
                    }).then(function(data) {
                        throw new Error(data.error || ('Preview request failed (' + response.status + ')'));
                    });
                }
                return response.json();
            })
            .then(function(data) {
                if (data.error) {
                    throw new Error(data.error);
                }
                Preview.video.src = data.downloadUrl + '?t=' + Date.now();
                Preview.showVideo();
                Preview.setStatus('Preview ready', true);
                UI.notify('Preview ready.', 'success');
                return Preview.video.play();
            })
            .catch(function(err) {
                Preview.setStatus('Preview failed: ' + err.message);
                UI.notify('Preview failed: ' + err.message + ' — check MLT status and try again.', 'error');
            })
            .finally(function() {
                Preview.running = false;
                UI.setButtonBusy(document.getElementById('playBtn'), false);
                UI.setButtonBusy(document.getElementById('previewPlayBtn'), false);
            });
    },
    stop: function() {
        this.showPlaceholder();
        if (this.status) {
            this.status.classList.add('visually-hidden');
            this.status.textContent = '';
        }
        if (this.statusTimer) {
            clearTimeout(this.statusTimer);
            this.statusTimer = null;
        }
        Playhead.setPositionMs(0);
    }
};

const Export = {
    init: function() {
        var startBtn = document.getElementById('startExportBtn');
        if (startBtn) {
            startBtn.addEventListener('click', function() { Export.start(); });
        }
        document.querySelectorAll('.close-export').forEach(function(btn) {
            btn.addEventListener('click', function() {
                document.getElementById('exportModal').style.display = 'none';
            });
        });
    },
    start: function() {
        var progressFill = document.getElementById('progressFill');
        var progressText = document.getElementById('progressText');
        var downloadWrap = document.getElementById('exportDownload');
        var downloadLink = document.getElementById('exportDownloadLink');
        var startBtn = document.getElementById('startExportBtn');
        var exportBtn = document.getElementById('exportBtn');
        UI.setButtonBusy(startBtn, true);
        UI.setButtonBusy(exportBtn, true);
        if (progressFill) {
            progressFill.style.width = '15%';
        }
        if (progressText) {
            progressText.textContent = 'Saving project...';
        }
        if (downloadWrap) {
            downloadWrap.classList.add('visually-hidden');
        }
        ProjectSave.save({ silent: true })
            .then(function() {
                if (progressFill) {
                    progressFill.style.width = '45%';
                }
                if (progressText) {
                    progressText.textContent = 'Rendering video (this may take a while)...';
                }
                return fetch('/api/editor/' + currentProject.id + '/render', { method: 'POST' });
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (data.error) {
                    throw new Error(data.error);
                }
                if (progressFill) {
                    progressFill.style.width = '100%';
                }
                if (progressText) {
                    progressText.textContent = data.message || 'Export complete';
                }
                if (downloadLink && downloadWrap) {
                    downloadLink.href = data.downloadUrl;
                    downloadLink.download = data.outputFilename;
                    downloadWrap.classList.remove('visually-hidden');
                }
            })
            .catch(function(err) {
                if (progressText) {
                    progressText.textContent = 'Export failed: ' + err.message;
                }
                UI.notify('Export failed: ' + err.message + ' — check MLT status and try again.', 'error');
            })
            .finally(function() {
                UI.setButtonBusy(document.getElementById('startExportBtn'), false);
                UI.setButtonBusy(document.getElementById('exportBtn'), false);
            });
    }
};

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
        },
        dragend: function() {
            DragNDrop.clearActiveDrag();
            UI.removeShadowComponent();
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
        },
        dragend: function() {
            UI.setClipDraggingState(null, false);
            UI.removeShadowComponent();
            DragNDrop.clearActiveDrag();
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
                if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    MediaLibrary.addFiles(e.dataTransfer.files);
                }
            }
        }
    }, 
    'tracks-container': {
        dragover: function(e) {
            if (!e.target.closest || !e.target.closest('.track-line')) {
                return;
            }
            DragNDrop.verifyEventClass(e, 'track-line');
            var trackIndex = UI.getTrackIndexFromEvent(e);
            if (DragNDrop.isMedia(e)) {
                var mediaType = DragNDrop.getMediaType(e);
                if (!Project.trackAcceptsMedia(trackIndex, mediaType)) {
                    return;
                }
                UI.highlightTrack(e, true);
                var position = DragNDrop.calculateDropPosition(e);
                if (position >= 0) {
                    UI.updateShadowElementsForMedia(position, DragNDrop.getMediaHash(e), trackIndex);
                }
                e.preventDefault();
                e.stopPropagation();
            } else if (UI.handleClipDragover(e, trackIndex)) {
                return;
            }
        },
        dragleave: function(e) {
            if (!e.target.closest || !e.target.closest('.track-line')) {
                return;
            }
            DragNDrop.verifyEventClass(e, 'track-line');
            if (DragNDrop.isMedia(e)) {
                UI.highlightTrack(e, false);
                e.preventDefault();
                e.stopPropagation();
                UI.removeShadowComponent();
            } else if (DragNDrop.isClip(e)) {
                UI.highlightTrack(e, false);
            }
        },
        drop: function(e) {
            if (!e.target.closest || !e.target.closest('.track-line')) {
                return;
            }
            DragNDrop.verifyEventClass(e, 'track-line');
            var trackIndex = UI.getTrackIndexFromEvent(e);
            if (DragNDrop.isMedia(e)) {
                var mediaType = DragNDrop.getMediaType(e);
                if (!Project.trackAcceptsMedia(trackIndex, mediaType)) {
                    return;
                }
                UI.highlightTrack(e, false);
                var position = DragNDrop.calculateDropPosition(e);
                UI.removeShadowComponent();
                Project.addMediaToTimeline(DragNDrop.getMediaHash(e), position, trackIndex);
                e.preventDefault();
                e.stopPropagation();
            } else if (UI.handleClipDrop(e, trackIndex)) {
                return;
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
    Project.ensureStructure();
    UI.reconciliateTracks();
    UI.reconciliateMedias();
    UI.reconciliateClips();
    UI.bindProjectProperties();
    TimelineZoom.updateRulerMarks();
    Playhead.init();
    Preview.init();
    Export.init();
    ProjectSave.init();
    UI.updateTimelineActions();

    var addVideoTrackBtn = document.getElementById('addVideoTrackBtn');
    var addAudioTrackBtn = document.getElementById('addAudioTrackBtn');
    var cutClipBtn = document.getElementById('cutClipBtn');
    var deleteClipBtn = document.getElementById('deleteClipBtn');
    if (addVideoTrackBtn) {
        addVideoTrackBtn.addEventListener('click', function() { Project.addTrack('VIDEO'); });
    }
    if (addAudioTrackBtn) {
        addAudioTrackBtn.addEventListener('click', function() { Project.addTrack('AUDIO'); });
    }
    if (cutClipBtn) {
        cutClipBtn.addEventListener('click', function() { Project.cutSelectedClipAtPlayhead(); });
    }
    if (deleteClipBtn) {
        deleteClipBtn.addEventListener('click', function() {
            var hash = UI.getSelectedClipHash();
            if (hash) {
                Project.requestDeleteClip(hash);
            } else {
                UI.notify('Select a clip to delete.', 'error');
            }
        });
    }
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Delete' || e.key === 'Backspace') {
            var hash = UI.getSelectedClipHash();
            if (hash && document.activeElement && document.activeElement.tagName !== 'INPUT') {
                e.preventDefault();
                Project.requestDeleteClip(hash);
            }
        }
    });

    var uploadBtn = document.getElementById('uploadBtn');
    var mediaFileInput = document.getElementById('mediaFileInput');
    if (uploadBtn && mediaFileInput) {
        uploadBtn.addEventListener('click', function() {
            mediaFileInput.click();
        });
        mediaFileInput.addEventListener('change', function() {
            if (mediaFileInput.files && mediaFileInput.files.length > 0) {
                MediaLibrary.addFiles(mediaFileInput.files);
                mediaFileInput.value = '';
            }
        });
    }

    // Tab switching
    const tabs = document.querySelectorAll('.tab');
    const tabComingSoon = document.getElementById('tabComingSoon');
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            tabs.forEach(function(t) { t.classList.remove('active'); });
            tab.classList.add('active');
            var label = tab.textContent.trim();
            if (tabComingSoon) {
                if (label === 'Properties') {
                    tabComingSoon.classList.add('visually-hidden');
                    tabComingSoon.textContent = '';
                } else {
                    tabComingSoon.textContent = label + ' are not available yet.';
                    tabComingSoon.classList.remove('visually-hidden');
                }
            }
        });
    });

    var timelineContainer = document.querySelector('.timeline-container');
    if (timelineContainer) {
        timelineContainer.addEventListener('click', function(e) {
            if (e.target.closest('.clip') || e.target.closest('.track-btn') ||
                e.target.closest('.timeline-toolbar') || e.target.closest('.timeline-controls') ||
                e.target.closest('#playhead')) {
                return;
            }
            if (e.target.closest('.track-line') || e.target.closest('.timeline-ruler') ||
                e.target.closest('.tracks-container') || e.target.closest('.track-area')) {
                UI.deselectAll();
            }
        });
    }

    var previewPlaceholder = document.getElementById('previewPlaceholder');
    if (previewPlaceholder) {
        previewPlaceholder.addEventListener('click', function() {
            UI.deselectAll();
        });
    }

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

    document.addEventListener('dragend', function() {
        DragNDrop.clearActiveDrag();
        UI.removeShadowComponent();
        UI.setClipDraggingState(null, false);
        document.querySelectorAll('.track-area.active, .track-line.active').forEach(function(elm) {
            elm.classList.remove('active');
        });
    });

    function findTrackAreaUnderPointer(evnt) {
        function eventIsOverElement(e, element) {
            var rects = element.getBoundingClientRect();
            var x = typeof e.clientX === 'number' ? e.clientX : e.x;
            var y = typeof e.clientY === 'number' ? e.clientY : e.y;
            return x >= rects.x && x <= rects.x + rects.width &&
                   y >= rects.y && y <= rects.y + rects.height;
        }
        return Array.from(document.querySelectorAll('.track-area')).find(function(elm) {
            return eventIsOverElement(evnt, elm);
        });
    }

    document.addEventListener('dragover', function(e) {
        var trackArea = findTrackAreaUnderPointer(e);
        if (trackArea) {
            var track = Array.from(trackArea.querySelectorAll('.track-line')).find(function(line) {
                var rects = line.getBoundingClientRect();
                var x = typeof e.clientX === 'number' ? e.clientX : e.x;
                var y = typeof e.clientY === 'number' ? e.clientY : e.y;
                return x >= rects.x && x <= rects.x + rects.width &&
                       y >= rects.y && y <= rects.y + rects.height;
            });
            if (track) {
                e.realTarget = track;
                staticElementsEvents['tracks-container'].dragover(e);
            }
        }
    });

    document.addEventListener('dragleave', function(e) {
        var trackArea = findTrackAreaUnderPointer(e);
        if (trackArea) {
            var track = trackArea.querySelector('.track-line');
            if (track) {
                e.realTarget = track;
                staticElementsEvents['tracks-container'].dragleave(e);
            }
        }
    });

    document.addEventListener('drop', function(e) {
        var trackArea = findTrackAreaUnderPointer(e);
        if (trackArea) {
            var track = trackArea.querySelector('.track-line');
            if (track) {
                e.realTarget = track;
                staticElementsEvents['tracks-container'].drop(e);
            }
        }
    });
});