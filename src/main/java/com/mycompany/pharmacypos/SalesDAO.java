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

    /** Simple holder for the fields SalesFrame needs when a medicine is looked up.
     *  id is the medicines table primary key - the one field guaranteed to identify
     *  this exact row, even when another row shares the same name and company (e.g.
     *  a different batch). SalesFrame keys everything (bill-row merging, stock
     *  reservation lookups) off id rather than off name/company text. */
    public static class MedicineInfo {
        public final int id;
        public final String name;
        public final String company;
        public final int quantity;
        public final double salePrice;

        public MedicineInfo(int id, String name, String company, int quantity, double salePrice) {
            this.id = id;
            this.name = name;
            this.company = company;
            this.quantity = quantity;
            this.salePrice = salePrice;
        }
    }

    /** Same lookup used by the Sales screen's Search button: first match containing the text
     *  (across any company). Kept for typed-and-clicked searches where no specific suggestion
     *  (and therefore no specific company/id) was chosen. */
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
                        rs.getInt("id"),
                        rs.getString("medicine_name"),
                        rs.getString("company"),
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

    /** Exact medicine name + company match - used once a specific suggestion (name and
     *  manufacturer) has been picked from the search dropdown, so that when two medicines
     *  share a name but come from different companies, the right one (with its own stock/
     *  price) is the one actually looked up. */
    public MedicineInfo findMedicineByNameAndCompany(String medicineName, String company) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM medicines WHERE medicine_name = ? AND company " +
                    (company == null || company.isEmpty() ? "IS NULL" : "= ?");
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, medicineName);
            if (!(company == null || company.isEmpty())) {
                pst.setString(2, company);
            }
            ResultSet rs = pst.executeQuery();

            MedicineInfo result = null;
            if (rs.next()) {
                result = new MedicineInfo(
                        rs.getInt("id"),
                        rs.getString("medicine_name"),
                        rs.getString("company"),
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

    /** The authoritative lookup: fetches the exact medicine row by its primary key,
     *  the only identifier guaranteed unique even across rows that share every other
     *  visible field (name, company, batch, price...). Used once a row's id is already
     *  known (e.g. re-checking live stock for a row already sitting in the bill). */
    public MedicineInfo findMedicineById(int id) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM medicines WHERE id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            MedicineInfo result = null;
            if (rs.next()) {
                result = new MedicineInfo(
                        rs.getInt("id"),
                        rs.getString("medicine_name"),
                        rs.getString("company"),
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

    /** One row in the medicine search suggestion dropdown: a medicine name paired with
     *  its manufacturer and batch number, so medicines that share a name and company but
     *  come from different batches can be told apart before one is picked. id is carried
     *  along too so, once picked, the exact row (not just its text) is known. */
    public static class MedicineSuggestion {
        public final int id;
        public final String medicineName;
        public final String company;
        public final String batchNo;

        public MedicineSuggestion(int id, String medicineName, String company, String batchNo) {
            this.id = id;
            this.medicineName = medicineName;
            this.company = company;
            this.batchNo = batchNo;
        }
    }

    /** Same lookup used by the autocomplete dropdown: id + name + company + batch no for
     *  every medicine starting with the typed text, limit 10 - so the dropdown can show
     *  "Panadol | GSK | B-1025" and "Panadol | GSK | B-2044" as distinct choices instead
     *  of one ambiguous "Panadol - GSK", and so picking one hands back its exact row id. */
    public List<MedicineSuggestion> suggestMedicines(String prefixText) {
        List<MedicineSuggestion> suggestions = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement pst = con.prepareStatement(
                    "SELECT id, medicine_name, company, batch_no FROM medicines WHERE medicine_name LIKE ? " +
                    "ORDER BY medicine_name ASC LIMIT 10"
            );
            pst.setString(1, prefixText + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                suggestions.add(new MedicineSuggestion(rs.getInt("id"), rs.getString("medicine_name"),
                        rs.getString("company"), rs.getString("batch_no")));
            }
            rs.close();
            pst.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return suggestions;
    }

    public String getExpiryDate(String medicineName) {
        return getExpiryDate(medicineName, null);
    }

    /** Expiry date for a specific medicine + company, falling back to a name-only match
     *  when no company is known (e.g. the Dr-Fee-only placeholder row never looks this up). */
    public String getExpiryDate(String medicineName, String company) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT expiry_date FROM medicines WHERE medicine_name = ?" +
                    (company == null || company.isEmpty() ? "" : " AND company = ?");
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, medicineName);
            if (!(company == null || company.isEmpty())) {
                ps.setString(2, company);
            }
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

    /** True once, cached, so repeated calls to insertSaleRecord() don't re-check schema
     *  every time (same pattern ReportsDAO uses for its optional-column reports). */
    private static Boolean hasCompanyColumn = null;

    private boolean hasCompanyColumn(Connection con) throws java.sql.SQLException {
        if (hasCompanyColumn != null) return hasCompanyColumn;
        try (ResultSet rs = con.getMetaData().getColumns(null, null, "sales", "company")) {
            hasCompanyColumn = rs.next();
        }
        return hasCompanyColumn;
    }

    public boolean insertSaleRecord(String invoiceNo, String medicine, int qty,
                                     double price, double total, String expiryDate, String cashier, double drFee) {
        return insertSaleRecord(invoiceNo, medicine, null, qty, price, total, expiryDate, cashier, drFee);
    }

    /**
     * Same as above, but also saves the manufacturer (Company Name) of the medicine
     * actually sold, so medicines that share a name across different companies can
     * still be told apart later in reports/sales history. Requires a "company" column
     * on the "sales" table (not present in the original schema) - if it's missing,
     * this throws a clear, actionable error instead of silently dropping the company,
     * matching the pattern ReportsDAO uses for its own optional-column reports.
     *
     * Opens and closes its own connection - use this from anywhere that just wants to
     * save one sale record on its own. When several writes need to succeed or fail
     * together as one sale (as InvoiceService.saveSale() does), use the Connection-
     * accepting overload below instead, sharing one connection/transaction across all of them.
     */
    public boolean insertSaleRecord(String invoiceNo, String medicine, String company, int qty,
                                     double price, double total, String expiryDate, String cashier, double drFee) {
        try (Connection con = DBConnection.getConnection()) {
            return insertSaleRecord(con, invoiceNo, medicine, company, qty, price, total, expiryDate, cashier, drFee);
        } catch (IllegalStateException schemaIssue) {
            throw schemaIssue;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Same as insertSaleRecord(...) above, but runs on a connection the caller already
     *  has open - so it can be one statement inside a larger transaction (see
     *  InvoiceService.saveSale(), which shares one connection/transaction across the
     *  sale record, the stock deduction, and any test fees, so they all commit or all
     *  roll back together instead of an app crash leaving them half-done). */
    public boolean insertSaleRecord(Connection con, String invoiceNo, String medicine, String company, int qty,
                                     double price, double total, String expiryDate, String cashier, double drFee)
            throws java.sql.SQLException {
        if (!hasCompanyColumn(con)) {
            throw new IllegalStateException(
                "Sales can't be saved with a company name yet - the \"sales\" table needs a " +
                "\"company\" column first. Ask your DB admin to run:\n\n" +
                "ALTER TABLE sales ADD COLUMN company VARCHAR(255);\n\n" +
                "Existing sale records will simply have no company on file - only sales made " +
                "after that point will have one.");
        }
        String sql = "INSERT INTO sales (invoice_no, medicine_name, company, quantity, sale_price, total_bill, expiry_date, cashier, dr_fee) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, invoiceNo);
            ps.setString(2, medicine);
            ps.setString(3, company);
            ps.setInt(4, qty);
            ps.setDouble(5, price);
            ps.setDouble(6, total);
            ps.setString(7, expiryDate);
            ps.setString(8, cashier);
            ps.setDouble(9, drFee);
            ps.executeUpdate();
            return true;
        }
    }

    /** Decrements stock for the exact medicine sold - matched by name AND company,
     *  same identity key used everywhere else in this DAO (findMedicineByNameAndCompany,
     *  getExpiryDate). Matching by name alone would incorrectly deduct stock from every
     *  medicine sharing that name across *different* companies too.
     *  Also guards against quantity going negative (e.g. two near-simultaneous sales of
     *  the last few units) by only deducting when enough stock is actually on hand.
     *  Opens and closes its own connection - use the Connection-accepting overload
     *  when this needs to commit/rollback together with other writes in one sale. */
    public boolean updateStockById(int medicineId, int qtySold) {
        try (Connection con = DBConnection.getConnection()) {
            return updateStockById(con, medicineId, qtySold);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Same as updateStockById(int, int) above, but keyed by the medicines table primary
     *  key instead of name/company text - the precise, unambiguous way to deduct stock
     *  from the exact row that was actually sold. Preferred whenever the id is known
     *  (i.e. whenever the sale originated from a row picked via the suggestion dropdown).
     *  Runs on a connection the caller already has open, so it can share one transaction
     *  with the sale record it belongs to (see InvoiceService.saveSale()). */
    public boolean updateStockById(Connection con, int medicineId, int qtySold) throws java.sql.SQLException {
        String sql = "UPDATE medicines SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qtySold);
            ps.setInt(2, medicineId);
            ps.setInt(3, qtySold);
            return ps.executeUpdate() > 0;
        }
    }

    /** id-keyed counterpart to restoreStock(name, company, qty) - see updateStockById(). */
    public boolean restoreStockById(int medicineId, int qtySold) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE medicines SET quantity = quantity + ? WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, qtySold);
            ps.setInt(2, medicineId);
            int rowsUpdated = ps.executeUpdate();
            ps.close();
            con.close();
            return rowsUpdated > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Opens and closes its own connection - use the Connection-accepting overload
     *  when this needs to commit/rollback together with other writes in one sale. */
    public boolean updateStock(String medicineName, String company, int qtySold) {
        try (Connection con = DBConnection.getConnection()) {
            return updateStock(con, medicineName, company, qtySold);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Same as updateStock(String, String, int) above, but runs on a connection the
     *  caller already has open, so it can share one transaction with the sale record
     *  it belongs to (see InvoiceService.saveSale()). */
    public boolean updateStock(Connection con, String medicineName, String company, int qtySold) throws java.sql.SQLException {
        String sql = "UPDATE medicines SET quantity = quantity - ? WHERE medicine_name = ? AND company " +
                (company == null || company.isEmpty() ? "IS NULL" : "= ?") +
                " AND quantity >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, qtySold);
            ps.setString(idx++, medicineName);
            if (!(company == null || company.isEmpty())) {
                ps.setString(idx++, company);
            }
            ps.setInt(idx, qtySold);
            return ps.executeUpdate() > 0;
        }
    }

    /** Adds qtySold back to stock - the exact reverse of updateStock(), used to undo a
     *  stock deduction when a sale fails partway through and needs to be rolled back. */
    public boolean restoreStock(String medicineName, String company, int qtySold) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE medicines SET quantity = quantity + ? WHERE medicine_name = ? AND company " +
                    (company == null || company.isEmpty() ? "IS NULL" : "= ?");
            PreparedStatement ps = con.prepareStatement(sql);
            int idx = 1;
            ps.setInt(idx++, qtySold);
            ps.setString(idx++, medicineName);
            if (!(company == null || company.isEmpty())) {
                ps.setString(idx, company);
            }
            int rowsUpdated = ps.executeUpdate();
            ps.close();
            con.close();
            return rowsUpdated > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Deletes every "sales" row for one invoice - used to undo a sale that failed
     *  partway through, so a failed sale never leaves rows behind in Sales History. */
    public boolean deleteSalesByInvoice(String invoiceNo) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "DELETE FROM sales WHERE invoice_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
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