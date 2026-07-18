package com.mycompany.pharmacypos;

public class InvoiceGenerator {

    private static final SalesDAO salesDAO = new SalesDAO();

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
}