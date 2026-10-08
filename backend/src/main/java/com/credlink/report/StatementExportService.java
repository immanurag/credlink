package com.credlink.report;

import com.credlink.report.dto.StatementResponse;
import com.credlink.transaction.dto.TransactionResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class StatementExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.systemDefault());

    public byte[] toPdf(StatementResponse statement) throws IOException {
        String html = buildHtml(statement);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    public byte[] toExcel(StatementResponse statement) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statement");
            int rowIdx = 0;

            Row title = sheet.createRow(rowIdx++);
            title.createCell(0).setCellValue("CredLink Statement - " + statement.getCustomerName());
            rowIdx++;

            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"Date", "Description", "Type", "Amount", "Balance After"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

            for (TransactionResponse t : statement.getTransactions()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getCreatedAt() != null ? FMT.format(t.getCreatedAt()) : "");
                row.createCell(1).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(2).setCellValue(t.getType());
                row.createCell(3).setCellValue(t.getAmount() != null ? t.getAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(t.getBalanceAfter() != null ? t.getBalanceAfter().doubleValue() : 0);
            }

            rowIdx++;
            Row summary = sheet.createRow(rowIdx++);
            summary.createCell(0).setCellValue("Opening Balance");
            summary.createCell(1).setCellValue(statement.getOpeningBalance().doubleValue());
            Row summary2 = sheet.createRow(rowIdx++);
            summary2.createCell(0).setCellValue("Closing Balance");
            summary2.createCell(1).setCellValue(statement.getClosingBalance().doubleValue());

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String buildHtml(StatementResponse s) {
        StringBuilder rows = new StringBuilder();
        for (TransactionResponse t : s.getTransactions()) {
            rows.append("<tr>")
                .append("<td>").append(t.getCreatedAt() != null ? FMT.format(t.getCreatedAt()) : "").append("</td>")
                .append("<td>").append(escape(t.getDescription())).append("</td>")
                .append("<td>").append(t.getType()).append("</td>")
                .append("<td style='text-align:right'>").append(t.getAmount()).append("</td>")
                .append("<td style='text-align:right'>").append(t.getBalanceAfter()).append("</td>")
                .append("</tr>");
        }
        return "<html><head><style>"
                + "body{font-family:sans-serif;color:#111c2c;} table{width:100%;border-collapse:collapse;margin-top:16px;}"
                + "th,td{border:1px solid #cbd5e1;padding:6px 8px;font-size:12px;} th{background:#e7eeff;text-align:left;}"
                + "h1{font-size:20px;} .summary{margin-top:16px;font-size:13px;}"
                + "</style></head><body>"
                + "<h1>CredLink Ledger Statement</h1>"
                + "<p><strong>Customer:</strong> " + escape(s.getCustomerName()) + "</p>"
                + "<table><thead><tr><th>Date</th><th>Description</th><th>Type</th><th>Amount</th><th>Balance After</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "<div class='summary'>"
                + "<p>Opening Balance: \u20B9" + s.getOpeningBalance() + "</p>"
                + "<p>Total Udhaar: \u20B9" + s.getTotalUdhaar() + " &nbsp; Total Jama: \u20B9" + s.getTotalJama() + "</p>"
                + "<p><strong>Closing Balance: \u20B9" + s.getClosingBalance() + "</strong></p>"
                + "</div></body></html>";
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
