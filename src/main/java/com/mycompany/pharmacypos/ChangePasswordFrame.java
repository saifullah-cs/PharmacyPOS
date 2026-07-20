package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dedicated "Change User Password" screen, opened from the Settings menu.
 * Same fields and logic that used to live inline in SettingsFrame.
 */
public class ChangePasswordFrame {
    private final UserDAO userDAO = new UserDAO();

    public void show() {
        JFrame frame = new JFrame("Change User Password");
        frame.setSize(460, 320);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);

        JLabel title = new JLabel("Change User Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 102, 102));
        title.setBounds(30, 20, 350, 30);
        frame.add(title);

        JLabel userLbl = new JLabel("User:");
        userLbl.setBounds(30, 80, 120, 28);
        frame.add(userLbl);

        JComboBox<String> userBox = new JComboBox<>();
        userBox.setBounds(160, 80, 260, 28);
        frame.add(userBox);

        List<String> usernames = userDAO.getAllUsernames();
        for (String name : usernames) {
            userBox.addItem(name);
        }

        JLabel passLbl = new JLabel("New Password:");
        passLbl.setBounds(30, 130, 120, 28);
        frame.add(passLbl);

        JPasswordField passField = new JPasswordField();
        passField.setBounds(160, 130, 260, 28);
        frame.add(passField);

        JLabel confirmLbl = new JLabel("Confirm Password:");
        confirmLbl.setBounds(30, 180, 140, 28);
        frame.add(confirmLbl);

        JPasswordField confirmField = new JPasswordField();
        confirmField.setBounds(160, 180, 260, 28);
        frame.add(confirmField);

        JButton changeBtn = new JButton("Change Password");
        changeBtn.setBounds(160, 230, 180, 36);
        changeBtn.setBackground(new Color(22, 78, 99));
        changeBtn.setForeground(Color.WHITE);
        changeBtn.setFocusPainted(false);
        frame.add(changeBtn);

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

        frame.setVisible(true);
    }
}
