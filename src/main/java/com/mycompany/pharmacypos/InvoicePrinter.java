package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class InvoicePrinter {

    public static void printInvoice(String invoiceNo) {
        JFrame printFrame = new JFrame("Invoice - " + invoiceNo);
        printFrame.setSize(400, 600);
        printFrame.setLocationRelativeTo(null);

        JTextArea invoiceArea = new JTextArea();
        invoiceArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        invoiceArea.setEditable(false);

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("          HEALTH HAVEN PHARMACY\n");
        sb.append("========================================\n");
        sb.append("Invoice No: ").append(invoiceNo).append("\n");
        sb.append("Date: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("----------------------------------------\n");

        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM sales WHERE invoice_no = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, invoiceNo);
            ResultSet rs = ps.executeQuery();

            double grandTotal = 0;

            while (rs.next()) {
                sb.append(rs.getString("medicine_name")).append("\n");
                sb.append("Qty: ").append(rs.getInt("quantity"));
                sb.append("   Price: ").append(rs.getDouble("sale_price"));
                sb.append("   Total: ").append(rs.getDouble("total_bill")).append("\n\n");
                grandTotal += rs.getDouble("total_bill");
            }

            sb.append("----------------------------------------\n");
            sb.append("Grand Total: Rs. ").append(grandTotal).append("\n");
            sb.append("Thank You! Visit Again.\n");
            sb.append("========================================\n");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        invoiceArea.setText(sb.toString());

        JScrollPane sp = new JScrollPane(invoiceArea);
        printFrame.add(sp);

        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(printFrame, "Printing Invoice " + invoiceNo + "...");
            // Real printing logic later
        });
        printFrame.add(printBtn, BorderLayout.SOUTH);

        printFrame.setVisible(true);
    }
}