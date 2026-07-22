package com.mycompany.pharmacypos;

/**
 * Central place for everything that shapes the printed receipt: clinic
 * details, footer text and the physical/character layout of the 80 mm
 * thermal paper.
 *
 * Keeping this separate (instead of hard-coding strings inside the printer)
 * is what makes future changes - a different clinic name, a logo, a GST/VAT
 * line, a QR code, a discount line, etc. - a one-file edit instead of a
 * surgery on the printing code.
 */
public class ReceiptConfig {

    // ---- Header -------------------------------------------------------
    public static final String CLINIC_NAME = "HEALTH HAVEN CLINIC";
    public static final String CLINIC_ADDRESS_LINE1 = "Baharwal Chowk, Guliana Rd,";
    public static final String CLINIC_ADDRESS_LINE2 = "Opp. Almudassir Trust, Kharian, Pakistan";
    public static final String CLINIC_PHONE = "0370-4433405";

    // ---- Footer ---------------------------------------------------------
    // Phone/complaint line dropped from the footer - the phone number is
    // already shown once in the header, no need to repeat it.
    public static final String FOOTER_LINE1 = "Thank you for choosing us.";

    // ---- Currency ---------------------------------------------------------
    public static final String CURRENCY_PREFIX = "Rs. ";

    // ---- Layout (character grid for a standard 80 mm / 58-column thermal
    // roll printed with a monospaced font). Column widths below add up to
    // the receipt width and are shared by the on-screen preview and the
    // physical print, so both always match. ----
    public static final int CHAR_WIDTH = 42;

    public static final int COL_NAME = 16;
    public static final int COL_QTY = 5;
    public static final int COL_PRICE = 9;
    public static final int COL_TOTAL = 12;

    // ---- Physical paper (points, 1 mm = 2.8346 pt) ----
    public static final double MM_TO_PT = 2.8346457;
    public static final double PAPER_WIDTH_MM = 80.0;
    public static final double SIDE_MARGIN_MM = 3.0;
    public static final double TOP_BOTTOM_MARGIN_MM = 4.0;

    // ---- Target printer ----
    // Exact name of the thermal printer as it appears in Windows'
    // "Printers & Scanners" list (e.g. "XP-58" or "POS-80 Series").
    // Leave blank ("") to just use whatever printer is set as the
    // Windows default - either way, printing happens with no dialog
    // and no extra clicks.
    public static final String PRINTER_NAME = "";

    // ---- Fonts ----
    public static final String FONT_FAMILY = "Monospaced";
    public static final float BODY_FONT_SIZE = 9f;
    public static final float HEADER_FONT_SIZE = 11f;
    public static final float LINE_SPACING = 1.25f; // multiplier on font size

    private ReceiptConfig() {
    }
}
