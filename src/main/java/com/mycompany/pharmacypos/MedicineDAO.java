package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

/**
 * Handles all database operations for the "medicines" table.
 * No UI code here - MedicineManager calls into this class.
 */
public class MedicineDAO {

    /** Total number of medicine records currently in inventory - independent of
     *  any search filter, so it always reflects the true inventory size. Backs
     *  the "Total Medicines" summary label on the Medicine Management screen. */
    public int getTotalMedicineCount() {
        String sql = "SELECT COUNT(*) AS total FROM medicines";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * Adds a medicine, or - if a row already exists with the exact same
     * name/company/purchase price/sale price/expiry/category/batch/distributor -
     * merges into it by increasing its quantity instead of creating a duplicate row.
     *
     * quantity_added always tracks the running total ever added for that row (via
     * INSERT or merge), independent of how much has since been sold, so reports
     * can show the true "added" figure instead of the live remaining stock.
     */
    public boolean addMedicine(String name, String company, double purchasePrice,
                                double salePrice, int quantity, String expiryDate,
                                String category, String batchNo, String distributor) {
        try (Connection con = DBConnection.getConnection()) {
            Integer existingId = findMatchingMedicineId(con, name, company, purchasePrice,
                    salePrice, expiryDate, category, batchNo, distributor);

            if (existingId != null) {
                String sql = "UPDATE medicines SET quantity = quantity + ?, quantity_added = quantity_added + ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, quantity);
                    ps.setInt(3, existingId);
                    return ps.executeUpdate() > 0;
                }
            }

            String sql = "INSERT INTO medicines (medicine_name, company, purchase_price, sale_price, quantity, quantity_added, expiry_date, category, batch_no, distributor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, company);
                ps.setDouble(3, purchasePrice);
                ps.setDouble(4, salePrice);
                ps.setInt(5, quantity);
                ps.setInt(6, quantity);
                ps.setString(7, expiryDate);
                ps.setString(8, category);
                ps.setString(9, batchNo);
                ps.setString(10, distributor);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /** Returns the id of the existing medicine row whose name/company/purchase
     *  price/sale price/expiry/category/batch/distributor all match exactly, or
     *  null if there is no such row (i.e. this is a genuinely new item). */
    private Integer findMatchingMedicineId(Connection con, String name, String company,
                                            double purchasePrice, double salePrice, String expiryDate,
                                            String category, String batchNo, String distributor) throws Exception {
        String sql = "SELECT id FROM medicines WHERE medicine_name = ? AND company = ? " +
                "AND purchase_price = ? AND sale_price = ? AND expiry_date = ? " +
                "AND category = ? AND batch_no = ? AND distributor = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, company);
            ps.setDouble(3, purchasePrice);
            ps.setDouble(4, salePrice);
            ps.setString(5, expiryDate);
            ps.setString(6, category);
            ps.setString(7, batchNo);
            ps.setString(8, distributor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    public boolean deleteMedicine(int id) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "DELETE FROM medicines WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Loads medicines into the given table model, optionally filtered by search text.
     * Same behavior as before: empty/null searchText loads everything.
     */
    public void loadMedicineTable(DefaultTableModel tableModel, String searchText) {
        try {
            tableModel.setRowCount(0);
            Connection con = DBConnection.getConnection();

            String sql;
            PreparedStatement ps;

            if (searchText == null || searchText.trim().isEmpty()) {
                sql = "SELECT * FROM medicines ORDER BY medicine_name ASC";
                ps = con.prepareStatement(sql);
            } else {
                sql = "SELECT * FROM medicines WHERE medicine_name LIKE ? OR company LIKE ? OR category LIKE ? ORDER BY medicine_name ASC";
                ps = con.prepareStatement(sql);
                String pattern = "%" + searchText + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getString("company"),
                    rs.getDouble("purchase_price"),
                    rs.getDouble("sale_price"),
                    rs.getInt("quantity"),
                    rs.getDate("expiry_date"),
                    rs.getString("category"),
                    rs.getString("batch_no"),
                    rs.getString("distributor")
                });
            }
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Updates a single column for a single medicine row (used for inline table editing).
     * dbColumn must already be the resolved DB column name (e.g. "medicine_name").
     */
    /** Same query as HomeFrame's Low Stock screen used: medicines under the given
     *  quantity threshold, lowest first. Returns the row count, matching the
     *  original "no rows found" check. Throws on failure so the caller can show
     *  the same error dialog as before. */
    public int loadLowStock(DefaultTableModel model, int threshold) throws Exception {
        Connection con = DBConnection.getConnection();
        String sql = "SELECT * FROM medicines WHERE quantity < ? ORDER BY quantity ASC";
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setInt(1, threshold);
        ResultSet rs = pst.executeQuery();

        int count = 0;
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("medicine_name"),
                rs.getString("company"),
                rs.getString("distributor"),
                rs.getString("batch_no"),
                rs.getInt("quantity"),
                rs.getDate("expiry_date")
            });
            count++;
        }

        rs.close();
        pst.close();
        con.close();

        return count;
    }

    public boolean updateField(int id, String dbColumn, Object newValue) {
        try (Connection con = DBConnection.getConnection()) {
            // Manual quantity edits (inline table editing) are recorded in the
            // stock adjustment log, so the "Newly Added Stock" figure never has
            // to be touched but a full audit trail still exists.
            if ("quantity".equals(dbColumn)) {
                return updateQuantityWithLog(con, id, newValue);
            }

            String sql = "UPDATE medicines SET " + dbColumn + " = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setObject(1, newValue);
                ps.setInt(2, id);
                ps.executeUpdate();
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean updateQuantityWithLog(Connection con, int id, Object newValue) throws Exception {
        int newQty = Integer.parseInt(String.valueOf(newValue).trim());

        String medicineName;
        int previousQty;
        String selectSql = "SELECT medicine_name, quantity FROM medicines WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                medicineName = rs.getString("medicine_name");
                previousQty = rs.getInt("quantity");
            }
        }

        if (previousQty == newQty) return true; // nothing changed, nothing to log

        String updateSql = "UPDATE medicines SET quantity = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setInt(1, newQty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        new StockAdjustmentDAO().logAdjustment(con, id, medicineName, previousQty, newQty, LoginFrame.loggedInUser);
        return true;
    }
}