package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * Handles the tests catalog (managed in Settings) and the tests actually
 * attached to a given sale/invoice (used from the Sales screen and receipt).
 */
public class TestDAO {

    public static class TestInfo {
        public final int id;
        public final String name;
        public final double price;

        public TestInfo(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    public static class TestLineItem {
        public final String name;
        public final double price;

        public TestLineItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    // ================= Tests catalog (Settings) =================

    public boolean addTest(String name, double price) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO tests (test_name, default_price) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean updateTest(int id, String name, double price) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE tests SET test_name = ?, default_price = ? WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, id);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean deleteTest(int id) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "DELETE FROM tests WHERE id = ?";
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

    public void loadTests(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM tests ORDER BY test_name ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("test_name"),
                    rs.getDouble("default_price")
                });
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<TestInfo> getAllTests() {
        List<TestInfo> tests = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM tests ORDER BY test_name ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tests.add(new TestInfo(
                        rs.getInt("id"),
                        rs.getString("test_name"),
                        rs.getDouble("default_price")
                ));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return tests;
    }

    // ================= Tests attached to a sale (Sales screen / receipt) =================

    public boolean insertSaleTest(String invoiceNo, String testName, double price) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO sale_tests (invoice_no, test_name, price) VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ps.setString(2, testName);
            ps.setDouble(3, price);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public List<TestLineItem> getTestsForInvoice(String invoiceNo) {
        List<TestLineItem> items = new ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT test_name, price FROM sale_tests WHERE invoice_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new TestLineItem(rs.getString("test_name"), rs.getDouble("price")));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }
}