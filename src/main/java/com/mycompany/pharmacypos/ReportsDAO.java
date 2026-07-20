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

    private boolean hasCreatedAtColumn(Connection con) throws SQLException {
        if (hasCreatedAtColumn != null) return hasCreatedAtColumn;
        try (ResultSet rs = con.getMetaData().getColumns(null, null, "medicines", "created_at")) {
            hasCreatedAtColumn = rs.next();
        }
        return hasCreatedAtColumn;
    }

    /**
     * Medicines added to inventory in range. Requires a "created_at" TIMESTAMP
     * column on the medicines table (not present in the original schema).
     * If it's missing, this throws a clear, actionable error instead of
     * silently showing wrong data - the screen surfaces that message with
     * the exact SQL needed to add it:
     *   ALTER TABLE medicines ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
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
            String sql = "SELECT medicine_name, company, distributor, batch_no, quantity, created_at " +
                         "FROM medicines WHERE created_at >= ? AND created_at < ? ORDER BY created_at DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(1, startOf(start));
                ps.setTimestamp(2, endOf(end));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[]{
                            rs.getString("medicine_name"), rs.getString("company"),
                            rs.getString("distributor"), rs.getString("batch_no"),
                            rs.getInt("quantity"), rs.getTimestamp("created_at")
                        });
                    }
                }
            }
        }
        return rows;
    }

    /** Per medicine: units sold and medicine-fee revenue, in range. Backs the
     *  "Medicines Sold", "Medicine-wise Sales", and "Most Sold Medicines" report
     *  entries, and is the base data for Total Medicines Sold / Total Quantity Sold. */
    public List<Object[]> medicineWiseSales(LocalDate start, LocalDate end) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT medicine_name, SUM(quantity) AS units_sold, SUM(total_bill) AS revenue " +
                     "FROM sales WHERE sale_date >= ? AND sale_date < ? AND medicine_name IS NOT NULL AND medicine_name <> '' " +
                     "GROUP BY medicine_name ORDER BY units_sold DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("medicine_name"), rs.getInt("units_sold"), rs.getDouble("revenue")
                    });
                }
            }
        }
        return rows;
    }

    /** Count of distinct medicines that had at least one sale in range. */
    public int totalMedicinesSold(LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COUNT(DISTINCT medicine_name) AS cnt FROM sales " +
                     "WHERE sale_date >= ? AND sale_date < ? AND medicine_name IS NOT NULL AND medicine_name <> ''";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }

    /** Total units of medicine sold (all medicines combined) in range. */
    public int totalQuantitySold(LocalDate start, LocalDate end) throws Exception {
        String sql = "SELECT COALESCE(SUM(quantity),0) AS total_qty FROM sales WHERE sale_date >= ? AND sale_date < ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, startOf(start));
            ps.setTimestamp(2, endOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total_qty") : 0;
            }
        }
    }

    /** Top N medicines by units sold in range (Most Sold Medicines). */
    public List<Object[]> mostSoldMedicines(LocalDate start, LocalDate end, int limit) throws Exception {
        List<Object[]> all = medicineWiseSales(start, end);
        return all.size() > limit ? all.subList(0, limit) : all;
    }
}
