// Global Error Handler for Studia Application

// Error notification component
class ErrorNotification {
    constructor() {
        this.container = document.createElement('div');
        this.container.id = 'error-notification-container';
        this.container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10000;
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-width: 400px;
        `;
        document.body.appendChild(this.container);
    }
    
    show(message, type = 'error', duration = 5000) {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.style.cssText = `
            background: ${type === 'error' ? '#f44336' : type === 'warning' ? '#ff9800' : '#4caf50'};
            color: white;
            padding: 15px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            margin-bottom: 10px;
            animation: slideIn 0.3s ease;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 10px;
        `;
        
        // Add icon
        const icon = document.createElement('i');
        icon.className = type === 'error' ? 'fas fa-exclamation-circle' : 
                       type === 'warning' ? 'fas fa-exclamation-triangle' : 
                       'fas fa-check-circle';
        
        // Add message
        const messageEl = document.createElement('span');
        messageEl.textContent = message;
        
        notification.appendChild(icon);
        notification.appendChild(messageEl);
        
        // Click to dismiss
        notification.addEventListener('click', () => {
            this.dismiss(notification);
        });
        
        this.container.appendChild(notification);
        
        // Auto dismiss
        if (duration > 0) {
            setTimeout(() => {
                this.dismiss(notification);
            }, duration);
        }
        
        return notification;
    }
    
    dismiss(notification) {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            notification.remove();
        }, 300);
    }
}

// Create global instance
const errorNotifier = new ErrorNotification();

// Add animation styles
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Global error handlers
window.addEventListener('error', (event) => {
    console.error('Global error:', event.error);
    
    // Check if it's a network error
    if (event.error && event.error.message && event.error.message.includes('fetch')) {
        errorNotifier.show('Network connection error. Please check your internet connection.', 'error');
    }
});

window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    
    // Handle specific error types
    if (event.reason && event.reason.message) {
        const message = event.reason.message;
        
        // Network errors
        if (message.includes('Network') || message.includes('fetch')) {
            errorNotifier.show('Connection error. Please check your network and try again.', 'error');
        } 
        // Authentication errors
        else if (message.includes('401') || message.includes('Unauthorized')) {
            errorNotifier.show('Session expired. Please login again.', 'warning');
            setTimeout(() => {
                window.location.href = '/';
            }, 2000);
        }
        // Server errors
        else if (message.includes('500') || message.includes('Internal Server Error')) {
            errorNotifier.show('Server error. Please try again later.', 'error');
        }
        // Generic errors
        else {
            errorNotifier.show(message, 'error');
        }
    }
});

// Network status monitoring
let isOnline = navigator.onLine;

window.addEventListener('online', () => {
    if (!isOnline) {
        isOnline = true;
        errorNotifier.show('Connection restored', 'success', 3000);
    }
});

window.addEventListener('offline', () => {
    isOnline = false;
    errorNotifier.show('No internet connection. Some features may not work.', 'warning', 0);
});

// Utility functions
window.showError = function(message, duration = 5000) {
    return errorNotifier.show(message, 'error', duration);
};

window.showWarning = function(message, duration = 5000) {
    return errorNotifier.show(message, 'warning', duration);
};

window.showSuccess = function(message, duration = 3000) {
    return errorNotifier.show(message, 'success', duration);
};

window.showNotification = function(message, type = 'success', duration = 5000) {
    return errorNotifier.show(message, type, duration);
};

// Override console.error to also show notifications for critical errors
const originalError = console.error;
console.error = function(...args) {
    originalError.apply(console, args);
    
    // Check if this is a critical error that should be shown to the user
    const errorStr = args.join(' ');
    if (errorStr.includes('CRITICAL') || errorStr.includes('FATAL')) {
        errorNotifier.show('A critical error occurred. Please refresh the page.', 'error');
    }
};

// Loading indicator
class LoadingIndicator {
    constructor() {
        this.loadingCount = 0;
        this.element = null;
    }
    
    show(message = 'Loading...') {
        this.loadingCount++;
        
        if (!this.element) {
            this.element = document.createElement('div');
            this.element.style.cssText = `
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: rgba(0, 0, 0, 0.8);
                color: white;
                padding: 20px 40px;
                border-radius: 10px;
                display: flex;
                align-items: center;
                gap: 15px;
                z-index: 10000;
            `;
            
            this.element.innerHTML = `
                <div class="spinner" style="
                    width: 30px;
                    height: 30px;
                    border: 3px solid rgba(255,255,255,0.3);
                    border-radius: 50%;
                    border-top-color: white;
                    animation: spin 1s ease-in-out infinite;
                "></div>
                <span>${message}</span>
            `;
            
            document.body.appendChild(this.element);
        }
    }
    
    hide() {
        this.loadingCount--;
        
        if (this.loadingCount <= 0 && this.element) {
            this.element.remove();
            this.element = null;
            this.loadingCount = 0;
        }
    }
}

const loadingIndicator = new LoadingIndicator();

window.showLoading = function(message) {
    loadingIndicator.show(message);
};

window.hideLoading = function() {
    loadingIndicator.hide();
};

// Add spinner animation
const spinnerStyle = document.createElement('style');
spinnerStyle.textContent = `
    @keyframes spin {
        to { transform: rotate(360deg); }
    }
`;
document.head.appendChild(spinnerStyle);

// Export for use in other modules
window.ErrorHandler = {
    showError,
    showWarning,
    showSuccess,
    showNotification,
    showLoading,
    hideLoading
};
