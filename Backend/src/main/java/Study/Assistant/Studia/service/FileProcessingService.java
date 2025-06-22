package Study.Assistant.Studia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProcessingService {
    
    /**
     * 업로드된 파일에서 텍스트를 추출합니다.
     */
    public String extractTextFromFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String fileType = getFileExtension(fileName);
        
        log.info("Extracting text from file: {} (type: {})", fileName, fileType);
        
        return switch (fileType.toLowerCase()) {
            case "pdf" -> extractFromPDF(file.getInputStream());
            case "pptx", "ppt" -> extractFromPPT(file.getInputStream());
            case "txt" -> new String(file.getBytes());
            case "docx" -> extractFromDOCX(file.getInputStream());
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
        };
    }
    
    private String extractFromPDF(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    private String extractFromPPT(InputStream inputStream) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            for (XSLFSlide slide : ppt.getSlides()) {
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                        String content = textShape.getText();
                        if (content != null) {
                            text.append(content).append("\n");
                        }
                    }
                });
            }
            
            return text.toString();
        }
    }
    
    private String extractFromDOCX(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            
            // 모든 단락의 텍스트 추출
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    text.append(paragraphText).append("\n");
                }
            }
            
            // 테이블 내의 텍스트도 추출
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            text.append(cellText).append(" ");
                        }
                    });
                    text.append("\n");
                });
            });
            
            return text.toString();
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * 강의 영상 URL에서 텍스트를 추출합니다 (YouTube 등)
     */
    public String extractFromVideoUrl(String url) {
        // TODO: YouTube API를 사용하여 자막 추출 또는
        // Whisper API를 사용한 음성 인식 구현
        return "Video transcript";
    }
}
