package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * The "Sales History" screen, opened from the Home dashboard.
 * Shows every sale ever made, newest first, with a running total.
 */
public class SalesHistoryFrame {

    private final SalesDAO salesDAO = new SalesDAO();

    public void show() {
        JFrame salesFrame = new JFrame("Sales History");
        salesFrame.setSize(1050, 600);
        salesFrame.setLocationRelativeTo(null);
        salesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {
            "ID", "Medicine", "Quantity", "Sale Price",
            "Total Bill", "Invoice No", "Expiry Date", "Sale Date"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 1000, 480);
        salesFrame.add(sp);

        JLabel totalSalesLabel = new JLabel("Total Sales: Rs. 0.00");
        totalSalesLabel.setBounds(20, 520, 400, 30);
        totalSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        salesFrame.add(totalSalesLabel);

        try {
            double totalSales = salesDAO.loadSalesHistory(model);
            totalSalesLabel.setText("Total Sales: Rs. " + String.format("%.2f", totalSales));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(salesFrame, "Error loading sales history: " + ex.getMessage());
        }

        salesFrame.setVisible(true);
    }
}