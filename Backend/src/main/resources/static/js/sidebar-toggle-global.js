// Global toggleSidebar function
window.toggleSidebar = function() {
    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;
    
    // Toggle classes
    sidebar.classList.toggle('inactive');
    document.body.classList.toggle('sidebar-inactive');
    
    // Save state to localStorage
    const isOpen = !sidebar.classList.contains('inactive');
    localStorage.setItem('sidebarState', isOpen ? 'open' : 'closed');
    localStorage.setItem('sidebarManuallySet', 'true');
    
    console.log('Sidebar toggled:', isOpen ? 'open' : 'closed');
};