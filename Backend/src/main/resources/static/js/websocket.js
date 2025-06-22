// WebSocket and real-time notifications
let stompClient = null;
let notificationCount = 0;

function connectWebSocket() {
    if (!checkAuth()) return;
    
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('WebSocket Connected: ' + frame);
        
        // Subscribe to user-specific notifications
        const userEmail = TokenManager.getUsername() + '@sju.ac.kr'; // Assuming email format
        stompClient.subscribe('/user/' + userEmail + '/queue/notifications', function(notification) {
            const message = JSON.parse(notification.body);
            showNotification(message);
        });
    }, function(error) {
        console.error('WebSocket connection error:', error);
        // Retry connection after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function showNotification(notification) {
    notificationCount++;
    updateNotificationBadge();
    
    // Create browser notification if permission granted
    if (Notification.permission === 'granted') {
        const browserNotif = new Notification(notification.title, {
            body: notification.message,
            icon: '/assets/images/icon.png',
            badge: '/assets/images/badge.png'
        });
        
        browserNotif.onclick = function() {
            window.open(notification.link || '/', '_blank');
            browserNotif.close();
        };
    }
    
    // Show in-app notification
    const notifElement = createNotificationElement(notification);
    document.body.appendChild(notifElement);
    
    // Auto-hide after 5 seconds
    setTimeout(() => {
        notifElement.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notifElement.remove(), 300);
    }, 5000);
}

function createNotificationElement(notification) {
    const notif = document.createElement('div');
    notif.className = 'notification-toast';
    notif.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        background: white;
        padding: 20px;
        border-radius: 10px;
        box-shadow: 0 5px 20px rgba(0,0,0,0.2);
        max-width: 350px;
        z-index: 10000;
        animation: slideIn 0.3s ease;
        cursor: pointer;
    `;
    
    notif.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: start;">
            <div style="flex: 1;">
                <h4 style="margin: 0 0 5px 0; color: #333;">${notification.title}</h4>
                <p style="margin: 0 0 10px 0; color: #666; font-size: 0.9em;">${notification.message}</p>
                <span style="color: #999; font-size: 0.8em;">${formatTime(notification.timestamp)}</span>
            </div>
            <button onclick="this.parentElement.parentElement.remove()" style="background: none; border: none; color: #999; cursor: pointer; font-size: 1.2em;">×</button>
        </div>
    `;
    
    if (notification.link) {
        notif.onclick = function(e) {
            if (e.target.tagName !== 'BUTTON') {
                window.location.href = notification.link;
            }
        };
    }
    
    return notif;
}

function updateNotificationBadge() {
    const badges = document.querySelectorAll('.notification-badge');
    badges.forEach(badge => {
        badge.textContent = notificationCount;
        badge.style.display = notificationCount > 0 ? 'inline' : 'none';
    });
}

function formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return 'just now';
    if (diff < 3600000) return Math.floor(diff / 60000) + ' minutes ago';
    if (diff < 86400000) return Math.floor(diff / 3600000) + ' hours ago';
    return date.toLocaleDateString();
}

// Add CSS for animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
    .notification-badge {
        position: absolute;
        top: -5px;
        right: -5px;
        background: #f56a6a;
        color: white;
        border-radius: 50%;
        width: 20px;
        height: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.8em;
        font-weight: bold;
    }
`;
document.head.appendChild(style);

// Request notification permission on load
document.addEventListener('DOMContentLoaded', function() {
    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
    
    // Connect WebSocket
    if (checkAuth()) {
        connectWebSocket();
    }
});

// Add notification bell to header
function addNotificationBell() {
    const headers = document.querySelectorAll('#header .actions, #header .icons');
    headers.forEach(header => {
        if (header && !header.querySelector('.notification-bell')) {
            const bellItem = document.createElement('li');
            bellItem.style.position = 'relative';
            bellItem.innerHTML = `
                <a href="#" class="notification-bell" onclick="toggleNotificationPanel(event)" style="position: relative;">
                    <i class="fas fa-bell"></i>
                    <span class="notification-badge" style="display: none;">0</span>
                </a>
            `;
            header.insertBefore(bellItem, header.firstChild);
        }
    });
}

// Initialize notification bell when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', addNotificationBell);
} else {
    addNotificationBell();
}

function toggleNotificationPanel(event) {
    event.preventDefault();
    // TODO: Show notification history panel
    alert('Notification panel coming soon!');
}
