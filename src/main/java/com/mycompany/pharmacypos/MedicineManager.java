package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MedicineManager {

    private static JFrame medicineFrame;
    private static JPanel mainPanel;

    // Form Fields (admin only)
    private static JTextField txtMedicineName, txtCompany, txtPurchasePrice, txtSalePrice;
    private static JTextField txtQuantity, txtExpiry, txtBatchNo;
    private static JTextField txtSearch;
    private static JComboBox<String> cmbCategory;

    // Table
    private static JTable medicineTable;
    private static DefaultTableModel tableModel;
    private static TableRowSorter<DefaultTableModel> sorter;

    // Buttons (admin only)
    private static JButton btnAdd, btnDelete, btnClear;

    // DAO - handles all database access for this screen
    private static final MedicineDAO medicineDAO = new MedicineDAO();

    // Role-based access: admin gets the full management form,
    // cashier gets a read-only inventory view only.
    private static boolean isAdminUser = false;

    public static void open(String role) {
        isAdminUser = "admin".equalsIgnoreCase(role);

        medicineFrame = new JFrame(isAdminUser ? "Medicine Management" : "Medicine Inventory");
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
        JLabel title = new JLabel(isAdminUser ? "Medicine Management" : "Medicine Inventory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBounds(30, 20, 400, 40);
        mainPanel.add(title);

        // Search - available to everyone
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

        if (isAdminUser) {
            createAdminForm();
        }

        createTable();
    }

    // ================= ADMIN-ONLY FORM (Add / Delete / Clear) =================
    private static void createAdminForm() {
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

        y += 55;
        btnAdd = createButton("Add", x, y, new Color(0, 153, 76));
        btnDelete = createButton("Delete", x + 110, y, new Color(204, 51, 51));
        btnClear = createButton("Clear", x + 220, y, new Color(128, 128, 128));

        btnAdd.addActionListener(e -> addMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearFields());
    }

    // ================= TABLE (shown to everyone, editable admin-only) =================
    private static void createTable() {
        String[] columns = {"ID", "Name", "Company", "Purchase", "Sale", "Qty", "Expiry", "Category", "Batch"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (!isAdminUser) return false; // Cashier: read-only inventory view
                return column != 0;              // Admin: everything editable except ID
            }
        };

        medicineTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        medicineTable.setRowSorter(sorter);

        if (!isAdminUser) {
            // Hide the Purchase (cost) column from cashiers - same reasoning as
            // hiding the Purchase Price field/edit access for non-admins elsewhere.
            medicineTable.getColumnModel().removeColumn(medicineTable.getColumnModel().getColumn(3));
        }

        // Auto Save on Cell Edit (admin only - table is non-editable for cashier anyway)
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
        if (isAdminUser) {
            // Table sits to the right of the management form
            scrollPane.setBounds(420, 70, 700, 520);
        } else {
            // No form to share space with - table takes the centered, full view
            scrollPane.setBounds(30, 90, 1080, 560);
        }
        mainPanel.add(scrollPane);
    }

    private static JLabel addLabel(String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setBounds(x, y, 120, 28);
        mainPanel.add(lbl);
        return lbl;
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

    // ================= CRUD METHODS (admin only - delegate to MedicineDAO) =================
    private static void addMedicine() {
        try {
            if (txtMedicineName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = medicineDAO.addMedicine(
                    txtMedicineName.getText().trim(),
                    txtCompany.getText().trim(),
                    Double.parseDouble(txtPurchasePrice.getText().trim()),
                    Double.parseDouble(txtSalePrice.getText().trim()),
                    Integer.parseInt(txtQuantity.getText().trim()),
                    txtExpiry.getText().trim(),
                    cmbCategory.getSelectedItem().toString(),
                    txtBatchNo.getText().trim()
            );

            if (success) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMedicineTable(txtSearch.getText().trim());
                clearFields();
                HomeFrame.refreshDashboardStatic();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(medicineFrame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void deleteMedicine() {
        if (!isAdminUser) {
            JOptionPane.showMessageDialog(medicineFrame, "Only Admin can delete medicines!", "Access Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(medicineFrame, "Please select a medicine to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(medicineFrame, "Delete " + name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean success = medicineDAO.deleteMedicine(id);
        if (success) {
            JOptionPane.showMessageDialog(medicineFrame, "Medicine Deleted Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMedicineTable(txtSearch.getText().trim());
            HomeFrame.refreshDashboardStatic();
        } else {
            JOptionPane.showMessageDialog(medicineFrame, "Delete Error!", "Error", JOptionPane.ERROR_MESSAGE);
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
        medicineDAO.loadMedicineTable(tableModel, searchText);
    }

    // Auto Save on Cell Edit
    private static void saveEditedCell(int row, int col) {
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

        boolean success = medicineDAO.updateField(id, dbColumn, newValue);
        if (success) {
            System.out.println("Saved: " + dbColumn + " = " + newValue);
            HomeFrame.refreshDashboardStatic();
        } else {
            JOptionPane.showMessageDialog(medicineFrame, "Save Error!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}