package com.sushilambekar.expensetracker.expense;

import com.sushilambekar.expensetracker.expense.ExpenseDtos.ExpenseResponse;
import java.util.List;

/** Writes expenses as CSV (RFC 4180), ready to open in Excel or Google Sheets. */
final class ExpenseCsvWriter {

    private static final String HEADER = "Date,Category,Description,Payment method,Amount";

    private ExpenseCsvWriter() {
    }

    static String write(List<ExpenseResponse> expenses) {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (ExpenseResponse expense : expenses) {
            csv.append(expense.date()).append(',')
                    .append(field(expense.category().name())).append(',')
                    .append(field(expense.description())).append(',')
                    .append(expense.paymentMethod()).append(',')
                    .append(expense.amount().toPlainString()).append("\r\n");
        }
        return csv.toString();
    }

    /** Quotes a value that contains a comma, quote or line break, and neutralises spreadsheet formulas. */
    static String field(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
