document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.epoch-milli').forEach(elm => {
        console.log(elm);
        let content = elm.textContent.trim();
        if (content.match(/^[0-9]+$/)) {
            const date = new Date(parseInt(content));
            elm.textContent = `${date.toLocaleDateString()} - ${date.toLocaleTimeString()}`;
        }
    })
});