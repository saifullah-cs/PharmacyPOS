package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations related to selling medicines:
 * medicine lookup/search for billing, stock updates, sale records,
 * and invoice numbering. No UI code here.
 */
public class SalesDAO {

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
                                     double price, double total, String expiryDate, String cashier) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO sales (invoice_no, medicine_name, quantity, sale_price, total_bill, expiry_date, cashier) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ps.setString(2, medicine);
            ps.setInt(3, qty);
            ps.setDouble(4, price);
            ps.setDouble(5, total);
            ps.setString(6, expiryDate);
            ps.setString(7, cashier);
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