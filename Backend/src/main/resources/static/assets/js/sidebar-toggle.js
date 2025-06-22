// Enhanced sidebar toggle functionality
document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.getElementById('sidebar');
    const main = document.getElementById('main');
    let toggle = sidebar ? sidebar.querySelector('.toggle') : null;
    
    // Create toggle button if it doesn't exist
    if (sidebar && !toggle) {
        toggle = document.createElement('a');
        toggle.href = '#sidebar';
        toggle.className = 'toggle';
        toggle.innerHTML = 'Toggle';
        sidebar.appendChild(toggle);
    }
    
    if (toggle) {
        // Ensure toggle is always visible
        toggle.style.cssText = `
            display: flex !important;
            visibility: visible !important;
            opacity: 1 !important;
            position: fixed !important;
            top: 30px !important;
            left: 30px !important;
            z-index: 10001 !important;
        `;
        
        // Remove any existing click handlers
        const newToggle = toggle.cloneNode(true);
        toggle.parentNode.replaceChild(newToggle, toggle);
        toggle = newToggle;
        
        // Add enhanced click handler
        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            // Toggle sidebar
            sidebar.classList.toggle('inactive');
            
            // Toggle body class
            if (sidebar.classList.contains('inactive')) {
                document.body.classList.add('sidebar-inactive');
            } else {
                document.body.classList.remove('sidebar-inactive');
            }
            
            // Animate main content
            if (main) {
                if (sidebar.classList.contains('inactive')) {
                    main.style.transition = 'margin-left 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                    main.style.marginLeft = '0';
                } else {
                    main.style.transition = 'margin-left 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                    if (window.innerWidth > 1280) {
                        main.style.marginLeft = '26em';
                    } else {
                        main.style.marginLeft = '0';
                    }
                }
            }
        });
    }
    
    // Set initial state based on screen size
    if (window.innerWidth <= 1280) {
        sidebar.classList.add('inactive');
        document.body.classList.add('sidebar-inactive');
        if (main) main.style.marginLeft = '0';
    } else {
        // On larger screens, sidebar is open by default
        sidebar.classList.remove('inactive');
        document.body.classList.remove('sidebar-inactive');
        if (main) main.style.marginLeft = '26em';
    }
    
    // Handle window resize
    let resizeTimeout;
    window.addEventListener('resize', function() {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(function() {
            if (window.innerWidth <= 1280) {
                sidebar.classList.add('inactive');
                document.body.classList.add('sidebar-inactive');
                if (main) main.style.marginLeft = '0';
            } else if (!sidebar.classList.contains('inactive')) {
                document.body.classList.remove('sidebar-inactive');
                if (main) main.style.marginLeft = '26em';
            }
        }, 250);
    });
    
    // Prevent sidebar from closing on internal clicks
    sidebar.addEventListener('click', function(e) {
        if (window.innerWidth <= 1280) {
            e.stopPropagation();
        }
    });
    
    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', function(e) {
        if (window.innerWidth <= 1280 && !sidebar.classList.contains('inactive')) {
            if (!sidebar.contains(e.target) && e.target !== toggle) {
                sidebar.classList.add('inactive');
                document.body.classList.add('sidebar-inactive');
                if (main) main.style.marginLeft = '0';
            }
        }
    });
});
