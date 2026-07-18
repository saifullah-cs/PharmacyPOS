package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;

public class LoginFrame {

    public static String loggedInUser = "";

    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        JFrame frame = new JFrame("Login");
        frame.setSize(400, 300);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 50, 100, 30);
        JTextField userField = new JTextField();
        userField.setBounds(150, 50, 180, 30);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 100, 100, 30);
        JPasswordField passField = new JPasswordField();
        passField.setBounds(150, 100, 180, 30);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(150, 160, 100, 35);

        frame.add(userLabel);
        frame.add(userField);
        frame.add(passLabel);
        frame.add(passField);
        frame.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String role = userDAO.checkLogin(userField.getText(), String.valueOf(passField.getPassword()));

            if (role != null) {
                JOptionPane.showMessageDialog(null, "Login Successful!");
                loggedInUser = userField.getText();
                frame.dispose();
                new HomeFrame(role);
            } else {
                JOptionPane.showMessageDialog(null, "Invalid Login!");
            }
        });

        frame.setVisible(true);
    }
}