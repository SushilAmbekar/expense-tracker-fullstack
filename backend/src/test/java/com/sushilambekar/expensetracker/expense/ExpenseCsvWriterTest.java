package com.sushilambekar.expensetracker.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExpenseCsvWriterTest {

    @Test
    void plainTextIsWrittenAsIs() {
        assertThat(ExpenseCsvWriter.field("Groceries")).isEqualTo("Groceries");
    }

    @Test
    void commasQuotesAndLineBreaksAreQuoted() {
        assertThat(ExpenseCsvWriter.field("Tea, biscuits")).isEqualTo("\"Tea, biscuits\"");
        assertThat(ExpenseCsvWriter.field("The \"big\" shop")).isEqualTo("\"The \"\"big\"\" shop\"");
        assertThat(ExpenseCsvWriter.field("Line 1\nLine 2")).isEqualTo("\"Line 1\nLine 2\"");
    }

    @Test
    void textThatASpreadsheetWouldRunAsAFormulaIsEscaped() {
        assertThat(ExpenseCsvWriter.field("=HYPERLINK(\"http://evil\")")).startsWith("\"'=HYPERLINK");
        assertThat(ExpenseCsvWriter.field("+91 call")).isEqualTo("'+91 call");
    }

    @Test
    void aMissingValueIsAnEmptyField() {
        assertThat(ExpenseCsvWriter.field(null)).isEmpty();
    }
}
