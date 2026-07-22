package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All read-only queries backing the Reports screen. Every method takes an
 * inclusive [start, end] LocalDate range - the screen turns the chosen date
 * filter (Today / Last 7 Days / Custom Range / etc.) into a concrete
 * start/end pair before calling in here, so this class only ever deals with
 * a resolved range and stays independent of how that range was picked.
 *
 * Test fees live in "sale_tests", keyed by invoice_no with no date of their
 * own, so every test-fee query joins back to "sales" (via MIN(sale_date) per
 * invoice) to know which range each test fee falls into.
 */
public class ReportsDAO {

    private static Timestamp startOf(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    /** Exclusive upper bound: the instant right after the end date's last millisecond. */
    private static Timestamp endOf(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }

    // ============================================================
    //  REVENUE REPORTS
    // ============================================================

    /** One row per calendar day in range: date, medicine fee, test fee, doctor fee, total. */
    public List<Object[]> dailyRevenue(LocalDate start, LocalDate end) throws Exception {
        return groupedRevenue(start, end, "DATE(s.sale_date)", "DATE(inv.inv_date)");
    }

    /** One row per ISO week in range: week label (e.g. 2026-W29), medicine fee, test fee, doctor fee, total. */
    public List<Object[]> weeklyRevenue(LocalDate start, LocalDate end) throws Exception {
        return groupedRevenue(start, end,
                "CONCAT(YEAR(s.sale_date), '-W', LPAD(WEEK(s.sale_date, 3), 2, '0'))",
                "CONCAT(YEAR(inv.inv_date), '-W', LPAD(WEEK(inv.inv_date, 3), 2, '0'))");
    }

    /** One row per calendar month in range: month label (e.g. 2026-07), medicine fee, test fee, doctor fee, total. */
    public List<Object[]> monthlyRevenue(LocalDate start, LocalDate end) throws Exception {
        return groupedRevenue(start, end,
                "DATE_FORMAT(s.sale_date, '%Y-%m')",
                "DATE_FORMAT(inv.inv_date, '%Y-%m')");
    }

    /** Shared implementation for dailyRevenue/weeklyRevenue/monthlyRevenue - only the
     *  grouping expression (applied to "sales" as saleGroupExpr, and to the invoice-date
     *  subquery as testGroupExpr) differs between them. */
    private List<Object[]> groupedRevenue(LocalDate start, LocalDate end, String saleGroupExpr, String testGroupExpr) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        String sql =
            "SELECT " + saleGroupExpr + " AS bucket, " +
            "SUM(s.total_bill) AS medicine_fee, " +
            "SUM(s.dr_fee) AS doctor_fee, " +
            "COALESCE((SELECT SUM(st.price) FROM sale_tests st " +
            "          JOIN (SELECT invoice_no, MIN(sale_date) AS inv_date FROM sales GROUP BY invoice_no) inv " +
            "          ON st.invoice_no = inv.invoice_no WHERE " + testGroupExpr + " = " + saleGroupExpr + "), 0) AS test_fee " +
            "FROM sales s WHERE s.sale_date >= ? AND s.sale_date < ? " +
            "GROUP BY bucket ORDER BY bucket DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double medicineFee = rs.getDouble("medicine_fee");
                    double doctorFee = rs.getDouble("doctor_fee");
                    double testFee = rs.getDouble("test_fee");
                    rows.add(new Object[]{
                        rs.getString("bucket"), medicineFee, testFee, doctorFee, medicineFee + testFee + doctorFee
                    });
                }
            }
        }
        return rows;
    }

    /** Total charged for medicines only (Sale Price x Quantity, summed), in range. */
    public double medicineRevenue(LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COALESCE(SUM(total_bill),0) AS total FROM sales WHERE sale_date >= ? AND sale_date < ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0;
            }
        }
    }

    /** Total Doctor Fee collected in range. */
    public double doctorFeeRevenue(LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COALESCE(SUM(dr_fee),0) AS total FROM sales WHERE sale_date >= ? AND sale_date < ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0;
            }
        }
    }

    /** Total Test Fee collected in range (tests attributed to their invoice's sale date). */
    public double testFeeRevenue(LocalDate start, LocalDate end) throws Exception {
        String sql =
            "SELECT COALESCE(SUM(st.price),0) AS total FROM sale_tests st " +
            "JOIN (SELECT invoice_no, MIN(sale_date) AS inv_date FROM sales GROUP BY invoice_no) inv " +
            "ON st.invoice_no = inv.invoice_no " +
            "WHERE inv.inv_date >= ? AND inv.inv_date < ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("total") : 0;
            }
        }
    }

    /** Medicine Fee, Test Fee, Doctor Fee, and their sum, for the range - one row. */
    public Object[] revenueBreakdown(LocalDate start, LocalDate end) throws Exception {
        double medicineFee = medicineRevenue(start, end);
        double doctorFee = doctorFeeRevenue(start, end);
        double testFee = testFeeRevenue(start, end);
        return new Object[]{ medicineFee, testFee, doctorFee, medicineFee + testFee + doctorFee };
    }

    /** Invoice count, total revenue, and average revenue per invoice, for the range. */
    public Object[] salesSummary(LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COUNT(DISTINCT invoice_no) AS invoice_count FROM sales WHERE sale_date >= ? AND sale_date < ?";
        int invoiceCount = 0;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) invoiceCount = rs.getInt("invoice_count");
            }
        }
        Object[] breakdown = revenueBreakdown(start, end);
        double totalRevenue = (double) breakdown[3];
        double avgPerInvoice = invoiceCount > 0 ? totalRevenue / invoiceCount : 0;
        return new Object[]{ invoiceCount, totalRevenue, avgPerInvoice };
    }

    // ============================================================
    //  MEDICINE REPORTS
    // ============================================================

    /** True once, cached, so repeated calls to newlyAddedStock() don't re-check schema every time. */
    private static Boolean hasCreatedAtColumn = null;
    private static Boolean hasQuantityAddedColumn = null;

    private boolean hasCreatedAtColumn(Connection con) throws SQLException {
        if (hasCreatedAtColumn != null) return hasCreatedAtColumn;
        try (ResultSet rs = con.getMetaData().getColumns(null, null, "medicines", "created_at")) {
            hasCreatedAtColumn = rs.next();
        }
        return hasCreatedAtColumn;
    }

    private boolean hasQuantityAddedColumn(Connection con) throws SQLException {
        if (hasQuantityAddedColumn != null) return hasQuantityAddedColumn;
        try (ResultSet rs = con.getMetaData().getColumns(null, null, "medicines", "quantity_added")) {
            hasQuantityAddedColumn = rs.next();
        }
        return hasQuantityAddedColumn;
    }

    /**
     * Medicines added to inventory in range. Requires a "created_at" TIMESTAMP
     * column and a "quantity_added" column on the medicines table (neither
     * present in the original schema). If either is missing, this throws a
     * clear, actionable error instead of silently showing wrong data - the
     * screen surfaces that message with the exact SQL needed to add it.
     *
     * "quantity_added" is the original quantity added to inventory and is
     * never decremented by sales, unlike "quantity" (the live remaining
     * stock) - so this report always shows what was actually added, not
     * what's currently left.
     */
    public List<Object[]> newlyAddedStock(LocalDate start, LocalDate end) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            if (!hasCreatedAtColumn(con)) {
                throw new IllegalStateException(
                    "This report needs a \"created_at\" column on the medicines table to know when " +
                    "each medicine was added. Ask your DB admin to run:\n\n" +
                    "ALTER TABLE medicines ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;\n\n" +
                    "Existing rows will show as added \"now\" - only medicines added after that point " +
                    "will have an accurate date.");
            }
            if (!hasQuantityAddedColumn(con)) {
                throw new IllegalStateException(
                    "This report needs a \"quantity_added\" column on the medicines table so it can show " +
                    "the original quantity added instead of the current remaining stock. Ask your DB " +
                    "admin to run:\n\n" +
                    "ALTER TABLE medicines ADD COLUMN quantity_added INT NOT NULL DEFAULT 0;\n" +
                    "UPDATE medicines SET quantity_added = quantity;\n\n" +
                    "That backfills existing rows with their current quantity as a starting point - only " +
                    "medicines added or restocked after that point will have a fully accurate figure.");
            }
            String sql = "SELECT medicine_name, company, distributor, batch_no, purchase_price, quantity_added, created_at " +
                         "FROM medicines WHERE created_at >= ? AND created_at < ? ORDER BY created_at DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(1, startOf(start));
                ps.setTimestamp(2, endOf(end));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{
                            rs.getString("medicine_name"), rs.getString("company"),
                            rs.getString("distributor"), rs.getString("batch_no"),
                            rs.getDouble("purchase_price"),
                            rs.getInt("quantity_added"), rs.getTimestamp("created_at")
                        });
                    }
                }
            }
        }
        return rows;
    }

    /** Manual quantity adjustments made on the Medicine Management screen, in
     *  range - the audit trail behind the "Inventory Adjustment History" report. */
    public List<Object[]> inventoryAdjustments(LocalDate start, LocalDate end) throws Exception {
        return new StockAdjustmentDAO().loadAdjustments(start, end);
    }

    /** True once, cached, so repeated calls to medicineSalesReport() don't re-check
     *  schema every time. Mirrors SalesDAO's own cached check for the same column -
     *  kept as a separate cache here since this class doesn't share state with SalesDAO. */
    private static Boolean hasSalesCompanyColumn = null;

    private boolean hasSalesCompanyColumn(Connection con) throws SQLException {
        if (hasSalesCompanyColumn != null) return hasSalesCompanyColumn;
        try (ResultSet rs = con.getMetaData().getColumns(null, null, "sales", "company")) {
            hasSalesCompanyColumn = rs.next();
        }
        return hasSalesCompanyColumn;
    }

    /**
     * One row per medicine line item sold in range - the full detail behind the
     * "Medicine Sales Report". "sales" only stores what was charged (medicine
     * name, quantity, sale price, expiry date, and - since the Medicine Search &
     * Company Identification change - company); it never stores purchase price
     * or batch/distributor, so this joins back to "medicines" (matched on
     * medicine_name + expiry_date, which together identify the exact batch that
     * was sold) to pull those in. That join picks one medicines row per
     * (medicine_name, expiry_date) - via MIN(id) - so a sale never fans out into
     * duplicate rows even if the same medicine/expiry combination was added to
     * inventory more than once.
     *
     * Columns: Invoice No, Sale Date & Time, Medicine Name, Company, Quantity
     * Sold, Purchase Price, Sale Price, Profit/Unit, Total Profit, Expiry Date,
     * Batch No, Distributor.
     *
     * If a sold medicine can no longer be matched back to a "medicines" row
     * (e.g. it was later deleted from inventory), Purchase Price/Batch No/
     * Distributor come back blank/zero for that row rather than dropping the
     * sale from the report; Company still shows correctly as long as it was
     * captured on the sale itself.
     */
    public List<Object[]> medicineSalesReport(LocalDate start, LocalDate end) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            // Sales made after the Medicine Search & Company Identification change record
            // their own company directly on "sales" - that's the exact manufacturer that
            // was actually sold, so it's preferred over the medicines-table join below
            // whenever it's available. Older sale rows (or installs that haven't added the
            // column yet) fall back to the best-effort medicines-table match.
            boolean hasSalesCompany = hasSalesCompanyColumn(con);
            String companyExpr = hasSalesCompany ? "COALESCE(s.company, m.company)" : "m.company";
            String sql =
                "SELECT s.invoice_no, s.sale_date, s.medicine_name, " + companyExpr + " AS company, s.quantity, " +
                "m.purchase_price, s.sale_price, s.expiry_date, m.batch_no, m.distributor " +
                "FROM sales s " +
                "LEFT JOIN (SELECT medicine_name, expiry_date, MIN(id) AS id FROM medicines " +
                "           GROUP BY medicine_name, expiry_date) mm " +
                "  ON mm.medicine_name = s.medicine_name AND mm.expiry_date = s.expiry_date " +
                "LEFT JOIN medicines m ON m.id = mm.id " +
                "WHERE s.sale_date >= ? AND s.sale_date < ? AND s.medicine_name IS NOT NULL AND s.medicine_name <> '' " +
                "ORDER BY s.sale_date DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(1, startOf(start));
                ps.setTimestamp(2, endOf(end));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int quantity = rs.getInt("quantity");
                        double purchasePrice = rs.getDouble("purchase_price");
                        double salePrice = rs.getDouble("sale_price");
                        double profitPerUnit = salePrice - purchasePrice;
                        rows.add(new Object[]{
                            rs.getString("invoice_no"), rs.getTimestamp("sale_date"),
                            rs.getString("medicine_name"), rs.getString("company"),
                            quantity, purchasePrice, salePrice, profitPerUnit, profitPerUnit * quantity,
                            rs.getDate("expiry_date"), rs.getString("batch_no"), rs.getString("distributor")
                        });
                    }
                }
            }
        }
        return rows;
    }

    /** Totals for the "Medicine Sales Report" summary section, derived from that
     *  report's own rows so the numbers can never drift out of sync with the
     *  detail table above it: distinct medicines sold, total units sold, total
     *  medicine revenue (sale price x quantity), and total profit. */
    public Object[] medicineSalesSummary(List<Object[]> medicineSalesRows) {
        java.util.Set<Object> distinctMedicines = new java.util.HashSet<>();
        int totalQuantity = 0;
        double totalRevenue = 0;
        double totalProfit = 0;
        for (Object[] row : medicineSalesRows) {
            distinctMedicines.add(row[2]);
            int quantity = (int) row[4];
            double salePrice = (double) row[6];
            totalQuantity += quantity;
            totalRevenue += salePrice * quantity;
            totalProfit += (double) row[8];
        }
        return new Object[]{ distinctMedicines.size(), totalQuantity, totalRevenue, totalProfit };
    }
}
