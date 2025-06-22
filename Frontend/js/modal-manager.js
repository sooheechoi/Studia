// Global Modal Manager
// 팝업창 관리를 위한 전역 시스템

let activeModals = new Set();

// 모달 생성 함수
function createModal(content, modalId = null) {
    const id = modalId || 'modal-' + Date.now();
    const modal = document.createElement('div');
    modal.id = id;
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
        animation: fadeIn 0.3s ease;
    `;
    modal.innerHTML = content;
    
    // 모달 추가 시 Set에 등록
    activeModals.add(id);
    document.body.appendChild(modal);
    
    // 바디 스크롤 방지
    if (activeModals.size === 1) {
        document.body.style.overflow = 'hidden';
    }
    
    // ESC 키로 닫기
    const escHandler = (e) => {
        if (e.key === 'Escape') {
            closeModal(id);
            document.removeEventListener('keydown', escHandler);
        }
    };
    document.addEventListener('keydown', escHandler);
    
    // 오버레이 클릭으로 닫기
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeModal(id);
        }
    });
    
    return {
        id: id,
        close: () => closeModal(id),
        element: modal
    };
}

// 모달 닫기 함수
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        // 페이드 아웃 애니메이션
        modal.style.animation = 'fadeOut 0.3s ease';
        
        setTimeout(() => {
            modal.remove();
            activeModals.delete(modalId);
            
            // 모든 모달이 닫히면 바디 스크롤 복구
            if (activeModals.size === 0) {
                document.body.style.overflow = '';
                document.body.style.pointerEvents = '';
            }
        }, 300);
    }
}

// 모든 모달 닫기
function closeAllModals() {
    activeModals.forEach(modalId => closeModal(modalId));
}

// 애니메이션 스타일 추가
if (!document.getElementById('modal-animations')) {
    const style = document.createElement('style');
    style.id = 'modal-animations';
    style.textContent = `
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        @keyframes fadeOut {
            from { opacity: 1; }
            to { opacity: 0; }
        }
        
        /* 모달 내부 컨텐츠 기본 스타일 */
        .modal-content {
            background: white;
            padding: 30px;
            border-radius: 15px;
            max-width: 90%;
            max-height: 90vh;
            overflow-y: auto;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            animation: slideIn 0.3s ease;
        }
        
        @keyframes slideIn {
            from { transform: translateY(-20px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
        
        /* z-index 수정 */
        .modal, [style*="position: fixed"] {
            z-index: 9999 !important;
        }
        
        /* 모달이 열려있을 때 바디 스타일 */
        body.modal-open {
            overflow: hidden;
        }
    `;
    document.head.appendChild(style);
}

// 간단한 알림 모달
function showAlert(message, type = 'info') {
    const modal = createModal(`
        <div class="modal-content" style="max-width: 400px;">
            <div style="text-align: center;">
                ${type === 'success' ? '<i class="fas fa-check-circle" style="font-size: 3em; color: #4caf50; margin-bottom: 20px;"></i>' : ''}
                ${type === 'error' ? '<i class="fas fa-exclamation-circle" style="font-size: 3em; color: #f44336; margin-bottom: 20px;"></i>' : ''}
                ${type === 'info' ? '<i class="fas fa-info-circle" style="font-size: 3em; color: #2196f3; margin-bottom: 20px;"></i>' : ''}
                <h3 style="margin: 0 0 15px 0; color: #333;">${type.charAt(0).toUpperCase() + type.slice(1)}</h3>
                <p style="color: #666; margin-bottom: 25px;">${message}</p>
                <button onclick="closeModal('${modal.id}')" style="padding: 10px 30px; border: none; background: #667eea; color: white; border-radius: 8px; cursor: pointer; font-size: 1em;">OK</button>
            </div>
        </div>
    `);
    
    return modal;
}

// 확인 모달
function showConfirm(message, onConfirm, onCancel) {
    const modal = createModal(`
        <div class="modal-content" style="max-width: 400px;">
            <h3 style="margin: 0 0 20px 0; color: #333;">Confirm</h3>
            <p style="color: #666; margin-bottom: 30px;">${message}</p>
            <div style="display: flex; gap: 10px; justify-content: flex-end;">
                <button onclick="closeModal('${modal.id}'); ${onCancel ? onCancel() : ''}" style="padding: 10px 25px; border: 1px solid #ddd; background: white; border-radius: 8px; cursor: pointer;">Cancel</button>
                <button onclick="closeModal('${modal.id}'); ${onConfirm()}" style="padding: 10px 25px; border: none; background: #667eea; color: white; border-radius: 8px; cursor: pointer;">Confirm</button>
            </div>
        </div>
    `);
    
    return modal;
}

// 윈도우가 닫힐 때 모든 모달 정리
window.addEventListener('beforeunload', () => {
    closeAllModals();
});

// Export for use
window.ModalManager = {
    create: createModal,
    close: closeModal,
    closeAll: closeAllModals,
    alert: showAlert,
    confirm: showConfirm,
    activeModals: activeModals
};
