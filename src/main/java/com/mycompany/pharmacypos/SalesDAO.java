package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles all database operations related to selling medicines:
 * medicine lookup/search for billing, stock updates, sale records,
 * and invoice numbering. No UI code here.
 */
public class SalesDAO {

    /** One line item on a printed invoice. */
    public static class SaleLineItem {
        public final String medicineName;
        public final int quantity;
        public final double salePrice;
        public final double totalBill;

        public SaleLineItem(String medicineName, int quantity, double salePrice, double totalBill) {
            this.medicineName = medicineName;
            this.quantity = quantity;
            this.salePrice = salePrice;
            this.totalBill = totalBill;
        }
    }

    /** Same query InvoicePrinter used: every line item belonging to one invoice number. */
    public List<SaleLineItem> getSalesByInvoice(String invoiceNo) {
        List<SaleLineItem> items = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM sales WHERE invoice_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                items.add(new SaleLineItem(
                        rs.getString("medicine_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("sale_price"),
                        rs.getDouble("total_bill")
                ));
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }

    /** Simple holder for the fields SalesFrame needs when a medicine is looked up. */
    public static class MedicineInfo {
        public final String name;
        public final int quantity;
        public final double salePrice;

        public MedicineInfo(String name, int quantity, double salePrice) {
            this.name = name;
            this.quantity = quantity;
            this.salePrice = salePrice;
        }
    }

    /** Same lookup used by the Sales screen's Search button: first match containing the text. */
    public MedicineInfo findMedicineByName(String searchText) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM medicines WHERE medicine_name LIKE ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, "%" + searchText + "%");
            ResultSet rs = pst.executeQuery();

            MedicineInfo result = null;
            if (rs.next()) {
                result = new MedicineInfo(
                        rs.getString("medicine_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("sale_price")
                );
            }
            rs.close();
            pst.close();
            con.close();
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /** Same lookup used by the autocomplete dropdown: names starting with the typed text, limit 10. */
    public List<String> suggestMedicineNames(String prefixText) {
        List<String> names = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement pst = con.prepareStatement(
                    "SELECT medicine_name FROM medicines WHERE medicine_name LIKE ? LIMIT 10"
            );
            pst.setString(1, prefixText + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("medicine_name"));
            }
            rs.close();
            pst.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return names;
    }

    public String getExpiryDate(String medicineName) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT expiry_date FROM medicines WHERE medicine_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, medicineName);
            ResultSet rs = ps.executeQuery();
            String expiry = null;
            if (rs.next()) {
                expiry = rs.getDate("expiry_date").toString();
            }
            rs.close();
            ps.close();
            con.close();
            return expiry;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean insertSaleRecord(String invoiceNo, String medicine, int qty,
                                     double price, double total, String expiryDate, String cashier, double drFee) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO sales (invoice_no, medicine_name, quantity, sale_price, total_bill, expiry_date, cashier, dr_fee) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ps.setString(2, medicine);
            ps.setInt(3, qty);
            ps.setDouble(4, price);
            ps.setDouble(5, total);
            ps.setString(6, expiryDate);
            ps.setString(7, cashier);
            ps.setDouble(8, drFee);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean updateStock(String medicineName, int qtySold) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE medicines SET quantity = quantity - ? WHERE medicine_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, qtySold);
            ps.setString(2, medicineName);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Same query InvoiceGenerator used: last invoice number saved, or null if no sales yet. */
    /** Same query as HomeFrame's Sales History screen used: all sales, newest first.
     *  Returns the running total (sum of the Grand-Total-matching total bill),
     *  matching what's shown on the Sales screen and printed invoice.
     *  Throws on failure so the caller can show the same error dialog as before. */
    public double loadSalesHistory(javax.swing.table.DefaultTableModel model) throws Exception {
        return loadSalesHistory(model, null);
    }

    /** Same as above, but filtered to sales whose medicine name, invoice number,
     *  or sale date contains the given search text. Null/empty text loads everything.
     *
     *  The "sales" table stores one row per medicine line item, with dr_fee saved
     *  only once (on the first row of each invoice, 0 elsewhere) so it isn't
     *  duplicated across rows - see InvoiceService.saveSale(). Test fees live
     *  entirely in a separate "sale_tests" table, keyed by invoice_no.
     *
     *  To make the displayed Total Bill match the Grand Total for the invoice
     *  (medicine total + Dr Fee + Test Fee(s)) without double-counting when an
     *  invoice has multiple medicine rows, the Dr Fee and Test Fee are folded in
     *  the same way dr_fee already is: only on that invoice's first row, 0 on
     *  the rest. Summing the displayed Total Bill column therefore still equals
     *  the true grand total across all invoices, not an inflated one. */
    public double loadSalesHistory(javax.swing.table.DefaultTableModel model, String searchText) throws Exception {
        double totalSales = 0;

        Connection con = DBConnection.getConnection();

        // First id per invoice, so we know which row is the "first row" that
        // already carries the Dr Fee - Test Fee(s) get folded in on that same row.
        java.util.Map<String, Integer> firstIdByInvoice = new java.util.HashMap<>();
        PreparedStatement minPs = con.prepareStatement(
                "SELECT invoice_no, MIN(id) AS min_id FROM sales GROUP BY invoice_no");
        ResultSet minRs = minPs.executeQuery();
        while (minRs.next()) {
            firstIdByInvoice.put(minRs.getString("invoice_no"), minRs.getInt("min_id"));
        }
        minRs.close();
        minPs.close();

        // Total test fees per invoice.
        java.util.Map<String, Double> testFeeByInvoice = new java.util.HashMap<>();
        PreparedStatement testPs = con.prepareStatement(
                "SELECT invoice_no, SUM(price) AS total_price FROM sale_tests GROUP BY invoice_no");
        ResultSet testRs = testPs.executeQuery();
        while (testRs.next()) {
            testFeeByInvoice.put(testRs.getString("invoice_no"), testRs.getDouble("total_price"));
        }
        testRs.close();
        testPs.close();

        String sql;
        PreparedStatement pst;

        if (searchText == null || searchText.trim().isEmpty()) {
            sql = "SELECT * FROM sales ORDER BY sale_date DESC";
            pst = con.prepareStatement(sql);
        } else {
            sql = "SELECT * FROM sales WHERE medicine_name LIKE ? " +
                  "OR invoice_no LIKE ? " +
                  "OR CAST(sale_date AS CHAR) LIKE ? " +
                  "ORDER BY sale_date DESC";
            pst = con.prepareStatement(sql);
            String pattern = "%" + searchText.trim() + "%";
            pst.setString(1, pattern);
            pst.setString(2, pattern);
            pst.setString(3, pattern);
        }

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String invoiceNo = rs.getString("invoice_no");
            double medicineTotal = rs.getDouble("total_bill");
            double drFee = rs.getDouble("dr_fee");

            Integer firstId = firstIdByInvoice.get(invoiceNo);
            boolean isFirstRowOfInvoice = firstId != null && firstId == id;
            double testFee = isFirstRowOfInvoice
                    ? testFeeByInvoice.getOrDefault(invoiceNo, 0.0)
                    : 0.0;

            double grandTotalBill = medicineTotal + drFee + testFee;

            model.addRow(new Object[]{
                id,
                rs.getString("medicine_name"),
                rs.getInt("quantity"),
                rs.getDouble("sale_price"),
                grandTotalBill,
                invoiceNo,
                rs.getDate("expiry_date"),
                rs.getTimestamp("sale_date"),
                drFee,
                testFee
            });
            totalSales += grandTotalBill;
        }

        rs.close();
        pst.close();
        con.close();

        return totalSales;
    }

    /** One summarized row per invoice (Sale Date, Cashier, Dr Fee, Test Fee, Grand Total),
     *  newest first, for the Sales History screen. Unlike loadSalesHistory() above,
     *  this does not list each medicine line item separately - use
     *  getSalesByInvoice(invoiceNo) / InvoicePrinter.printInvoice(invoiceNo) to see
     *  the full breakdown for one invoice.
     *  Returns the running total (sum of each invoice's Grand Total). Throws on
     *  failure so the caller can show the same error dialog as before. */
    public double loadSalesHistoryByInvoice(javax.swing.table.DefaultTableModel model, String searchText) throws Exception {
        double totalSales = 0;

        Connection con = DBConnection.getConnection();

        // Total test fees per invoice (tests live in a separate table, keyed by invoice_no).
        java.util.Map<String, Double> testFeeByInvoice = new java.util.HashMap<>();
        PreparedStatement testPs = con.prepareStatement(
                "SELECT invoice_no, SUM(price) AS total_price FROM sale_tests GROUP BY invoice_no");
        ResultSet testRs = testPs.executeQuery();
        while (testRs.next()) {
            testFeeByInvoice.put(testRs.getString("invoice_no"), testRs.getDouble("total_price"));
        }
        testRs.close();
        testPs.close();

        // dr_fee is only ever non-zero on one row per invoice (see InvoiceService.saveSale),
        // so MAX(dr_fee) recovers the invoice's actual Dr Fee regardless of how many
        // medicine rows it has.
        String sql = "SELECT invoice_no, MIN(sale_date) AS sale_date, " +
                     "SUM(total_bill) AS medicine_total, MAX(dr_fee) AS dr_fee FROM sales ";
        boolean filtering = searchText != null && !searchText.trim().isEmpty();
        if (filtering) {
            sql += "WHERE invoice_no IN (SELECT invoice_no FROM sales WHERE medicine_name LIKE ? " +
                   "OR invoice_no LIKE ? OR CAST(sale_date AS CHAR) LIKE ?) ";
        }
        sql += "GROUP BY invoice_no ORDER BY sale_date DESC";

        PreparedStatement pst = con.prepareStatement(sql);
        if (filtering) {
            String pattern = "%" + searchText.trim() + "%";
            pst.setString(1, pattern);
            pst.setString(2, pattern);
            pst.setString(3, pattern);
        }

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            String invoiceNo = rs.getString("invoice_no");
            double medicineTotal = rs.getDouble("medicine_total");
            double drFee = rs.getDouble("dr_fee");
            double testFee = testFeeByInvoice.getOrDefault(invoiceNo, 0.0);
            double grandTotalBill = medicineTotal + drFee + testFee;

            model.addRow(new Object[]{
                invoiceNo,
                medicineTotal,
                testFee,
                drFee,
                grandTotalBill,
                rs.getTimestamp("sale_date"),
                "View"
            });
            totalSales += grandTotalBill;
        }

        rs.close();
        pst.close();
        con.close();

        return totalSales;
    }

    /** One row per invoice number, quantity, sale price, total bill and time. */
    public static class RecentSale {
        public final String invoiceNo;
        public final double totalBill;
        public final Timestamp time;

        public RecentSale(String invoiceNo, double totalBill, Timestamp time) {
            this.invoiceNo = invoiceNo;
            this.totalBill = totalBill;
            this.time = time;
        }
    }

    /** The most recent completed sales (newest first), one row per invoice, with
     *  the full Grand Total (Medicine + Dr Fee + Test Fee) - for the Dashboard's
     *  Recent Activity table. */
    public List<RecentSale> getRecentSales(int limit) {
        List<RecentSale> recent = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            Map<String, Double> testFeeByInvoice = new java.util.HashMap<>();
            try (PreparedStatement testPs = con.prepareStatement(
                    "SELECT invoice_no, SUM(price) AS total_price FROM sale_tests GROUP BY invoice_no");
                 ResultSet testRs = testPs.executeQuery()) {
                while (testRs.next()) {
                    testFeeByInvoice.put(testRs.getString("invoice_no"), testRs.getDouble("total_price"));
                }
            }

            String sql = "SELECT invoice_no, MAX(sale_date) AS sale_date, " +
                         "SUM(total_bill) AS medicine_total, MAX(dr_fee) AS dr_fee FROM sales " +
                         "GROUP BY invoice_no ORDER BY sale_date DESC LIMIT ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, limit);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        String invoiceNo = rs.getString("invoice_no");
                        double medicineTotal = rs.getDouble("medicine_total");
                        double drFee = rs.getDouble("dr_fee");
                        double testFee = testFeeByInvoice.getOrDefault(invoiceNo, 0.0);
                        double grandTotal = medicineTotal + drFee + testFee;
                        recent.add(new RecentSale(invoiceNo, grandTotal, rs.getTimestamp("sale_date")));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return recent;
    }

    /** The Dr Fee stored for a given invoice (it's saved once, on the first row of that invoice). */
    public double getDoctorFeeForInvoice(String invoiceNo) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT MAX(dr_fee) AS fee FROM sales WHERE invoice_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();

            double fee = 0;
            if (rs.next()) {
                fee = rs.getDouble("fee");
            }

            rs.close();
            ps.close();
            con.close();
            return fee;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public String getLastInvoiceNo() {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT invoice_no FROM sales ORDER BY id DESC LIMIT 1";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            String last = null;
            if (rs.next()) {
                last = rs.getString("invoice_no");
            }
            rs.close();
            pst.close();
            con.close();
            return last;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}