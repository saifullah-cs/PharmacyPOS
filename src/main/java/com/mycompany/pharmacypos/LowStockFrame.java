package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * The "Low Stock" screen, opened from the Home dashboard.
 * Shows medicines whose quantity is below the low-stock threshold.
 */
public class LowStockFrame {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    public void show() {
        JFrame lowFrame = new JFrame("Low Stock Medicines");
        lowFrame.setSize(900, 550);
        lowFrame.setLocationRelativeTo(null);
        lowFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = { "ID", "Medicine", "Company", "Quantity", "Expiry Date" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 20, 840, 450);
        lowFrame.add(sp);

        int threshold = appSettingsDAO.getLowStockThreshold();

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