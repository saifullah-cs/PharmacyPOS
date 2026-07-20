package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;

/**
 * Dedicated "Low Stock Alert Threshold" screen, opened from the Settings menu.
 * Same fields and logic that used to live inline in SettingsFrame.
 */
public class LowStockThresholdFrame {
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    public void show() {
        JFrame frame = new JFrame("Low Stock Alert Threshold");
        frame.setSize(500, 280);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);

        JLabel lowStockTitle = new JLabel("Low Stock Alert Threshold");
        lowStockTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lowStockTitle.setForeground(new Color(0, 102, 102));
        lowStockTitle.setBounds(30, 20, 380, 30);
        frame.add(lowStockTitle);

        JLabel lowStockDesc = new JLabel("Show a medicine in Low Stock when quantity falls below:");
        lowStockDesc.setBounds(30, 60, 440, 25);
        frame.add(lowStockDesc);

        String[] presetOptions = { "25", "50", "100", "Custom" };
        JComboBox<String> presetBox = new JComboBox<>(presetOptions);
        presetBox.setBounds(30, 100, 150, 28);
        frame.add(presetBox);

        JTextField customField = new JTextField();
        customField.setBounds(190, 100, 100, 28);
        customField.setEnabled(false);
        frame.add(customField);

        int currentThreshold = appSettingsDAO.getLowStockThreshold();
        String currentAsString = String.valueOf(currentThreshold);
        boolean isPreset = currentAsString.equals("25") || currentAsString.equals("50") || currentAsString.equals("100");
        if (isPreset) {
            presetBox.setSelectedItem(currentAsString);
        } else {
            presetBox.setSelectedItem("Custom");
            customField.setEnabled(true);
            customField.setText(currentAsString);
        }

        presetBox.addActionListener(e -> {
            boolean custom = "Custom".equals(presetBox.getSelectedItem());
            customField.setEnabled(custom);
        });

        JButton saveThresholdBtn = new JButton("Save Threshold");
        saveThresholdBtn.setBounds(30, 150, 180, 36);
        saveThresholdBtn.setBackground(new Color(0, 150, 136));
        saveThresholdBtn.setForeground(Color.WHITE);
        saveThresholdBtn.setFocusPainted(false);
        frame.add(saveThresholdBtn);

        saveThresholdBtn.addActionListener(e -> {
            String selected = (String) presetBox.getSelectedItem();
            int newThreshold;
            if ("Custom".equals(selected)) {
                try {
                    newThreshold = Integer.parseInt(customField.getText().trim());
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(frame, "Enter a valid number for the custom threshold!", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (newThreshold <= 0) {
                    JOptionPane.showMessageDialog(frame, "Threshold must be greater than 0!", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                newThreshold = Integer.parseInt(selected);
            }
            boolean success = appSettingsDAO.setLowStockThreshold(newThreshold);
            if (success) {
                JOptionPane.showMessageDialog(frame, "Low Stock threshold set to " + newThreshold + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to save threshold!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setVisible(true);
    }
}
