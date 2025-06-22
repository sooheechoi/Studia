// Summary Page JavaScript

// Global variables
let allMaterials = [];
let refreshInterval;

// Page initialization
document.addEventListener('DOMContentLoaded', async function() {
    console.log('DOMContentLoaded event fired');
    
    // Set initial sidebar state - always start closed
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.add('inactive');
        document.body.classList.add('sidebar-inactive');
    }

    if (!checkAuth()) {
        alert('Please login first');
        window.location.href = '/';
        return;
    }

    console.log('Auth check passed');
    
    updateHeader();
    console.log('Header updated');
    
    await loadMaterials();
    console.log('Materials loaded');
    
    setupUploadArea();
    console.log('Upload area setup called');
    
    // Add event listeners for filters
    const classFilter = document.getElementById('classFilter');
    const searchFilter = document.getElementById('searchFilter');
    
    if (classFilter) {
        classFilter.addEventListener('change', filterAndDisplayMaterials);
    }
    if (searchFilter) {
        searchFilter.addEventListener('input', filterAndDisplayMaterials);
    }
});

function updateHeader() {
    const headerActions = document.getElementById('headerActions');
    const username = TokenManager.getUsername();
    if (headerActions) {
        headerActions.innerHTML = `
            <li><span style="margin-right: 1em;">Welcome, ${username || 'User'}!</span></li>
            <li><button class="button" onclick="logout()">Log Out</button></li>
        `;
    }
}

function logout() {
    API.auth.logout();
}

function setupUploadArea() {
    console.log('setupUploadArea() function started');
    
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');

    if (!uploadArea || !fileInput) {
        console.error('Upload area or file input not found!');
        return;
    }

    // Click event
    uploadArea.addEventListener('click', function(e) {
        console.log('Upload area clicked!');
        fileInput.click();
    });

    // File input change event
    fileInput.addEventListener('change', function(e) {
        console.log('File input changed!');
        const files = Array.from(e.target.files);
        console.log('Selected files:', files);
        if (files.length > 0) {
            uploadFiles(files);
        }
    });

    // Drag and drop
    uploadArea.addEventListener('dragover', function(e) {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', function() {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', function(e) {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        console.log('Drop event!');
        const files = Array.from(e.dataTransfer.files);
        console.log('Dropped files:', files);
        if (files.length > 0) {
            uploadFiles(files);
        }
    });
    
    console.log('setupUploadArea() function completed');
}

async function uploadFiles(files) {
    const className = await promptForClassName();
    
    if (className === null) {
        return;
    }
    
    if (!className || className.trim() === '') {
        alert('Class name is required for uploading materials.');
        return;
    }
    
    const progressModal = showProgressModal('Uploading files...');
    
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const progress = ((i + 1) / files.length) * 100;
        progressModal.updateProgress(progress);
        progressModal.updateMessage(`Uploading ${file.name}... (${i + 1}/${files.length})`);
        
        // Validate file
        if (file.size > 100 * 1024 * 1024) {
            progressModal.showError(`${file.name} is too large (max 100MB)`);
            await new Promise(resolve => setTimeout(resolve, 2000));
            continue;
        }
        
        const allowedTypes = ['.pdf', '.docx', '.pptx', '.txt'];
        const fileExtension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
        if (!allowedTypes.includes(fileExtension)) {
            progressModal.showError(`${file.name} has unsupported format`);
            await new Promise(resolve => setTimeout(resolve, 2000));
            continue;
        }
        
        const title = file.name.replace(/\.[^/.]+$/, '');
        
        try {
            progressModal.updateMessage(`Processing ${file.name}...`);
            await API.materials.upload(file, title, className);
            progressModal.updateMessage(`✓ ${file.name} uploaded successfully`);
        } catch (error) {
            console.error('Upload error for file:', file.name, error);
            progressModal.showError(`Failed to upload ${file.name}: ${error.message}`);
            await new Promise(resolve => setTimeout(resolve, 2000));
        }
    }
    
    progressModal.complete('All files processed!');
    setTimeout(() => {
        progressModal.close();
        document.getElementById('fileInput').value = '';
        loadMaterials();
        checkAndStartAutoRefresh();
    }, 1500);
}

async function promptForClassName() {
    const savedSchedules = localStorage.getItem('studiaSchedules');
    let classes = [];
    
    if (savedSchedules) {
        const schedules = JSON.parse(savedSchedules);
        classes = [...new Set(schedules
            .filter(s => s.type === 'class')
            .map(s => s.title))];
    }
    
    return new Promise((resolve) => {
        const modal = document.createElement('div');
        modal.innerHTML = `
            <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
                <div style="background: white; padding: 30px; border-radius: 15px; max-width: 400px; width: 90%; box-shadow: 0 10px 40px rgba(0,0,0,0.2);">
                    <h3 style="margin-top: 0; color: #2c3e50;">Add Class Information</h3>
                    <p style="color: #666; margin-bottom: 20px;">Select or enter the class name for these materials</p>
                    
                    ${classes.length > 0 ? `
                        <label style="display: block; margin-bottom: 5px; color: #495057;">Select from your classes:</label>
                        <select id="classSelect" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px; margin-bottom: 15px;">
                            <option value="">-- Select a class --</option>
                            ${classes.map(cls => `<option value="${cls}">${cls}</option>`).join('')}
                            <option value="__new__">+ Add new class</option>
                        </select>
                    ` : ''}
                    
                    <div id="newClassInput" style="${classes.length > 0 ? 'display: none;' : ''}">
                        <label style="display: block; margin-bottom: 5px; color: #495057;">Or enter a new class:</label>
                        <input type="text" id="classNameInput" placeholder="e.g., CS101, Math 202" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                    </div>
                    
                    <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                        <button onclick="document.getElementById('classNameModal').resolve(null)" style="padding: 10px 20px; border: 1px solid #ddd; background: white; border-radius: 5px; cursor: pointer;">Cancel</button>
                        <button onclick="
                            const select = document.getElementById('classSelect');
                            const input = document.getElementById('classNameInput');
                            let value = '';
                            if (select && select.value && select.value !== '__new__') {
                                value = select.value;
                            } else if (input && input.value) {
                                value = input.value;
                            }
                            document.getElementById('classNameModal').resolve(value || null);
                        " style="padding: 10px 20px; border: none; background: #667eea; color: white; border-radius: 5px; cursor: pointer;">Continue</button>
                    </div>
                </div>
            </div>
        `;
        modal.id = 'classNameModal';
        modal.resolve = (value) => {
            resolve(value);
            modal.remove();
        };
        document.body.appendChild(modal);
        
        // Add event listener for select change
        if (classes.length > 0) {
            const select = document.getElementById('classSelect');
            const newInput = document.getElementById('newClassInput');
            select.addEventListener('change', (e) => {
                if (e.target.value === '__new__') {
                    newInput.style.display = 'block';
                    document.getElementById('classNameInput').focus();
                } else {
                    newInput.style.display = 'none';
                }
            });
        }
        
        // Focus on appropriate element
        setTimeout(() => {
            if (classes.length > 0) {
                const selectEl = document.getElementById('classSelect');
                if (selectEl) selectEl.focus();
            } else {
                const inputEl = document.getElementById('classNameInput');
                if (inputEl) inputEl.focus();
            }
        }, 100);
    });
}

async function loadMaterials() {
    try {
        allMaterials = await API.materials.list();
        updateClassFilter();
        filterAndDisplayMaterials();
    } catch (error) {
        console.error('Failed to load materials:', error);
    }
}

function updateClassFilter() {
    const classFilter = document.getElementById('classFilter');
    if (!classFilter) return;
    
    const classes = [...new Set(allMaterials
        .filter(m => m.className)
        .map(m => m.className))];
    
    classFilter.innerHTML = '<option value="">All Classes</option>' +
        classes.map(className => 
            `<option value="${className}">${className}</option>`
        ).join('');
}

function filterAndDisplayMaterials() {
    const container = document.getElementById('materialsContainer');
    if (!container) return;
    
    const classFilter = document.getElementById('classFilter');
    const searchFilter = document.getElementById('searchFilter');
    
    const classFilterValue = classFilter ? classFilter.value : '';
    const searchFilterValue = searchFilter ? searchFilter.value.toLowerCase() : '';
    
    let filteredMaterials = allMaterials;
    
    // Apply class filter
    if (classFilterValue) {
        filteredMaterials = filteredMaterials.filter(m => m.className === classFilterValue);
    }
    
    // Apply search filter
    if (searchFilterValue) {
        filteredMaterials = filteredMaterials.filter(m => 
            m.title.toLowerCase().includes(searchFilterValue) ||
            (m.summary && m.summary.toLowerCase().includes(searchFilterValue)) ||
            (m.className && m.className.toLowerCase().includes(searchFilterValue))
        );
    }
        
    if (!filteredMaterials || filteredMaterials.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-folder-open"></i>
                <h3>No materials found</h3>
                <p>${classFilterValue || searchFilterValue ? 'Try adjusting your filters' : 'Upload your first study material to get started!'}</p>
            </div>
        `;
        return;
    }

    container.innerHTML = filteredMaterials.map(material => {
        const escapedTitle = (material.title || '').replace(/'/g, "\\'");
        const escapedClassName = (material.className || '').replace(/'/g, "\\'");
        
        return `
            <div class="material-card">
                <div class="material-header">
                    <div>
                        <h3 class="material-title">${material.title || material.originalFileName}</h3>
                        <div class="material-meta">
                            <span><i class="fas fa-calendar"></i> ${new Date(material.createdAt || material.uploadedAt).toLocaleDateString()}</span>
                            <span><i class="fas fa-file"></i> ${formatFileSize(material.fileSize || 0)}</span>
                            <span><i class="fas fa-file-alt"></i> ${material.fileType || 'Unknown'}</span>
                            ${material.className ? `<span style="background: #e3f2fd; color: #1976d2; padding: 2px 8px; border-radius: 12px;"><i class="fas fa-graduation-cap"></i> ${material.className}</span>` : ''}
                        </div>
                    </div>
                    <span class="status-badge status-${(material.status || 'PROCESSING').toLowerCase()}">
                        ${material.status === 'COMPLETED' ? '<i class="fas fa-check-circle"></i>' : '<i class="fas fa-spinner fa-spin"></i>'}
                        ${material.status || 'PROCESSING'}
                    </span>
                </div>
                
                ${material.summary ? `
                    <div class="summary-section">
                        <h4><i class="fas fa-align-left"></i> Summary</h4>
                        <div class="summary-content">${material.summary.replace(/\n/g, '<br>')}</div>
                    </div>
                ` : ''}
                
                ${material.keyPoints ? `
                    <div class="summary-section">
                        <h4><i class="fas fa-list-ul"></i> Key Points</h4>
                        <ul class="key-points">
                            ${material.keyPoints.split('\n').filter(p => p.trim()).map(point => 
                                `<li>${point.replace(/^[-•*]\s*/, '')}</li>`
                            ).join('')}
                        </ul>
                    </div>
                ` : ''}
                
                <div class="material-actions">
                    ${material.status === 'COMPLETED' ? `
                        ${material.fileType === 'pdf' ? 
                            `<button class="action-btn btn-secondary" onclick="viewPDF(${material.id}, '${material.originalFileName}')">
                                <i class="fas fa-eye"></i> View PDF
                            </button>` : ''
                        }
                        ${!material.summary ? 
                            `<button class="action-btn btn-primary" onclick="generateSummary(${material.id})">
                                <i class="fas fa-magic"></i> Generate Summary
                            </button>` : 
                            `<button class="action-btn btn-secondary" onclick="regenerateSummary(${material.id})">
                                <i class="fas fa-redo"></i> Regenerate
                            </button>`
                        }
                        <button class="action-btn btn-primary" onclick="generateQuiz(${material.id})">
                            ❓ Create Quiz
                        </button>
                        <button class="action-btn btn-secondary" onclick="editMaterial(${material.id}, '${escapedTitle}', '${escapedClassName}')">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                    ` : `
                        <button class="action-btn btn-secondary" disabled>
                            <span class="spinner"></span> Processing...
                        </button>
                    `}
                    <button class="action-btn btn-danger" onclick="deleteMaterial(${material.id})">
                        🗑️ Delete
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// Global functions that need to be accessible from HTML
window.toggleSidebar = function() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('inactive');
    
    if (sidebar.classList.contains('inactive')) {
        document.body.classList.add('sidebar-inactive');
    } else {
        document.body.classList.remove('sidebar-inactive');
    }
};

window.generateSummary = async function(materialId) {
    const btn = event.target.closest('button') || event.target;
    const originalHTML = btn.innerHTML;
    btn.innerHTML = '<span class="spinner"></span> Generating...';
    btn.disabled = true;
    
    const progressNotification = showProgressNotification('Generating summary...');
    
    try {
        const response = await fetch(`${API_BASE_URL}/materials/${materialId}/summary`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${TokenManager.getToken()}`
            }
        });
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to generate summary');
        }
        
        const data = await response.json();
        
        progressNotification.update('Summary generated successfully!', 'success');
        setTimeout(() => {
            progressNotification.remove();
            loadMaterials();
        }, 2000);
    } catch (error) {
        console.error('Summary generation error:', error);
        progressNotification.update('Failed to generate summary: ' + error.message, 'error');
        btn.innerHTML = originalHTML;
        btn.disabled = false;
        setTimeout(() => progressNotification.remove(), 3000);
    }
};

window.regenerateSummary = async function(materialId) {
    if (!confirm('Are you sure you want to regenerate the summary?')) return;
    await generateSummary(materialId);
};

window.generateQuiz = async function(materialId) {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div style="background: white; padding: 30px; border-radius: 15px; max-width: 400px; width: 90%;">
                <h3 style="margin-top: 0; color: #2c3e50;">Create Quiz</h3>
                <div style="margin-bottom: 20px;">
                    <label style="display: block; margin-bottom: 5px; color: #495057;">Number of Questions:</label>
                    <input type="number" id="quizCount" min="5" max="20" value="10" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                </div>
                <div style="margin-bottom: 20px;">
                    <label style="display: block; margin-bottom: 5px; color: #495057;">Difficulty:</label>
                    <select id="quizDifficulty" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                        <option value="EASY">Easy</option>
                        <option value="MEDIUM" selected>Medium</option>
                        <option value="HARD">Hard</option>
                    </select>
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end;">
                    <button onclick="this.closest('div').parentElement.remove()" style="padding: 10px 20px; border: 1px solid #ddd; background: white; border-radius: 5px; cursor: pointer;">Cancel</button>
                    <button onclick="confirmQuizGeneration(${materialId})" style="padding: 10px 20px; border: none; background: #667eea; color: white; border-radius: 5px; cursor: pointer;">Create</button>
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
};

window.confirmQuizGeneration = async function(materialId) {
    const count = document.getElementById('quizCount').value;
    const difficulty = document.getElementById('quizDifficulty').value;
    
    const questionCount = parseInt(count);
    if (isNaN(questionCount) || questionCount < 5 || questionCount > 20) {
        alert('Please enter a number between 5 and 20');
        return;
    }

    document.querySelector('[style*="position: fixed"]').remove();

    const progressNotification = showProgressNotification('Creating quiz questions...');

    try {
        await API.materials.generateQuizzes(materialId, questionCount, difficulty);
        progressNotification.update('Quiz created successfully!', 'success');
        setTimeout(() => {
            progressNotification.remove();
            window.location.href = 'quiz.html';
        }, 1500);
    } catch (error) {
        progressNotification.update('Failed to generate quiz: ' + error.message, 'error');
        setTimeout(() => progressNotification.remove(), 3000);
    }
};

window.deleteMaterial = async function(materialId) {
    if (!confirm('Are you sure you want to delete this material? This cannot be undone.')) return;
    
    try {
        await API.materials.delete(materialId);
        const card = event.target.closest('.material-card');
        card.style.opacity = '0';
        card.style.transform = 'scale(0.9)';
        setTimeout(() => {
            loadMaterials();
        }, 300);
    } catch (error) {
        alert('Failed to delete material: ' + error.message);
    }
};

window.editMaterial = async function(materialId, currentTitle, currentClassName) {
    const modal = document.createElement('div');
    modal.innerHTML = `
        <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;">
            <div style="background: white; padding: 30px; border-radius: 15px; max-width: 400px; width: 90%;">
                <h3 style="margin-top: 0; color: #2c3e50;">Edit Material</h3>
                <div style="margin-bottom: 20px;">
                    <label style="display: block; margin-bottom: 5px; color: #495057;">Title:</label>
                    <input type="text" id="editTitle" value="${currentTitle}" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                </div>
                <div style="margin-bottom: 20px;">
                    <label style="display: block; margin-bottom: 5px; color: #495057;">Class Name:</label>
                    <input type="text" id="editClassName" value="${currentClassName}" placeholder="e.g., CS101, Math 202" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px;">
                </div>
                <div style="display: flex; gap: 10px; justify-content: flex-end;">
                    <button onclick="this.closest('div').parentElement.remove()" style="padding: 10px 20px; border: 1px solid #ddd; background: white; border-radius: 5px; cursor: pointer;">Cancel</button>
                    <button onclick="saveEditMaterial(${materialId})" style="padding: 10px 20px; border: none; background: #667eea; color: white; border-radius: 5px; cursor: pointer;">Save</button>
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    
    setTimeout(() => {
        const titleInput = document.getElementById('editTitle');
        if (titleInput) titleInput.focus();
    }, 100);
};

window.saveEditMaterial = async function(materialId) {
    const title = document.getElementById('editTitle').value.trim();
    const className = document.getElementById('editClassName').value.trim();
    
    if (!title) {
        alert('Title is required');
        return;
    }
    
    document.querySelector('[style*="position: fixed"]').remove();
    
    try {
        const response = await apiClient.put(`/materials/${materialId}`, {
            title: title,
            className: className
        });
        
        if (!response.ok) throw new Error('Failed to update material');
        
        showNotification('Material updated successfully!');
        loadMaterials();
    } catch (error) {
        showNotification('Failed to update material: ' + error.message, 'error');
    }
};

window.viewPDF = function(materialId, fileName) {
    window.open(`/pdf-viewer.html?id=${materialId}`, '_blank');
};

// Helper functions
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function showProgressModal(initialMessage) {
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0,0,0,0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
    `;
    
    modal.innerHTML = `
        <div style="background: white; padding: 40px; border-radius: 20px; min-width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,0.3);">
            <h3 style="margin: 0 0 20px 0; color: #2c3e50; text-align: center;">Processing</h3>
            <div style="margin-bottom: 20px;">
                <div style="background: #e9ecef; border-radius: 10px; height: 10px; overflow: hidden;">
                    <div id="modalProgressBar" style="background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); height: 100%; width: 0%; transition: width 0.3s ease;"></div>
                </div>
            </div>
            <div id="modalMessage" style="text-align: center; color: #495057; margin-bottom: 20px;">${initialMessage}</div>
            <div id="modalStatus" style="text-align: center; font-size: 2em;">
                <div class="spinner" style="margin: 0 auto;"></div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    return {
        updateProgress: (percent) => {
            document.getElementById('modalProgressBar').style.width = percent + '%';
        },
        updateMessage: (message) => {
            document.getElementById('modalMessage').textContent = message;
        },
        showError: (message) => {
            document.getElementById('modalMessage').innerHTML = `<span style="color: #f44336;">${message}</span>`;
            document.getElementById('modalStatus').innerHTML = '<span style="color: #f44336;">❌</span>';
        },
        complete: (message) => {
            document.getElementById('modalProgressBar').style.width = '100%';
            document.getElementById('modalMessage').innerHTML = `<span style="color: #4caf50;">${message}</span>`;
            document.getElementById('modalStatus').innerHTML = '<span style="color: #4caf50;">✅</span>';
        },
        close: () => {
            modal.remove();
        }
    };
}

function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 25px;
        background: ${type === 'success' ? '#4caf50' : '#f44336'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 1000;
        animation: slideIn 0.3s ease;
        max-width: 300px;
    `;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

function showProgressNotification(message) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 20px 25px;
        background: white;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        z-index: 1000;
        animation: slideIn 0.3s ease;
        min-width: 300px;
    `;
    
    notification.innerHTML = `
        <div style="display: flex; align-items: center; gap: 15px;">
            <div class="spinner"></div>
            <div>
                <div style="font-weight: 600; color: #333; margin-bottom: 5px;">${message}</div>
                <div class="progress-bar" style="margin-top: 10px;">
                    <div class="progress-fill" style="animation: progressAnimation 2s ease-in-out infinite;"></div>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(notification);
    
    return {
        update: (newMessage, type) => {
            if (type === 'success') {
                notification.innerHTML = `
                    <div style="display: flex; align-items: center; gap: 15px;">
                        <div style="color: #4caf50; font-size: 1.5em;">✓</div>
                        <div style="font-weight: 600; color: #333;">${newMessage}</div>
                    </div>
                `;
            } else if (type === 'error') {
                notification.innerHTML = `
                    <div style="display: flex; align-items: center; gap: 15px;">
                        <div style="color: #f44336; font-size: 1.5em;">✗</div>
                        <div style="font-weight: 600; color: #333;">${newMessage}</div>
                    </div>
                `;
            } else {
                notification.querySelector('div > div > div:first-child').textContent = newMessage;
            }
        },
        remove: () => {
            notification.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => notification.remove(), 300);
        }
    };
}

// Auto-refresh functionality
function startAutoRefresh() {
    if (refreshInterval) return;
    
    refreshInterval = setInterval(async () => {
        try {
            const materials = await API.materials.list();
            const hasProcessing = materials.some(m => m.status === 'PROCESSING');
            
            if (hasProcessing) {
                console.log('Found materials still processing, refreshing...');
                allMaterials = materials;
                filterAndDisplayMaterials();
            } else {
                console.log('No materials processing, stopping auto-refresh');
                stopAutoRefresh();
            }
        } catch (error) {
            console.error('Auto-refresh error:', error);
        }
    }, 5000);
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

async function checkAndStartAutoRefresh() {
    try {
        const materials = await API.materials.list();
        if (materials.some(m => m.status === 'PROCESSING')) {
            startAutoRefresh();
        }
    } catch (error) {
        console.error('Error checking materials:', error);
    }
}

// Call this after initial load
setTimeout(checkAndStartAutoRefresh, 1000);

// Add CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
    @keyframes progressAnimation {
        0% { width: 0%; }
        50% { width: 70%; }
        100% { width: 100%; }
    }
`;
document.head.appendChild(style);
