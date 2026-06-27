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
