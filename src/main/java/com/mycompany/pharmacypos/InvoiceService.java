package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JTable;

public class InvoiceService {

    public static String saveSale(JTable table, String cashier) {
        String invoiceNo = InvoiceGenerator.getNextInvoiceNumber();

        try {
            Connection con = DBConnection.getConnection();

            String insertSql = "INSERT INTO sales (invoice_no, medicine_name, quantity, sale_price, total_bill, expiry_date, cashier) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement insertPst = con.prepareStatement(insertSql);

            String updateStockSql = "UPDATE medicines SET quantity = quantity - ? WHERE medicine_name = ?";
            PreparedStatement updatePst = con.prepareStatement(updateStockSql);

            for (int i = 0; i < table.getRowCount(); i++) {
                String medicine = table.getValueAt(i, 0).toString();
                double price = Double.parseDouble(table.getValueAt(i, 1).toString());
                int qty = Integer.parseInt(table.getValueAt(i, 2).toString());
                double total = Double.parseDouble(table.getValueAt(i, 3).toString());

                // Get expiry date from medicines table
                String expiryDate = getExpiryDate(medicine);

                // Save Sale
                insertPst.setString(1, invoiceNo);
                insertPst.setString(2, medicine);
                insertPst.setInt(3, qty);
                insertPst.setDouble(4, price);
                insertPst.setDouble(5, total);
                insertPst.setString(6, expiryDate);
                insertPst.setString(7, cashier);
                insertPst.executeUpdate();

                // Update Stock
                updatePst.setInt(1, qty);
                updatePst.setString(2, medicine);
                updatePst.executeUpdate();
            }

            insertPst.close();
            updatePst.close();

            return invoiceNo;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to get expiry date of medicine
    private static String getExpiryDate(String medicineName) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT expiry_date FROM medicines WHERE medicine_name = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, medicineName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDate("expiry_date").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}