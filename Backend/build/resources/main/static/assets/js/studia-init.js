// Studia Application Initialization Module
console.log('[studia-init.js] Loading...');

window.StudiaInit = (function() {
    'use strict';
    
    // Private variables
    let initialized = false;
    
    // Public methods
    return {
        // Initialize the application
        initialize: async function(options = {}) {
            console.log('[StudiaInit] initialize called with options:', options);
            
            if (initialized && !options.force) {
                console.log('[StudiaInit] Already initialized');
                return true;
            }
            
            try {
                console.log('[StudiaInit] Waiting for API to be ready...');
                
                // Wait for API to be ready
                if (window.waitForStudia) {
                    await window.waitForStudia();
                } else if (window.StudiaReady) {
                    await window.StudiaReady;
                } else {
                    // Fallback: wait for API manually
                    console.log('[StudiaInit] Using fallback API wait');
                    let attempts = 0;
                    while (!window.API && attempts < 50) {
                        await new Promise(resolve => setTimeout(resolve, 100));
                        attempts++;
                    }
                    if (!window.API) {
                        throw new Error('API failed to load after 5 seconds');
                    }
                }
                
                console.log('[StudiaInit] API loaded, checking components:');
                console.log('- window.API:', !!window.API);
                console.log('- window.checkAuth:', !!window.checkAuth);
                console.log('- window.TokenManager:', !!window.TokenManager);
                
                // Ensure checkAuth is available
                if (!window.checkAuth) {
                    console.warn('[StudiaInit] checkAuth not found, creating fallback');
                    window.checkAuth = () => {
                        const token = localStorage.getItem('token') || 
                                     sessionStorage.getItem('token') ||
                                     (window.TokenManager && window.TokenManager.getToken());
                        return !!token;
                    };
                }
                
                // Check authentication for non-public pages
                const publicPages = ['/', '/index.html', 'index.html'];
                const currentPage = window.location.pathname;
                const isPublicPage = publicPages.some(page => 
                    currentPage === page || currentPage.endsWith(page)
                );
                
                console.log('[StudiaInit] Current page:', currentPage);
                console.log('[StudiaInit] Is public page:', isPublicPage);
                console.log('[StudiaInit] Auth check result:', window.checkAuth());
                
                if (!isPublicPage && !options.allowUnauthenticated) {
                    if (!window.checkAuth()) {
                        console.log('[StudiaInit] User not authenticated, redirecting to login...');
                        alert('Please login first to access this page.');
                        window.location.href = '/index.html';
                        return false;
                    }
                }
                
                // Update header with user info
                this.updateHeader();
                
                // Set sidebar state
                this.initializeSidebar();
                
                initialized = true;
                console.log('[StudiaInit] Initialization complete');
                return true;
                
            } catch (error) {
                console.error('[StudiaInit] Failed to initialize:', error);
                alert('Failed to initialize application. Please refresh the page.');
                return false;
            }
        },
        
        // Update header with user information
        updateHeader: function() {
            console.log('[StudiaInit] Updating header...');
            
            const headerActions = document.getElementById('headerActions') || 
                                document.querySelector('#header .actions') ||
                                document.querySelector('.actions');
                                
            if (headerActions && window.TokenManager) {
                const username = window.TokenManager.getUsername() || 'User';
                console.log('[StudiaInit] Setting header for user:', username);
                
                headerActions.innerHTML = `
                    <li><span style="margin-right: 1em;">Welcome, ${username}!</span></li>
                    <li><button class="button" onclick="StudiaInit.logout()">Log Out</button></li>
                `;
            }
        },
        
        // Initialize sidebar state
        initializeSidebar: function() {
            const sidebar = document.getElementById('sidebar');
            if (sidebar) {
                // Always start with sidebar closed on non-index pages
                const isIndexPage = window.location.pathname === '/' || 
                                  window.location.pathname.endsWith('index.html');
                
                if (!isIndexPage) {
                    sidebar.classList.add('inactive');
                    document.body.classList.add('sidebar-inactive');
                }
            }
        },
        
        // Logout function
        logout: function() {
            console.log('[StudiaInit] Logout called');
            
            if (window.API && window.API.auth && window.API.auth.logout) {
                window.API.auth.logout();
            } else {
                // Fallback logout
                console.log('[StudiaInit] Using fallback logout');
                localStorage.clear();
                sessionStorage.clear();
                if (window.StudiaConfig && window.StudiaConfig.storage) {
                    window.StudiaConfig.storage.clear();
                }
                window.location.href = '/index.html';
            }
        },
        
        // Check if initialized
        isInitialized: function() {
            return initialized;
        },
        
        // Reset initialization (for testing)
        reset: function() {
            initialized = false;
        }
    };
})();

// Auto-initialize on DOMContentLoaded for authenticated pages
document.addEventListener('DOMContentLoaded', async function() {
    console.log('[studia-init.js] DOMContentLoaded');
    
    // Don't auto-initialize on index page
    const isIndexPage = window.location.pathname === '/' || 
                       window.location.pathname.endsWith('index.html');
    
    console.log('[studia-init.js] Is index page:', isIndexPage);
    
    if (!isIndexPage) {
        // Wait a bit for all scripts to load
        setTimeout(async () => {
            console.log('[studia-init.js] Auto-initializing for authenticated page...');
            const result = await window.StudiaInit.initialize();
            console.log('[studia-init.js] Auto-initialization result:', result);
        }, 100);
    }
});

console.log('[studia-init.js] Module loaded');
