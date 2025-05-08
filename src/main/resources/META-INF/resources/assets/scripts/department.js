const url = new URL(location.href);
const urlParams = new URLSearchParams(url.search);

const date = document.getElementById('date');
if (urlParams.has('date')) {
    date.value = urlParams.get('date');
} else {
    date.value = new Date().toISOString().split('T')[0];
}
