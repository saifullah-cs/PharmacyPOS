package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * Handles the distributors catalog (managed in Settings -> Distributor
 * Management) and feeds the read-only Distributor drop-down used on the
 * Medicine Management screen.
 *
 * Requires this table:
 *
 *   CREATE TABLE distributors (
 *       id INT AUTO_INCREMENT PRIMARY KEY,
 *       distributor_name VARCHAR(255) NOT NULL,
 *       phone_number VARCHAR(50),
 *       notes TEXT
 *   );
 */
public class DistributorDAO {

    public static class DistributorInfo {
        public final int id;
        public final String name;
        public final String phone;
        public final String notes;

        public DistributorInfo(int id, String name, String phone, String notes) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.notes = notes;
        }
    }

    // ================= Distributors catalog (Settings) =================

    public boolean addDistributor(String name, String phone, String notes) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO distributors (distributor_name, phone_number, notes) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, notes);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean updateDistributor(int id, String name, String phone, String notes) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE distributors SET distributor_name = ?, phone_number = ?, notes = ? WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, notes);
            ps.setInt(4, id);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean deleteDistributor(int id) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "DELETE FROM distributors WHERE id = ?";
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

    public void loadDistributors(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM distributors ORDER BY distributor_name ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("distributor_name"),
                    rs.getString("phone_number"),
                    rs.getString("notes")
                });
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<DistributorInfo> getAllDistributors() {
        List<DistributorInfo> distributors = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM distributors ORDER BY distributor_name ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                distributors.add(new DistributorInfo(
                        rs.getInt("id"),
                        rs.getString("distributor_name"),
                        rs.getString("phone_number"),
                        rs.getString("notes")
                ));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return distributors;
    }

    /** Distributor names only, for populating the read-only drop-down on the
     *  Medicine Management screen (mirrors the Category combo box there). */
    public List<String> getAllDistributorNames() {
        List<String> names = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT distributor_name FROM distributors ORDER BY distributor_name ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("distributor_name"));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return names;
    }
}
