package com.mycompany.pharmacypos;

import java.time.LocalDateTime;
import java.util.List;

public class InvoiceGenerator {

    private static final SalesDAO salesDAO = new SalesDAO();
    private static final TestDAO testDAO = new TestDAO();

    public static String getNextInvoiceNumber() {
        String invoice = "INV-000001";
        try {
            String last = salesDAO.getLastInvoiceNo();
            if (last != null) {
                int num = Integer.parseInt(last.replace("INV-", ""));
                num++;
                invoice = String.format("INV-%06d", num);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return invoice;
    }

    /**
     * Pulls together everything needed to render/print one invoice
     * (medicines, doctor fee, tests) into a single {@link InvoiceData}
     * object. This is the only place that talks to the DAOs for printing
     * purposes - ReceiptFormatter and InvoicePrinter only ever see the
     * resulting data, so generation stays separate from rendering/printing.
     */
    public static InvoiceData buildInvoiceData(String invoiceNo) {
        InvoiceData data = new InvoiceData(invoiceNo, LocalDateTime.now());

        List<SalesDAO.SaleLineItem> items = salesDAO.getSalesByInvoice(invoiceNo);
        for (SalesDAO.SaleLineItem item : items) {
            // Rows with a blank medicine name are placeholder records
            // InvoiceService inserts purely to attach the Dr Fee to an
            // invoice that has no real medicines - not an actual line item.
            if (item.medicineName == null || item.medicineName.trim().isEmpty()) {
                continue;
            }
            data.medicines.add(new InvoiceData.MedicineLine(
                    item.medicineName, item.quantity, item.salePrice, item.totalBill));
        }

        data.doctorFee = salesDAO.getDoctorFeeForInvoice(invoiceNo);

        List<TestDAO.TestLineItem> tests = testDAO.getTestsForInvoice(invoiceNo);
        for (TestDAO.TestLineItem test : tests) {
            data.tests.add(new InvoiceData.TestLine(test.name, test.price));
        }

        return data;
    }
}
