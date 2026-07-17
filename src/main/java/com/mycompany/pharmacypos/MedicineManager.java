package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;

public class MedicineManager {

    private static JFrame medicineFrame;
    private static JPanel mainPanel;

    // Form Fields
    private static JTextField txtMedicineName, txtCompany, txtPurchasePrice, txtSalePrice;
    private static JTextField txtQuantity, txtExpiry, txtBatchNo;
    private static JTextField txtSearch;
    private static JComboBox<String> cmbCategory;

    // Table
    private static JTable medicineTable;
    private static DefaultTableModel tableModel;
    private static TableRowSorter<DefaultTableModel> sorter;

    // Buttons
    private static JButton btnAdd, btnDelete, btnClear;

    public static void open() {
        medicineFrame = new JFrame("Medicine Management");
        medicineFrame.setSize(1150, 700);
        medicineFrame.setLocationRelativeTo(null);
        medicineFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(245, 248, 250));
        medicineFrame.add(mainPanel);

        createUI();
        loadMedicineTable("");   // Initial load

        medicineFrame.setVisible(true);
    }

    private static void createUI() {
        // Title
        JLabel title = new JLabel("Medicine Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBounds(30, 20, 400, 40);
        mainPanel.add(title);

        // Search
        JLabel searchLbl = new JLabel("Search Medicine:");
        searchLbl.setBounds(680, 25, 120, 25);
        mainPanel.add(searchLbl);

        txtSearch = new JTextField();
        txtSearch.setBounds(800, 22, 280, 32);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mainPanel.add(txtSearch);

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                loadMedicineTable(txtSearch.getText().trim());
            }
        });

        // Left Form
        int x = 30, y = 80, gap = 45;

        addLabel("Medicine Name:", x, y);
        txtMedicineName = createTextField(x + 130, y); y += gap;

        addLabel("Company:", x, y);
        txtCompany = createTextField(x + 130, y); y += gap;

        addLabel("Purchase Price:", x, y);
        txtPurchasePrice = createTextField(x + 130, y); y += gap;

        addLabel("Sale Price:", x, y);
        txtSalePrice = createTextField(x + 130, y); y += gap;

        addLabel("Quantity:", x, y);
        txtQuantity = createTextField(x + 130, y); y += gap;

        addLabel("Expiry (YYYY-MM-DD):", x, y);
        txtExpiry = createTextField(x + 130, y); y += gap;

        addLabel("Batch No:", x, y);
        txtBatchNo = createTextField(x + 130, y); y += gap;

        addLabel("Category:", x, y);
        String[] categories = {"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Other"};
        cmbCategory = new JComboBox<>(categories);
        cmbCategory.setBounds(x + 130, y, 200, 28);
        mainPanel.add(cmbCategory);

        // Buttons
        y += 55;
        btnAdd = createButton("Add", x, y, new Color(0, 153, 76));
        btnDelete = createButton("Delete", x + 110, y, new Color(204, 51, 51));
        btnClear = createButton("Clear", x + 220, y, new Color(128, 128, 128));

        btnAdd.addActionListener(e -> addMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearFields());

        // Table with Inline Editing
        String[] columns = {"ID", "Name", "Company", "Purchase", "Sale", "Qty", "Expiry", "Category", "Batch"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // ID not editable
            }
        };

        medicineTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        medicineTable.setRowSorter(sorter);

        // Auto Save on Cell Edit
        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && col >= 0) {
                    saveEditedCell(row, col);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(medicineTable);
        scrollPane.setBounds(420, 70, 700, 520);
        mainPanel.add(scrollPane);
    }

    private static void addLabel(String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setBounds(x, y, 120, 28);
        mainPanel.add(lbl);
    }

    private static JTextField createTextField(int x, int y) {
        JTextField tf = new JTextField();
        tf.setBounds(x, y, 200, 28);
        mainPanel.add(tf);
        return tf;
    }

    private static JButton createButton(String text, int x, int y, Color color) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, 95, 35);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        mainPanel.add(btn);
        return btn;
    }

    // ================= CRUD METHODS =================
    private static void addMedicine() {
        try {
            if (txtMedicineName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Connection con = DBConnection.getConnection();
            String sql = "INSERT INTO medicines (medicine_name, company, purchase_price, sale_price, quantity, expiry_date, category, batch_no) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, txtMedicineName.getText().trim());
            ps.setString(2, txtCompany.getText().trim());
            ps.setDouble(3, Double.parseDouble(txtPurchasePrice.getText().trim()));
            ps.setDouble(4, Double.parseDouble(txtSalePrice.getText().trim()));
            ps.setInt(5, Integer.parseInt(txtQuantity.getText().trim()));
            ps.setString(6, txtExpiry.getText().trim());
            ps.setString(7, cmbCategory.getSelectedItem().toString());
            ps.setString(8, txtBatchNo.getText().trim());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMedicineTable(txtSearch.getText().trim());
                clearFields();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(medicineFrame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void deleteMedicine() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(medicineFrame, "Please select a medicine to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(medicineFrame, "Delete " + name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Connection con = DBConnection.getConnection();
            String sql = "DELETE FROM medicines WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(medicineFrame, "Medicine Deleted Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMedicineTable(txtSearch.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(medicineFrame, "Delete Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void clearFields() {
        txtMedicineName.setText("");
        txtCompany.setText("");
        txtPurchasePrice.setText("");
        txtSalePrice.setText("");
        txtQuantity.setText("");
        txtExpiry.setText("");
        txtBatchNo.setText("");
        cmbCategory.setSelectedIndex(0);
    }

    private static void loadMedicineTable(String searchText) {
        try {
            tableModel.setRowCount(0);
            Connection con = DBConnection.getConnection();

            String sql;
            PreparedStatement ps;

            if (searchText == null || searchText.trim().isEmpty()) {
                sql = "SELECT * FROM medicines ORDER BY medicine_name ASC";
                ps = con.prepareStatement(sql);
            } else {
                sql = "SELECT * FROM medicines WHERE medicine_name LIKE ? OR company LIKE ? OR category LIKE ? ORDER BY medicine_name ASC";
                ps = con.prepareStatement(sql);
                String pattern = "%" + searchText + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getString("company"),
                    rs.getDouble("purchase_price"),
                    rs.getDouble("sale_price"),
                    rs.getInt("quantity"),
                    rs.getDate("expiry_date"),
                    rs.getString("category"),
                    rs.getString("batch_no")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Auto Save on Cell Edit
    private static void saveEditedCell(int row, int col) {
        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Object newValue = tableModel.getValueAt(row, col);
            String tableColumn = tableModel.getColumnName(col);

            String dbColumn;
            switch (tableColumn.toLowerCase()) {
                case "name": dbColumn = "medicine_name"; break;
                case "company": dbColumn = "company"; break;
                case "purchase": dbColumn = "purchase_price"; break;
                case "sale": dbColumn = "sale_price"; break;
                case "qty": dbColumn = "quantity"; break;
                case "expiry": dbColumn = "expiry_date"; break;
                case "category": dbColumn = "category"; break;
                case "batch": dbColumn = "batch_no"; break;
                default: return;
            }

            Connection con = DBConnection.getConnection();
            String sql = "UPDATE medicines SET " + dbColumn + " = ? WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setObject(1, newValue);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();

            System.out.println("Saved: " + dbColumn + " = " + newValue);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(medicineFrame, "Save Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}