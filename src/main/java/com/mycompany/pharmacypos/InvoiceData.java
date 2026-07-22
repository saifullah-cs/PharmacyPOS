package com.mycompany.pharmacypos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain data holder for a single invoice/receipt.
 * <p>
 * This is the boundary between "fetching the data" (InvoiceGenerator) and
 * "turning it into something printable" (ReceiptFormatter / InvoicePrinter).
 * Neither of those two classes talks to the database or to Swing directly
 * through this object - it is just data.
 */
public class InvoiceData {

    public static class MedicineLine {
        public final String name;
        public final int qty;
        public final double price;
        public final double total;

        public MedicineLine(String name, int qty, double price, double total) {
            this.name = name;
            this.qty = qty;
            this.price = price;
            this.total = total;
        }
    }

    public static class TestLine {
        public final String name;
        public final double price;

        public TestLine(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    public final String invoiceNo;
    public final LocalDateTime dateTime;
    public final List<MedicineLine> medicines = new ArrayList<>();
    public final List<TestLine> tests = new ArrayList<>();
    public double doctorFee = 0.0;

    public InvoiceData(String invoiceNo, LocalDateTime dateTime) {
        this.invoiceNo = invoiceNo;
        this.dateTime = dateTime;
    }

    public double getMedicineTotal() {
        double total = 0;
        for (MedicineLine m : medicines) {
            total += m.total;
        }
        return total;
    }

    public double getTestTotal() {
        double total = 0;
        for (TestLine t : tests) {
            total += t.price;
        }
        return total;
    }

    public double getGrandTotal() {
        return getMedicineTotal() + doctorFee + getTestTotal();
    }

    public boolean hasDoctorFee() {
        return doctorFee > 0;
    }

    public boolean hasTests() {
        return !tests.isEmpty();
    }
}
