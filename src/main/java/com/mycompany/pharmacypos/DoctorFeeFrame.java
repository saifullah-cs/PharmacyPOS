package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;

/**
 * Dedicated "Default Consultation Fee" screen, opened from the Settings menu.
 * Same fields and logic that used to live inline in SettingsFrame.
 */
public class DoctorFeeFrame {
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    public void show() {
        JFrame frame = new JFrame("Default Consultation Fee");
        frame.setSize(500, 280);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);

        JLabel drFeeTitle = new JLabel("Default Consultation Fee");
        drFeeTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        drFeeTitle.setForeground(new Color(0, 102, 102));
        drFeeTitle.setBounds(30, 20, 380, 30);
        frame.add(drFeeTitle);

        JLabel drFeeDesc = new JLabel("Default fee added to each sale (cashier can adjust or remove it per sale):");
        drFeeDesc.setBounds(30, 60, 440, 25);
        frame.add(drFeeDesc);

        JLabel drFeeLbl = new JLabel("Fee (Rs.):");
        drFeeLbl.setBounds(30, 100, 100, 28);
        frame.add(drFeeLbl);

        JTextField drFeeField = new JTextField();
        drFeeField.setBounds(140, 100, 150, 28);
        drFeeField.setText(String.valueOf(appSettingsDAO.getDoctorFee()));
        frame.add(drFeeField);

        JButton saveDrFeeBtn = new JButton("Save Fee");
        saveDrFeeBtn.setBounds(30, 150, 150, 36);
        saveDrFeeBtn.setBackground(new Color(0, 150, 136));
        saveDrFeeBtn.setForeground(Color.WHITE);
        saveDrFeeBtn.setFocusPainted(false);
        frame.add(saveDrFeeBtn);

        saveDrFeeBtn.addActionListener(e -> {
            double newFee;
            try {
                newFee = Double.parseDouble(drFeeField.getText().trim());
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(frame, "Enter a valid amount for the fee!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (newFee < 0) {
                JOptionPane.showMessageDialog(frame, "Fee cannot be negative!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = appSettingsDAO.setDoctorFee(newFee);
            if (success) {
                JOptionPane.showMessageDialog(frame, "Doctor fee set to Rs. " + newFee + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to save doctor fee!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setVisible(true);
    }
}
