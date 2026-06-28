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
        Project.repairAvSyncGroups();
    },
    repairAvSyncGroups: function() {
        var clips = Project.allClips();
        clips.forEach(function(clip) {
            if (clip.syncGroup) {
                return;
            }
            var media = Project.findMedia(clip.mediaHash);
            if (!media || Project.resolveMediaType(media) !== 'VIDEO') {
                return;
            }
            var partner = clips.find(function(other) {
                if (other.hash === clip.hash || other.syncGroup) {
                    return false;
                }
                if (other.mediaHash !== clip.mediaHash || other.start !== clip.start) {
                    return false;
                }
                return (clip.type === 'VIDEO' && other.type === 'AUDIO') ||
                       (clip.type === 'AUDIO' && other.type === 'VIDEO');
            });
            if (partner) {
                var syncGroup = 'sync-' + clip.mediaHash + '-' + clip.start + '-repaired';
                clip.syncGroup = syncGroup;
                partner.syncGroup = syncGroup;
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
    contentDurationMs: function() {
        var maxEnd = 0;
        Project.allClips().forEach(function(clip) {
            var media = Project.findMedia(clip.mediaHash);
            var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
            maxEnd = Math.max(maxEnd, clip.start + duration);
        });
        return maxEnd;
    },
    timelineDurationMs: function() {
        if (!currentProject) {
            return 0;
        }
        return Math.max(currentProject.duration || 0, Project.contentDurationMs());
    },
    findClip: function(hash) {
        if (currentProject && currentProject.clips && currentProject.clips.length > 0) {
            return currentProject.clips.find(t => t.hash == hash);
        }
        return null;
    },
    findClipAtTime: function(timeMs) {
        return Project.allClips().find(function(clip) {
            return Project.clipContainsTime(clip, timeMs);
        }) || null;
    },
    clipContainsTime: function(clip, timeMs) {
        if (!clip) {
            return false;
        }
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(Project.findMedia(clip.mediaHash));
        return timeMs > clip.start && timeMs < clip.start + duration;
    },
    clipEffectiveDuration: function(clip) {
        if (!clip) {
            return 0;
        }
        return clip.duration > 0 ? clip.duration : Project.clipDuration(Project.findMedia(clip.mediaHash));
    },
    canCutAtPlayhead: function() {
        return Project.findClipAtTime(Playhead.positionMs()) !== null;
    },
    findSyncedClips: function(syncGroup) {
        if (!syncGroup || !currentProject || !currentProject.clips) {
            return [];
        }
        return currentProject.clips.filter(function(clip) {
            return clip.syncGroup === syncGroup;
        });
    },
    findAvPartners: function(clip) {
        if (!clip) {
            return [];
        }
        if (clip.syncGroup) {
            return Project.findSyncedClips(clip.syncGroup);
        }
        var media = Project.findMedia(clip.mediaHash);
        if (!media || Project.resolveMediaType(media) !== 'VIDEO') {
            return [clip];
        }
        var partners = Project.allClips().filter(function(other) {
            if (other.mediaHash !== clip.mediaHash || other.start !== clip.start) {
                return false;
            }
            if (other.hash === clip.hash) {
                return true;
            }
            return (clip.type === 'VIDEO' && other.type === 'AUDIO') ||
                   (clip.type === 'AUDIO' && other.type === 'VIDEO');
        });
        return partners.length ? partners : [clip];
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
        History.execute(function() {
            Project.addMediaToTimelineImpl(mediaHash, positionPercent, targetTrackIndex);
        }, 'Add clip');
    },
    addMediaToTimelineImpl: function(mediaHash, positionPercent, targetTrackIndex) {
        var media = Project.findMedia(mediaHash);
        if (!media) {
            return;
        }
        if (!currentProject.clips) {
            currentProject.clips = [];
        }

        var startMs = TimelineSnap.snapMs(Math.round(Project.timelineDurationMs() * positionPercent / 100));
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
        return Math.max(0, Math.round(startMs));
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
        History.execute(function() {
            Project.moveClipToStartImpl(clipHash, startMs, trackIndex);
        }, 'Move clip');
    },
    moveClipToStartImpl: function(clipHash, startMs, trackIndex) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            console.error('Clip not found for move', clipHash);
            return;
        }
        var clips = Project.findAvPartners(clip);
        var media = Project.findMedia(clip.mediaHash);
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var clampedStart = Project.clampClipStart(TimelineSnap.snapMs(startMs), duration);
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
        History.execute(function() {
            Project.deleteClipImpl(clipHash);
        }, 'Delete clip');
    },
    deleteClipImpl: function(clipHash) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var toRemove = Project.findAvPartners(clip);
        var removeHashes = toRemove.map(function(c) { return c.hash; });
        var rippleByTrack = {};
        if (EditSettings.rippleDelete) {
            toRemove.forEach(function(c) {
                var dur = Project.clipEffectiveDuration(c);
                if (!rippleByTrack[c.trackIndex]) {
                    rippleByTrack[c.trackIndex] = { start: c.start, shift: dur };
                } else {
                    rippleByTrack[c.trackIndex].start = Math.min(rippleByTrack[c.trackIndex].start, c.start);
                    rippleByTrack[c.trackIndex].shift = Math.max(rippleByTrack[c.trackIndex].shift, dur);
                }
            });
        }
        currentProject.clips = currentProject.clips.filter(function(c) {
            return removeHashes.indexOf(c.hash) === -1;
        });
        if (EditSettings.rippleDelete) {
            currentProject.clips.forEach(function(c) {
                var ripple = rippleByTrack[c.trackIndex];
                if (ripple && c.start >= ripple.start) {
                    c.start = Math.max(0, c.start - ripple.shift);
                }
            });
        }
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
    unlinkAv: function(clipHash) {
        var clip = Project.findClip(clipHash);
        if (!clip || !clip.syncGroup) {
            UI.notify('Clip is not linked to A/V partner.', 'info');
            return;
        }
        History.execute(function() {
            Project.findAvPartners(clip).forEach(function(c) {
                c.syncGroup = null;
            });
            UI.reconciliateClips();
            ProjectSave.schedule();
            UI.notify('A/V unlinked.', 'success');
        }, 'Unlink A/V');
    },
    splitClip: function(clipHash, splitAtMs, options) {
        options = options || {};
        var clip = Project.findClip(clipHash);
        if (!clip || splitAtMs <= 0 || splitAtMs >= Project.clipEffectiveDuration(clip)) {
            return Promise.resolve(null);
        }
        var media = Project.findMedia(clip.mediaHash);
        var sourceIn = clip.sourceIn || 0;
        var rightDuration = clip.duration - splitAtMs;
        clip.duration = splitAtMs;
        clip.sourceOut = sourceIn + splitAtMs;
        var rightSyncGroup = options.rightSyncGroup !== undefined ? options.rightSyncGroup : null;
        return Hash.generate(clip.hash + '-split-' + Date.now() + '-' + Math.random()).then(function(newHash) {
            var rightClip = {
                name: clip.name,
                hash: newHash,
                mediaHash: clip.mediaHash,
                duration: rightDuration,
                type: clip.type,
                start: clip.start + splitAtMs,
                speed: clip.speed || 1,
                syncGroup: rightSyncGroup,
                trackIndex: clip.trackIndex,
                sourceIn: sourceIn + splitAtMs,
                sourceOut: media && media.duration > 0 ? media.duration : 0
            };
            currentProject.clips.push(rightClip);
            return rightClip;
        });
    },
    cutAtPlayhead: function() {
        var playheadMs = Playhead.positionMs();
        var clip = Project.findClipAtTime(playheadMs);
        var selectedHash = UI.getSelectedClipHash();
        if (selectedHash) {
            var selected = Project.findClip(selectedHash);
            if (selected && Project.clipContainsTime(selected, playheadMs)) {
                clip = selected;
            }
        }
        if (!clip) {
            UI.notify('Move the playhead inside a clip to cut.', 'error');
            return;
        }
        var group = Project.findAvPartners(clip);
        var newSyncGroup = group.length > 1
            ? 'sync-cut-' + playheadMs + '-' + Date.now()
            : null;
        var cuts = group.map(function(c) {
            return { hash: c.hash, cutPoint: playheadMs - c.start };
        }).filter(function(entry) {
            var c = Project.findClip(entry.hash);
            if (!c) {
                return false;
            }
            return entry.cutPoint > 0 && entry.cutPoint < Project.clipEffectiveDuration(c);
        });
        if (cuts.length === 0) {
            UI.notify('Move the playhead inside the clip to cut.', 'error');
            return;
        }
        History.execute(function() {
            Promise.all(cuts.map(function(entry) {
                return Project.splitClip(entry.hash, entry.cutPoint, { rightSyncGroup: newSyncGroup });
            })).then(function() {
                if (newSyncGroup) {
                    Project.repairAvSyncGroups();
                }
                UI.reconciliateClips();
                ProjectSave.schedule();
                UI.updateTimelineActions();
                var label = UI.mediaDuration(playheadMs);
                UI.notify('Cut at ' + label + (cuts.length > 1 ? ' (video + audio).' : '.'), 'success');
            });
        }, 'Cut at playhead');
    },
    cutSelectedClipAtPlayhead: function() {
        Project.cutAtPlayhead();
    },
    trimClipLeft: function(clipHash, newStartMs) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return false;
        }
        var delta = Math.round(newStartMs - clip.start);
        if (delta <= 0 || delta >= Project.clipEffectiveDuration(clip) - 100) {
            return false;
        }
        var partners = Project.findAvPartners(clip);
        partners.forEach(function(c) {
            c.sourceIn = (c.sourceIn || 0) + delta;
            c.duration = Project.clipEffectiveDuration(c) - delta;
            c.start = c.start + delta;
            c.sourceOut = c.sourceIn + c.duration;
        });
        return true;
    },
    trimClipRight: function(clipHash, newEndMs) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return false;
        }
        var newDuration = Math.round(newEndMs - clip.start);
        if (newDuration < 100 || newDuration >= Project.clipEffectiveDuration(clip)) {
            return false;
        }
        var partners = Project.findAvPartners(clip);
        partners.forEach(function(c) {
            c.duration = newDuration;
            c.sourceOut = (c.sourceIn || 0) + newDuration;
        });
        return true;
    },
    applyClipTransition: function(clipHash, transition) {
        History.execute(function() {
            var clip = Project.findClip(clipHash);
            if (!clip) {
                return;
            }
            clip.transition = transition || null;
            ProjectSave.schedule();
        }, 'Apply transition');
    },
    applyClipEffect: function(clipHash, effect) {
        History.execute(function() {
            var clip = Project.findClip(clipHash);
            if (!clip) {
                return;
            }
            clip.effect = effect || null;
            ProjectSave.schedule();
        }, 'Apply effect');
    },
    updateClipProperty: function(clipHash, field, value) {
        History.execute(function() {
            Project.updateClipPropertyImpl(clipHash, field, value);
        }, 'Edit clip property');
    },
    updateClipPropertyImpl: function(clipHash, field, value) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var partners = Project.findAvPartners(clip);
        if (field === 'name') {
            partners.forEach(function(c) { c.name = value; });
        } else if (field === 'start') {
            var startMs = TimelineSnap.snapMs(Math.round(parseFloat(value) * 1000));
            var media = Project.findMedia(clip.mediaHash);
            var duration = Project.clipEffectiveDuration(clip);
            var clamped = Project.clampClipStart(startMs, duration);
            var delta = clamped - clip.start;
            partners.forEach(function(c) { c.start = c.start + delta; });
        } else if (field === 'speed') {
            var speed = Math.max(0.1, Math.min(3, parseFloat(value) || 1));
            partners.forEach(function(c) { c.speed = speed; });
        } else if (field === 'volume') {
            var volume = Math.max(0, Math.min(100, parseFloat(value) || 100));
            partners.forEach(function(c) { c.volume = volume; });
        } else if (field === 'duration') {
            var durMs = Math.round(parseFloat(value) * 1000);
            if (durMs >= 100) {
                clip.duration = durMs;
                clip.sourceOut = (clip.sourceIn || 0) + durMs;
            }
        }
        UI.reconciliateClips();
        ProjectSave.schedule();
    },
    removeMedia: function(mediaHash) {
        return fetch('/api/editor/' + currentProject.id + '/media/' + mediaHash, { method: 'DELETE' })
            .then(function(response) { return response.json().then(function(data) { return { ok: response.ok, data: data }; }); })
            .then(function(result) {
                if (!result.ok) {
                    throw new Error(result.data.error || 'Failed to remove media');
                }
                currentProject.medias = currentProject.medias.filter(function(m) { return m.hash !== mediaHash; });
                UI.reconciliateMedias();
                document.querySelectorAll('[item-hash="' + mediaHash + '"]').forEach(function(elm) {
                    if (elm.parentElement) {
                        elm.parentElement.removeChild(elm);
                    }
                });
                UI.clearItemProperties();
                ProjectSave.schedule();
            });
    },
    renameMedia: function(mediaHash, name) {
        return fetch('/api/editor/' + currentProject.id + '/media/' + mediaHash, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: name })
        })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().then(function(data) { throw new Error(data.error || 'Rename failed'); });
                }
                return response.json();
            })
            .then(function(updated) {
                var media = Project.findMedia(mediaHash);
                if (media) {
                    media.name = updated.name || name;
                }
                UI.reconciliateMedias();
                ProjectSave.schedule();
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
        var media = Project.findMedia(clip.mediaHash);
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var grabOffsetMs = 0;
        if (clipElm && typeof e.clientX === 'number') {
            var clipRect = clipElm.getBoundingClientRect();
            if (clipRect.width > 0 && duration > 0) {
                grabOffsetMs = Math.round(((e.clientX - clipRect.left) / clipRect.width) * duration);
            }
        }
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
        const metrics = TimelineZoom.getMetrics();
        const visibleWidth = containerRect.width;
        const pointerX = typeof e.clientX === 'number' ? e.clientX : e.x;
        const clientX = pointerX - containerRect.left + metrics.scrollOffset;
        return Math.max(0, Math.min(100, (clientX / metrics.totalWidth) * 100));
    },
    clipStartFromPosition: function(positionPercent) {
        var startMs = Math.round(Project.timelineDurationMs() * positionPercent / 100);
        if (this.activeDrag && this.activeDrag.kind === 'CLIP') {
            startMs -= this.activeDrag.grabOffsetMs || 0;
            startMs = Project.clampClipStart(TimelineSnap.snapMs(startMs), this.activeDrag.duration);
        } else {
            startMs = TimelineSnap.snapMs(startMs);
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
        this.setStatus(message, type || 'info');
    },
    setStatus: function(message, type, options) {
        type = type || 'info';
        options = options || {};
        var bar = document.getElementById('editorStatus');
        if (!bar) {
            console.error(message);
            return;
        }
        bar.textContent = message;
        bar.className = 'editor-status editor-status--' + type;
        if (this.notifyTimer) {
            clearTimeout(this.notifyTimer);
            this.notifyTimer = null;
        }
        if (type !== 'error' && !options.persistent) {
            var self = this;
            this.notifyTimer = setTimeout(function() {
                self.clearStatus();
            }, 6000);
        }
    },
    setPreviewProgress: function(percent, etaSeconds) {
        var safePercent = Math.max(0, Math.min(100, Math.round(percent || 0)));
        var message = 'Rendering preview: ' + safePercent + '%';
        if (typeof etaSeconds === 'number' && etaSeconds > 0) {
            message += ' — ETA ' + UI.formatEta(etaSeconds);
        }
        this.setStatus(message, 'info', { persistent: true });
    },
    formatEta: function(totalSeconds) {
        totalSeconds = Math.max(0, Math.round(totalSeconds));
        var minutes = Math.floor(totalSeconds / 60);
        var seconds = totalSeconds % 60;
        if (minutes > 0) {
            return minutes + ':' + seconds.toString().padStart(2, '0');
        }
        return seconds + 's';
    },
    clearStatus: function() {
        var bar = document.getElementById('editorStatus');
        if (!bar) {
            return;
        }
        bar.textContent = '';
        bar.className = 'editor-status editor-status--idle';
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
        var canCut = Project.canCutAtPlayhead();
        if (cutBtn) {
            cutBtn.disabled = !canCut;
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
    mediaIconFromMime: function(mimeType) {
        var iconId = 'unknown';
        if (mimeType && mimeType.indexOf('video/') === 0) {
            iconId = 'video';
        } else if (mimeType && mimeType.indexOf('audio/') === 0) {
            iconId = 'audio';
        } else if (mimeType && mimeType.indexOf('image/') === 0) {
            iconId = 'image';
        }
        return '<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#' + iconId + '"/></svg>';
    },
    loadingIcon: function() {
        return '<svg class="icon icon-spin" aria-hidden="true"><use href="/icons/icons.svg#loading"/></svg>';
    },
    addPendingUpload: function(uploadId, file) {
        var mediaList = document.getElementById('media-list');
        if (!mediaList) {
            return;
        }
        var safeName = UI.escapeHtml(file.name || 'Untitled');
        var typeIcon = UI.mediaIconFromMime(file.type);
        mediaList.insertAdjacentHTML('afterbegin',
            '<div class="file-item file-item--uploading" item-upload-id="' + uploadId + '"' +
            ' aria-busy="true" aria-label="Uploading ' + safeName + '">' +
            '<div class="file-icon file-icon--loading">' + UI.loadingIcon() + '</div>' +
            '<div class="file-info">' +
            '<div class="file-name" title="' + safeName + '">' + safeName + '</div>' +
            '<div class="file-status">Uploading…</div>' +
            '</div></div>');
        MediaBin.refresh();
    },
    completePendingUpload: function(uploadId, media) {
        var item = document.querySelector('[item-upload-id="' + uploadId + '"]');
        if (!item) {
            UI.reconciliateMedias();
            return;
        }
        item.removeAttribute('item-upload-id');
        item.removeAttribute('aria-busy');
        item.setAttribute('item-hash', media.hash);
        item.setAttribute('draggable', 'true');
        item.classList.remove('file-item--uploading');
        item.setAttribute('aria-label', media.name || 'Media');
        var iconEl = item.querySelector('.file-icon');
        if (iconEl) {
            iconEl.classList.remove('file-icon--loading');
            iconEl.innerHTML = UI.mediaIcon(media);
        }
        var statusEl = item.querySelector('.file-status');
        if (statusEl) {
            statusEl.className = 'file-duration';
            statusEl.textContent = UI.mediaDuration(media.duration);
        }
        UI.setupElement(item, 'MEDIA');
        MediaBin.refresh();
    },
    failPendingUpload: function(uploadId) {
        var item = document.querySelector('[item-upload-id="' + uploadId + '"]');
        if (!item) {
            return;
        }
        item.classList.remove('file-item--uploading');
        item.classList.add('file-item--error');
        item.removeAttribute('aria-busy');
        var iconEl = item.querySelector('.file-icon');
        if (iconEl) {
            iconEl.classList.remove('file-icon--loading');
            iconEl.innerHTML = '<svg class="icon" aria-hidden="true"><use href="/icons/icons.svg#status-error"/></svg>';
        }
        var statusEl = item.querySelector('.file-status');
        if (statusEl) {
            statusEl.textContent = 'Upload failed';
        }
        setTimeout(function() {
            if (item.parentElement) {
                item.parentElement.removeChild(item);
            }
            MediaBin.refresh();
        }, 8000);
        MediaBin.refresh();
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
                                                                     <label for="prop-media-name">Media Name</label>
                                                                     <input id="prop-media-name" type="text" value="${UI.escapeHtml(media.name)}">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Duration</label>
                                                                     <input type="text" disabled value="${ UI.mediaDuration(media.duration) }">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <button type="button" class="btn btn-secondary btn-sm" id="prop-media-remove">Remove from project</button>
                                                                 </div>`);
                var nameInput = document.getElementById('prop-media-name');
                if (nameInput) {
                    nameInput.addEventListener('change', function() {
                        var newName = nameInput.value.trim();
                        if (newName && newName !== media.name) {
                            Project.renameMedia(hash, newName)
                                .then(function() { UI.notify('Media renamed.', 'success'); })
                                .catch(function(err) { UI.notify(err.message, 'error'); nameInput.value = media.name; });
                        }
                    });
                }
                var removeBtn = document.getElementById('prop-media-remove');
                if (removeBtn) {
                    removeBtn.addEventListener('click', function() {
                        if (!confirm('Remove "' + media.name + '" from the project?')) {
                            return;
                        }
                        Project.removeMedia(hash)
                            .then(function() { UI.notify('Media removed.', 'info'); })
                            .catch(function(err) { UI.notify(err.message, 'error'); });
                    });
                }
            } else {
                console.error("Media cannot be found!", hash);
            }
        } else if (type == 'CLIP') {
            let clip = Project.findClip(hash);
            if (clip) {
                var displayName = clip.name || (Project.findMedia(clip.mediaHash) && Project.findMedia(clip.mediaHash).name) || '';
                var media = Project.findMedia(clip.mediaHash);
                var mediaType = Project.resolveMediaType(media);
                var volumeHtml = (clip.type === 'AUDIO' || mediaType === 'VIDEO') ?
                    '<div class="property-group">' +
                    '<label for="prop-clip-volume">Volume</label>' +
                    '<input id="prop-clip-volume" type="range" min="0" max="100" step="1" value="' + (clip.volume != null ? clip.volume : 100) + '">' +
                    '<span id="prop-clip-volume-label">' + (clip.volume != null ? clip.volume : 100) + '%</span>' +
                    '</div>' : '';
                var durationHtml = mediaType === 'IMAGE' ?
                    '<div class="property-group">' +
                    '<label for="prop-clip-duration">Duration <i>(s)</i></label>' +
                    '<input id="prop-clip-duration" type="number" value="' + (clip.duration / 1000).toFixed(3) + '" step="0.1" min="0.1">' +
                    '</div>' : '';
                itemProperties.insertAdjacentHTML('afterbegin', `<h3>Clip</h3>
                                                                 <div class="property-group">
                                                                     <label for="prop-clip-name">Clip Name</label>
                                                                     <input id="prop-clip-name" type="text" value="${UI.escapeHtml(displayName)}">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label for="prop-clip-start">Start Time <i>(s)</i></label>
                                                                     <input id="prop-clip-start" type="number" value="${(clip.start / 1000).toFixed(3)}" step="0.001" min="0">
                                                                 </div>
                                                                 <div class="property-group">
                                                                     <label>Duration</label>
                                                                     <input type="text" disabled value="${UI.mediaDuration(clip.duration)}" >
                                                                 </div>
                                                                 ${durationHtml}
                                                                 <div class="property-group">
                                                                     <label for="prop-clip-speed">Speed</label>
                                                                     <input id="prop-clip-speed" type="number" value="${clip.speed || 1}" step="0.01" min="0.1" max="3">
                                                                 </div>
                                                                 ${volumeHtml}`);
                UI.bindClipPropertyInputs(hash);
            } else {
                console.error("Clip cannot be found!", hash);
            }

        }
    },
    escapeHtml: function(text) {
        if (!text) {
            return '';
        }
        return String(text).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    },
    bindClipPropertyInputs: function(clipHash) {
        var nameInput = document.getElementById('prop-clip-name');
        var startInput = document.getElementById('prop-clip-start');
        var speedInput = document.getElementById('prop-clip-speed');
        var volumeInput = document.getElementById('prop-clip-volume');
        var volumeLabel = document.getElementById('prop-clip-volume-label');
        var durationInput = document.getElementById('prop-clip-duration');
        if (nameInput) {
            nameInput.addEventListener('change', function() {
                Project.updateClipProperty(clipHash, 'name', nameInput.value.trim());
            });
        }
        if (startInput) {
            startInput.addEventListener('change', function() {
                Project.updateClipProperty(clipHash, 'start', startInput.value);
                UI.setupItemProperties('CLIP', clipHash);
            });
        }
        if (speedInput) {
            speedInput.addEventListener('change', function() {
                Project.updateClipProperty(clipHash, 'speed', speedInput.value);
            });
        }
        if (volumeInput) {
            volumeInput.addEventListener('input', function() {
                if (volumeLabel) {
                    volumeLabel.textContent = volumeInput.value + '%';
                }
            });
            volumeInput.addEventListener('change', function() {
                Project.updateClipProperty(clipHash, 'volume', volumeInput.value);
            });
        }
        if (durationInput) {
            durationInput.addEventListener('change', function() {
                Project.updateClipProperty(clipHash, 'duration', durationInput.value);
                UI.setupItemProperties('CLIP', clipHash);
            });
        }
    },
    switchTab: function(tabName) {
        var tabs = document.querySelectorAll('.properties-tabs .tab');
        tabs.forEach(function(t) {
            t.classList.toggle('active', t.textContent.trim() === tabName);
        });
        var propsPanel = document.getElementById('tabPanelProperties');
        var transPanel = document.getElementById('tabPanelTransitions');
        var effectsPanel = document.getElementById('tabPanelEffects');
        if (propsPanel) {
            propsPanel.classList.toggle('visually-hidden', tabName !== 'Properties');
            propsPanel.classList.toggle('tab-panel--active', tabName === 'Properties');
        }
        if (transPanel) {
            transPanel.classList.toggle('visually-hidden', tabName !== 'Transitions');
        }
        if (effectsPanel) {
            effectsPanel.classList.toggle('visually-hidden', tabName !== 'Effects');
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
                mediaList.insertAdjacentHTML('beforeend', `<div draggable="true" class="file-item" item-hash="${media.hash}">
                                                            <div class="file-icon">${ UI.mediaIcon(media) }</div>
                                                            <div class="file-info">
                                                                <div class="file-name" title="${ UI.escapeHtml(media.name) }">${ UI.escapeHtml(media.name) }</div>
                                                                <div class="file-duration">${ UI.mediaDuration(media.duration) }</div>
                                                            </div>
                                                        </div>`);
                let mediaElm = document.querySelector(`[item-hash="${media.hash}"]`);
                if (!mediaElm) {
                    console.error("Element not added!!!");
                }
                UI.setupElement(mediaElm, 'MEDIA');
            } else {
                var nameEl = mediaItem.querySelector('.file-name');
                if (nameEl) {
                    nameEl.textContent = media.name;
                    nameEl.title = media.name;
                }
            }
        });
        MediaBin.refresh();
    },
    clipLayoutPercent: function(clip, media) {
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var timelineDuration = Project.timelineDurationMs();
        return {
            left: timelineDuration > 0 ? (clip.start * 100) / timelineDuration : 0,
            width: timelineDuration > 0 ? (duration * 100) / timelineDuration : 0
        };
    },
    clipPixelLayout: function(startMs, durationMs) {
        var metrics = TimelineZoom.getMetrics();
        var timelineDuration = Project.timelineDurationMs();
        if (!timelineDuration) {
            return { left: '0px', width: '0px' };
        }
        var startPixels = (startMs / timelineDuration) * metrics.totalWidth;
        var durationPixels = (durationMs / timelineDuration) * metrics.totalWidth;
        return {
            left: (startPixels - metrics.scrollOffset) + 'px',
            width: durationPixels + 'px'
        };
    },
    applyClipPixelLayout: function(clipElm, startMs, durationMs) {
        var layout = UI.clipPixelLayout(startMs, durationMs);
        clipElm.style.left = layout.left;
        clipElm.style.width = layout.width;
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
        Playhead.ensureElement(container);
        TimelineZoom.updateTrackLines();
        Playhead.updateLayout();
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
            var displayName = clip.name || media.name;
            var safeName = UI.escapeHtml(displayName);
            trackLine.insertAdjacentHTML('beforeend',
                '<div draggable="true" class="' + clipClass + '" item-hash="' + clip.hash + '"' + syncAttr +
                ' style="left: ' + layout.left + '%; width: ' + layout.width + '%;">' +
                '<div class="clip-trim clip-trim--left" data-trim="left" title="Trim start"></div>' +
                '<div class="clip-content"><span class="clip-name" title="' + safeName + '">' + safeName + '</span></div>' +
                '<div class="clip-actions">' +
                '<button type="button" class="clip-btn clip-btn--edit" title="Edit properties" aria-label="Edit clip">&hellip;</button>' +
                '<button type="button" class="clip-btn clip-btn--delete" title="Delete clip" aria-label="Delete clip">&times;</button>' +
                '</div>' +
                '<div class="clip-trim clip-trim--right" data-trim="right" title="Trim end"></div></div>');
            var clipElm = document.querySelector('[item-hash="' + clip.hash + '"]');
            if (clipElm) {
                UI.setupElement(clipElm, 'CLIP');
                UI.setupClipControls(clipElm, clip.hash);
            }
        });
        TimelineZoom.updateClipPositions();
    },
    setupClipControls: function(clipElm, clipHash) {
        var editBtn = clipElm.querySelector('.clip-btn--edit');
        var deleteBtn = clipElm.querySelector('.clip-btn--delete');
        if (editBtn) {
            editBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                UI.selectElement('CLIP', clipHash);
                UI.switchTab('Properties');
            });
        }
        if (deleteBtn) {
            deleteBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                Project.requestDeleteClip(clipHash);
            });
        }
        clipElm.querySelectorAll('.clip-trim, .clip-btn').forEach(function(el) {
            el.addEventListener('dragstart', function(e) {
                e.preventDefault();
                e.stopPropagation();
            });
        });
        clipElm.querySelectorAll('.clip-trim').forEach(function(handle) {
            handle.addEventListener('mousedown', function(e) {
                e.stopPropagation();
                ClipTrim.start(e, clipHash, handle.getAttribute('data-trim'));
            });
        });
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
        var hashes = Project.findAvPartners(clip).map(function(c) { return c.hash; });
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
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var trackLine = UI.getTrackLine(clip.trackIndex);
        if (!trackLine) {
            return;
        }
        var layout = UI.clipPixelLayout(startMs, duration);
        var tempId = 'move-' + clip.hash;
        var clipClass = 'clip' + (extraClass ? ' ' + extraClass : '');
        var previousElm = UI.findShadowElement(tempId);
        if (previousElm && previousElm.parentElement !== trackLine) {
            previousElm.parentElement.removeChild(previousElm);
            previousElm = null;
        }
        if (!previousElm) {
            trackLine.insertAdjacentHTML('beforeend',
                '<div class="' + clipClass + '" item-temp-hash="' + tempId + '" style="left: ' + layout.left +
                '; width: ' + layout.width + '; opacity: 0.6;">' +
                '<div class="clip-content"><span class="clip-name" title="' + media.name + '">' + media.name + '</span></div></div>');
        } else {
            previousElm.style.left = layout.left;
            previousElm.style.width = layout.width;
        }
    },
    updateShadowElementsForClipMove: function(startMs, clipHash, targetTrackIndex) {
        UI.removeShadowComponent();
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var clips = Project.findAvPartners(clip);
        var avTracks = typeof targetTrackIndex === 'number'
            ? Project.resolveAvTracksForDrop(targetTrackIndex)
            : null;
        clips.forEach(function(c) {
            var shadowClip = c;
            if (avTracks) {
                shadowClip = Object.assign({}, c, {
                    trackIndex: c.type === 'AUDIO' ? avTracks.audio : avTracks.video
                });
            }
            UI.updateShadowElementForClip(startMs, shadowClip, c.type === 'AUDIO' ? 'clip--audio' : '');
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
        UI.updateShadowElementsForClipMove(startMs, clipHash, trackIndex);
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
    updateShadowElementOnTrack: function(positionPercent, tempId, trackIndex, durationMs, extraClass) {
        var trackLine = UI.getTrackLine(trackIndex);
        if (!trackLine) {
            return;
        }

        var mediaHash = tempId.replace(/-(video|audio)$/, '');
        var media = Project.findMedia(mediaHash);
        if (!media) {
            return;
        }
        var clipDuration = durationMs > 0 ? durationMs : Project.clipDuration(media);
        var startMs = Math.round(Project.timelineDurationMs() * positionPercent / 100);
        var layout = UI.clipPixelLayout(startMs, clipDuration);

        var clipClass = 'clip' + (extraClass ? ' ' + extraClass : '');
        var previousElm = UI.findShadowElement(tempId);
        if (previousElm && previousElm.parentElement !== trackLine) {
            previousElm.parentElement.removeChild(previousElm);
            previousElm = null;
        }
        if (!previousElm) {
            trackLine.insertAdjacentHTML('beforeend',
                '<div class="' + clipClass + '" item-temp-hash="' + tempId + '" style="left: ' + layout.left +
                '; width: ' + layout.width + '; opacity: 0.6;">' +
                '<div class="clip-content"><span class="clip-name" title="' + media.name + '">' + media.name + '</span></div></div>');
        } else {
            previousElm.style.left = layout.left;
            previousElm.style.width = layout.width;
        }
    },
    updateShadowElementsForMedia: function(position, mediaHash, targetTrackIndex) {
        var media = Project.findMedia(mediaHash);
        if (!media) {
            return;
        }
        var clipDuration = Project.clipDuration(media);
        var tracks = Project.resolveAvTracksForDrop(targetTrackIndex);
        var mediaType = Project.resolveMediaType(media);

        if (mediaType === 'VIDEO') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-video', tracks.video, clipDuration, '');
            UI.updateShadowElementOnTrack(position, mediaHash + '-audio', tracks.audio, clipDuration, 'clip--audio');
        } else if (mediaType === 'IMAGE') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-video', tracks.video, clipDuration, '');
        } else if (mediaType === 'AUDIO') {
            UI.updateShadowElementOnTrack(position, mediaHash + '-audio', tracks.audio, clipDuration, 'clip--audio');
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
        var txtDescription = document.getElementById('txt-project-description');
        if (txtDescription) {
            txtDescription.value = currentProject.description || '';
            txtDescription.onchange = function() {
                currentProject.description = txtDescription.value;
                ProjectSave.schedule();
            };
        }
        let durationProject = document.getElementById('dur-project');
        if (durationProject) {
            UI.setupDurationControl(durationProject,
                                    function(value) {
                                        currentProject.duration = value;
                                        TimelineZoom.updateTimelineDisplay();
                                    },
                                    function() { return currentProject.duration; });
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
        if (!scrollHandle || !timelineScroll) {
            return;
        }

        var self = this;
        var isDragging = false;
        var startMovementX = 0;
        var startScrollPosition = 0;

        scrollHandle.addEventListener('mousedown', function(e) {
            isDragging = true;
            startMovementX = e.clientX;
            startScrollPosition = self.scrollPosition;
            scrollHandle.style.cursor = 'grabbing';
            e.preventDefault();
        });

        document.addEventListener('mousemove', function(e) {
            if (!isDragging) {
                return;
            }
            var scrollWidth = timelineScroll.clientWidth;
            var handleWidth = scrollHandle.offsetWidth;
            var maxScroll = scrollWidth - handleWidth;
            if (maxScroll > 0) {
                var deltaRatio = (e.clientX - startMovementX) / maxScroll;
                self.setScrollPosition(startScrollPosition + deltaRatio);
            }
        });

        document.addEventListener('mouseup', function() {
            if (!isDragging) {
                return;
            }
            isDragging = false;
            scrollHandle.style.cursor = 'pointer';
        });

        timelineScroll.addEventListener('click', function(e) {
            if (e.target !== timelineScroll) {
                return;
            }
            var rect = timelineScroll.getBoundingClientRect();
            var handleWidth = scrollHandle.offsetWidth;
            var maxScroll = rect.width - handleWidth;
            if (maxScroll <= 0) {
                return;
            }
            var clickX = e.clientX - rect.left - handleWidth / 2;
            self.setScrollPosition(clickX / maxScroll);
        });
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
        var timelineWidth = document.querySelector('.timeline-ruler').clientWidth;
        var totalSeconds = Project.timelineDurationMs() / 1000;
        if (totalSeconds <= 0) {
            return;
        }
        var requiredPixelsPerSecond = timelineWidth / totalSeconds;

        this.zoomLevel = Math.max(this.minZoom, Math.min(this.maxZoom, requiredPixelsPerSecond / this.pixelsPerSecond));
        this.scrollPosition = 0;
        this.updateTimelineDisplay();
    },
    
    getMetrics: function() {
        var ruler = document.querySelector('.timeline-ruler');
        var visibleWidth = ruler ? ruler.clientWidth : 0;
        var totalWidth = visibleWidth * this.zoomLevel;
        var maxScroll = Math.max(0, totalWidth - visibleWidth);
        var scrollOffset = this.scrollPosition * maxScroll;
        return {
            visibleWidth: visibleWidth,
            totalWidth: totalWidth,
            maxScroll: maxScroll,
            scrollOffset: scrollOffset
        };
    },

    timeReference: function() {
        return document.querySelector('.track-line') || document.querySelector('.timeline-ruler');
    },

    clientXToAbsoluteX: function(clientX) {
        var metrics = this.getMetrics();
        var ref = this.timeReference();
        if (!ref) {
            return 0;
        }
        var rect = ref.getBoundingClientRect();
        return metrics.scrollOffset + (clientX - rect.left);
    },

    clientXToTimeMs: function(clientX) {
        var duration = Project.timelineDurationMs();
        var metrics = this.getMetrics();
        if (!duration || metrics.totalWidth <= 0) {
            return 0;
        }
        var absoluteX = this.clientXToAbsoluteX(clientX);
        var percent = absoluteX / metrics.totalWidth;
        return Math.round(Math.max(0, Math.min(duration, duration * percent)));
    },

    timeMsToVisibleX: function(timeMs) {
        var duration = Project.timelineDurationMs();
        var metrics = this.getMetrics();
        if (!duration || metrics.totalWidth <= 0) {
            return 0;
        }
        var absoluteX = (timeMs / duration) * metrics.totalWidth;
        return absoluteX - metrics.scrollOffset;
    },

    setScrollPosition: function(ratio) {
        this.scrollPosition = Math.max(0, Math.min(1, ratio));
        this.updateTimelineDisplay();
    },

    ensureTimeVisible: function(timeMs) {
        var duration = Project.timelineDurationMs();
        if (!duration) {
            return;
        }
        var metrics = this.getMetrics();
        if (metrics.totalWidth <= metrics.visibleWidth) {
            return;
        }
        var absoluteX = (timeMs / duration) * metrics.totalWidth;
        var margin = metrics.visibleWidth * 0.08;
        var visibleStart = metrics.scrollOffset;
        var visibleEnd = visibleStart + metrics.visibleWidth;
        if (absoluteX < visibleStart + margin) {
            var newOffset = Math.max(0, absoluteX - margin);
            this.setScrollPosition(metrics.maxScroll > 0 ? newOffset / metrics.maxScroll : 0);
        } else if (absoluteX > visibleEnd - margin) {
            var trailingOffset = Math.min(metrics.maxScroll, absoluteX - metrics.visibleWidth + margin);
            this.setScrollPosition(metrics.maxScroll > 0 ? trailingOffset / metrics.maxScroll : 0);
        }
    },

    updateTimelineDisplay: function() {
        this.updateRulerMarks();
        this.updateTrackLines();
        this.updateScrollHandle();
        this.updateClipPositions();
        Playhead.updateVisual();
        UI.updateTimelineActions();
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
        var timelineDuration = Project.timelineDurationMs();
        if (!rulerMarks || !timelineDuration) {
            return;
        }

        rulerMarks.innerHTML = '';
        const markWidthPx = 60;
        rulerMarks.style.width = `${100 * this.zoomLevel}%`;
        const totalWidth = rulerMarks.offsetWidth || document.querySelector('.timeline-ruler').clientWidth;
        const metrics = this.getMetrics();
        rulerMarks.style.transform = `translateX(-${metrics.scrollOffset}px)`;
        const totalMarks = Math.max(1, Math.floor(totalWidth / markWidthPx));
        const timeIntervalMs = timelineDuration / totalMarks;

        for (let i = 0; i <= totalMarks; i++) {
            const timeMs = Math.min(timelineDuration, Math.round(i * timeIntervalMs));
            const position = (timeMs * 100) / timelineDuration;
            const label = TimelineZoom.formatRulerTime(timeMs);
            rulerMarks.insertAdjacentHTML('beforeend',
                '<div class="time-mark" style="left: ' + position + '%">' + label + '</div>');
        }
    },
    
    updateTrackLines: function() {
        const trackLines = document.querySelectorAll('.track-line');
        const metrics = this.getMetrics();

        trackLines.forEach(trackLine => {
            trackLine.style.background = `repeating-linear-gradient(
                90deg,
                transparent,
                transparent ${this.getGridSize() - 1}px,
                rgba(52, 73, 94, 0.3) ${this.getGridSize()}px
            )`;
            trackLine.style.width = `${metrics.totalWidth}px`;
            trackLine.style.transform = `translateX(-${metrics.scrollOffset}px)`;
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
        const metrics = this.getMetrics();
        var timelineDuration = Project.timelineDurationMs();

        clips.forEach(clip => {
            const clipHash = clip.getAttribute('item-hash');
            const clipData = Project.findClip(clipHash);

            if (clipData && timelineDuration > 0) {
                const media = Project.findMedia(clipData.mediaHash);
                const duration = clipData.duration > 0 ? clipData.duration : Project.clipDuration(media);

                const startPixels = (clipData.start / timelineDuration) * metrics.totalWidth;
                const durationPixels = (duration / timelineDuration) * metrics.totalWidth;

                clip.style.left = `${startPixels - metrics.scrollOffset}px`;
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
    
    timeToPixels: function(timeInMillis) {
        var timelineDuration = Project.timelineDurationMs();
        var visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        var totalWidth = visibleWidth * this.zoomLevel;
        if (!timelineDuration) {
            return 0;
        }
        return (timeInMillis / timelineDuration) * totalWidth;
    },

    pixelsToTime: function(pixels) {
        var timelineDuration = Project.timelineDurationMs();
        var visibleWidth = document.querySelector('.timeline-ruler').clientWidth;
        var totalWidth = visibleWidth * this.zoomLevel;
        if (!totalWidth) {
            return 0;
        }
        return (pixels / totalWidth) * timelineDuration;
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
        var uploadId = 'upload-' + Date.now() + '-' + Math.random().toString(36).slice(2, 9);
        MediaLibrary.uploadsInProgress += 1;
        UI.setButtonBusy(uploadBtn, true);
        UI.addPendingUpload(uploadId, file);
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
                UI.failPendingUpload(uploadId);
                UI.notify('Upload failed: ' + data.error + ' — try again.', 'error');
                return;
            }
            if (!currentProject.medias) {
                currentProject.medias = [];
            }
            currentProject.medias.push(data);
            UI.completePendingUpload(uploadId, data);
            UI.notify('Added ' + file.name + ' to project.', 'success');
        })
        .catch(function(error) {
            console.error('Error:', error);
            UI.failPendingUpload(uploadId);
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
    followingPlayback: false,
    dragging: false,
    positionMs: function() {
        return this.positionMs_;
    },
    init: function() {
        Playhead.ensureElement(document.getElementById('tracks-container'));
        var ruler = document.querySelector('.timeline-ruler');
        if (ruler) {
            ruler.addEventListener('mousedown', function(e) {
                Playhead.dragging = true;
                Playhead.setFollowingPlayback(false);
                Playhead.setPositionFromClientX(e.clientX);
                e.preventDefault();
            });
        }

        document.addEventListener('mousemove', function(e) {
            if (!Playhead.dragging) {
                return;
            }
            Playhead.setPositionFromClientX(e.clientX);
        });

        document.addEventListener('mouseup', function() {
            Playhead.dragging = false;
        });

        var tracksContainer = document.getElementById('tracks-container');
        if (tracksContainer) {
            tracksContainer.addEventListener('mousedown', function(e) {
                if (e.target.closest('.clip') || e.target.closest('.track-btn') ||
                    e.target.closest('.track-header') || ClipMove.isActive()) {
                    return;
                }
                if (e.target.closest('.track-line') || e.target.closest('.track-area')) {
                    Playhead.dragging = true;
                    Playhead.setFollowingPlayback(false);
                    Playhead.setPositionFromClientX(e.clientX);
                    e.preventDefault();
                }
            });
            tracksContainer.addEventListener('scroll', function() {
                Playhead.updateLayout();
            });
        }

        this.updateVisual();
    },
    ensureElement: function(container) {
        if (!container) {
            return;
        }
        var playhead = document.getElementById('playhead');
        if (!playhead) {
            container.insertAdjacentHTML('afterbegin',
                '<div class="playhead" id="playhead" role="slider" aria-label="Playhead" aria-valuemin="0" tabindex="0">' +
                '<div class="playhead-line"></div></div>');
            return;
        }
        if (playhead.parentElement !== container) {
            container.insertBefore(playhead, container.firstChild);
        } else if (container.firstChild !== playhead) {
            container.insertBefore(playhead, container.firstChild);
        }
    },
    setPositionFromClientX: function(clientX) {
        var metrics = TimelineZoom.getMetrics();
        if (metrics.visibleWidth <= 0 || metrics.totalWidth <= 0) {
            return;
        }
        this.setPositionMs(TimelineZoom.clientXToTimeMs(clientX), { follow: true });
    },
    setPositionPercent: function(percent) {
        this.setPositionMs(Math.round(Project.timelineDurationMs() * percent), { follow: true });
    },
    setPositionMs: function(ms, options) {
        options = options || {};
        var duration = Project.timelineDurationMs();
        if (!duration) {
            return;
        }
        this.positionMs_ = Math.max(0, Math.min(Math.round(ms), duration));
        this.updateVisual(options);
        if (!options.fromVideo) {
            Preview.syncFromPlayhead(this.positionMs_);
        }
    },
    setFollowingPlayback: function(active) {
        this.followingPlayback = !!active;
        var playhead = document.getElementById('playhead');
        if (playhead) {
            playhead.classList.toggle('playhead--following', this.followingPlayback);
        }
    },
    updateLayout: function() {
        var playhead = document.getElementById('playhead');
        var container = document.getElementById('tracks-container');
        if (!playhead || !container) {
            return;
        }
        var areas = container.querySelectorAll('.track-area');
        if (!areas.length) {
            playhead.style.visibility = 'hidden';
            return;
        }
        var containerRect = container.getBoundingClientRect();
        var firstRect = areas[0].getBoundingClientRect();
        var lastRect = areas[areas.length - 1].getBoundingClientRect();
        playhead.style.top = (firstRect.top - containerRect.top + container.scrollTop) + 'px';
        playhead.style.height = (lastRect.bottom - firstRect.top) + 'px';
    },
    updateVisual: function(options) {
        options = options || {};
        var playhead = document.getElementById('playhead');
        var rulerMarker = document.getElementById('playheadRulerMarker');
        if (!playhead || !Project.timelineDurationMs()) {
            return;
        }
        this.updateLayout();
        var metrics = TimelineZoom.getMetrics();
        var visibleX = TimelineZoom.timeMsToVisibleX(this.positionMs_);
        var inView = visibleX >= -1 && visibleX <= metrics.visibleWidth + 1;
        if (metrics.visibleWidth > 0 && inView) {
            playhead.style.visibility = 'visible';
            playhead.style.left = visibleX + 'px';
            if (rulerMarker) {
                rulerMarker.style.visibility = 'visible';
                rulerMarker.style.left = visibleX + 'px';
            }
        } else {
            playhead.style.visibility = 'hidden';
            if (rulerMarker) {
                rulerMarker.style.visibility = 'hidden';
            }
        }
        playhead.setAttribute('aria-valuenow', this.positionMs_);
        playhead.setAttribute('aria-valuemax', Project.timelineDurationMs());
        var formatted = UI.mediaDuration(this.positionMs_);
        playhead.setAttribute('aria-valuetext', formatted);
        playhead.title = formatted;
        var timeLabel = document.getElementById('playheadTimeLabel');
        if (timeLabel) {
            timeLabel.textContent = formatted;
        }
        if (options.follow) {
            TimelineZoom.ensureTimeVisible(this.positionMs_);
        }
        UI.updateTimelineActions();
    }
};

const ClipMove = {
    pending: null,
    active: null,
    suppressClick: false,
    dragThreshold: 4,
    isActive: function() {
        return !!this.active;
    },
    init: function() {
        var self = this;
        document.addEventListener('mousedown', function(e) { self.onMouseDown(e); });
        document.addEventListener('mousemove', function(e) { self.onMouseMove(e); });
        document.addEventListener('mouseup', function(e) { self.onMouseUp(e); });
    },
    onMouseDown: function(e) {
        if (e.button !== 0) {
            return;
        }
        var clipElm = e.target.closest('.clip[item-hash]');
        if (!clipElm || e.target.closest('.clip-trim') || e.target.closest('.clip-btn')) {
            return;
        }
        var hash = clipElm.getAttribute('item-hash');
        var clip = Project.findClip(hash);
        if (!clip) {
            return;
        }
        var track = Project.findTrack(clip.trackIndex);
        if (track && track.locked) {
            return;
        }
        this.pending = {
            hash: hash,
            startX: e.clientX,
            startY: e.clientY,
            clipElm: clipElm
        };
    },
    beginDrag: function(e) {
        if (!this.pending) {
            return;
        }
        var hash = this.pending.hash;
        var clip = Project.findClip(hash);
        if (!clip) {
            this.pending = null;
            return;
        }
        var media = Project.findMedia(clip.mediaHash);
        var duration = clip.duration > 0 ? clip.duration : Project.clipDuration(media);
        var clipRect = this.pending.clipElm.getBoundingClientRect();
        var grabOffsetMs = 0;
        if (clipRect.width > 0 && duration > 0) {
            grabOffsetMs = Math.round(((e.clientX - clipRect.left) / clipRect.width) * duration);
        }
        DragNDrop.activeDrag = {
            kind: 'CLIP',
            hash: hash,
            clipType: clip.type,
            syncGroup: clip.syncGroup || null,
            grabOffsetMs: grabOffsetMs,
            duration: duration
        };
        UI.selectElement('CLIP', hash);
        UI.setClipDraggingState(hash, true);
        this.pending.clipElm.classList.add('clip--moving');
        this.active = { hash: hash };
        this.pending = null;
        document.body.classList.add('clip-drag-active');
    },
    trackLineUnderPointer: function(e) {
        var trackArea = Array.from(document.querySelectorAll('.track-area')).find(function(area) {
            var rect = area.getBoundingClientRect();
            return e.clientX >= rect.left && e.clientX <= rect.right &&
                   e.clientY >= rect.top && e.clientY <= rect.bottom;
        });
        return trackArea ? trackArea.querySelector('.track-line') : null;
    },
    onMouseMove: function(e) {
        if (this.pending && !this.active) {
            var dx = Math.abs(e.clientX - this.pending.startX);
            var dy = Math.abs(e.clientY - this.pending.startY);
            if (dx > this.dragThreshold || dy > this.dragThreshold) {
                this.beginDrag(e);
            }
        }
        if (!this.active) {
            return;
        }
        var trackLine = this.trackLineUnderPointer(e);
        if (!trackLine) {
            UI.removeShadowComponent();
            document.querySelectorAll('.track-area.active, .track-line.active').forEach(function(elm) {
                elm.classList.remove('active');
            });
            return;
        }
        var trackArea = trackLine.closest('.track-area');
        var trackIndex = parseInt(trackArea.getAttribute('data-track-index'), 10);
        var fakeEvent = { clientX: e.clientX, realTarget: trackLine, target: trackLine };
        if (!Project.acceptsClipOnTrack(this.active.hash, trackIndex)) {
            UI.removeShadowComponent();
            return;
        }
        var startMs = DragNDrop.clipStartFromPosition(DragNDrop.calculateDropPosition(fakeEvent));
        UI.updateShadowElementsForClipMove(startMs, this.active.hash, trackIndex);
        UI.highlightTrack(fakeEvent, true);
    },
    onMouseUp: function(e) {
        if (this.pending && !this.active) {
            this.pending = null;
            return;
        }
        if (!this.active) {
            return;
        }
        this.suppressClick = true;
        var trackLine = this.trackLineUnderPointer(e);
        if (trackLine) {
            var trackArea = trackLine.closest('.track-area');
            var trackIndex = parseInt(trackArea.getAttribute('data-track-index'), 10);
            var fakeEvent = { clientX: e.clientX, realTarget: trackLine, target: trackLine };
            if (Project.acceptsClipOnTrack(this.active.hash, trackIndex)) {
                var startMs = DragNDrop.clipStartFromPosition(DragNDrop.calculateDropPosition(fakeEvent));
                Project.moveClipToStart(this.active.hash, startMs, trackIndex);
            }
        }
        UI.removeShadowComponent();
        UI.setClipDraggingState(null, false);
        document.querySelectorAll('.track-area.active, .track-line.active').forEach(function(elm) {
            elm.classList.remove('active');
        });
        document.querySelectorAll('.clip.clip--moving').forEach(function(elm) {
            elm.classList.remove('clip--moving');
        });
        DragNDrop.clearActiveDrag();
        this.active = null;
        document.body.classList.remove('clip-drag-active');
    }
};

const EditSettings = {
    rippleDelete: false
};

const History = {
    undoStack: [],
    redoStack: [],
    maxSize: 50,
    applying: false,
    snapshotClips: function() {
        return {
            clips: JSON.parse(JSON.stringify(currentProject.clips || [])),
            tracks: JSON.parse(JSON.stringify(currentProject.tracks || []))
        };
    },
    push: function(label) {
        if (this.applying) {
            return;
        }
        this.undoStack.push({ label: label || 'Edit', snapshot: this.snapshotClips() });
        if (this.undoStack.length > this.maxSize) {
            this.undoStack.shift();
        }
        this.redoStack = [];
        MenuBar.updateEditState();
    },
    restore: function(snapshot) {
        currentProject.clips = JSON.parse(JSON.stringify(snapshot.clips));
        currentProject.tracks = JSON.parse(JSON.stringify(snapshot.tracks));
        Project.ensureStructure();
        UI.reconciliateTracks();
        UI.reconciliateClips();
        UI.updateTimelineActions();
    },
    undo: function() {
        if (this.undoStack.length === 0) {
            return;
        }
        this.applying = true;
        var current = this.snapshotClips();
        var entry = this.undoStack.pop();
        this.redoStack.push({ label: entry.label, snapshot: current });
        this.restore(entry.snapshot);
        this.applying = false;
        MenuBar.updateEditState();
        ProjectSave.schedule();
        UI.notify('Undo: ' + entry.label, 'info');
    },
    redo: function() {
        if (this.redoStack.length === 0) {
            return;
        }
        this.applying = true;
        var current = this.snapshotClips();
        var entry = this.redoStack.pop();
        this.undoStack.push({ label: entry.label, snapshot: current });
        this.restore(entry.snapshot);
        this.applying = false;
        MenuBar.updateEditState();
        ProjectSave.schedule();
        UI.notify('Redo: ' + entry.label, 'info');
    },
    execute: function(fn, label) {
        if (this.applying) {
            fn();
            return;
        }
        this.push(label);
        fn();
    },
    canUndo: function() {
        return this.undoStack.length > 0;
    },
    canRedo: function() {
        return this.redoStack.length > 0;
    }
};

const Clipboard = {
    clips: null,
    collectClips: function(clipHash) {
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return [];
        }
        return Project.findAvPartners(clip).map(function(c) {
            return JSON.parse(JSON.stringify(c));
        });
    },
    copy: function() {
        var hash = UI.getSelectedClipHash();
        if (!hash) {
            UI.notify('Select a clip to copy.', 'error');
            return;
        }
        this.clips = this.collectClips(hash);
        UI.notify('Clip copied.', 'info');
    },
    paste: function() {
        if (!this.clips || this.clips.length === 0) {
            UI.notify('Nothing to paste.', 'error');
            return;
        }
        var self = this;
        History.execute(function() {
            var playheadMs = Playhead.positionMs();
            var syncMap = {};
            var promises = self.clips.map(function(source) {
                return Hash.generate('paste-' + source.hash + '-' + Date.now() + '-' + Math.random()).then(function(newHash) {
                    var copy = JSON.parse(JSON.stringify(source));
                    copy.hash = newHash;
                    copy.start = playheadMs;
                    if (copy.syncGroup) {
                        if (!syncMap[copy.syncGroup]) {
                            syncMap[copy.syncGroup] = 'sync-paste-' + Date.now() + '-' + Math.random();
                        }
                        copy.syncGroup = syncMap[copy.syncGroup];
                    }
                    currentProject.clips.push(copy);
                });
            });
            Promise.all(promises).then(function() {
                UI.reconciliateClips();
                ProjectSave.schedule();
                UI.notify('Clip pasted at playhead.', 'success');
            });
        }, 'Paste clip');
    },
    duplicate: function() {
        var hash = UI.getSelectedClipHash();
        if (!hash) {
            UI.notify('Select a clip to duplicate.', 'error');
            return;
        }
        this.copy();
        var clip = Project.findClip(hash);
        if (!clip) {
            return;
        }
        var offsetMs = Project.clipEffectiveDuration(clip);
        var saved = this.clips;
        History.execute(function() {
            Clipboard.clips = saved;
            var playheadMs = clip.start + offsetMs;
            var syncMap = {};
            var promises = saved.map(function(source) {
                return Hash.generate('dup-' + source.hash + '-' + Date.now() + '-' + Math.random()).then(function(newHash) {
                    var copy = JSON.parse(JSON.stringify(source));
                    copy.hash = newHash;
                    copy.start = playheadMs;
                    if (copy.syncGroup) {
                        if (!syncMap[copy.syncGroup]) {
                            syncMap[copy.syncGroup] = 'sync-dup-' + Date.now() + '-' + Math.random();
                        }
                        copy.syncGroup = syncMap[copy.syncGroup];
                    }
                    currentProject.clips.push(copy);
                });
            });
            Promise.all(promises).then(function() {
                UI.reconciliateClips();
                ProjectSave.schedule();
                UI.notify('Clip duplicated.', 'success');
            });
        }, 'Duplicate clip');
    }
};

const TimelineSnap = {
    enabled: true,
    thresholdMs: 120,
    snapMs: function(timeMs) {
        if (!this.enabled) {
            return Math.round(timeMs);
        }
        var candidates = [Math.round(timeMs)];
        var gridMs = 1000;
        candidates.push(Math.round(timeMs / gridMs) * gridMs);
        Project.allClips().forEach(function(clip) {
            var media = Project.findMedia(clip.mediaHash);
            var duration = Project.clipEffectiveDuration(clip);
            candidates.push(clip.start);
            candidates.push(clip.start + duration);
        });
        candidates.push(0);
        candidates.push(Playhead.positionMs());
        var best = Math.round(timeMs);
        var bestDist = this.thresholdMs + 1;
        candidates.forEach(function(candidate) {
            var dist = Math.abs(candidate - timeMs);
            if (dist <= TimelineSnap.thresholdMs && dist < bestDist) {
                best = candidate;
                bestDist = dist;
            }
        });
        return best;
    }
};

const ContextMenu = {
    menuEl: null,
    init: function() {
        if (!document.getElementById('context-menu-styles')) {
            var style = document.createElement('style');
            style.id = 'context-menu-styles';
            style.textContent = '.context-menu{position:fixed;z-index:1000;min-width:180px;background:var(--vc-color-bg-panel,#2b2b2b);border:1px solid var(--vc-color-border,#555);border-radius:4px;box-shadow:0 4px 12px rgba(0,0,0,.35);padding:4px 0}.context-menu-item{display:block;width:100%;text-align:left;padding:6px 12px;background:none;border:none;color:var(--vc-color-text,#eee);font-size:13px;cursor:pointer}.context-menu-item:hover:not(:disabled){background:var(--vc-color-bg-hover,#3d3d3d)}.context-menu-item:disabled{opacity:.5;cursor:default}.context-menu-sep{height:1px;margin:4px 0;background:var(--vc-color-border,#555)}';
            document.head.appendChild(style);
        }
        var self = this;
        this.menuEl = document.createElement('div');
        this.menuEl.className = 'context-menu visually-hidden';
        this.menuEl.setAttribute('role', 'menu');
        document.body.appendChild(this.menuEl);
        document.addEventListener('click', function() { self.hide(); });
        document.addEventListener('contextmenu', function(e) {
            if (!e.target.closest('.clip[item-hash]') && !e.target.closest('.track-area')) {
                self.hide();
            }
        });
        var tracksContainer = document.getElementById('tracks-container');
        if (!tracksContainer) {
            return;
        }
        tracksContainer.addEventListener('contextmenu', function(e) {
            var clip = e.target.closest('.clip[item-hash]');
            if (clip) {
                e.preventDefault();
                self.showClipMenu(e, clip.getAttribute('item-hash'));
                return;
            }
            var trackArea = e.target.closest('.track-area');
            if (trackArea) {
                e.preventDefault();
                self.showTrackMenu(e, parseInt(trackArea.getAttribute('data-track-index'), 10));
            }
        });
    },
    show: function(x, y, items) {
        var self = this;
        this.menuEl.innerHTML = '';
        items.forEach(function(item) {
            if (item.separator) {
                var sep = document.createElement('div');
                sep.className = 'context-menu-sep';
                self.menuEl.appendChild(sep);
                return;
            }
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'context-menu-item';
            btn.textContent = item.label;
            btn.disabled = !!item.disabled;
            btn.addEventListener('click', function(ev) {
                ev.stopPropagation();
                self.hide();
                if (item.action) {
                    item.action();
                }
            });
            self.menuEl.appendChild(btn);
        });
        this.menuEl.classList.remove('visually-hidden');
        this.menuEl.style.left = x + 'px';
        this.menuEl.style.top = y + 'px';
    },
    hide: function() {
        if (this.menuEl) {
            this.menuEl.classList.add('visually-hidden');
        }
    },
    showClipMenu: function(e, clipHash) {
        var clip = Project.findClip(clipHash);
        var linked = clip && clip.syncGroup;
        this.show(e.clientX, e.clientY, [
            { label: 'Cut at Playhead', action: function() { Project.cutAtPlayhead(); } },
            { label: 'Copy', action: function() { UI.selectElement('CLIP', clipHash); Clipboard.copy(); } },
            { label: 'Paste at Playhead', action: function() { Clipboard.paste(); }, disabled: !Clipboard.clips },
            { label: 'Duplicate', action: function() { UI.selectElement('CLIP', clipHash); Clipboard.duplicate(); } },
            { separator: true },
            { label: 'Delete', action: function() { UI.selectElement('CLIP', clipHash); Project.requestDeleteClip(clipHash); } },
            { label: 'Unlink A/V', action: function() { UI.selectElement('CLIP', clipHash); Project.unlinkAv(clipHash); }, disabled: !linked },
            { separator: true },
            { label: 'Properties', action: function() { UI.selectElement('CLIP', clipHash); UI.switchTab('Properties'); } }
        ]);
    },
    showTrackMenu: function(e, trackIndex) {
        var track = Project.findTrack(trackIndex);
        if (!track) {
            return;
        }
        this.show(e.clientX, e.clientY, [
            { label: track.muted ? 'Unmute Track' : 'Mute Track', action: function() {
                track.muted = !track.muted;
                UI.reconciliateTracks();
                ProjectSave.schedule();
            }},
            { label: track.locked ? 'Unlock Track' : 'Lock Track', action: function() {
                track.locked = !track.locked;
                UI.reconciliateTracks();
                ProjectSave.schedule();
            }},
            { label: 'Add ' + (track.type === 'AUDIO' ? 'Audio' : 'Video') + ' Track', action: function() {
                Project.addTrack(track.type);
            }}
        ]);
    }
};

const MediaBin = {
    filterQuery: '',
    sortKey: 'name',
    init: function() {
        var search = document.getElementById('mediaBinSearch');
        var sort = document.getElementById('mediaBinSort');
        if (search) {
            search.addEventListener('input', function() {
                MediaBin.filterQuery = search.value.trim().toLowerCase();
                MediaBin.applyFilter();
            });
        }
        if (sort) {
            sort.addEventListener('change', function() {
                MediaBin.sortKey = sort.value;
                MediaBin.applySort();
            });
        }
    },
    applyFilter: function() {
        document.querySelectorAll('#media-list .file-item').forEach(function(item) {
            var nameEl = item.querySelector('.file-name');
            var text = nameEl ? nameEl.textContent.toLowerCase() : '';
            item.style.display = !MediaBin.filterQuery || text.indexOf(MediaBin.filterQuery) !== -1 ? '' : 'none';
        });
    },
    applySort: function() {
        var list = document.getElementById('media-list');
        if (!list) {
            return;
        }
        var items = Array.from(list.querySelectorAll('.file-item'));
        items.sort(function(a, b) {
            var uploadA = a.hasAttribute('item-upload-id');
            var uploadB = b.hasAttribute('item-upload-id');
            if (uploadA && !uploadB) {
                return -1;
            }
            if (!uploadA && uploadB) {
                return 1;
            }
            var mediaA = Project.findMedia(a.getAttribute('item-hash'));
            var mediaB = Project.findMedia(b.getAttribute('item-hash'));
            if (!mediaA || !mediaB) {
                return 0;
            }
            if (MediaBin.sortKey === 'duration') {
                return (mediaB.duration || 0) - (mediaA.duration || 0);
            }
            if (MediaBin.sortKey === 'type') {
                return Project.resolveMediaType(mediaA).localeCompare(Project.resolveMediaType(mediaB));
            }
            return (mediaA.name || '').localeCompare(mediaB.name || '');
        });
        items.forEach(function(item) { list.appendChild(item); });
        this.applyFilter();
    },
    refresh: function() {
        this.applySort();
    }
};

const RecentProjects = {
    storageKey: 'videoCreatorRecent',
    maxItems: 8,
    track: function() {
        if (!currentProject || !currentProject.id) {
            return;
        }
        var list = [];
        try {
            list = JSON.parse(localStorage.getItem(this.storageKey) || '[]');
        } catch (err) {
            list = [];
        }
        list = list.filter(function(p) { return p.id !== currentProject.id; });
        list.unshift({
            id: currentProject.id,
            name: currentProject.name || 'Untitled project',
            openedAt: Date.now()
        });
        list = list.slice(0, this.maxItems);
        localStorage.setItem(this.storageKey, JSON.stringify(list));
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
            Preview.scheduleRefresh();
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
    suppressVideoSync: false,
    durationMs: 0,
    hls: null,
    sessionId: null,
    manifestUrl: null,
    ws: null,
    refreshTimer: null,
    progressPollTimer: null,
    shuttleSpeed: 0,
    init: function() {
        this.container = document.querySelector('.preview-container');
        this.controls = document.getElementById('previewControls');
        this.video = document.getElementById('previewVideo');
        this.placeholder = document.getElementById('previewPlaceholder');
        this.bindTransport(document.getElementById('playBtn'), 'play');
        this.bindTransport(document.getElementById('stopBtn'), 'stop');
        this.bindTransport(document.getElementById('previewPlayBtn'), 'play');
        this.bindTransport(document.getElementById('previewRewindBtn'), 'rewind');
        this.bindTransport(document.getElementById('previewForwardBtn'), 'forward');
        this.bindTransport(document.getElementById('previewPrevFrameBtn'), 'prevFrame');
        this.bindTransport(document.getElementById('previewNextFrameBtn'), 'nextFrame');
        var fullscreenBtn = document.getElementById('previewFullscreenBtn');
        if (fullscreenBtn) {
            fullscreenBtn.addEventListener('click', function() { Preview.toggleFullscreen(); });
        }
        if (this.video) {
            this.video.addEventListener('timeupdate', function() {
                Preview.syncPlayheadFromVideo();
            });
            this.video.addEventListener('play', function() {
                Playhead.setFollowingPlayback(true);
            });
            this.video.addEventListener('pause', function() {
                Playhead.setFollowingPlayback(false);
            });
            this.video.addEventListener('ended', function() {
                Playhead.setFollowingPlayback(false);
            });
            this.video.addEventListener('seeked', function() {
                Preview.syncPlayheadFromVideo();
            });
            this.video.addEventListener('dblclick', function() {
                Preview.toggleFullscreen();
            });
        }
        if (this.container) {
            this.container.addEventListener('dblclick', function(e) {
                if (e.target === Preview.container || e.target === Preview.placeholder) {
                    Preview.toggleFullscreen();
                }
            });
        }
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
        this.disconnectSession();
        if (this.container) {
            this.container.classList.remove('preview-container--active');
        }
        if (this.video) {
            this.video.classList.add('visually-hidden');
            this.video.pause();
            if (this.hls) {
                this.hls.destroy();
                this.hls = null;
            }
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
    syncPlayheadFromVideo: function() {
        if (this.suppressVideoSync || !this.hasLoadedVideo() || !this.video) {
            return;
        }
        Playhead.setPositionMs(Math.round(this.video.currentTime * 1000), { fromVideo: true, follow: true });
    },
    syncFromPlayhead: function(positionMs) {
        if (!this.hasLoadedVideo() || !this.video) {
            return;
        }
        var seekSec = positionMs / 1000;
        var maxSec = isFinite(this.video.duration) ? this.video.duration : seekSec;
        if (seekSec < 0 || seekSec > maxSec) {
            return;
        }
        if (Math.abs(this.video.currentTime - seekSec) < 0.05) {
            return;
        }
        this.suppressVideoSync = true;
        this.video.currentTime = seekSec;
        this.suppressVideoSync = false;
    },
    seekVideoToPlayhead: function() {
        this.syncFromPlayhead(Playhead.positionMs());
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
            this.syncPlayheadFromVideo();
            return;
        }
        Playhead.setPositionMs(Playhead.positionMs() + deltaSeconds * 1000, { follow: true });
    },
    stepFrame: function(direction) {
        var fps = (currentProject && currentProject.frameRate) || 30;
        var stepMs = Math.round(1000 / fps) * direction;
        if (this.hasLoadedVideo()) {
            this.video.pause();
            this.video.currentTime = Math.max(0, this.video.currentTime + stepMs / 1000);
            this.syncPlayheadFromVideo();
            return;
        }
        Playhead.setPositionMs(Playhead.positionMs() + stepMs, { follow: true });
    },
    generate: function() {
        if (!currentProject.id || this.running) {
            return;
        }
        if (typeof meltAvailable !== 'undefined' && !meltAvailable) {
            UI.notify('Preview unavailable — MLT (melt) is not installed.', 'error');
            return;
        }
        this.running = true;
        var playBtn = document.getElementById('playBtn');
        var previewPlayBtn = document.getElementById('previewPlayBtn');
        UI.setButtonBusy(playBtn, true);
        UI.setButtonBusy(previewPlayBtn, true);
        UI.setPreviewProgress(0, null);
        ProjectSave.save({ silent: true })
            .then(function() {
                return fetch('/api/editor/' + currentProject.id + '/preview/session', { method: 'POST' });
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
                Preview.sessionId = data.sessionId;
                UI.setPreviewProgress(data.percent || 0, data.etaSeconds);
                return Preview.waitForSessionReady(data.sessionId);
            })
            .then(function(data) {
                Preview.sessionId = data.sessionId;
                Preview.manifestUrl = data.manifestUrl;
                Preview.durationMs = Project.contentDurationMs();
                Preview.connectWebSocket(data.sessionId);
                Preview.loadHls(data.manifestUrl);
                Preview.showVideo();
                UI.notify('Preview ready.', 'success');
                return new Promise(function(resolve, reject) {
                    Preview.video.onloadedmetadata = function() {
                        Preview.durationMs = Math.round(Preview.video.duration * 1000) || Preview.durationMs;
                        Preview.seekVideoToPlayhead();
                        Preview.video.play().then(resolve).catch(reject);
                    };
                    Preview.video.onerror = function() {
                        reject(new Error('Failed to load preview stream'));
                    };
                });
            })
            .catch(function(err) {
                UI.notify('Preview failed: ' + err.message + ' — check MLT status and try again.', 'error');
            })
            .finally(function() {
                Preview.stopProgressPoll();
                Preview.running = false;
                UI.setButtonBusy(document.getElementById('playBtn'), false);
                UI.setButtonBusy(document.getElementById('previewPlayBtn'), false);
            });
    },
    stopProgressPoll: function() {
        if (this.progressPollTimer) {
            clearTimeout(this.progressPollTimer);
            this.progressPollTimer = null;
        }
    },
    waitForSessionReady: function(sessionId) {
        var self = this;
        return new Promise(function(resolve, reject) {
            function poll() {
                fetch('/api/editor/' + currentProject.id + '/preview/session/' + sessionId)
                    .then(function(response) {
                        if (!response.ok) {
                            throw new Error('Preview status request failed');
                        }
                        return response.json();
                    })
                    .then(function(data) {
                        if (data.status === 'ready') {
                            self.stopProgressPoll();
                            resolve(data);
                            return;
                        }
                        if (data.status === 'failed') {
                            self.stopProgressPoll();
                            reject(new Error(data.error || 'Preview rendering failed'));
                            return;
                        }
                        UI.setPreviewProgress(data.percent || 0, data.etaSeconds);
                        self.progressPollTimer = setTimeout(poll, 400);
                    })
                    .catch(function(err) {
                        self.stopProgressPoll();
                        reject(err);
                    });
            }
            poll();
        });
    },
    loadHls: function(manifestUrl) {
        if (!this.video) {
            return;
        }
        if (this.hls) {
            this.hls.destroy();
            this.hls = null;
        }
        var url = manifestUrl + (manifestUrl.indexOf('?') === -1 ? '?' : '&') + 't=' + Date.now();
        if (typeof Hls !== 'undefined' && Hls.isSupported()) {
            this.hls = new Hls({ enableWorker: true });
            this.hls.loadSource(url);
            this.hls.attachMedia(this.video);
            this.hls.on(Hls.Events.ERROR, function(event, data) {
                if (data.fatal) {
                    console.error('HLS error', data);
                }
            });
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
            this.video.src = url;
        } else {
            throw new Error('HLS playback is not supported in this browser');
        }
    },
    reloadHls: function() {
        if (!this.manifestUrl) {
            return;
        }
        this.loadHls(this.manifestUrl);
        this.seekVideoToPlayhead();
    },
    connectWebSocket: function(sessionId) {
        this.disconnectWebSocket();
        var proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        this.ws = new WebSocket(proto + '//' + window.location.host + '/ws/preview/' + sessionId);
        this.ws.onmessage = function(ev) {
            try {
                var data = JSON.parse(ev.data);
                if (data.event === 'refreshed') {
                    Preview.reloadHls();
                    UI.notify('Preview refreshed.', 'info');
                }
            } catch (err) {
                console.warn('Preview WS message', ev.data);
            }
        };
        this.ws.onerror = function() {
            console.warn('Preview WebSocket error');
        };
    },
    disconnectWebSocket: function() {
        if (this.ws) {
            this.ws.onclose = null;
            if (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING) {
                this.ws.close();
            }
            this.ws = null;
        }
    },
    disconnectSession: function() {
        this.stopProgressPoll();
        this.disconnectWebSocket();
        this.sessionId = null;
        this.manifestUrl = null;
        this.shuttleSpeed = 0;
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
            this.refreshTimer = null;
        }
    },
    scheduleRefresh: function() {
        var self = this;
        if (!this.sessionId) {
            return;
        }
        if (this.refreshTimer) {
            clearTimeout(this.refreshTimer);
        }
        this.refreshTimer = setTimeout(function() {
            if (self.ws && self.ws.readyState === WebSocket.OPEN) {
                self.ws.send('refresh');
            }
        }, 1500);
    },
    handleShuttleKey: function(key) {
        if (!this.hasLoadedVideo() || !this.video) {
            return;
        }
        if (key === 'k' || key === 'K') {
            this.video.pause();
            this.shuttleSpeed = 0;
            this.video.playbackRate = 1;
            return;
        }
        if (key === 'j' || key === 'J') {
            if (this.video.paused) {
                this.seek(-1);
            } else {
                this.shuttleSpeed = Math.max(-4, this.shuttleSpeed - 1);
                this.applyShuttle();
            }
            return;
        }
        if (key === 'l' || key === 'L') {
            if (this.video.paused || this.shuttleSpeed <= 0) {
                this.shuttleSpeed = Math.max(1, this.shuttleSpeed + 1);
                this.applyShuttle();
            } else {
                this.shuttleSpeed = Math.min(4, this.shuttleSpeed + 1);
                this.applyShuttle();
            }
        }
    },
    applyShuttle: function() {
        if (!this.video) {
            return;
        }
        if (this.shuttleSpeed <= 0) {
            this.video.pause();
            this.video.playbackRate = 1;
            return;
        }
        this.video.playbackRate = this.shuttleSpeed;
        this.video.play();
    },
    toggleFullscreen: function() {
        var target = this.container || this.video;
        if (!target) {
            return;
        }
        if (document.fullscreenElement) {
            document.exitFullscreen();
            return;
        }
        if (target.requestFullscreen) {
            target.requestFullscreen();
        }
    },
    stop: function() {
        Playhead.setFollowingPlayback(false);
        this.showPlaceholder();
        Playhead.setPositionMs(0);
    }
};

const ClipTrim = {
    active: null,
    init: function() {
        var self = this;
        document.addEventListener('mousemove', function(e) { self.onMove(e); });
        document.addEventListener('mouseup', function(e) { self.onEnd(e); });
    },
    start: function(e, clipHash, edge) {
        e.preventDefault();
        var clip = Project.findClip(clipHash);
        if (!clip) {
            return;
        }
        var track = Project.findTrack(clip.trackIndex);
        if (track && track.locked) {
            UI.notify('Track is locked.', 'error');
            return;
        }
        History.push('Trim clip');
        var partners = Project.findAvPartners(clip);
        this.active = {
            clipHash: clipHash,
            edge: edge,
            startX: e.clientX,
            snapshot: partners.map(function(c) {
                return {
                    hash: c.hash,
                    start: c.start,
                    duration: Project.clipEffectiveDuration(c),
                    sourceIn: c.sourceIn || 0,
                    sourceOut: c.sourceOut || 0
                };
            })
        };
    },
    restoreSnapshot: function() {
        if (!this.active) {
            return;
        }
        this.active.snapshot.forEach(function(s) {
            var c = Project.findClip(s.hash);
            if (c) {
                c.start = s.start;
                c.duration = s.duration;
                c.sourceIn = s.sourceIn;
                c.sourceOut = s.sourceOut;
            }
        });
    },
    onMove: function(e) {
        if (!this.active) {
            return;
        }
        var metrics = TimelineZoom.getMetrics();
        if (metrics.totalWidth <= 0) {
            return;
        }
        this.restoreSnapshot();
        var deltaPx = e.clientX - this.active.startX;
        var deltaMs = Math.round((deltaPx / metrics.totalWidth) * Project.timelineDurationMs());
        var primary = this.active.snapshot[0];
        var changed = false;
        if (this.active.edge === 'left') {
            changed = Project.trimClipLeft(this.active.clipHash, primary.start + deltaMs);
        } else {
            changed = Project.trimClipRight(this.active.clipHash, primary.start + primary.duration + deltaMs);
        }
        if (changed) {
            UI.reconciliateClips();
        }
    },
    onEnd: function() {
        if (!this.active) {
            return;
        }
        ProjectSave.schedule();
        this.active = null;
    }
};

const MenuBar = {
    init: function() {
        document.querySelectorAll('.menu-item--dropdown').forEach(function(item) {
            item.addEventListener('click', function(e) {
                if (e.target.closest('.menu-dropdown-item')) {
                    return;
                }
                e.stopPropagation();
                var open = item.classList.contains('menu-item--open');
                document.querySelectorAll('.menu-item--open').forEach(function(el) {
                    el.classList.remove('menu-item--open');
                });
                if (!open) {
                    item.classList.add('menu-item--open');
                }
            });
        });
        document.addEventListener('click', function() {
            document.querySelectorAll('.menu-item--open').forEach(function(el) {
                el.classList.remove('menu-item--open');
            });
        });
        document.querySelectorAll('.menu-dropdown-item').forEach(function(btn) {
            btn.addEventListener('click', function() {
                MenuBar.handleAction(btn.getAttribute('data-action'));
            });
        });
        MenuBar.updateEditState();
    },
    toggleLabel: function(action, enabled, onLabel, offLabel) {
        var btn = document.querySelector('[data-action="' + action + '"]');
        if (btn) {
            btn.textContent = enabled ? onLabel : offLabel;
            btn.setAttribute('aria-checked', enabled ? 'true' : 'false');
        }
    },
    updateEditState: function() {
        var undoBtn = document.querySelector('[data-action="edit-undo"]');
        var redoBtn = document.querySelector('[data-action="edit-redo"]');
        if (undoBtn) {
            undoBtn.disabled = !History.canUndo();
        }
        if (redoBtn) {
            redoBtn.disabled = !History.canRedo();
        }
        MenuBar.toggleLabel('view-snap', TimelineSnap.enabled, 'Snap: On', 'Snap: Off');
        MenuBar.toggleLabel('view-ripple', EditSettings.rippleDelete, 'Ripple Delete: On', 'Ripple Delete: Off');
    },
    handleAction: function(action) {
        switch (action) {
            case 'file-new':
                window.location.href = '/editor/new';
                break;
            case 'file-open':
                window.location.href = '/';
                break;
            case 'file-save':
                ProjectSave.save({ silent: false });
                break;
            case 'file-export':
                document.getElementById('exportModal').style.display = 'flex';
                break;
            case 'edit-undo':
                History.undo();
                break;
            case 'edit-redo':
                History.redo();
                break;
            case 'edit-cut':
                Project.cutAtPlayhead();
                break;
            case 'edit-delete':
                var hash = UI.getSelectedClipHash();
                if (hash) {
                    Project.requestDeleteClip(hash);
                } else {
                    UI.notify('Select a clip to delete.', 'error');
                }
                break;
            case 'edit-copy':
                Clipboard.copy();
                break;
            case 'edit-paste':
                Clipboard.paste();
                break;
            case 'edit-duplicate':
                Clipboard.duplicate();
                break;
            case 'edit-unlink':
                var unlinkHash = UI.getSelectedClipHash();
                if (unlinkHash) {
                    Project.unlinkAv(unlinkHash);
                } else {
                    UI.notify('Select a linked clip to unlink.', 'error');
                }
                break;
            case 'edit-deselect':
                UI.deselectAll();
                break;
            case 'view-zoom-in':
                TimelineZoom.zoomIn();
                break;
            case 'view-zoom-out':
                TimelineZoom.zoomOut();
                break;
            case 'view-fit':
                TimelineZoom.fitToTimeline();
                break;
            case 'view-snap':
                TimelineSnap.enabled = !TimelineSnap.enabled;
                MenuBar.updateEditState();
                UI.notify(TimelineSnap.enabled ? 'Timeline snap enabled.' : 'Timeline snap disabled.', 'info');
                break;
            case 'view-ripple':
                EditSettings.rippleDelete = !EditSettings.rippleDelete;
                MenuBar.updateEditState();
                UI.notify(EditSettings.rippleDelete ? 'Ripple delete enabled.' : 'Ripple delete disabled.', 'info');
                break;
            case 'help-about':
                window.open('/docs', '_blank', 'noopener,noreferrer');
                break;
        }
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
                return fetch('/api/editor/' + currentProject.id + '/render', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        format: (document.getElementById('exportFormat') || {}).value || 'mp4',
                        quality: (document.getElementById('exportQuality') || {}).value || 'high'
                    })
                });
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
            if (ClipMove.suppressClick) {
                ClipMove.suppressClick = false;
                return;
            }
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
    ClipMove.init();
    Preview.init();
    Export.init();
    ProjectSave.init();
    MenuBar.init();
    ClipTrim.init();
    ContextMenu.init();
    MediaBin.init();
    RecentProjects.track();
    UI.updateTimelineActions();

    var applyTransitionBtn = document.getElementById('applyTransitionBtn');
    if (applyTransitionBtn) {
        applyTransitionBtn.addEventListener('click', function() {
            var hash = UI.getSelectedClipHash();
            if (!hash) {
                UI.notify('Select a clip to apply a transition.', 'error');
                return;
            }
            var value = (document.getElementById('cmb-transition') || {}).value || '';
            Project.applyClipTransition(hash, value);
            UI.notify(value ? 'Transition applied.' : 'Transition cleared.', 'success');
        });
    }
    var applyEffectBtn = document.getElementById('applyEffectBtn');
    if (applyEffectBtn) {
        applyEffectBtn.addEventListener('click', function() {
            var hash = UI.getSelectedClipHash();
            if (!hash) {
                UI.notify('Select a clip to apply an effect.', 'error');
                return;
            }
            var value = (document.getElementById('cmb-effect') || {}).value || '';
            Project.applyClipEffect(hash, value);
            UI.notify(value ? 'Effect applied.' : 'Effect cleared.', 'success');
        });
    }

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
        cutClipBtn.addEventListener('click', function() { Project.cutAtPlayhead(); });
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
        if (document.activeElement && (document.activeElement.tagName === 'INPUT' ||
            document.activeElement.tagName === 'TEXTAREA' || document.activeElement.tagName === 'SELECT')) {
            return;
        }
        if (e.ctrlKey || e.metaKey) {
            if (e.key === 'z' || e.key === 'Z') {
                e.preventDefault();
                if (e.shiftKey) {
                    History.redo();
                } else {
                    History.undo();
                }
                return;
            }
            if (e.key === 'y' || e.key === 'Y') {
                e.preventDefault();
                History.redo();
                return;
            }
            if (e.key === 'c' || e.key === 'C') {
                e.preventDefault();
                Clipboard.copy();
                return;
            }
            if (e.key === 'v' || e.key === 'V') {
                e.preventDefault();
                Clipboard.paste();
                return;
            }
            if (e.key === 'd' || e.key === 'D') {
                e.preventDefault();
                Clipboard.duplicate();
                return;
            }
        }
        if (e.key === 'j' || e.key === 'J' || e.key === 'k' || e.key === 'K' || e.key === 'l' || e.key === 'L') {
            if (Preview.hasLoadedVideo()) {
                e.preventDefault();
                Preview.handleShuttleKey(e.key);
                return;
            }
        }
        if (e.key === 'Delete' || e.key === 'Backspace') {
            var hash = UI.getSelectedClipHash();
            if (hash) {
                e.preventDefault();
                Project.requestDeleteClip(hash);
            }
        } else if (e.key === 's' || e.key === 'S') {
            if (Project.canCutAtPlayhead()) {
                e.preventDefault();
                Project.cutAtPlayhead();
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
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            UI.switchTab(tab.textContent.trim());
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