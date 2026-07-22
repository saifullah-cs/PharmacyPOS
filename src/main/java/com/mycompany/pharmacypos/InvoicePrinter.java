package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.print.*;
import java.util.List;

import com.mycompany.pharmacypos.ReceiptFormatter.Align;
import com.mycompany.pharmacypos.ReceiptFormatter.ReceiptLine;

/**
 * Shows an on-screen preview of an invoice and sends it to an 80 mm thermal
 * printer.
 * <p>
 * All the "what goes on the receipt" work happens in {@link InvoiceGenerator}
 * (fetches the data) and {@link ReceiptFormatter} (turns it into lines).
 * This class only ever deals with displaying and printing those lines, so it
 * stays a thin presentation/printing layer that's easy to swap out (e.g. for
 * a raw ESC/POS driver) without touching how receipts are built.
 */
public class InvoicePrinter {

    public static void printInvoice(String invoiceNo) {
        InvoiceData data;
        try {
            data = InvoiceGenerator.buildInvoiceData(invoiceNo);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading invoice.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<ReceiptLine> lines = ReceiptFormatter.format(data);

        JFrame previewFrame = new JFrame("Invoice - " + invoiceNo);
        previewFrame.setLayout(new BorderLayout());

        JTextPane preview = new JTextPane();
        preview.setEditable(false);
        preview.setFont(new Font(ReceiptConfig.FONT_FAMILY, Font.PLAIN, 13));
        renderPreview(preview, lines);

        JScrollPane scrollPane = new JScrollPane(preview);
        scrollPane.setPreferredSize(new Dimension(360, 560));

        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> printToThermalPrinter(invoiceNo, lines, previewFrame));

        previewFrame.add(scrollPane, BorderLayout.CENTER);
        previewFrame.add(printBtn, BorderLayout.SOUTH);
        previewFrame.pack();
        previewFrame.setLocationRelativeTo(null);
        previewFrame.setVisible(true);
    }

    /** Renders the receipt lines into the preview pane, bolding the lines ReceiptFormatter flagged as bold. */
    private static void renderPreview(JTextPane preview, List<ReceiptLine> lines) {
        StyledDocument doc = preview.getStyledDocument();
        Style regular = preview.addStyle("regular", null);
        StyleConstants.setFontFamily(regular, ReceiptConfig.FONT_FAMILY);
        StyleConstants.setFontSize(regular, 13);

        Style bold = preview.addStyle("bold", regular);
        StyleConstants.setBold(bold, true);

        Style centerAttr = preview.addStyle("center", null);
        StyleConstants.setAlignment(centerAttr, StyleConstants.ALIGN_CENTER);
        Style leftAttr = preview.addStyle("left", null);
        StyleConstants.setAlignment(leftAttr, StyleConstants.ALIGN_LEFT);

        try {
            for (ReceiptLine line : lines) {
                int start = doc.getLength();
                doc.insertString(doc.getLength(), line.text + "\n", line.bold ? bold : regular);
                doc.setParagraphAttributes(start, line.text.length() + 1,
                        line.align == Align.CENTER ? centerAttr : leftAttr, false);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends the receipt straight to an 80 mm thermal printer: paper width is
     * fixed at 80 mm, but the height is computed from the actual number of
     * lines so there's no fixed page height, no leftover blank paper, and no
     * extra blank pages after the receipt content ends.
     */
    private static void printToThermalPrinter(String invoiceNo, List<ReceiptLine> lines, Component parent) {
        PrinterJob job = PrinterJob.getPrinterJob();

        javax.print.PrintService printService = resolvePrintService();
        if (printService == null) {
            JOptionPane.showMessageDialog(parent,
                    "No printer is available. Connect/turn on the thermal printer "
                            + "(and install its driver) and try again.",
                    "Printer Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            job.setPrintService(printService);
        } catch (PrinterException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Could not select printer '" + printService.getName() + "': " + ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double widthPt = ReceiptConfig.PAPER_WIDTH_MM * ReceiptConfig.MM_TO_PT;
        double sideMarginPt = ReceiptConfig.SIDE_MARGIN_MM * ReceiptConfig.MM_TO_PT;
        double topBottomMarginPt = ReceiptConfig.TOP_BOTTOM_MARGIN_MM * ReceiptConfig.MM_TO_PT;

        float lineHeight = ReceiptConfig.BODY_FONT_SIZE * ReceiptConfig.LINE_SPACING;
        double contentHeightPt = lines.size() * lineHeight;
        double heightPt = contentHeightPt + (2 * topBottomMarginPt);

        Paper paper = new Paper();
        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(sideMarginPt, topBottomMarginPt,
                widthPt - (2 * sideMarginPt), contentHeightPt);

        PageFormat pageFormat = new PageFormat();
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        pageFormat.setPaper(paper);

        Book book = new Book();
        book.append(new ThermalReceiptPrintable(lines, lineHeight), pageFormat);
        job.setPageable(book);

        // No print dialog: the printer was already resolved above (by name, or
        // the OS default), so clicking "Print" sends straight to the printer -
        // no further action needed.
        try {
            job.print();
        } catch (PrinterException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Failed to print invoice " + invoiceNo + ": " + ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Picks the printer to send the receipt to, with no user interaction:
     * the printer named in ReceiptConfig.PRINTER_NAME if one is configured
     * and currently available, otherwise whatever printer Windows has set
     * as the default. Returns null only if nothing is available at all
     * (e.g. no printer connected/installed yet).
     */
    private static javax.print.PrintService resolvePrintService() {
        javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);

        if (!ReceiptConfig.PRINTER_NAME.isEmpty()) {
            for (javax.print.PrintService service : services) {
                if (service.getName().equalsIgnoreCase(ReceiptConfig.PRINTER_NAME)) {
                    return service;
                }
            }
            // Configured name not found (e.g. printer off/unplugged) - fall through
            // to the default printer rather than failing outright.
        }

        return javax.print.PrintServiceLookup.lookupDefaultPrintService();
    }

    /** Draws the receipt lines directly onto the page - one page only, sized exactly to the content. */
    private static class ThermalReceiptPrintable implements Printable {

        private final List<ReceiptLine> lines;
        private final float lineHeight;

        ThermalReceiptPrintable(List<ReceiptLine> lines, float lineHeight) {
            this.lines = lines;
            this.lineHeight = lineHeight;
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            // Everything fits on one page: the page height was computed to
            // exactly match the content, so there's never a second page.
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            g2d.setColor(Color.BLACK);

            Font regularFont = new Font(ReceiptConfig.FONT_FAMILY, Font.PLAIN,
                    Math.round(ReceiptConfig.BODY_FONT_SIZE));
            Font boldFont = regularFont.deriveFont(Font.BOLD);

            double imageableWidth = pf.getImageableWidth();
            float y = ReceiptConfig.BODY_FONT_SIZE; // baseline for first line

            for (ReceiptLine line : lines) {
                Font font = line.bold ? boldFont : regularFont;
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics(font);

                float x;
                if (line.align == Align.CENTER) {
                    int textWidth = fm.stringWidth(line.text);
                    x = (float) Math.max(0, (imageableWidth - textWidth) / 2.0);
                } else {
                    x = 0;
                }

                g2d.drawString(line.text, x, y);
                y += lineHeight;
            }

            return PAGE_EXISTS;
        }
    }
}
