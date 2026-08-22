package com.example.myservice.service;

import com.example.myservice.entity.SessionNote;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    private static final String[] HEADERS = {
            "Session Date", "Tutor Name", "Student Name", "Note Type",
            "Grade Level", "District/State", "Curriculum Standard",
            "Engagement", "Approved Note"
    };

    public byte[] exportSessionNotes(List<SessionNote> notes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Session Notes");

            // Header row styling
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Wrapped text style for the long note column
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            int rowIdx = 1;
            for (SessionNote note : notes) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(safe(note.getSessionDate()));
                row.createCell(1).setCellValue(safe(note.getTutorName()));
                row.createCell(2).setCellValue(safe(note.getStudentName()));
                row.createCell(3).setCellValue(safe(note.getSubject()));
                row.createCell(4).setCellValue(safe(note.getGradeLevel()));
                row.createCell(5).setCellValue(safe(note.getDistrictOrState()));
                row.createCell(6).setCellValue(safe(note.getCurriculumStandard()));
                row.createCell(7).setCellValue(safe(note.getEngagement()));

                Cell noteCell = row.createCell(8);
                noteCell.setCellValue(safe(note.getFinalApprovedNote()));
                noteCell.setCellStyle(wrapStyle);
            }

            // Auto-size the narrow columns, fix a wide width for the note column
            for (int i = 0; i < HEADERS.length - 1; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setColumnWidth(8, 18000);

            // Freeze header row so it stays visible while scrolling
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}