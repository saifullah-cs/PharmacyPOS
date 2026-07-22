package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the "stock_adjustments" audit table: one row per manual quantity
 * edit made on the Medicine Management screen. Keeps a full history of who
 * changed a medicine's quantity, when, and by how much - independent of the
 * "Newly Added Stock" figures on the medicines table, which are never
 * touched by these edits.
 *
 * Requires this table (see the ALTER/CREATE statements provided alongside
 * this change):
 *
 *   CREATE TABLE stock_adjustments (
 *       id INT AUTO_INCREMENT PRIMARY KEY,
 *       medicine_id INT NOT NULL,
 *       medicine_name VARCHAR(255) NOT NULL,
 *       previous_quantity INT NOT NULL,
 *       new_quantity INT NOT NULL,
 *       change_amount INT NOT NULL,
 *       adjusted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *       updated_by VARCHAR(255)
 *   );
 */
public class StockAdjustmentDAO {

    /** Inserts one adjustment row on the given (already-open) connection, so it
     *  can be part of the same unit of work as the quantity update itself. */
    public void logAdjustment(Connection con, int medicineId, String medicineName,
                               int previousQuantity, int newQuantity, String updatedBy) throws Exception {
        String sql = "INSERT INTO stock_adjustments " +
                "(medicine_id, medicine_name, previous_quantity, new_quantity, change_amount, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.setString(2, medicineName);
            ps.setInt(3, previousQuantity);
            ps.setInt(4, newQuantity);
            ps.setInt(5, newQuantity - previousQuantity);
            ps.setString(6, (updatedBy == null || updatedBy.isEmpty()) ? "Unknown" : updatedBy);
            ps.executeUpdate();
        }
    }

    /** Convenience overload that opens/closes its own connection, for callers
     *  outside of an existing update transaction. */
    public void logAdjustment(int medicineId, String medicineName,
                               int previousQuantity, int newQuantity, String updatedBy) throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            logAdjustment(con, medicineId, medicineName, previousQuantity, newQuantity, updatedBy);
        }
    }

    /** Adjustment history rows in range, most recent first. Backs the
     *  "Inventory Adjustment History" report. */
    public List<Object[]> loadAdjustments(LocalDate start, LocalDate end) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT medicine_name, previous_quantity, new_quantity, change_amount, adjusted_at, updated_by " +
                "FROM stock_adjustments WHERE adjusted_at >= ? AND adjusted_at < ? ORDER BY adjusted_at DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("medicine_name"),
                        rs.getInt("previous_quantity"),
                        rs.getInt("new_quantity"),
                        rs.getInt("change_amount"),
                        rs.getTimestamp("adjusted_at"),
                        rs.getString("updated_by")
                    });
                }
            }
        }
        return rows;
    }
}
