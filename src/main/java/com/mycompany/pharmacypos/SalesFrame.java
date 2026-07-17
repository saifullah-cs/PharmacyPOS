package com.mycompany.pharmacypos;

import com.mycompany.pharmacypos.LoginFrame;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SalesFrame {
    JFrame frame;
  
    JButton searchBtn;
    JButton addBtn;
    JButton removeBtn;
    JButton clearBtn;
    JButton completeSaleBtn;
    JButton printBtn;
    JTable billTable;
    DefaultTableModel model;
    JLabel totalLabel;
    JTextField qtyField;
    JLabel medicineLabel;
    JLabel stockLabel;
    JLabel priceLabel;
    JComboBox<String> medicineBox;

    public SalesFrame() {
        frame = new JFrame("Pharmacy POS - Sales");
        frame.setSize(1000,700);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);

        JLabel title = new JLabel("Sales / Billing");
        title.setFont(new Font("Arial",Font.BOLD,22));
        title.setBounds(390,20,250,30);
        frame.add(title);

        JLabel searchLbl = new JLabel("Medicine");
        searchLbl.setBounds(30,80,100,30);
        frame.add(searchLbl);

        medicineBox = new JComboBox<>();
        medicineBox.setEditable(true);
        medicineBox.setBounds(120,80,250,30);
        frame.add(medicineBox);

        JTextField editor = (JTextField) medicineBox.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { showSuggestions(); }
            @Override
            public void removeUpdate(DocumentEvent e) { showSuggestions(); }
            @Override
            public void changedUpdate(DocumentEvent e) { showSuggestions(); }
        });

        searchBtn = new JButton("Search");
        searchBtn.setBounds(390,80,100,30);
        frame.add(searchBtn);

        medicineLabel = new JLabel("Medicine : -");
        medicineLabel.setBounds(30,140,300,30);
        frame.add(medicineLabel);

        stockLabel = new JLabel("Stock : -");
        stockLabel.setBounds(30,180,300,30);
        frame.add(stockLabel);

        priceLabel = new JLabel("Price : -");
        priceLabel.setBounds(30,220,300,30);
        frame.add(priceLabel);

        JLabel qtyLbl = new JLabel("Quantity");
        qtyLbl.setBounds(30,270,100,30);
        frame.add(qtyLbl);

        qtyField = new JTextField();
        qtyField.setBounds(120,270,100,30);
        frame.add(qtyField);

        addBtn = new JButton("Add To Bill");
        addBtn.setBounds(250,270,140,30);
        frame.add(addBtn);

        String columns[] = {"Medicine", "Price", "Quantity", "Total"};
        model = new DefaultTableModel(columns,0);
        billTable = new JTable(model);
        JScrollPane sp = new JScrollPane(billTable);
        sp.setBounds(420,140,530,350);
        frame.add(sp);

        totalLabel = new JLabel("Grand Total : Rs. 0");
        totalLabel.setFont(new Font("Arial",Font.BOLD,18));
        totalLabel.setBounds(420,520,300,30);
        frame.add(totalLabel);

        removeBtn = new JButton("Remove Item");
        removeBtn.setBounds(30,350,160,35);
        frame.add(removeBtn);

        clearBtn = new JButton("Clear Bill");
        clearBtn.setBounds(210,350,160,35);
        frame.add(clearBtn);

            completeSaleBtn = new JButton("Complete Sale");
        completeSaleBtn.setBounds(420,580,180,40);
        frame.add(completeSaleBtn);

        printBtn = new JButton("Print Invoice");
        printBtn.setBounds(630,580,180,40);
        printBtn.setEnabled(false);
        frame.add(printBtn);

        // To share invoiceNo between buttons
        final String[] invoiceNoHolder = new String[1];

        completeSaleBtn.addActionListener(e -> {
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "Bill is empty!");
                return;
            }

            String invoiceNo = InvoiceService.saveSale(billTable, LoginFrame.loggedInUser);

            if (invoiceNo == null) {
                JOptionPane.showMessageDialog(null, "Sale Failed!");
                return;
            }

            invoiceNoHolder[0] = invoiceNo;

            JOptionPane.showMessageDialog(null, "Sale Completed Successfully!\nInvoice No: " + invoiceNo);

            model.setRowCount(0);
            updateGrandTotal();

            medicineLabel.setText("Medicine : -");
            stockLabel.setText("Stock : -");
            priceLabel.setText("Price : -");
                        // Safe Dashboard Refresh

            printBtn.setEnabled(true);
        });

        printBtn.addActionListener(e -> {
            if (invoiceNoHolder[0] != null) {
                InvoicePrinter.printInvoice(invoiceNoHolder[0]);
            } else {
                JOptionPane.showMessageDialog(null, "No invoice to print!");
            }
        });
searchBtn.addActionListener(e -> {

    try {

        Connection con = DBConnection.getConnection();

        String sql =
                "SELECT * FROM medicines WHERE medicine_name LIKE ?";

        PreparedStatement pst =
                con.prepareStatement(sql);

     pst.setString(1, "%" + editor.getText() + "%");

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {

            medicineLabel.setText(
                    "Medicine : " +
                    rs.getString("medicine_name")
            );

            stockLabel.setText(
                    "Stock : " +
                    rs.getInt("quantity")
            );

            priceLabel.setText(
                    "Price : Rs. " +
                    rs.getDouble("sale_price")
            );

        } else {

            JOptionPane.showMessageDialog(
                    null,
                    "Medicine Not Found!"
            );

        }

    } catch (Exception ex) {

        ex.printStackTrace();

    }

});
editor.addActionListener(e -> {

    searchBtn.doClick();

    qtyField.requestFocus();

});
addBtn.addActionListener(e -> {

    try {

        String medicine =
                medicineLabel.getText().replace("Medicine : ", "");

        if (medicine.equals("-")) {

            JOptionPane.showMessageDialog(
                    null,
                    "Search a medicine first!"
            );

            return;

        }

        double price =
                Double.parseDouble(
                        priceLabel.getText()
                                .replace("Price : Rs. ", "")
                );

        int qty =
                Integer.parseInt(qtyField.getText());
        int availableStock =
        Integer.parseInt(
                stockLabel.getText()
                        .replace("Stock : ", "")
        );

if (qty <= 0) {

    JOptionPane.showMessageDialog(
            null,
            "Quantity must be greater than 0!"
    );

    return;
}

if (qty > availableStock) {

    JOptionPane.showMessageDialog(
            null,
            "Only " + availableStock + " items available!"
    );

    return;
}

        boolean found = false;

        for (int i = 0; i < model.getRowCount(); i++) {

            String med =
                    model.getValueAt(i, 0).toString();

            if (med.equalsIgnoreCase(medicine)) {

                int oldQty =
                        Integer.parseInt(
                                model.getValueAt(i, 2).toString()
                        );

                int newQty = oldQty + qty;
                if (newQty > availableStock) {

    JOptionPane.showMessageDialog(
            null,
            "Only " + availableStock + " items available!"
    );

    return;
}

                model.setValueAt(newQty, i, 2);

                model.setValueAt(price * newQty, i, 3);

                found = true;

                break;

            }

        }

        if (!found) {

            model.addRow(new Object[]{

                    medicine,
                    price,
                    qty,
                    price * qty

            });

        }

        updateGrandTotal();

        qtyField.setText("");

       qtyField.setText("");

medicineBox.setSelectedItem("");

medicineLabel.setText("Medicine : -");
stockLabel.setText("Stock : -");
priceLabel.setText("Price : -");

medicineBox.requestFocus();

    }

    catch (Exception ex) {

        JOptionPane.showMessageDialog(
                null,
                "Invalid Quantity!"
        );

    }

});
qtyField.addActionListener(e -> {

    addBtn.doClick();

});

        frame.setVisible(true);
    }
private void showSuggestions() {

    try {

        JTextField editor =
                (JTextField) medicineBox.getEditor().getEditorComponent();

        String text = editor.getText().trim();

        medicineBox.removeAllItems();

        if (text.isEmpty()) {
            medicineBox.hidePopup();
            return;
        }

        Connection con = DBConnection.getConnection();

        PreparedStatement pst = con.prepareStatement(
                "SELECT medicine_name FROM medicines WHERE medicine_name LIKE ? LIMIT 10"
        );

        pst.setString(1, text + "%");

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {

            medicineBox.addItem(
                    rs.getString("medicine_name")
            );

        }

        editor.setText(text);

        medicineBox.showPopup();

    } catch (Exception ex) {

        ex.printStackTrace();

    }

}
private void loadMedicine() {

    try {

        Connection con = DBConnection.getConnection();

        String sql =
                "SELECT * FROM medicines WHERE medicine_name=?";

        PreparedStatement pst =
                con.prepareStatement(sql);

       JTextField editor =
        (JTextField) medicineBox.getEditor().getEditorComponent();

pst.setString(1, editor.getText());

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {

            medicineLabel.setText(
                    "Medicine : " +
                    rs.getString("medicine_name")
            );

            stockLabel.setText(
                    "Stock : " +
                    rs.getInt("quantity")
            );

            priceLabel.setText(
                    "Price : Rs. " +
                    rs.getDouble("sale_price")
            );

        }

    } catch (Exception ex) {

        ex.printStackTrace();

    }

}
private void updateGrandTotal() {

    double grandTotal = 0;

    for (int i = 0; i < model.getRowCount(); i++) {

        grandTotal += Double.parseDouble(
                model.getValueAt(i, 3).toString()
        );

    }

    totalLabel.setText("Grand Total : Rs. " + grandTotal);

}
private void printInvoice(String invoiceNo) {
    try {
        int choice = JOptionPane.showConfirmDialog(frame,
                "Do you want to print Invoice No: " + invoiceNo + "?",
                "Print Invoice",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            // Yahan tum apna printing logic daal sakte ho
            JOptionPane.showMessageDialog(frame, 
                "Invoice " + invoiceNo + " sent to printer.\n(Printing logic can be added later)", 
                "Printing", JOptionPane.INFORMATION_MESSAGE);
            
            // Future mein yahan proper printing code aayega (JasperReport ya thermal printer)
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
}