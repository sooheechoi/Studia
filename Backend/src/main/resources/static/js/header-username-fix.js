/**
 * Global username display fix
 * Ensures username is properly displayed in header across all pages
 */

(function() {
    'use strict';
    
    // Function to update username display
    function updateUsernameDisplay() {
        const welcomeElement = document.getElementById('userWelcome');
        if (!welcomeElement) return;
        
        // Check if user is authenticated
        if (typeof checkAuth === 'function' && checkAuth()) {
            // Get username from TokenManager
            let username = null;
            
            if (typeof TokenManager !== 'undefined' && TokenManager.getUsername) {
                username = TokenManager.getUsername();
            }
            
            // Fallback: try to get from localStorage
            if (!username) {
                try {
                    const token = localStorage.getItem('access_token');
                    if (token) {
                        const payload = JSON.parse(atob(token.split('.')[1]));
                        username = payload.username || payload.sub || payload.name;
                    }
                } catch (e) {
                    console.log('Could not parse token for username');
                }
            }
            
            // Update welcome message
            welcomeElement.textContent = `Welcome, ${username || 'User'}!`;
        } else {
            // Not authenticated - this shouldn't happen on protected pages
            welcomeElement.textContent = 'Welcome!';
        }
    }
    
    // Run on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', updateUsernameDisplay);
    } else {
        // DOM already loaded
        updateUsernameDisplay();
    }
    
    // Also run after a short delay to ensure all scripts are loaded
    setTimeout(updateUsernameDisplay, 100);
    
    // Run again after auth-related events
    window.addEventListener('auth-success', updateUsernameDisplay);
    window.addEventListener('storage', function(e) {
        if (e.key === 'access_token' || e.key === 'username') {
            updateUsernameDisplay();
        }
    });
})();