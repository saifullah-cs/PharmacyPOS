package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame {
public static String loggedInUser = "";
    public  LoginFrame() {
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

            try {
                Connection con = DBConnection.getConnection();

                String sql = "SELECT role FROM users WHERE username=? AND password=?";
                PreparedStatement pst = con.prepareStatement(sql);

                pst.setString(1, userField.getText());
                pst.setString(2, String.valueOf(passField.getPassword()));

                ResultSet rs = pst.executeQuery();

                if (rs.next()) {

                    String role = rs.getString("role");

                    JOptionPane.showMessageDialog(null, "Login Successful!");
loggedInUser = userField.getText();
                    frame.dispose();

                   new HomeFrame(role);

                } else {
                    JOptionPane.showMessageDialog(null, "Invalid Login!");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        frame.setVisible(true);
    }
}