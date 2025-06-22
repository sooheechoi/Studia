// 모든 페이지에서 username을 올바르게 표시하는 스크립트
document.addEventListener('DOMContentLoaded', function() {
    console.log('[Header Username] Script loaded');
    
    // username 가져오기 - 다양한 방법 시도
    function getUsername() {
        // 1. TokenManager 사용 (가장 신뢰할 수 있는 방법)
        if (window.TokenManager && typeof window.TokenManager.getUsername === 'function') {
            const username = window.TokenManager.getUsername();
            console.log('[Header Username] Got username from TokenManager:', username);
            if (username && username !== 'null' && username !== 'undefined') {
                return username;
            }
        }
        
        // 2. localStorage에서 직접 가져오기
        try {
            const token = localStorage.getItem('jwtToken');
            if (token) {
                const payload = JSON.parse(atob(token.split('.')[1]));
                if (payload.username && payload.username !== 'null') {
                    console.log('[Header Username] Got username from token:', payload.username);
                    return payload.username;
                }
            }
        } catch (e) {
            console.log('[Header Username] Error parsing token:', e);
        }
        
        // 3. data-username 속성 확인
        const elements = document.querySelectorAll('[data-username]');
        for (let el of elements) {
            if (el.dataset.username && el.dataset.username !== 'None' && el.dataset.username !== '') {
                console.log('[Header Username] Got username from data attribute:', el.dataset.username);
                return el.dataset.username;
            }
        }
        
        // 4. 기존 환영 메시지에서 추출
        const welcomeElements = document.querySelectorAll('span');
        for (let el of welcomeElements) {
            if (el.textContent.includes('Welcome,')) {
                const match = el.textContent.match(/Welcome,\s*([^!]+)!/);
                if (match && match[1] && match[1] !== 'User' && match[1].trim() !== '') {
                    console.log('[Header Username] Got username from welcome text:', match[1].trim());
                    return match[1].trim();
                }
            }
        }
        
        // 5. 세션 스토리지 확인
        const storedUsername = sessionStorage.getItem('username');
        if (storedUsername && storedUsername !== 'None' && storedUsername !== 'null') {
            console.log('[Header Username] Got username from session storage:', storedUsername);
            return storedUsername;
        }
        
        console.log('[Header Username] No username found');
        return null;
    }
    
    // 환영 메시지 업데이트
    function updateWelcomeMessage() {
        const username = getUsername();
        
        // 다양한 선택자로 환영 메시지 요소 찾기
        const selectors = [
            '.header-user-info span',
            '#userWelcome',
            '.user-info span',
            'span[data-username]',
            '.icons span'
        ];
        
        let updated = false;
        
        for (const selector of selectors) {
            const elements = document.querySelectorAll(selector);
            elements.forEach(span => {
                if (span.textContent.includes('Welcome') || span.id === 'userWelcome') {
                    if (username) {
                        span.textContent = `Welcome, ${username}!`;
                        span.setAttribute('data-username', username);
                        // 세션 스토리지에 저장
                        sessionStorage.setItem('username', username);
                        updated = true;
                        console.log('[Header Username] Updated welcome message for:', selector);
                    } else {
                        // username이 없을 때도 형식 통일
                        if (!span.textContent.includes('!')) {
                            span.textContent = 'Welcome!';
                        }
                    }
                }
            });
        }
        
        if (!updated) {
            console.log('[Header Username] No welcome message elements found');
        }
    }
    
    // 초기 실행
    updateWelcomeMessage();
    
    // TokenManager가 나중에 로드될 수 있으므로 잠시 후 다시 시도
    setTimeout(updateWelcomeMessage, 500);
    setTimeout(updateWelcomeMessage, 1000);
    
    // DOM 변경 감지하여 다시 실행
    const observer = new MutationObserver(function(mutations) {
        let shouldUpdate = false;
        mutations.forEach(function(mutation) {
            if (mutation.type === 'childList') {
                // 헤더 관련 요소가 변경되었는지 확인
                const target = mutation.target;
                if (target.tagName === 'HEADER' || 
                    target.classList.contains('header-user-info') ||
                    target.id === 'header' ||
                    target.id === 'headerActions') {
                    shouldUpdate = true;
                }
            }
        });
        
        if (shouldUpdate) {
            console.log('[Header Username] DOM changed, updating welcome message');
            updateWelcomeMessage();
        }
    });
    
    // header와 body 감시
    const header = document.querySelector('header');
    if (header) {
        observer.observe(header, { childList: true, subtree: true });
    }
    
    // body도 감시 (동적으로 헤더가 추가될 수 있음)
    observer.observe(document.body, { childList: true, subtree: true });
    
    // updateHeader 함수가 있으면 오버라이드
    if (typeof window.updateHeader === 'function') {
        const originalUpdateHeader = window.updateHeader;
        window.updateHeader = function() {
            originalUpdateHeader();
            updateWelcomeMessage();
        };
    }
});

// 전역 함수로도 제공
window.updateHeaderUsername = function() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            window.updateHeaderUsername();
        });
        return;
    }
    
    const event = new Event('DOMContentLoaded');
    document.dispatchEvent(event);
};
