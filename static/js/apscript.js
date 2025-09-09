function confirmation(location) {
    result = confirm('Are you sure you want to delete this?');
    if(result) {
        window.location = location;
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const nodesWithTimestamp = document.querySelectorAll('[data-timestamp]');
    const nodesArray = Array.from(nodesWithTimestamp);

    nodesArray.forEach(node => {
        let format;
        if (node.getAttribute('data-timestamp-format') == 'date') { format = false; } else { format = true; }
        node.innerHTML = timeUntil(node.getAttribute('data-timestamp'), format);
    });
});

function timeUntil(dateInput, unix) {
    let inputDate;
    if (unix) {
        inputDate = new Date(dateInput * 1000);
    } else {
        inputDate = new Date(dateInput.replace(' ', 'T') + 'Z');
    }

    const currentDate = new Date();
    const differenceInSeconds = Math.floor((inputDate - currentDate) / 1000);

    const isPast = differenceInSeconds < 0;
    const absDifferenceInSeconds = Math.abs(differenceInSeconds);

    const timeUnits = [
        { unit: 'year', seconds: 29030400 },
        { unit: 'month', seconds: 2419200 },
        { unit: 'week', seconds: 604800 },
        { unit: 'day', seconds: 86400 },
        { unit: 'hour', seconds: 3600 },
        { unit: 'minute', seconds: 60 },
        { unit: 'second', seconds: 1 }
    ];

    let result = '';

    for (const { unit, seconds } of timeUnits) {
        const value = Math.floor(absDifferenceInSeconds / seconds);
        if (value > 0) {
            result += `${value} ${unit}${value > 1 ? 's' : ''} `;
            break;
        }
    }

    return isPast ? result.trim() + ' ago' : result.trim() + ' from now';
}

function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    const eyeIcon = document.getElementById('eye-' + inputId);
    
    if (input.type === 'password') {
        input.type = 'text';
        eyeIcon.classList.remove('fa-eye');
        eyeIcon.classList.add('fa-eye-slash');
    } else {
        input.type = 'password';
        eyeIcon.classList.remove('fa-eye-slash');
        eyeIcon.classList.add('fa-eye');
    }
}

function copyToClipboard(inputId) {
    const input = document.getElementById(inputId);
    const value = input.value;
    
    // Create a temporary textarea element to copy the value
    const tempTextarea = document.createElement('textarea');
    tempTextarea.value = value;
    document.body.appendChild(tempTextarea);
    tempTextarea.select();
    tempTextarea.setSelectionRange(0, 99999); // For mobile devices
    
    try {
        document.execCommand('copy');
        // Show a brief success indication
        showCopySuccess(inputId);
    } catch (err) {
        console.error('Failed to copy: ', err);
        // Fallback for modern browsers
        if (navigator.clipboard) {
            navigator.clipboard.writeText(value).then(() => {
                showCopySuccess(inputId);
            }).catch(err => {
                console.error('Failed to copy: ', err);
            });
        }
    }
    
    document.body.removeChild(tempTextarea);
}

function showCopySuccess(inputId) {
    // Find the copy button for this input
    const input = document.getElementById(inputId);
    const copyButton = input.parentNode.querySelector('button[onclick*="copyToClipboard"]');
    const icon = copyButton.querySelector('i');
    
    // Temporarily change the icon to show success
    const originalClasses = icon.className;
    icon.className = 'fa-solid fa-check text-success';
    
    // Reset after 1.5 seconds
    setTimeout(() => {
        icon.className = originalClasses;
    }, 1500);
}