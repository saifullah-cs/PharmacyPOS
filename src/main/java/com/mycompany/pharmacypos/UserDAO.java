package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Handles authentication and account management against the "users" table.
 * No UI code here.
 */
public class UserDAO {

    /** Same check LoginFrame used: exact username + password match.
     *  Returns the user's role if found, or null if the login is invalid. */
    public String checkLogin(String username, String password) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            String role = null;
            if (rs.next()) {
                role = rs.getString("role");
            }

            rs.close();
            pst.close();
            con.close();
            return role;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /** All usernames in the system, for the admin Settings screen's user picker. */
    public java.util.List<String> getAllUsernames() {
        java.util.List<String> usernames = new java.util.ArrayList<>();
        try {
            Connection con = DBConnection.getConnection();
            java.sql.Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT username FROM users ORDER BY username ASC");
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return usernames;
    }

    /** Updates a user's password. Returns true if a matching user was found and updated. */
    public boolean changePassword(String username, String newPassword) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "UPDATE users SET password = ? WHERE username = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            ps.close();
            con.close();
            return rows > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}