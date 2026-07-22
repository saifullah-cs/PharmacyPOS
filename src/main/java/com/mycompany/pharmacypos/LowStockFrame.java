package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * The "Low Stock" screen, opened from the Home dashboard.
 * Shows medicines whose quantity is below the low-stock threshold.
 */
public class LowStockFrame {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    public void show() {
        JFrame lowFrame = new JFrame("Low Stock Medicines");
        lowFrame.setSize(1100, 550);
        lowFrame.setLocationRelativeTo(null);
        lowFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        lowFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        lowFrame.setLayout(new BorderLayout());

        int threshold = appSettingsDAO.getLowStockThreshold();

        // Threshold label, pulled live from the Settings module every time this
        // screen opens - so it's always in sync even if the threshold was just
        // changed on the Settings screen, with no need to reopen this one.
        JLabel thresholdLabel = new JLabel("Low Stock Threshold: " + threshold + " units");
        thresholdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        thresholdLabel.setForeground(new Color(90, 90, 90));
        thresholdLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        lowFrame.add(thresholdLabel, BorderLayout.NORTH);

        String[] columns = { "ID", "Medicine", "Company", "Distributor Name", "Batch Number", "Quantity", "Expiry Date" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        lowFrame.add(sp, BorderLayout.CENTER);

        try {
            int count = medicineDAO.loadLowStock(model, threshold);

            if (count == 0) {
                JOptionPane.showMessageDialog(lowFrame, "No medicines with quantity less than " + threshold + "!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(lowFrame, "Error loading low stock data!");
        }

        lowFrame.setVisible(true);
    }
}