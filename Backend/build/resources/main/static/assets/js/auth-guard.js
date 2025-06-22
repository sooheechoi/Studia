// Authentication Guard for Protected Pages
(function() {
    'use strict';
    
    console.log('[Auth Guard] Initializing...');
    
    // List of pages that don't require authentication
    const publicPages = ['index.html', 'login.html', 'signup.html', ''];
    
    // Function to check if current page requires authentication
    function requiresAuth() {
        const currentPage = window.location.pathname.split('/').pop();
        return !publicPages.includes(currentPage);
    }
    
    // Function to redirect to login
    function redirectToLogin() {
        console.log('[Auth Guard] Redirecting to login...');
        // Store the intended destination
        sessionStorage.setItem('redirectUrl', window.location.href);
        window.location.href = '/index.html';
    }
    
    // Main auth check function
    async function checkAuthentication() {
        // Skip check for public pages
        if (!requiresAuth()) {
            console.log('[Auth Guard] Public page, skipping auth check');
            return;
        }
        
        console.log('[Auth Guard] Checking authentication...');
        
        // Wait for API to load
        let attempts = 0;
        const maxAttempts = 30; // 3 seconds
        
        while ((!window.checkAuth || !window.TokenManager) && attempts < maxAttempts) {
            await new Promise(resolve => setTimeout(resolve, 100));
            attempts++;
        }
        
        if (attempts >= maxAttempts) {
            console.error('[Auth Guard] API failed to load, redirecting to login');
            redirectToLogin();
            return;
        }
        
        // Check if user is authenticated
        if (!window.checkAuth()) {
            console.log('[Auth Guard] User not authenticated');
            alert('Please login to access this page');
            redirectToLogin();
            return;
        }
        
        console.log('[Auth Guard] User authenticated');
        
        // Update UI with user info
        updateUserInterface();
    }
    
    // Function to update UI with user info
    function updateUserInterface() {
        if (!window.TokenManager) return;
        
        const username = window.TokenManager.getUsername();
        const headerElement = document.querySelector('#header .actions');
        
        if (headerElement && username) {
            headerElement.innerHTML = `
                <li><span style="margin-right: 1em;">Welcome, ${username}!</span></li>
                <li><button class="button" onclick="logout()">Log Out</button></li>
            `;
        }
    }
    
    // Global logout function
    window.logout = function() {
        console.log('[Auth Guard] Logging out...');
        
        if (window.API && window.API.auth) {
            window.API.auth.logout();
        } else {
            // Fallback logout
            if (window.TokenManager) {
                window.TokenManager.removeToken();
                window.TokenManager.removeUsername();
                window.TokenManager.removeUserId();
            }
            localStorage.clear();
            sessionStorage.clear();
            window.location.href = '/index.html';
        }
    };
    
    // Run authentication check when DOM is loaded
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', checkAuthentication);
    } else {
        checkAuthentication();
    }
    
    // Export functions for external use
    window.authGuard = {
        check: checkAuthentication,
        requiresAuth: requiresAuth,
        updateUI: updateUserInterface
    };
})();
