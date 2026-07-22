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
        frame.setSize(1150, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        String[] columns = {"Medicine", "Company", "Distributor Name", "Batch Number", "Quantity", "Expiry Date", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 1100, 500);
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

                // Only medicines that are expired or expiring within the next 6 months
                // (180 days) require attention. Anything beyond that is "Safe" and
                // should not be shown on this screen at all.
                if (daysLeft > 180) {
                    continue;
                }

                String status = (daysLeft < 0) ? "Expired" : "Expiring Soon";

                model.addRow(new Object[]{
                    rs.getString("medicine_name"),
                    rs.getString("company"),
                    rs.getString("distributor"),
                    rs.getString("batch_no"),
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
                    String status = table.getValueAt(row, 6).toString();

                    if (status.equals("Expired")) {
                        c.setBackground(Color.RED);
                        c.setForeground(Color.WHITE);
                    } else if (status.equals("Expiring Soon")) {
                        c.setBackground(Color.YELLOW);
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
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