package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;

public class MedicineManager {

    private static JFrame medicineFrame;
    private static JPanel mainPanel;

    // Form Fields (admin only)
    private static JTextField txtMedicineName, txtCompany, txtPurchasePrice, txtSalePrice;
    private static JTextField txtQuantity, txtExpiry, txtBatchNo;
    private static JTextField txtSearch;
    private static JComboBox<String> cmbCategory;
    private static JComboBox<String> cmbDistributor;

    // DAO - handles distributor lookups for the read-only drop-down above
    private static final DistributorDAO distributorDAO = new DistributorDAO();

    // Table
    private static JTable medicineTable;
    private static DefaultTableModel tableModel;
    private static TableRowSorter<DefaultTableModel> sorter;

    // Buttons (admin only)
    private static JButton btnAdd, btnDelete, btnClear;

    // Total-inventory-size summary label (admin only, shown below the buttons)
    private static JLabel lblTotalMedicines;

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
        medicineFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new Color(245, 248, 250));
        medicineFrame.add(mainPanel);

        createUI();
        loadMedicineTable("");   // Initial load
        updateTotalMedicinesLabel();

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

        addLabel("Distributor:", x, y);
        java.util.List<String> distributorNames = distributorDAO.getAllDistributorNames();
        cmbDistributor = new JComboBox<>(distributorNames.toArray(new String[0]));
        cmbDistributor.setBounds(x + 130, y, 200, 28);
        mainPanel.add(cmbDistributor);
        y += gap;

        addLabel("Category:", x, y);
        String[] categories = {"Tablet", "Capsule", "Syrup", "Injection", "Cream", "Drops", "Other"};
        cmbCategory = new JComboBox<>(categories);
        cmbCategory.setBounds(x + 130, y, 200, 28);
        mainPanel.add(cmbCategory);

        // Enter moves focus through the fields in order (mirrors visual tab order);
        // the Add button itself is still only triggered by a mouse click.
        chainEnterFocus(txtMedicineName, txtCompany);
        chainEnterFocus(txtCompany, txtPurchasePrice);
        chainEnterFocus(txtPurchasePrice, txtSalePrice);
        chainEnterFocus(txtSalePrice, txtQuantity);
        chainEnterFocus(txtQuantity, txtExpiry);
        chainEnterFocus(txtExpiry, txtBatchNo);
        chainEnterFocus(txtBatchNo, cmbDistributor);

        y += 55;
        btnAdd = createButton("Add", x, y, new Color(0, 153, 76));
        btnDelete = createButton("Delete", x + 110, y, new Color(204, 51, 51));
        btnClear = createButton("Clear", x + 220, y, new Color(128, 128, 128));

        btnAdd.addActionListener(e -> addMedicine());
        btnDelete.addActionListener(e -> deleteMedicine());
        btnClear.addActionListener(e -> clearFields());

        // Compact inventory-size summary, sitting neatly below the action row.
        y += 45;
        lblTotalMedicines = new JLabel("Total Medicines: 0");
        lblTotalMedicines.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTotalMedicines.setForeground(new Color(90, 90, 90));
        lblTotalMedicines.setBounds(x, y, 300, 25);
        mainPanel.add(lblTotalMedicines);
    }

    /** Refreshes the "Total Medicines" label from the DB - always the true
     *  inventory size, regardless of any active search filter. No-op for the
     *  cashier's read-only view, which doesn't have this label. */
    private static void updateTotalMedicinesLabel() {
        if (lblTotalMedicines != null) {
            lblTotalMedicines.setText("Total Medicines: " + medicineDAO.getTotalMedicineCount());
        }
    }

    // ================= TABLE (shown to everyone, editable admin-only) =================
    private static void createTable() {
        String[] columns = {"ID", "Name", "Company", "Purchase", "Sale", "Qty", "Expiry", "Category", "Batch", "Distributor"};
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
        ResponsiveUtil.makeResponsive(medicineFrame, scrollPane, 30, 30);
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

    /** Pressing Enter in `field` moves focus to `next`, instead of doing nothing.
     *  Used to chain the medicine form fields in visual/tab order. */
    private static void chainEnterFocus(JTextField field, JComponent next) {
        field.addActionListener(e -> next.requestFocusInWindow());
    }

    /** A Yes/No confirmation dialog with proper keyboard support:
     *  - "Yes" is focused (and the active default button) as soon as it opens
     *  - Left/Right arrow keys move focus between Yes and No
     *  - Enter always activates whichever option currently has focus
     *  Returns true only if the user picked "Yes". */
    private static boolean confirmDelete(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = optionPane.createDialog(medicineFrame, "Confirm Delete");

        java.util.List<JButton> buttons = new java.util.ArrayList<>();
        collectButtons(dialog.getContentPane(), buttons);

        if (buttons.size() == 2) {
            JButton yesBtn = buttons.get(0);
            JButton noBtn = buttons.get(1);

            for (JButton btn : buttons) {
                JButton other = (btn == yesBtn) ? noBtn : yesBtn;

                btn.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        dialog.getRootPane().setDefaultButton(btn);
                    }
                });

                btn.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("LEFT"), "moveFocus");
                btn.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("RIGHT"), "moveFocus");
                btn.getActionMap().put("moveFocus", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        other.requestFocusInWindow();
                    }
                });
            }

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    yesBtn.requestFocusInWindow();
                    dialog.getRootPane().setDefaultButton(yesBtn);
                }
            });
        }

        dialog.setVisible(true);
        Object value = optionPane.getValue();
        dialog.dispose();
        return value instanceof Integer && (Integer) value == JOptionPane.YES_OPTION;
    }

    private static void collectButtons(Component comp, java.util.List<JButton> out) {
        if (comp instanceof JButton) {
            out.add((JButton) comp);
        } else if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                collectButtons(child, out);
            }
        }
    }

    // ================= CRUD METHODS (admin only - delegate to MedicineDAO) =================
    private static void addMedicine() {
        try {
            if (txtMedicineName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (cmbDistributor.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(medicineFrame, "Please add a distributor in Settings > Distributor Management first!", "Validation", JOptionPane.WARNING_MESSAGE);
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
                    txtBatchNo.getText().trim(),
                    cmbDistributor.getSelectedItem() != null ? cmbDistributor.getSelectedItem().toString() : ""
            );

            if (success) {
                JOptionPane.showMessageDialog(medicineFrame, "Medicine Added Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMedicineTable(txtSearch.getText().trim());
                clearFields();
                HomeFrame.refreshDashboardStatic();
                updateTotalMedicinesLabel();
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

        if (!confirmDelete("Delete " + name + "?")) return;

        boolean success = medicineDAO.deleteMedicine(id);
        if (success) {
            JOptionPane.showMessageDialog(medicineFrame, "Medicine Deleted Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMedicineTable(txtSearch.getText().trim());
            HomeFrame.refreshDashboardStatic();
            updateTotalMedicinesLabel();
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
        if (cmbDistributor.getItemCount() > 0) {
            cmbDistributor.setSelectedIndex(0);
        }
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
            case "distributor": dbColumn = "distributor"; break;
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