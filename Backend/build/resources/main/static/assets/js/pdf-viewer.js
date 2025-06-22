// PDF Viewer functionality using PDF.js
class PDFViewer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.currentPDF = null;
        this.currentPage = 1;
        this.totalPages = 0;
        this.scale = 1.5;
        this.renderingPage = false;
        
        // Load PDF.js library
        this.loadPDFJS();
    }
    
    loadPDFJS() {
        if (!window.pdfjsLib) {
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js';
            script.onload = () => {
                window.pdfjsLib.GlobalWorkerOptions.workerSrc = 
                    'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
            };
            document.head.appendChild(script);
        }
    }
    
    createViewerHTML() {
        return `
            <div class="pdf-viewer-container">
                <div class="pdf-toolbar">
                    <button onclick="pdfViewer.previousPage()" class="button small">
                        <i class="fas fa-chevron-left"></i> Previous
                    </button>
                    <span class="page-info">
                        Page <input type="number" id="pageNumber" value="1" min="1" 
                                   onchange="pdfViewer.goToPage(this.value)" style="width: 50px; text-align: center;">
                        of <span id="totalPages">0</span>
                    </span>
                    <button onclick="pdfViewer.nextPage()" class="button small">
                        Next <i class="fas fa-chevron-right"></i>
                    </button>
                    <div class="zoom-controls">
                        <button onclick="pdfViewer.zoomOut()" class="button small">
                            <i class="fas fa-search-minus"></i>
                        </button>
                        <span id="zoomLevel">150%</span>
                        <button onclick="pdfViewer.zoomIn()" class="button small">
                            <i class="fas fa-search-plus"></i>
                        </button>
                    </div>
                    <button onclick="pdfViewer.downloadPDF()" class="button small primary">
                        <i class="fas fa-download"></i> Download
                    </button>
                    <button onclick="pdfViewer.closePDF()" class="button small">
                        <i class="fas fa-times"></i> Close
                    </button>
                </div>
                <div class="pdf-canvas-container">
                    <canvas id="pdfCanvas"></canvas>
                </div>
            </div>
        `;
    }
    
    async loadPDF(url, fileName = 'document.pdf') {
        if (!window.pdfjsLib) {
            alert('PDF.js is still loading. Please try again in a moment.');
            return;
        }
        
        this.container.innerHTML = this.createViewerHTML();
        this.container.style.display = 'block';
        
        try {
            const loadingTask = pdfjsLib.getDocument(url);
            this.currentPDF = await loadingTask.promise;
            this.totalPages = this.currentPDF.numPages;
            this.fileName = fileName;
            
            document.getElementById('totalPages').textContent = this.totalPages;
            
            await this.renderPage(1);
        } catch (error) {
            console.error('Error loading PDF:', error);
            alert('Failed to load PDF. Please try again.');
            this.closePDF();
        }
    }
    
    async renderPage(pageNumber) {
        if (this.renderingPage || !this.currentPDF) return;
        
        this.renderingPage = true;
        
        try {
            const page = await this.currentPDF.getPage(pageNumber);
            const canvas = document.getElementById('pdfCanvas');
            const context = canvas.getContext('2d');
            
            const viewport = page.getViewport({ scale: this.scale });
            canvas.height = viewport.height;
            canvas.width = viewport.width;
            
            const renderContext = {
                canvasContext: context,
                viewport: viewport
            };
            
            await page.render(renderContext).promise;
            
            this.currentPage = pageNumber;
            document.getElementById('pageNumber').value = pageNumber;
            
        } catch (error) {
            console.error('Error rendering page:', error);
        } finally {
            this.renderingPage = false;
        }
    }
    
    previousPage() {
        if (this.currentPage > 1) {
            this.renderPage(this.currentPage - 1);
        }
    }
    
    nextPage() {
        if (this.currentPage < this.totalPages) {
            this.renderPage(this.currentPage + 1);
        }
    }
    
    goToPage(pageNumber) {
        pageNumber = parseInt(pageNumber);
        if (pageNumber >= 1 && pageNumber <= this.totalPages) {
            this.renderPage(pageNumber);
        }
    }
    
    zoomIn() {
        this.scale = Math.min(this.scale + 0.25, 3);
        this.updateZoomLevel();
        this.renderPage(this.currentPage);
    }
    
    zoomOut() {
        this.scale = Math.max(this.scale - 0.25, 0.5);
        this.updateZoomLevel();
        this.renderPage(this.currentPage);
    }
    
    updateZoomLevel() {
        document.getElementById('zoomLevel').textContent = Math.round(this.scale * 100) + '%';
    }
    
    downloadPDF() {
        if (this.currentPDF && this.fileName) {
            const link = document.createElement('a');
            link.href = this.currentPDF.url || '#';
            link.download = this.fileName;
            link.click();
        }
    }
    
    closePDF() {
        this.container.style.display = 'none';
        this.container.innerHTML = '';
        this.currentPDF = null;
        this.currentPage = 1;
        this.totalPages = 0;
    }
}

// Initialize PDF viewer
let pdfViewer;
document.addEventListener('DOMContentLoaded', function() {
    // Create PDF viewer container
    const viewerDiv = document.createElement('div');
    viewerDiv.id = 'pdfViewerContainer';
    viewerDiv.style.cssText = `
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.9);
        z-index: 10000;
        overflow: auto;
    `;
    document.body.appendChild(viewerDiv);
    
    pdfViewer = new PDFViewer('pdfViewerContainer');
});

// Add styles
const pdfStyles = document.createElement('style');
pdfStyles.textContent = `
    .pdf-viewer-container {
        max-width: 90%;
        margin: 20px auto;
        background: white;
        border-radius: 10px;
        overflow: hidden;
    }
    
    .pdf-toolbar {
        background: #f5f6f7;
        padding: 15px;
        display: flex;
        align-items: center;
        gap: 15px;
        flex-wrap: wrap;
        justify-content: space-between;
    }
    
    .pdf-toolbar .page-info {
        display: flex;
        align-items: center;
        gap: 5px;
    }
    
    .pdf-toolbar .zoom-controls {
        display: flex;
        align-items: center;
        gap: 10px;
    }
    
    .pdf-canvas-container {
        padding: 20px;
        text-align: center;
        overflow: auto;
        max-height: calc(100vh - 120px);
        background: #f0f0f0;
    }
    
    #pdfCanvas {
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        background: white;
        margin: 0 auto;
    }
    
    [data-theme="dark"] .pdf-viewer-container {
        background: var(--bg-secondary);
    }
    
    [data-theme="dark"] .pdf-toolbar {
        background: var(--bg-tertiary);
    }
    
    [data-theme="dark"] .pdf-canvas-container {
        background: var(--bg-primary);
    }
`;
document.head.appendChild(pdfStyles);

// Function to view PDF from material
function viewPDF(materialId, fileName, fileUrl) {
    if (pdfViewer) {
        pdfViewer.loadPDF(fileUrl, fileName);
    }
}
