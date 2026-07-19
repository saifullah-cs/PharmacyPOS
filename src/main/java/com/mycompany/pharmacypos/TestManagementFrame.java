package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TestManagementFrame {

    private final TestDAO testDAO = new TestDAO();

    private JFrame frame;
    private JTextField txtName, txtPrice;
    private JTable table;
    private DefaultTableModel model;
    private Integer selectedId = null;

    public void show() {
        frame = new JFrame("Manage Tests");
        frame.setSize(650, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);

        JLabel title = new JLabel("Manage Tests");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 102, 102));
        title.setBounds(20, 15, 300, 30);
        frame.add(title);

        JLabel nameLbl = new JLabel("Test Name:");
        nameLbl.setBounds(20, 60, 100, 28);
        frame.add(nameLbl);

        txtName = new JTextField();
        txtName.setBounds(120, 60, 220, 28);
        frame.add(txtName);

        JLabel priceLbl = new JLabel("Default Price:");
        priceLbl.setBounds(20, 100, 100, 28);
        frame.add(priceLbl);

        txtPrice = new JTextField();
        txtPrice.setBounds(120, 100, 220, 28);
        frame.add(txtPrice);

        JButton addBtn = new JButton("Add");
        addBtn.setBounds(20, 140, 90, 32);
        addBtn.setBackground(new Color(0, 153, 76));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        frame.add(addBtn);

        JButton updateBtn = new JButton("Update");
        updateBtn.setBounds(120, 140, 90, 32);
        updateBtn.setBackground(new Color(33, 150, 243));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFocusPainted(false);
        frame.add(updateBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBounds(220, 140, 90, 32);
        deleteBtn.setBackground(new Color(204, 51, 51));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        frame.add(deleteBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setBounds(320, 140, 90, 32);
        clearBtn.setBackground(new Color(128, 128, 128));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        frame.add(clearBtn);

        String[] columns = {"ID", "Test Name", "Default Price"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 190, 590, 260);
        frame.add(sp);

        testDAO.loadTests(model);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selectedId = (Integer) model.getValueAt(row, 0);
                txtName.setText(model.getValueAt(row, 1).toString());
                txtPrice.setText(model.getValueAt(row, 2).toString());
            }
        });

        addBtn.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Test name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double price;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Enter a valid price!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (testDAO.addTest(name, price)) {
                testDAO.loadTests(model);
                clearForm();
            }
        });

        updateBtn.addActionListener(e -> {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a test to update!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Test name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double price;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Enter a valid price!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (testDAO.updateTest(selectedId, name, price)) {
                testDAO.loadTests(model);
                clearForm();
            }
        });

        deleteBtn.addActionListener(e -> {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a test to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete this test?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (testDAO.deleteTest(selectedId)) {
                    testDAO.loadTests(model);
                    clearForm();
                }
            }
        });

        clearBtn.addActionListener(e -> clearForm());

        frame.setVisible(true);
    }

    private void clearForm() {
        selectedId = null;
        txtName.setText("");
        txtPrice.setText("");
        table.clearSelection();
    }
}