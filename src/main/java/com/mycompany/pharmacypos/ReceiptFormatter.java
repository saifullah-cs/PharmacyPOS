package com.mycompany.pharmacypos;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an {@link InvoiceData} into a flat list of {@link ReceiptLine}s -
 * plain text plus a couple of presentation hints (bold, centered).
 * <p>
 * This is the single source of truth for how a receipt looks. Both the
 * on-screen preview and the physical 80 mm print consume the same list, so
 * what the user sees before printing is exactly what comes out of the
 * printer.
 */
public class ReceiptFormatter {

    public enum Align { LEFT, CENTER }

    public static class ReceiptLine {
        public final String text;
        public final boolean bold;
        public final Align align;

        public ReceiptLine(String text, boolean bold, Align align) {
            this.text = text;
            this.bold = bold;
            this.align = align;
        }
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
    // Thin rule: separates rows/sub-sections within one block (table header, medicine rows).
    private static final String THIN_RULE = repeat('-', ReceiptConfig.CHAR_WIDTH);
    // Double rule: stronger break between the receipt's major sections (header / table / totals).
    private static final String DOUBLE_RULE = repeat('=', ReceiptConfig.CHAR_WIDTH);

    public static List<ReceiptLine> format(InvoiceData data) {
        List<ReceiptLine> lines = new ArrayList<>();

        // ---- Header ----
        lines.add(left(DOUBLE_RULE, false));
        lines.add(center(ReceiptConfig.CLINIC_NAME, true));
        lines.add(center(ReceiptConfig.CLINIC_ADDRESS_LINE1, false));
        lines.add(center(ReceiptConfig.CLINIC_ADDRESS_LINE2, false));
        lines.add(center("Ph: " + ReceiptConfig.CLINIC_PHONE, false));
        lines.add(left(DOUBLE_RULE, false));
        lines.add(left("Invoice No : " + data.invoiceNo, false));
        lines.add(left("Date       : " + data.dateTime.format(DATE_FMT), false));
        lines.add(left(DOUBLE_RULE, false));

        // ---- Medicines table ----
        lines.add(left(tableRow("Medicine", "Qty", "Price", "Total"), false));
        lines.add(left(THIN_RULE, false));

        if (data.medicines.isEmpty()) {
            lines.add(left("(no medicines on this invoice)", false));
        } else {
            for (InvoiceData.MedicineLine m : data.medicines) {
                addMedicineRow(lines, m);
            }
        }
        lines.add(left(DOUBLE_RULE, false));

        // ---- Totals ----
        lines.add(left(totalRow("Medicine Total", plainAmount(data.getMedicineTotal())), false));
        if (data.hasDoctorFee()) {
            lines.add(left(totalRow("Doctor Fee", plainAmount(data.doctorFee)), false));
        }
        if (data.hasTests()) {
            lines.add(left(totalRow("Test Charges", plainAmount(data.getTestTotal())), false));
        }
        lines.add(left(THIN_RULE, false));
        lines.add(left(totalRow("Grand Total", currencyAmount(data.getGrandTotal())), true));
        lines.add(left(DOUBLE_RULE, false));

        // ---- Footer ----
        lines.add(left("", false));
        lines.add(center(ReceiptConfig.FOOTER_LINE1, false));

        return lines;
    }

    // ---------------------------------------------------------------

    private static void addMedicineRow(List<ReceiptLine> lines, InvoiceData.MedicineLine m) {
        List<String> nameChunks = wrap(m.name, ReceiptConfig.COL_NAME);

        if (nameChunks.size() == 1) {
            lines.add(left(tableRow(
                    nameChunks.get(0),
                    String.valueOf(m.qty),
                    plainAmount(m.price),
                    plainAmount(m.total)), false));
            return;
        }

        // Name doesn't fit in one column: print it on its own line(s),
        // then the quantity/price/total right-aligned on the next line.
        for (String chunk : nameChunks) {
            lines.add(left(chunk, false));
        }
        lines.add(left(tableRow("", String.valueOf(m.qty), plainAmount(m.price), plainAmount(m.total)), false));
    }

    private static String tableRow(String name, String qty, String price, String total) {
        return String.format("%-" + ReceiptConfig.COL_NAME + "s%" + ReceiptConfig.COL_QTY + "s%"
                        + ReceiptConfig.COL_PRICE + "s%" + ReceiptConfig.COL_TOTAL + "s",
                name, qty, price, total);
    }

    private static String totalRow(String label, String formattedAmount) {
        String left = String.format("%-14s", label + " ");
        String right = String.format("%" + (ReceiptConfig.CHAR_WIDTH - left.length()) + "s",
                ": " + formattedAmount);
        return left + right;
    }

    /** Plain number, no currency prefix - used everywhere except the Grand Total. */
    private static String plainAmount(double amount) {
        return String.format("%.2f", amount);
    }

    /** "Rs. " prefixed amount - reserved for the Grand Total only. */
    private static String currencyAmount(double amount) {
        return ReceiptConfig.CURRENCY_PREFIX + String.format("%.2f", amount);
    }

    /** Breaks a long name into chunks that fit within the given width, breaking on spaces where possible. */
    private static List<String> wrap(String text, int width) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            text = "";
        }
        text = text.trim();
        if (text.length() <= width) {
            chunks.add(text);
            return chunks;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                // single word longer than the whole column - hard-break it
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                chunks.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                chunks.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        // Cap runaway names at 2 lines so a single item never overwhelms the receipt.
        if (chunks.size() > 2) {
            chunks = chunks.subList(0, 2);
        }
        return chunks;
    }

    private static ReceiptLine left(String text, boolean bold) {
        return new ReceiptLine(text, bold, Align.LEFT);
    }

    private static ReceiptLine center(String text, boolean bold) {
        return new ReceiptLine(text, bold, Align.CENTER);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
