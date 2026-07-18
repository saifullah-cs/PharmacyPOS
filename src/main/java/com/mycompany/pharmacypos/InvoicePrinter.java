package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InvoicePrinter {

    private static final SalesDAO salesDAO = new SalesDAO();

    public static void printInvoice(String invoiceNo) {
        JFrame printFrame = new JFrame("Invoice - " + invoiceNo);
        printFrame.setSize(450, 650);
        printFrame.setLocationRelativeTo(null);

        JTextArea invoiceArea = new JTextArea();
        invoiceArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        invoiceArea.setEditable(false);

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("============================================\n");
        sb.append("           HEALTH HAVEN PHARMACY\n");
        sb.append("============================================\n");
        sb.append("Address:Baharwal Chowk, Guliana Rd,\n");
        sb.append("Opp. Almudassir Trust,Kharian, Pakistan\n");
        sb.append("--------------------------------------------\n");
        sb.append("Invoice No : ").append(invoiceNo).append("\n");
        sb.append("Date       : ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("--------------------------------------------\n\n");

        try {
            List<SalesDAO.SaleLineItem> items = salesDAO.getSalesByInvoice(invoiceNo);
            double grandTotal = 0;

            // Table Header
            sb.append(String.format("%-18s %-5s %-8s %-8s%n",
                    "Medicine", "Qty", "Price", "Total"));
            sb.append("--------------------------------------------\n");

            for (SalesDAO.SaleLineItem item : items) {
                String medicine = item.medicineName;
                if (medicine.length() > 18) {
                    medicine = medicine.substring(0, 18);
                }

                sb.append(String.format(
                        "%-18s %-5d %-8.2f %-8.2f%n",
                        medicine,
                        item.quantity,
                        item.salePrice,
                        item.totalBill));

                grandTotal += item.totalBill;
            }

            sb.append("--------------------------------------------\n");
            sb.append(String.format("       Grand Total : Rs. %.2f%n", grandTotal));
            sb.append("============================================\n");
            sb.append("      Thank You for Choosing Us!\n");
            sb.append("============================================\n");
            sb.append("For any issue or complaint:\n");
            sb.append("Phone: 0370-4433405\n");
            sb.append("============================================\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(printFrame,
                    "Error loading invoice.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        invoiceArea.setText(sb.toString());
        JScrollPane scrollPane = new JScrollPane(invoiceArea);

        JButton printBtn = new JButton("Print");
        printBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    printFrame,
                    "Printing Invoice " + invoiceNo + "...");
            // Real printer logic will be added later.
        });

        printFrame.setLayout(new BorderLayout());
        printFrame.add(scrollPane, BorderLayout.CENTER);
        printFrame.add(printBtn, BorderLayout.SOUTH);
        printFrame.setVisible(true);
    }
}