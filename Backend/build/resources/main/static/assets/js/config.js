// Configuration for Studia Application
const CONFIG = {
    // Environment detection
    isProduction: window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1',
    
    // API Configuration
    API_BASE_URL: (() => {
        // Development environment
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            return 'http://localhost:8080/api';
        }
        
        // Production environment - use relative URL
        return '/api';
    })(),
    
    // WebSocket Configuration
    WS_URL: (() => {
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            return 'ws://localhost:8080/ws';
        }
        
        // Production - use wss:// for secure WebSocket
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        return `${protocol}//${window.location.host}/ws`;
    })(),
    
    // Storage Configuration - In-memory storage for Claude.ai compatibility
    storage: {
        // In-memory storage object
        data: {},
        
        getItem: function(key) {
            return this.data[key] || null;
        },
        
        setItem: function(key, value) {
            this.data[key] = value;
        },
        
        removeItem: function(key) {
            delete this.data[key];
        },
        
        clear: function() {
            this.data = {};
        }
    },
    
    // Cache Configuration
    CACHE_DURATION: 5 * 60 * 1000, // 5 minutes
    QUIZ_CACHE_DURATION: 10 * 60 * 1000, // 10 minutes
    
    // Retry Configuration
    MAX_RETRIES: 3,
    RETRY_DELAY: 1000, // 1 second
    
    // File Upload Configuration
    MAX_FILE_SIZE: 50 * 1024 * 1024, // 50MB
    ALLOWED_FILE_TYPES: [
        '.pdf', '.doc', '.docx', '.txt', '.ppt', '.pptx',
        '.xls', '.xlsx', '.jpg', '.jpeg', '.png', '.gif'
    ],
    
    // UI Configuration
    TOAST_DURATION: 5000, // 5 seconds
    DEBOUNCE_DELAY: 300, // 300ms
    
    // Feature Flags
    features: {
        enableWebSocket: true,
        enableCache: true,
        enableOfflineMode: false,
        enableDebugMode: window.location.hostname === 'localhost'
    }
};

// Fallback to localStorage if available, otherwise use in-memory storage
if (typeof(Storage) !== "undefined") {
    try {
        // Test localStorage availability
        localStorage.setItem('test', 'test');
        localStorage.removeItem('test');
        
        // If no error, use localStorage
        CONFIG.storage = {
            getItem: (key) => localStorage.getItem(key),
            setItem: (key, value) => localStorage.setItem(key, value),
            removeItem: (key) => localStorage.removeItem(key),
            clear: () => localStorage.clear()
        };
    } catch (e) {
        // localStorage not available, use in-memory storage
        console.warn('localStorage not available, using in-memory storage');
    }
}

// Debug logging
if (CONFIG.features.enableDebugMode) {
    console.log('Studia Configuration:', {
        API_BASE_URL: CONFIG.API_BASE_URL,
        WS_URL: CONFIG.WS_URL,
        isProduction: CONFIG.isProduction,
        storageType: CONFIG.storage === localStorage ? 'localStorage' : 'in-memory'
    });
}

// Export configuration
window.StudiaConfig = CONFIG;
