package com.mycompany.pharmacypos;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.sql.Connection;

public class InvoiceService {
    private static final SalesDAO salesDAO = new SalesDAO();
    private static final TestDAO testDAO = new TestDAO();

    /**
     * Saves one sale (medicine rows + Dr Fee + tests) and deducts stock for every
     * medicine sold, as ONE real database transaction.
     *
     * Previously each write (sale record, stock update, test fee) was its own separate
     * database call, with a manual "undo" step run afterwards if a later step failed.
     * That only protected against a Java-level exception (e.g. bad input) - if the app
     * crashed, lost power, or the DB connection dropped between two of those calls,
     * nothing would ever run the undo, and you'd be left with a sale recorded but stock
     * not deducted (or vice versa): a real mismatch between recorded stock and money
     * actually taken, with no way for the app to notice or fix it afterwards.
     *
     * Wrapping every write for one sale in a single connection with autoCommit off means
     * the database itself guarantees they land together: either every row (sale records,
     * stock deductions, test fees) is committed, or - on any failure, including the app
     * crashing before commit() runs - none of it is, because an uncommitted transaction
     * is simply never applied. There's no in-between state to accidentally leave behind.
     */
    public static String saveSale(JTable table, String cashier, double drFee, DefaultTableModel testsModel) {
        String invoiceNo = InvoiceGenerator.getNextInvoiceNumber();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                throw new Exception("Could not connect to the database - sale was not saved.");
            }

            con.setAutoCommit(false);
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
                    boolean saved = salesDAO.insertSaleRecord(con, invoiceNo, "", null, 0, 0, 0, null, cashier, drFee);
                    if (!saved) {
                        throw new Exception("Failed to save Dr Fee / Tests-only sale record for invoice " + invoiceNo);
                    }
                } else {
                    for (int i = 0; i < rowCount; i++) {
                        String medicine = table.getValueAt(i, 0).toString();
                        double price = Double.parseDouble(table.getValueAt(i, 1).toString());
                        int qty = Integer.parseInt(table.getValueAt(i, 2).toString());
                        double total = Double.parseDouble(table.getValueAt(i, 3).toString());
                        // Company Name and MedicineId captured at search time (hidden 5th and
                        // 6th bill columns). Both are removed from the *visible* JTable
                        // (SalesFrame hides column indices 4 and 5), so table.getColumnCount()/
                        // getValueAt() - which are view-based - never see them. Read them from
                        // the underlying model instead, where the columns still exist.
                        TableModel tableModel = table.getModel();
                        Object companyValue = tableModel.getColumnCount() > 4 ? tableModel.getValueAt(i, 4) : null;
                        String company = companyValue == null ? null : companyValue.toString();
                        Object idValue = tableModel.getColumnCount() > 5 ? tableModel.getValueAt(i, 5) : null;
                        int medicineId = idValue == null ? -1 : ((Number) idValue).intValue();
                        // Get expiry date from medicines table (same medicine + company).
                        // Read-only lookup - fine to run on its own connection, outside this
                        // sale's transaction.
                        String expiryDate = salesDAO.getExpiryDate(medicine, company);
                        // Store the Dr Fee once per invoice (on the first row only),
                        // so it isn't duplicated/multiplied across every medicine sold.
                        double feeForThisRow = (i == 0) ? drFee : 0.0;
                        // Save Sale - on the shared connection, so it's part of this transaction.
                        boolean saved = salesDAO.insertSaleRecord(con, invoiceNo, medicine, company, qty, price, total, expiryDate, cashier, feeForThisRow);
                        if (!saved) {
                            throw new Exception("Failed to save sale record for medicine '" + medicine + "' on invoice " + invoiceNo);
                        }
                        // Update Stock - by medicine id whenever it's known (the one identifier
                        // guaranteed unique to the exact row sold), falling back to name+company
                        // only for the rare row with no id on file. Also on the shared connection,
                        // so a failure here rolls back the sale record inserted just above too.
                        boolean stockUpdated = (medicineId != -1)
                                ? salesDAO.updateStockById(con, medicineId, qty)
                                : salesDAO.updateStock(con, medicine, company, qty);
                        if (!stockUpdated) {
                            throw new Exception("Failed to update stock for '" + medicine + "' on invoice " + invoiceNo
                                    + " - not enough stock on hand, or the medicine record is missing. No changes were saved for this sale.");
                        }
                    }
                }

                // Save any tests attached to this sale - same shared connection/transaction.
                for (int i = 0; i < testsModel.getRowCount(); i++) {
                    String testName = testsModel.getValueAt(i, 0).toString();
                    double testPrice = Double.parseDouble(testsModel.getValueAt(i, 1).toString());
                    boolean testSaved = testDAO.insertSaleTest(con, invoiceNo, testName, testPrice);
                    if (!testSaved) {
                        throw new Exception("Failed to save test '" + testName + "' on invoice " + invoiceNo);
                    }
                }

                con.commit();
                return invoiceNo;

            } catch (Exception inner) {
                try {
                    con.rollback();
                } catch (Exception rollbackFailed) {
                    rollbackFailed.printStackTrace();
                }
                throw inner;
            }

        } catch (IllegalStateException schemaIssue) {
            throw schemaIssue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
