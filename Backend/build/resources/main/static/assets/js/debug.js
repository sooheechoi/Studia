// Debug helper functions
const DEBUG = true;

// Debug logging function
function debugLog(message, data = null) {
    if (!DEBUG) return;
    
    const timestamp = new Date().toISOString();
    console.log(`[DEBUG ${timestamp}] ${message}`);
    if (data) {
        console.log(data);
    }
}

// Check authentication status
function checkAuth() {
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    
    debugLog('Authentication Status:');
    debugLog(`Token present: ${!!token}`);
    debugLog(`User data present: ${!!user}`);
    
    if (token) {
        // Decode JWT token (simple base64 decode, not verification)
        try {
            const parts = token.split('.');
            if (parts.length === 3) {
                const payload = JSON.parse(atob(parts[1]));
                debugLog('Token payload:', payload);
                
                // Check expiration
                if (payload.exp) {
                    const expDate = new Date(payload.exp * 1000);
                    const now = new Date();
                    debugLog(`Token expires at: ${expDate}`);
                    debugLog(`Token is ${now < expDate ? 'valid' : 'expired'}`);
                }
            }
        } catch (e) {
            debugLog('Error decoding token:', e);
        }
    }
    
    return !!token;
}

// Intercept all API calls
if (DEBUG) {
    const originalFetch = window.fetch;
    window.fetch = function(...args) {
        const [url, options = {}] = args;
        
        debugLog(`API Call: ${options.method || 'GET'} ${url}`);
        debugLog('Request options:', options);
        
        return originalFetch.apply(this, args)
            .then(response => {
                debugLog(`Response: ${response.status} ${response.statusText}`);
                
                // Clone response to read it without consuming
                const clonedResponse = response.clone();
                
                if (!response.ok) {
                    clonedResponse.text().then(text => {
                        try {
                            const error = JSON.parse(text);
                            debugLog('Error response:', error);
                        } catch (e) {
                            debugLog('Error response (text):', text);
                        }
                    });
                }
                
                return response;
            })
            .catch(error => {
                debugLog('Fetch error:', error);
                throw error;
            });
    };
}

// Add debug info to page
document.addEventListener('DOMContentLoaded', function() {
    if (DEBUG) {
        const debugDiv = document.createElement('div');
        debugDiv.id = 'debug-info';
        debugDiv.style.cssText = `
            position: fixed;
            bottom: 10px;
            right: 10px;
            background: rgba(0,0,0,0.8);
            color: #00ff00;
            padding: 10px;
            font-family: monospace;
            font-size: 12px;
            z-index: 10000;
            max-width: 300px;
            border-radius: 5px;
        `;
        
        function updateDebugInfo() {
            const isAuth = checkAuth();
            const apiUrl = window.API_BASE_URL || 'Not set';
            
            debugDiv.innerHTML = `
                <strong>Debug Info</strong><br>
                API URL: ${apiUrl}<br>
                Authenticated: ${isAuth ? 'YES' : 'NO'}<br>
                Page: ${window.location.pathname}<br>
                <button onclick="localStorage.clear(); location.reload();" style="margin-top: 5px;">Clear Storage</button>
            `;
        }
        
        updateDebugInfo();
        document.body.appendChild(debugDiv);
        
        // Update every 5 seconds
        setInterval(updateDebugInfo, 5000);
    }
});

// Export for use in other scripts
window.debugLog = debugLog;
window.checkAuth = checkAuth;
