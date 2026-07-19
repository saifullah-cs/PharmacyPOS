package com.mycompany.pharmacypos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Handles simple key-value application settings (e.g. low stock threshold),
 * stored in the app_settings table.
 */
public class AppSettingsDAO {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 100;
    private static final double DEFAULT_DOCTOR_FEE = 0.0;

    public int getLowStockThreshold() {
        String value = getSetting("low_stock_threshold");
        if (value == null) {
            return DEFAULT_LOW_STOCK_THRESHOLD;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return DEFAULT_LOW_STOCK_THRESHOLD;
        }
    }

    public boolean setLowStockThreshold(int threshold) {
        return setSetting("low_stock_threshold", String.valueOf(threshold));
    }

    public double getDoctorFee() {
        String value = getSetting("doctor_fee");
        if (value == null) {
            return DEFAULT_DOCTOR_FEE;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return DEFAULT_DOCTOR_FEE;
        }
    }

    public boolean setDoctorFee(double fee) {
        return setSetting("doctor_fee", String.valueOf(fee));
    }

    private String getSetting(String key) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();

            String value = null;
            if (rs.next()) {
                value = rs.getString("setting_value");
            }

            rs.close();
            ps.close();
            con.close();
            return value;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private boolean setSetting(String key, String value) {
        try {
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                         "ON DUPLICATE KEY UPDATE setting_value = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, value);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}