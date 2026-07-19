package com.mycompany.pharmacypos;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Admin-only Settings screen. Currently holds one setting (change any
 * user's password); more settings can be added here later as new sections.
 */
public class SettingsFrame {
    private final UserDAO userDAO = new UserDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    public void show() {
        JFrame frame = new JFrame("Settings");
        frame.setSize(500, 700);           // Good size for scrolling
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Content panel with absolute positioning
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setBackground(Color.WHITE);

        // Total height needed for all components
        int totalHeight = 820;
        contentPanel.setPreferredSize(new Dimension(480, totalHeight));

        // ================= Change User Password =================
        JLabel title = new JLabel("Change User Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 102, 102));
        title.setBounds(30, 20, 350, 30);
        contentPanel.add(title);

        JLabel userLbl = new JLabel("User:");
        userLbl.setBounds(30, 80, 120, 28);
        contentPanel.add(userLbl);

        JComboBox<String> userBox = new JComboBox<>();
        userBox.setBounds(160, 80, 260, 28);
        contentPanel.add(userBox);

        List<String> usernames = userDAO.getAllUsernames();
        for (String name : usernames) {
            userBox.addItem(name);
        }

        JLabel passLbl = new JLabel("New Password:");
        passLbl.setBounds(30, 130, 120, 28);
        contentPanel.add(passLbl);

        JPasswordField passField = new JPasswordField();
        passField.setBounds(160, 130, 260, 28);
        contentPanel.add(passField);

        JLabel confirmLbl = new JLabel("Confirm Password:");
        confirmLbl.setBounds(30, 180, 140, 28);
        contentPanel.add(confirmLbl);

        JPasswordField confirmField = new JPasswordField();
        confirmField.setBounds(160, 180, 260, 28);
        contentPanel.add(confirmField);

        JButton changeBtn = new JButton("Change Password");
        changeBtn.setBounds(160, 230, 180, 36);
        changeBtn.setBackground(new Color(22, 78, 99));
        changeBtn.setForeground(Color.WHITE);
        changeBtn.setFocusPainted(false);
        contentPanel.add(changeBtn);

        changeBtn.addActionListener(e -> {
            String selectedUser = (String) userBox.getSelectedItem();
            String newPassword = String.valueOf(passField.getPassword());
            String confirmPassword = String.valueOf(confirmField.getPassword());
            if (selectedUser == null) {
                JOptionPane.showMessageDialog(frame, "No user selected!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Password cannot be empty!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(frame, "Passwords do not match!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = userDAO.changePassword(selectedUser, newPassword);
            if (success) {
                JOptionPane.showMessageDialog(frame, "Password changed successfully for " + selectedUser + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                passField.setText("");
                confirmField.setText("");
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to change password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ================= Low Stock Alert Threshold =================
        JSeparator separator = new JSeparator();
        separator.setBounds(30, 280, 420, 2);
        contentPanel.add(separator);

        JLabel lowStockTitle = new JLabel("Low Stock Alert Threshold");
        lowStockTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lowStockTitle.setForeground(new Color(0, 102, 102));
        lowStockTitle.setBounds(30, 300, 380, 30);
        contentPanel.add(lowStockTitle);

        JLabel lowStockDesc = new JLabel("Show a medicine in Low Stock when quantity falls below:");
        lowStockDesc.setBounds(30, 335, 420, 25);
        contentPanel.add(lowStockDesc);

        String[] presetOptions = { "25", "50", "100", "Custom" };
        JComboBox<String> presetBox = new JComboBox<>(presetOptions);
        presetBox.setBounds(30, 370, 150, 28);
        contentPanel.add(presetBox);

        JTextField customField = new JTextField();
        customField.setBounds(190, 370, 100, 28);
        customField.setEnabled(false);
        contentPanel.add(customField);

        // Load current value
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
        saveThresholdBtn.setBounds(30, 415, 180, 36);
        saveThresholdBtn.setBackground(new Color(0, 150, 136));
        saveThresholdBtn.setForeground(Color.WHITE);
        saveThresholdBtn.setFocusPainted(false);
        contentPanel.add(saveThresholdBtn);

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

        // ================= Doctor Consultation Fee =================
        JSeparator drFeeSeparator = new JSeparator();
        drFeeSeparator.setBounds(30, 460, 420, 2);
        contentPanel.add(drFeeSeparator);

        JLabel drFeeTitle = new JLabel("Doctor Consultation Fee");
        drFeeTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        drFeeTitle.setForeground(new Color(0, 102, 102));
        drFeeTitle.setBounds(30, 480, 380, 30);
        contentPanel.add(drFeeTitle);

        JLabel drFeeDesc = new JLabel("Default fee added to each sale (cashier can adjust or remove it per sale):");
        drFeeDesc.setBounds(30, 515, 420, 25);
        contentPanel.add(drFeeDesc);

        JLabel drFeeLbl = new JLabel("Fee (Rs.):");
        drFeeLbl.setBounds(30, 550, 100, 28);
        contentPanel.add(drFeeLbl);

        JTextField drFeeField = new JTextField();
        drFeeField.setBounds(140, 550, 150, 28);
        drFeeField.setText(String.valueOf(appSettingsDAO.getDoctorFee()));
        contentPanel.add(drFeeField);

        JButton saveDrFeeBtn = new JButton("Save Fee");
        saveDrFeeBtn.setBounds(30, 590, 150, 36);
        saveDrFeeBtn.setBackground(new Color(0, 150, 136));
        saveDrFeeBtn.setForeground(Color.WHITE);
        saveDrFeeBtn.setFocusPainted(false);
        contentPanel.add(saveDrFeeBtn);

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

        // ================= Tests Management =================
        JSeparator testsSeparator = new JSeparator();
        testsSeparator.setBounds(30, 640, 420, 2);
        contentPanel.add(testsSeparator);

        JLabel testsTitle = new JLabel("Tests Management");
        testsTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        testsTitle.setForeground(new Color(0, 102, 102));
        testsTitle.setBounds(30, 660, 380, 30);
        contentPanel.add(testsTitle);

        JLabel testsDesc = new JLabel("Add, edit, or remove tests and their default prices.");
        testsDesc.setBounds(30, 695, 420, 25);
        contentPanel.add(testsDesc);

        JButton manageTestsBtn = new JButton("Manage Tests");
        manageTestsBtn.setBounds(30, 730, 180, 36);
        manageTestsBtn.setBackground(new Color(0, 150, 136));
        manageTestsBtn.setForeground(Color.WHITE);
        manageTestsBtn.setFocusPainted(false);
        contentPanel.add(manageTestsBtn);

        manageTestsBtn.addActionListener(e -> new TestManagementFrame().show());

        // Scrollable pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}