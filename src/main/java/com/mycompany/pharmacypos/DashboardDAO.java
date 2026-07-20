package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the SQL behind the Dashboard screen: today's revenue summary
 * cards, the Low Stock / Expiring Medicines alert lists, and (previously)
 * the old medicine/stock/sales totals. No UI code here.
 */
public class DashboardDAO {

    private final ReportsDAO reportsDAO = new ReportsDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    /** Holder for the three legacy dashboard numbers. Kept for backward
     *  compatibility; the Dashboard screen itself now uses getTodayStats(). */
    public static class DashboardStats {
        public final int medicineCount;
        public final int totalStock;
        public final int totalSales;

        public DashboardStats(int medicineCount, int totalStock, int totalSales) {
            this.medicineCount = medicineCount;
            this.totalStock = totalStock;
            this.totalSales = totalSales;
        }
    }

    public DashboardStats getStats() {
        int medicineCount = 0;
        int totalStock = 0;
        int totalSales = 0;

        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();

            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) FROM medicines");
            if (rs1.next()) {
                medicineCount = rs1.getInt(1);
            }
            rs1.close();

            ResultSet rs2 = st.executeQuery("SELECT SUM(quantity) FROM medicines");
            if (rs2.next()) {
                totalStock = rs2.getInt(1);
            }
            rs2.close();

            ResultSet rs3 = st.executeQuery("SELECT SUM(total_bill) FROM sales");
            if (rs3.next()) {
                totalSales = rs3.getInt(1);
            }
            rs3.close();

            st.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new DashboardStats(medicineCount, totalStock, totalSales);
    }

    /** Holder for the five "today" dashboard cards. */
    public static class TodayStats {
        public final double todayRevenue;
        public final int todayInvoices;
        public final double medicineFeeToday;
        public final double doctorFeeToday;
        public final double testFeeToday;

        public TodayStats(double todayRevenue, int todayInvoices, double medicineFeeToday,
                           double doctorFeeToday, double testFeeToday) {
            this.todayRevenue = todayRevenue;
            this.todayInvoices = todayInvoices;
            this.medicineFeeToday = medicineFeeToday;
            this.doctorFeeToday = doctorFeeToday;
            this.testFeeToday = testFeeToday;
        }
    }

    /** Today's revenue split by source, and today's invoice count - reuses the
     *  same queries as the Revenue Report / Sales Summary in Reports, just
     *  pinned to today's date so the two screens can never disagree. */
    public TodayStats getTodayStats() {
        LocalDate today = LocalDate.now();
        try {
            Object[] breakdown = reportsDAO.revenueBreakdown(today, today); // medFee, testFee, drFee, total
            Object[] summary = reportsDAO.salesSummary(today, today);       // invoiceCount, totalRevenue, avg

            double medicineFee = (double) breakdown[0];
            double testFee = (double) breakdown[1];
            double doctorFee = (double) breakdown[2];
            double total = (double) breakdown[3];
            int invoiceCount = (int) summary[0];

            return new TodayStats(total, invoiceCount, medicineFee, doctorFee, testFee);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new TodayStats(0, 0, 0, 0, 0);
        }
    }

    /** One line item for the Dashboard's Low Stock alert list. */
    public static class LowStockItem {
        public final String medicineName;
        public final int quantity;

        public LowStockItem(String medicineName, int quantity) {
            this.medicineName = medicineName;
            this.quantity = quantity;
        }
    }

    /** Same medicines/threshold as the Low Stock screen (MedicineDAO.loadLowStock),
     *  just returned as plain data instead of populated into a table. */
    public List<LowStockItem> getLowStockItems() {
        List<LowStockItem> items = new ArrayList<>();
        int threshold = appSettingsDAO.getLowStockThreshold();
        String sql = "SELECT medicine_name, quantity FROM medicines WHERE quantity < ? ORDER BY quantity ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new LowStockItem(rs.getString("medicine_name"), rs.getInt("quantity")));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }

    /** One line item for the Dashboard's Expiring Medicines alert list. */
    public static class ExpiringItem {
        public final String medicineName;
        public final String status;

        public ExpiringItem(String medicineName, String status) {
            this.medicineName = medicineName;
            this.status = status;
        }
    }

    /** Same status logic as the Expiry Alert screen (ExpiryAlert), but only the
     *  medicines that are actually EXPIRED or within 180 days of expiry - the
     *  ones worth surfacing as a Dashboard alert - ordered soonest-first. */
    public List<ExpiringItem> getExpiringItems() {
        List<ExpiringItem> items = new ArrayList<>();
        String sql = "SELECT medicine_name, expiry_date FROM medicines ORDER BY expiry_date ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            LocalDate today = LocalDate.now();
            while (rs.next()) {
                Date expirySql = rs.getDate("expiry_date");
                if (expirySql == null) continue;
                LocalDate expiry = expirySql.toLocalDate();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);

                String status;
                if (daysLeft < 0) {
                    status = "EXPIRED";
                } else if (daysLeft <= 180) {
                    status = daysLeft + " days left";
                } else {
                    continue; // not near-expiry - skip for the dashboard alert
                }

                items.add(new ExpiringItem(rs.getString("medicine_name"), status));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }
}
