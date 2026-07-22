package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Distributor Management screen, opened from Settings. Same design and
 * workflow as TestManagementFrame: a form on top (Add / Update / Delete /
 * Clear) and a table of all records below, with click-to-edit-in-form
 * behavior via the row selection listener.
 */
public class DistributorManagementFrame {

    private final DistributorDAO distributorDAO = new DistributorDAO();

    private JFrame frame;
    private JTextField txtName, txtPhone;
    private JTextArea txtNotes;
    private JTable table;
    private DefaultTableModel model;
    private Integer selectedId = null;

    public void show() {
        frame = new JFrame("Manage Distributors");
        frame.setSize(700, 650);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(null);

        JLabel title = new JLabel("Manage Distributors");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 102, 102));
        title.setBounds(20, 15, 300, 30);
        frame.add(title);

        JLabel nameLbl = new JLabel("Distributor Name:");
        nameLbl.setBounds(20, 60, 130, 28);
        frame.add(nameLbl);

        txtName = new JTextField();
        txtName.setBounds(150, 60, 220, 28);
        frame.add(txtName);

        JLabel phoneLbl = new JLabel("Phone Number:");
        phoneLbl.setBounds(20, 100, 130, 28);
        frame.add(phoneLbl);

        txtPhone = new JTextField();
        txtPhone.setBounds(150, 100, 220, 28);
        frame.add(txtPhone);

        JLabel notesLbl = new JLabel("Notes / Remarks:");
        notesLbl.setBounds(20, 140, 130, 28);
        frame.add(notesLbl);

        txtNotes = new JTextArea();
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane notesScroll = new JScrollPane(txtNotes);
        notesScroll.setBounds(150, 140, 420, 90);
        frame.add(notesScroll);

        JButton addBtn = new JButton("Add");
        addBtn.setBounds(20, 245, 90, 32);
        addBtn.setBackground(new Color(0, 153, 76));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        frame.add(addBtn);

        JButton updateBtn = new JButton("Update");
        updateBtn.setBounds(120, 245, 90, 32);
        updateBtn.setBackground(new Color(33, 150, 243));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFocusPainted(false);
        frame.add(updateBtn);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setBounds(220, 245, 90, 32);
        deleteBtn.setBackground(new Color(204, 51, 51));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        frame.add(deleteBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setBounds(320, 245, 90, 32);
        clearBtn.setBackground(new Color(128, 128, 128));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setFocusPainted(false);
        frame.add(clearBtn);

        String[] columns = {"ID", "Distributor Name", "Phone Number", "Notes / Remarks"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 295, 640, 300);
        frame.add(sp);

        distributorDAO.loadDistributors(model);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selectedId = (Integer) model.getValueAt(row, 0);
                txtName.setText(model.getValueAt(row, 1).toString());
                Object phoneVal = model.getValueAt(row, 2);
                txtPhone.setText(phoneVal != null ? phoneVal.toString() : "");
                Object notesVal = model.getValueAt(row, 3);
                txtNotes.setText(notesVal != null ? notesVal.toString() : "");
            }
        });

        addBtn.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Distributor name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (distributorDAO.addDistributor(name, txtPhone.getText().trim(), txtNotes.getText().trim())) {
                distributorDAO.loadDistributors(model);
                clearForm();
            }
        });

        updateBtn.addActionListener(e -> {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a distributor to update!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Distributor name is required!", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (distributorDAO.updateDistributor(selectedId, name, txtPhone.getText().trim(), txtNotes.getText().trim())) {
                distributorDAO.loadDistributors(model);
                clearForm();
            }
        });

        deleteBtn.addActionListener(e -> {
            if (selectedId == null) {
                JOptionPane.showMessageDialog(frame, "Select a distributor to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete this distributor?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (distributorDAO.deleteDistributor(selectedId)) {
                    distributorDAO.loadDistributors(model);
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
        txtPhone.setText("");
        txtNotes.setText("");
        table.clearSelection();
    }
}
