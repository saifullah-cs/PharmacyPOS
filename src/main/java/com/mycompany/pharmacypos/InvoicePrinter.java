package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InvoicePrinter {

    private static final SalesDAO salesDAO = new SalesDAO();
    private static final TestDAO testDAO = new TestDAO();

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
        sb.append("           HEALTH HAVEN CLINIC\n");
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
                // Rows with a blank medicine name are placeholder records
                // InvoiceService inserts purely to attach the Dr Fee to an
                // invoice that has no real medicines - not an actual line item.
                if (item.medicineName == null || item.medicineName.trim().isEmpty()) {
                    continue;
                }

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

            double drFee = salesDAO.getDoctorFeeForInvoice(invoiceNo);
            if (drFee > 0) {
                sb.append(String.format("       Dr Fee            : Rs. %.2f%n", drFee));
            }

            List<TestDAO.TestLineItem> tests = testDAO.getTestsForInvoice(invoiceNo);
            double testsTotal = 0;
            if (!tests.isEmpty()) {
                sb.append("--------------------------------------------\n");
                sb.append("Tests:\n");
                for (TestDAO.TestLineItem test : tests) {
                    sb.append(String.format("  %-30s Rs. %.2f%n", test.name, test.price));
                    testsTotal += test.price;
                }
            }

            sb.append(String.format("       Grand Total : Rs. %.2f%n", grandTotal + drFee + testsTotal));
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