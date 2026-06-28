var RECENT_STORAGE_KEY = 'videoCreatorRecent';
var MAX_RECENT = 8;

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.epoch-milli').forEach(function (elm) {
        var content = elm.textContent.trim();
        if (/^[0-9]+$/.test(content)) {
            var date = new Date(parseInt(content, 10));
            elm.textContent = date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    });

    renderRecentProjects();

    document.querySelectorAll('.project-link, .col-action a[href^="/editor/"]').forEach(function (link) {
        link.addEventListener('click', function () {
            var row = link.closest('tr');
            var id = row ? row.getAttribute('data-project-id') : null;
            var name = row ? row.getAttribute('data-project-name') : link.textContent.trim();
            if (!id) {
                var match = link.getAttribute('href').match(/\/editor\/([^/?#]+)/);
                id = match ? match[1] : null;
            }
            if (id) {
                trackRecentProject(id, name || 'Untitled project');
            }
        });
    });

    var searchInput = document.getElementById('projectSearch');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            var query = searchInput.value.trim().toLowerCase();
            document.querySelectorAll('.project-table tbody tr').forEach(function (row) {
                var name = (row.getAttribute('data-project-name') || '').toLowerCase();
                row.style.display = !query || name.indexOf(query) !== -1 ? '' : 'none';
            });
        });
    }

    document.querySelectorAll('.btn-delete-project').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var projectId = btn.getAttribute('data-project-id');
            var row = btn.closest('tr');
            var name = row ? row.getAttribute('data-project-name') : 'this project';
            if (!projectId || !confirm('Delete "' + name + '"? This cannot be undone.')) {
                return;
            }
            btn.disabled = true;
            fetch('/api/editor/' + projectId, { method: 'DELETE' })
                .then(function (response) { return response.json(); })
                .then(function () {
                    removeRecentProject(projectId);
                    if (row && row.parentElement) {
                        row.parentElement.removeChild(row);
                    }
                    var tbody = document.querySelector('.project-table tbody');
                    if (tbody && tbody.children.length === 0) {
                        window.location.reload();
                    }
                })
                .catch(function () {
                    btn.disabled = false;
                    alert('Failed to delete project.');
                });
        });
    });

    document.querySelectorAll('.btn-rename-project').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var projectId = btn.getAttribute('data-project-id');
            var row = btn.closest('tr');
            if (!projectId || !row) {
                return;
            }
            var currentName = row.getAttribute('data-project-name') || '';
            var newName = prompt('Rename project:', currentName);
            if (newName === null || newName.trim() === '' || newName.trim() === currentName) {
                return;
            }
            fetch('/api/projects/' + projectId, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: newName.trim() })
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Rename failed');
                    }
                    return response.json();
                })
                .then(function (data) {
                    var label = data.name || newName.trim();
                    row.setAttribute('data-project-name', label);
                    var link = row.querySelector('.project-link');
                    if (link) {
                        link.textContent = label;
                    }
                    updateRecentProjectName(projectId, label);
                })
                .catch(function () {
                    alert('Failed to rename project.');
                });
        });
    });

    fetch('/api/video/health')
        .then(function (response) { return response.json(); })
        .then(function (data) {
            var ok = data.status === 'OK';
            updateStatusRow('status-mlt', ok, ok ? 'OK' : 'Unavailable');
            updateStatusRow('status-engine', ok, ok ? 'Ready' : 'Not ready');
        })
        .catch(function () {
            updateStatusRow('status-mlt', false, 'Unavailable');
            updateStatusRow('status-engine', false, 'Not ready');
        });
});

function loadRecentProjects() {
    try {
        return JSON.parse(localStorage.getItem(RECENT_STORAGE_KEY) || '[]');
    } catch (err) {
        return [];
    }
}

function saveRecentProjects(list) {
    localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(list.slice(0, MAX_RECENT)));
}

function trackRecentProject(id, name) {
    var list = loadRecentProjects().filter(function (p) { return p.id !== id; });
    list.unshift({ id: id, name: name || 'Untitled project', openedAt: Date.now() });
    saveRecentProjects(list);
}

function removeRecentProject(id) {
    saveRecentProjects(loadRecentProjects().filter(function (p) { return p.id !== id; }));
}

function updateRecentProjectName(id, name) {
    var list = loadRecentProjects();
    list.forEach(function (p) {
        if (p.id === id) {
            p.name = name;
        }
    });
    saveRecentProjects(list);
    renderRecentProjects();
}

function renderRecentProjects() {
    var listEl = document.getElementById('recentProjectsList');
    var emptyEl = document.getElementById('recentProjectsEmpty');
    var section = document.getElementById('recentProjectsSection');
    if (!listEl) {
        return;
    }
    var list = loadRecentProjects();
    listEl.innerHTML = '';
    if (list.length === 0) {
        if (emptyEl) {
            emptyEl.style.display = '';
        }
        if (section) {
            section.classList.add('recent-panel--empty');
        }
        return;
    }
    if (emptyEl) {
        emptyEl.style.display = 'none';
    }
    if (section) {
        section.classList.remove('recent-panel--empty');
    }
    list.forEach(function (project) {
        var li = document.createElement('li');
        li.className = 'recent-project-item';
        var link = document.createElement('a');
        link.href = '/editor/' + project.id;
        link.className = 'recent-project-link';
        link.textContent = project.name || 'Untitled project';
        link.addEventListener('click', function () {
            trackRecentProject(project.id, project.name);
        });
        var time = document.createElement('span');
        time.className = 'recent-project-time';
        time.textContent = project.openedAt
            ? new Date(project.openedAt).toLocaleString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' })
            : '';
        li.appendChild(link);
        li.appendChild(time);
        listEl.appendChild(li);
    });
}

function updateStatusRow(id, isOk, label) {
    var row = document.getElementById(id);
    if (!row) {
        return;
    }
    row.classList.remove('status-row--ok', 'status-row--error');
    row.classList.add(isOk ? 'status-row--ok' : 'status-row--error');
    var value = row.querySelector('.status-row-value');
    if (value) {
        value.textContent = label;
    }
}
