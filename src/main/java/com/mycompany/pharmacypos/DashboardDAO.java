package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Handles the SQL behind the three dashboard summary cards
 * (medicine count, total stock, total sales). No UI code here.
 */
public class DashboardDAO {

    /** Holder for the three dashboard numbers. Matches the original queries exactly,
     *  including that total sales was read with getInt() (truncated), not getDouble(). */
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
}