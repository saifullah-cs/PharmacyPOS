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

    public boolean addMedicine(String name, String company, double purchasePrice,
                                double salePrice, int quantity, String expiryDate,
                                String category, String batchNo) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO medicines (medicine_name, company, purchase_price, sale_price, quantity, expiry_date, category, batch_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, company);
            ps.setDouble(3, purchasePrice);
            ps.setDouble(4, salePrice);
            ps.setInt(5, quantity);
            ps.setString(6, expiryDate);
            ps.setString(7, category);
            ps.setString(8, batchNo);

            int rows = ps.executeUpdate();
            ps.close();
            con.close();
            return rows > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
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
                    rs.getString("batch_no")
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
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE medicines SET " + dbColumn + " = ? WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, newValue);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}