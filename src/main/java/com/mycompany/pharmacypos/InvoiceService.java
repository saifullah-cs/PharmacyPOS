package com.mycompany.pharmacypos;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
public class InvoiceService {
    private static final SalesDAO salesDAO = new SalesDAO();
    private static final TestDAO testDAO = new TestDAO();
    public static String saveSale(JTable table, String cashier, double drFee, DefaultTableModel testsModel) {
        String invoiceNo = InvoiceGenerator.getNextInvoiceNumber();
        try {
            int rowCount = table.getRowCount();

            if (rowCount == 0) {
                // No medicines in this sale (Dr Fee only, and/or Tests only).
                // Still need one sale record tied to this invoice number so the
                // Dr Fee is actually persisted and shows up on the invoice -
                // previously, with no medicine rows, insertSaleRecord() (the only
                // place the fee gets saved) was never called at all.
                // qty/price/total are 0 and no stock update happens for it, since
                // there is no real medicine attached to this placeholder row.
                // expiryDate must be null (not "") - expiry_date is a DATE column,
                // and an empty string there makes the insert fail silently.
                boolean saved = salesDAO.insertSaleRecord(invoiceNo, "", 0, 0, 0, null, cashier, drFee);
                if (!saved) {
                    throw new Exception("Failed to save Dr Fee / Tests-only sale record for invoice " + invoiceNo);
                }
            } else {
                for (int i = 0; i < rowCount; i++) {
                    String medicine = table.getValueAt(i, 0).toString();
                    double price = Double.parseDouble(table.getValueAt(i, 1).toString());
                    int qty = Integer.parseInt(table.getValueAt(i, 2).toString());
                    double total = Double.parseDouble(table.getValueAt(i, 3).toString());
                    // Get expiry date from medicines table
                    String expiryDate = salesDAO.getExpiryDate(medicine);
                    // Store the Dr Fee once per invoice (on the first row only),
                    // so it isn't duplicated/multiplied across every medicine sold.
                    double feeForThisRow = (i == 0) ? drFee : 0.0;
                    // Save Sale
                    boolean saved = salesDAO.insertSaleRecord(invoiceNo, medicine, qty, price, total, expiryDate, cashier, feeForThisRow);
                    if (!saved) {
                        throw new Exception("Failed to save sale record for medicine '" + medicine + "' on invoice " + invoiceNo);
                    }
                    // Update Stock
                    salesDAO.updateStock(medicine, qty);
                }
            }

            // Save any tests attached to this sale
            for (int i = 0; i < testsModel.getRowCount(); i++) {
                String testName = testsModel.getValueAt(i, 0).toString();
                double testPrice = Double.parseDouble(testsModel.getValueAt(i, 1).toString());
                boolean testSaved = testDAO.insertSaleTest(invoiceNo, testName, testPrice);
                if (!testSaved) {
                    throw new Exception("Failed to save test '" + testName + "' on invoice " + invoiceNo);
                }
            }
            return invoiceNo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}