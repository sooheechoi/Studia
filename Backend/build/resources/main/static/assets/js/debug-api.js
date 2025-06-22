// API Debug Script
(function() {
    'use strict';
    
    console.log('===== API Debug Start =====');
    
    // Check if config is loaded
    console.log('1. Config Status:');
    console.log('   - window.StudiaConfig:', typeof window.StudiaConfig !== 'undefined' ? 'Loaded' : 'Not loaded');
    if (window.StudiaConfig) {
        console.log('   - API_BASE_URL:', window.StudiaConfig.API_BASE_URL);
        console.log('   - Storage type:', window.StudiaConfig.storage === localStorage ? 'localStorage' : 'in-memory');
    }
    
    // Check if API modules are loaded
    console.log('\n2. API Module Status:');
    console.log('   - window.API:', typeof window.API !== 'undefined' ? 'Loaded' : 'Not loaded');
    console.log('   - window.apiClient:', typeof window.apiClient !== 'undefined' ? 'Loaded' : 'Not loaded');
    console.log('   - window.TokenManager:', typeof window.TokenManager !== 'undefined' ? 'Loaded' : 'Not loaded');
    console.log('   - window.checkAuth:', typeof window.checkAuth !== 'undefined' ? 'Loaded' : 'Not loaded');
    
    // Test backend connection
    console.log('\n3. Testing Backend Connection...');
    
    const testBackendConnection = async () => {
        const apiUrl = window.StudiaConfig ? window.StudiaConfig.API_BASE_URL : 'http://localhost:8080/api';
        
        try {
            // Test basic connection
            console.log(`   - Testing connection to: ${apiUrl}/health`);
            const response = await fetch(`${apiUrl}/health`, {
                method: 'GET',
                mode: 'cors',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            
            if (response.ok) {
                console.log('   ✓ Backend is reachable');
                const text = await response.text();
                console.log('   - Response:', text);
            } else {
                console.error('   ✗ Backend returned error:', response.status, response.statusText);
            }
        } catch (error) {
            console.error('   ✗ Failed to connect to backend:', error.message);
            console.error('   - Error details:', error);
            
            // Try alternative endpoints
            console.log('\n   - Trying alternative test endpoint...');
            try {
                const testResponse = await fetch(`${apiUrl}/auth/test`, { method: 'GET', mode: 'cors' });
                console.log('   - Test endpoint status:', testResponse.status);
            } catch (testError) {
                console.error('   - Test endpoint also failed:', testError.message);
            }
        }
        
        // Check if it's a CORS issue
        console.log('\n4. CORS Check:');
        console.log('   - If you see CORS errors in the console, the backend server may not be running');
        console.log('   - Or CORS is not properly configured on the backend');
        
        // Display current auth state
        console.log('\n5. Current Auth State:');
        if (window.TokenManager) {
            console.log('   - Token:', window.TokenManager.getToken() ? 'Present' : 'Not present');
            console.log('   - Username:', window.TokenManager.getUsername() || 'Not set');
        } else {
            console.log('   - TokenManager not loaded');
        }
        
        console.log('\n===== API Debug End =====');
    };
    
    // Run tests after a short delay to ensure everything is loaded
    setTimeout(testBackendConnection, 1000);
    
    // Export debug function for manual testing
    window.debugAPI = {
        testConnection: testBackendConnection,
        checkStatus: () => {
            console.log('API Status:', {
                config: !!window.StudiaConfig,
                api: !!window.API,
                apiClient: !!window.apiClient,
                tokenManager: !!window.TokenManager,
                checkAuth: !!window.checkAuth
            });
        }
    };
})();
