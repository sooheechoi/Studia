// Initialize Studia Application
console.log('[init.js] Starting initialization...');

(function() {
    'use strict';
    
    // Ensure global objects are available
    window.StudiaConfig = window.StudiaConfig || {
        API_BASE_URL: window.location.hostname === 'localhost' 
            ? 'http://localhost:8080/api' 
            : '/api',
        storage: localStorage || {
            data: {},
            getItem: function(key) { return this.data[key] || null; },
            setItem: function(key, value) { this.data[key] = value; },
            removeItem: function(key) { delete this.data[key]; },
            clear: function() { this.data = {}; }
        }
    };
    
    console.log('[init.js] StudiaConfig set:', window.StudiaConfig);
    
    // Check if API is already loaded
    if (window.API && window.apiClient && window.TokenManager && window.checkAuth) {
        console.log('[init.js] Studia API already loaded');
        return;
    }
    
    // Create a promise that resolves when API is loaded
    window.StudiaReady = new Promise((resolve) => {
        const checkAPI = () => {
            console.log('[init.js] Checking for API...');
            console.log('- window.API:', !!window.API);
            console.log('- window.apiClient:', !!window.apiClient);
            console.log('- window.TokenManager:', !!window.TokenManager);
            console.log('- window.checkAuth:', !!window.checkAuth);
            
            if (window.API && window.apiClient && window.TokenManager && window.checkAuth) {
                console.log('[init.js] All API components loaded successfully');
                resolve();
                return true;
            }
            return false;
        };
        
        // Check immediately
        if (checkAPI()) return;
        
        // Check periodically
        let attempts = 0;
        const maxAttempts = 50; // 5 seconds
        const interval = setInterval(() => {
            attempts++;
            console.log(`[init.js] Checking for API... attempt ${attempts}/${maxAttempts}`);
            
            if (checkAPI() || attempts >= maxAttempts) {
                clearInterval(interval);
                if (attempts >= maxAttempts) {
                    console.error('[init.js] Failed to load Studia API after 5 seconds');
                    console.error('API components status:');
                    console.error('- window.API:', window.API);
                    console.error('- window.apiClient:', window.apiClient);
                    console.error('- window.TokenManager:', window.TokenManager);
                    console.error('- window.checkAuth:', window.checkAuth);
                    
                    // Provide minimal fallback
                    if (!window.checkAuth) {
                        window.checkAuth = () => {
                            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
                            return !!token;
                        };
                    }
                    resolve();
                }
            }
        }, 100);
    });
    
    // Global error handler
    window.addEventListener('error', (event) => {
        if (event.error && event.error.message && event.error.message.includes('API')) {
            console.error('[init.js] API-related error:', event.error);
        }
    });
    
    // Provide global wait function
    window.waitForStudia = async function() {
        console.log('[init.js] waitForStudia called');
        await window.StudiaReady;
        console.log('[init.js] StudiaReady resolved');
        return window.API;
    };
    
    console.log('[init.js] Studia initialization script loaded');
})();
