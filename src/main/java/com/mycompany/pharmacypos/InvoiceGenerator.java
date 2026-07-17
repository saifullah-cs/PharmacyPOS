package com.mycompany.pharmacypos;

import java.sql.*;

public class InvoiceGenerator {

    public static String getNextInvoiceNumber() {
        String invoice = "INV-000001";
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT invoice_no FROM sales ORDER BY id DESC LIMIT 1";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String last = rs.getString("invoice_no");
                int num = Integer.parseInt(last.replace("INV-", ""));
                num++;
                invoice = String.format("INV-%06d", num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return invoice;
    }
}