package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * The "Sales History" screen, opened from the Home dashboard.
 * Shows every sale ever made, newest first, with a running total,
 * and can be filtered by medicine name, invoice number, or sale date.
 */
public class SalesHistoryFrame {

    private final SalesDAO salesDAO = new SalesDAO();

    private JFrame salesFrame;
    private DefaultTableModel model;
    private JLabel totalSalesLabel;

    public void show() {
        salesFrame = new JFrame("Sales History");
        salesFrame.setSize(1050, 640);
        salesFrame.setLocationRelativeTo(null);
        salesFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        salesFrame.setLayout(null);

        JLabel searchLbl = new JLabel("Search (Medicine / Invoice No / Date):");
        searchLbl.setBounds(20, 15, 260, 25);
        salesFrame.add(searchLbl);

        JTextField searchField = new JTextField();
        searchField.setBounds(290, 12, 300, 30);
        salesFrame.add(searchField);

        String[] columns = {
            "ID", "Medicine", "Quantity", "Sale Price",
            "Total Bill", "Invoice No", "Expiry Date", "Sale Date", "Dr Fee"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 55, 1000, 480);
        salesFrame.add(sp);

        totalSalesLabel = new JLabel("Total Sales: Rs. 0.00");
        totalSalesLabel.setBounds(20, 555, 400, 30);
        totalSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        salesFrame.add(totalSalesLabel);

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                loadHistory(searchField.getText().trim());
            }
        });

        loadHistory("");

        salesFrame.setVisible(true);
    }

    private void loadHistory(String searchText) {
        model.setRowCount(0);
        try {
            double totalSales = salesDAO.loadSalesHistory(model, searchText);
            totalSalesLabel.setText("Total Sales: Rs. " + String.format("%.2f", totalSales));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(salesFrame, "Error loading sales history: " + ex.getMessage());
        }
    }
}