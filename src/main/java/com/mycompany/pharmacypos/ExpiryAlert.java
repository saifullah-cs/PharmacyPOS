package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class ExpiryAlert {

    public void showExpiry() {
        JFrame frame = new JFrame("Expiry Alert");
        frame.setSize(950, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {"ID", "Medicine", "Company", "Quantity", "Expiry Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 900, 500);
        frame.add(sp);

        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT * FROM medicines ORDER BY expiry_date ASC";
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            LocalDate today = LocalDate.now();

            while (rs.next()) {
                Date expirySql = rs.getDate("expiry_date");
                LocalDate expiry = expirySql.toLocalDate();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);

                String status;
                if (daysLeft < 0) {
                    status = "EXPIRED";
                } else if (daysLeft <= 180) {
                    status = daysLeft + " days left";
                } else {
                    status = "Safe";
                }

                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getString("company"),
                    rs.getInt("quantity"),
                    expirySql,
                    status
                });
            }

            // Row Coloring
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    String status = table.getValueAt(row, 5).toString();

                    if (status.contains("EXPIRED")) {
                        c.setBackground(Color.RED);
                        c.setForeground(Color.WHITE);
                    } else if (status.contains("days left")) {
                        c.setBackground(Color.ORANGE);
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    return c;
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error loading expiry data!");
        }

        frame.setVisible(true);
    }
}