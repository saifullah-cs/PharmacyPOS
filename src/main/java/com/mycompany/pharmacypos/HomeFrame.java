package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.*;

public class HomeFrame {

    private static HomeFrame instance;

    // ================= Window / Layout =================
    private JFrame frame;
    private JPanel contentPanel;

    // ================= Dashboard Cards =================
    private JPanel card1, card2, card3;
    private JLabel medicinesTitle, medicinesValue;
    private JLabel stockTitle, stockValue;
    private JLabel salesTitle, salesValue;

    // ================= Sidebar Buttons =================
    private JButton medicineBtn;
    private JButton lowStockBtn;
    private JButton expiryBtn;
    private JButton salesBtn;
    private JButton historyBtn;
    private JButton settingsBtn;
    private JButton logoutBtn;

    private String role;

    // ================= Data Access =================
    private final DashboardDAO dashboardDAO = new DashboardDAO();

    public HomeFrame(String role) {
        instance = this;
        this.role = role;
        initializeUI();
    }

    // ============================================================
    //  WINDOW SETUP
    // ============================================================

    private void initializeUI() {
        frame = new JFrame("Health Haven POS");
        frame.setSize(1200, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null);

        createSidebar();
        createContentArea();
        attachButtonActions();

        frame.setVisible(true);
    }

    // ============================================================
    //  SIDEBAR
    // ============================================================

    private void createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBounds(0, 0, 250, 700);
        sidebar.setBackground(new Color(11, 40, 55));
        frame.add(sidebar);

        JLabel clinicName = new JLabel("🏥 HEALTH HAVEN");
        clinicName.setForeground(Color.WHITE);
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        clinicName.setBounds(20, 25, 220, 35);
        sidebar.add(clinicName);

        JLabel subTitle = new JLabel("Clinic Pharmacy");
        subTitle.setForeground(new Color(220, 220, 220));
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subTitle.setBounds(22, 58, 180, 20);
        sidebar.add(subTitle);

        JSeparator separator = new JSeparator();
        separator.setBounds(20, 90, 200, 3);
        sidebar.add(separator);

        medicineBtn = createMenuButton("Medicines", 110);
        lowStockBtn = createMenuButton("Low Stock", 160);
        expiryBtn = createMenuButton("Expiry Alert", 210);
        salesBtn = createMenuButton("Sales", 260);
        historyBtn = createMenuButton("Sales History", 310);
        settingsBtn = createMenuButton("Settings", 360);
        logoutBtn = createMenuButton("Logout", 510);

        sidebar.add(medicineBtn);
        sidebar.add(lowStockBtn);
        sidebar.add(expiryBtn);
        sidebar.add(salesBtn);
        sidebar.add(historyBtn);
        sidebar.add(settingsBtn);
        sidebar.add(logoutBtn);

        styleButton(medicineBtn);
        styleButton(lowStockBtn);
        styleButton(expiryBtn);
        styleButton(salesBtn);
        styleButton(historyBtn);
        styleButton(settingsBtn);
        styleButton(logoutBtn);

        // Settings is admin-only for now (manages user passwords)
        boolean isAdmin = "admin".equalsIgnoreCase(role);
        settingsBtn.setVisible(isAdmin);
    }

    private JButton createMenuButton(String text, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(18, y, 214, 44);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(0, 18, 0, 0));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setBackground(new Color(22, 78, 99));
        btn.setForeground(Color.WHITE);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(34, 120, 150));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(22, 78, 99));
            }
        });

        return btn;
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(22, 78, 99));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 18, 0, 0));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(30, 110, 140));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(22, 78, 99));
            }
        });
    }

    private void setActiveButton(JButton activeBtn) {
        JButton[] buttons = { medicineBtn, lowStockBtn, salesBtn, historyBtn, settingsBtn, logoutBtn };

        for (JButton btn : buttons) {
            btn.setBackground(new Color(22, 78, 99));
        }

        activeBtn.setBackground(new Color(0, 150, 136));
    }

    // ============================================================
    //  CONTENT AREA / DASHBOARD
    // ============================================================

    private void createContentArea() {
        contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setBounds(250, 0, 950, 700);
        contentPanel.setBackground(new Color(245, 248, 250));

        JLabel heading = new JLabel("HEALTH HAVEN CLINIC");
        heading.setBounds(60, 30, 500, 40);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 32));
        heading.setForeground(new Color(0, 102, 102));
        contentPanel.add(heading);

        JLabel subHeading = new JLabel("Pharmacy Management System");
        subHeading.setBounds(62, 70, 400, 30);
        subHeading.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        subHeading.setForeground(Color.GRAY);
        contentPanel.add(subHeading);

        JLabel welcome = new JLabel("Welcome, " + LoginFrame.loggedInUser);
        welcome.setBounds(62, 120, 400, 30);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcome.setForeground(new Color(60, 60, 60));
        contentPanel.add(welcome);

        createDashboardCards();

        frame.add(contentPanel);
    }

    private void createDashboardCards() {
        card1 = new JPanel();
        card2 = new JPanel();
        card3 = new JPanel();

        card1.setLayout(null);
        card2.setLayout(null);
        card3.setLayout(null);

        card1.setBackground(Color.WHITE);
        card2.setBackground(Color.WHITE);
        card3.setBackground(Color.WHITE);

        card1.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card2.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        card3.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        card1.setBounds(60, 180, 230, 130);
        card2.setBounds(340, 180, 230, 130);
        card3.setBounds(620, 180, 230, 130);

        contentPanel.add(card1);
        contentPanel.add(card2);
        contentPanel.add(card3);

        addHoverEffect(card1);
        addHoverEffect(card2);
        addHoverEffect(card3);

        addCardAccent(card1, new Color(0, 150, 136));
        addCardAccent(card2, new Color(33, 150, 243));
        addCardAccent(card3, new Color(255, 152, 0));

        // Card 1 - Medicines
        medicinesTitle = createCardTitle("Medicines");
        medicinesValue = createCardValue("0", 36);
        card1.add(medicinesTitle);
        card1.add(medicinesValue);

        // Card 2 - Total Stock
        stockTitle = createCardTitle("Total Stock");
        stockValue = createCardValue("0", 36);
        card2.add(stockTitle);
        card2.add(stockValue);

        // Card 3 - Total Sales
        salesTitle = createCardTitle("Total Sales");
        salesValue = createCardValue("Rs. 0", 28);
        salesValue.setBounds(20, 60, 180, 40);
        card3.add(salesTitle);
        card3.add(salesValue);

        loadDashboardStats();
    }

    private void addCardAccent(JPanel card, Color color) {
        JPanel accent = new JPanel();
        accent.setBounds(0, 0, 8, 130);
        accent.setBackground(color);
        card.add(accent);
    }

    private JLabel createCardTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setBounds(20, 20, 150, 25);
        return label;
    }

    private JLabel createCardValue(String text, int fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        label.setBounds(20, 60, 150, 40);
        return label;
    }

    private void addHoverEffect(JPanel card) {
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(230, 245, 255));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(Color.WHITE);
            }
        });
    }

    // ============================================================
    //  DASHBOARD STATS (live refresh)
    // ============================================================

    private void loadDashboardStats() {
        DashboardDAO.DashboardStats stats = dashboardDAO.getStats();

        medicinesValue.setText(String.valueOf(stats.medicineCount));
        stockValue.setText(String.valueOf(stats.totalStock));
        salesValue.setText("Rs. " + stats.totalSales);
    }

    public void refreshDashboard() {
        loadDashboardStats();
    }

    /** Called from other screens (MedicineManager, SalesFrame) whenever data
     *  changes, so the dashboard cards update live without reopening Home. */
    public static void refreshDashboardStatic() {
        if (instance != null) {
            instance.loadDashboardStats();
        }
    }

    // ============================================================
    //  BUTTON ACTIONS
    // ============================================================

    private void attachButtonActions() {
        medicineBtn.addActionListener(e -> {
            setActiveButton(medicineBtn);
            MedicineManager.open(role);
        });

        lowStockBtn.addActionListener(e -> {
            setActiveButton(lowStockBtn);
            new LowStockFrame().show();
        });

        expiryBtn.addActionListener(e -> {
            setActiveButton(expiryBtn);
            new ExpiryAlert().showExpiry();
        });

        salesBtn.addActionListener(e -> {
            setActiveButton(salesBtn);
            new SalesFrame();
        });

        historyBtn.addActionListener(e -> {
            setActiveButton(historyBtn);
            new SalesHistoryFrame().show();
        });

        settingsBtn.addActionListener(e -> {
            setActiveButton(settingsBtn);
            new SettingsFrame().show();
        });

        logoutBtn.addActionListener(e -> {
            setActiveButton(logoutBtn);

            int option = JOptionPane.showConfirmDialog(
                    frame,
                    "Logout?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION
            );

            if (option == JOptionPane.YES_OPTION) {
                frame.dispose();
                new LoginFrame();
            }
        });
    }

}