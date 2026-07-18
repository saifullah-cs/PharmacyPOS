package com.mycompany.pharmacypos;

import javax.swing.JTable;

public class InvoiceService {

    private static final SalesDAO salesDAO = new SalesDAO();

    public static String saveSale(JTable table, String cashier) {
        String invoiceNo = InvoiceGenerator.getNextInvoiceNumber();
        try {
            for (int i = 0; i < table.getRowCount(); i++) {
                String medicine = table.getValueAt(i, 0).toString();
                double price = Double.parseDouble(table.getValueAt(i, 1).toString());
                int qty = Integer.parseInt(table.getValueAt(i, 2).toString());
                double total = Double.parseDouble(table.getValueAt(i, 3).toString());

                // Get expiry date from medicines table
                String expiryDate = salesDAO.getExpiryDate(medicine);

                // Save Sale
                salesDAO.insertSaleRecord(invoiceNo, medicine, qty, price, total, expiryDate, cashier);

                // Update Stock
                salesDAO.updateStock(medicine, qty);
            }
            return invoiceNo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}