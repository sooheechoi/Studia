// Sidebar state management across pages
(function() {
    const STORAGE_KEY = 'sidebarState';
    const FIRST_VISIT_KEY = 'hasVisitedBefore';
    const MANUAL_SET_KEY = 'sidebarManuallySet';
    
    console.log('[Sidebar State] Loading sidebar-state.js');
    
    // Check if this is the first visit to the site
    function isFirstSiteVisit() {
        return !localStorage.getItem(FIRST_VISIT_KEY);
    }
    
    // Mark that user has visited the site
    function markSiteVisited() {
        localStorage.setItem(FIRST_VISIT_KEY, 'true');
    }
    
    // Get sidebar state
    function getSidebarState() {
        return localStorage.getItem(STORAGE_KEY);
    }
    
    // Check if sidebar state was manually set
    function wasManuallySet() {
        return localStorage.getItem(MANUAL_SET_KEY) === 'true';
    }
    
    // Apply sidebar state
    function applySidebarState(forceState = null) {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) {
            console.log('[Sidebar State] No sidebar element found');
            return;
        }
        
        const state = forceState || getSidebarState() || 'closed';
        console.log('[Sidebar State] Applying state:', state);
        
        if (state === 'open') {
            sidebar.classList.remove('inactive');
            document.body.classList.remove('sidebar-inactive');
        } else {
            sidebar.classList.add('inactive');
            document.body.classList.add('sidebar-inactive');
        }
    }
    
    // Initialize sidebar on page load
    function initializeSidebar() {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) {
            console.log('[Sidebar State] No sidebar element found during initialization');
            return;
        }
        
        const isIndexPage = window.location.pathname === '/' || 
                           window.location.pathname.endsWith('index.html') ||
                           window.location.pathname === '';
        
        console.log('[Sidebar State] Current page:', window.location.pathname);
        console.log('[Sidebar State] Is index page:', isIndexPage);
        console.log('[Sidebar State] Is first visit:', isFirstSiteVisit());
        console.log('[Sidebar State] Saved state:', getSidebarState());
        
        // 최초 사이트 방문 시
        if (isFirstSiteVisit()) {
            console.log('[Sidebar State] First site visit detected');
            markSiteVisited();
            
            if (isIndexPage) {
                // index 페이지는 열린 상태로 시작
                console.log('[Sidebar State] Opening sidebar for first visit on index page');
                applySidebarState('open');
            } else {
                // 다른 페이지는 닫힌 상태로 시작
                console.log('[Sidebar State] Closing sidebar for first visit on non-index page');
                applySidebarState('closed');
            }
        } else {
            // 재방문 시 저장된 상태 적용
            const savedState = getSidebarState();
            console.log('[Sidebar State] Applying saved state:', savedState);
            applySidebarState(savedState || 'closed');
        }
    }
    
    // Apply state when DOM is ready
    if (document.readyState === 'loading') {
        console.log('[Sidebar State] DOM loading, adding event listener');
        document.addEventListener('DOMContentLoaded', initializeSidebar);
    } else {
        console.log('[Sidebar State] DOM already loaded, initializing immediately');
        initializeSidebar();
    }
    
    // Ensure state is applied after all scripts load
    window.addEventListener('load', function() {
        console.log('[Sidebar State] Window loaded, checking sidebar state again');
        setTimeout(function() {
            const sidebar = document.getElementById('sidebar');
            if (sidebar) {
                // Re-apply the saved state to ensure consistency
                const savedState = getSidebarState();
                if (savedState) {
                    console.log('[Sidebar State] Re-applying saved state:', savedState);
                    applySidebarState(savedState);
                }
                
                // Add transition after initial load
                if (sidebar.style.transition === '') {
                    sidebar.style.transition = 'all 0.5s cubic-bezier(0.4, 0, 0.2, 1)';
                }
            }
        }, 100);
    });
})();