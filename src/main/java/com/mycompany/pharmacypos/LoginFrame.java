package com.mycompany.pharmacypos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Full-screen, modern login experience for the Pharmacy POS System.
 *
 * Layout: a dark teal "brand" panel on the left with the product identity
 * and soft pharmacy-themed background graphics (cross, capsule, orbiting
 * rings), and a light panel on the right holding a centered, card-style
 * login form. The whole window opens maximized so the design fills the
 * available screen real estate instead of a small fixed-size dialog.
 */
public class LoginFrame {

    public static String loggedInUser = "";

    private final UserDAO userDAO = new UserDAO();

    // Palette - kept consistent with the rest of the app (HomeFrame, etc.)
    private static final Color BRAND_DARK = new Color(11, 40, 55);
    private static final Color BRAND_DARK_2 = new Color(16, 58, 78);
    private static final Color TEAL_ACCENT = new Color(0, 150, 136);
    private static final Color BLUE_ACCENT = new Color(33, 150, 243);
    private static final Color PAGE_BG = new Color(245, 248, 250);
    private static final Color TEXT_DARK = new Color(35, 45, 50);
    private static final Color TEXT_MUTED = new Color(120, 132, 138);
    private static final Color FIELD_BORDER = new Color(222, 228, 231);
    private static final Color FIELD_FOCUS = TEAL_ACCENT;

    private JFrame frame;

    public LoginFrame() {
        frame = new JFrame("Pharmacy POS System - Sign In");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(1000, 650));

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(PAGE_BG);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        BrandPanel brandPanel = new BrandPanel();
        gc.gridx = 0;
        gc.weightx = 0.42;
        root.add(brandPanel, gc);

        JPanel formSide = buildFormSide();
        gc.gridx = 1;
        gc.weightx = 0.58;
        root.add(formSide, gc);

        frame.setContentPane(root);

        // Open full-screen (maximized) when the application starts.
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(PAGE_BG);

        LoginCard card = new LoginCard();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.CENTER;
        side.add(card, gc);

        return side;
    }

    // ------------------------------------------------------------------
    // Left brand panel with soft pharmacy-themed background graphics
    // ------------------------------------------------------------------
    private class BrandPanel extends JPanel {
        BrandPanel() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(60, 60, 60, 60));

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setAlignmentX(Component.LEFT_ALIGNMENT);

            RoundedPanel badgeBg = new RoundedPanel(18, new Color(255, 255, 255, 30));
            badgeBg.setLayout(new GridBagLayout());
            badgeBg.setOpaque(false);
            badgeBg.setMaximumSize(new Dimension(64, 64));
            badgeBg.setPreferredSize(new Dimension(64, 64));
            badgeBg.add(new CrossIcon(28, Color.WHITE));
            badgeBg.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel brandName = new JLabel("PHARMACY POS");
            brandName.setForeground(new Color(255, 255, 255, 200));
            brandName.setFont(new Font("Segoe UI", Font.BOLD, 14));
            brandName.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandName.setBorder(new EmptyBorder(18, 2, 0, 0));

            JLabel welcome1 = new JLabel("Welcome to Pharmacy");
            JLabel welcome2 = new JLabel("POS System");
            for (JLabel l : new JLabel[]{welcome1, welcome2}) {
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 36));
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            welcome1.setBorder(new EmptyBorder(14, 2, 0, 0));

            JLabel subtitle = new JLabel("<html><div style='width:340px'>Please sign in to continue "
                    + "managing inventory, sales, and prescriptions from one secure dashboard.</div></html>");
            subtitle.setForeground(new Color(220, 232, 235));
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitle.setBorder(new EmptyBorder(16, 2, 0, 0));

            JPanel features = new JPanel();
            features.setOpaque(false);
            features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
            features.setAlignmentX(Component.LEFT_ALIGNMENT);
            features.setBorder(new EmptyBorder(34, 0, 0, 0));
            features.add(featureRow("Real-time inventory & low-stock alerts"));
            features.add(Box.createVerticalStrut(12));
            features.add(featureRow("Fast, accurate billing & invoicing"));
            features.add(Box.createVerticalStrut(12));
            features.add(featureRow("Secure, role-based staff access"));

            content.add(badgeBg);
            content.add(brandName);
            content.add(welcome1);
            content.add(welcome2);
            content.add(subtitle);
            content.add(features);

            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.weighty = 1;
            add(content, gc);
        }

        private JPanel featureRow(String text) {
            JPanel row = new JPanel();
            row.setOpaque(false);
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(400, 24));

            JPanel dot = new RoundedPanel(6, new Color(0, 150, 136));
            dot.setPreferredSize(new Dimension(10, 10));
            dot.setMaximumSize(new Dimension(10, 10));

            JLabel label = new JLabel(text);
            label.setForeground(new Color(225, 236, 238));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setBorder(new EmptyBorder(0, 10, 0, 0));

            row.add(dot);
            row.add(label);
            return row;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, BRAND_DARK, w, h, BRAND_DARK_2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Soft decorative rings (healthcare "orbit" motif)
            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(new Color(255, 255, 255, 18));
            g2.draw(new Ellipse2D.Double(w - 260, -160, 420, 420));
            g2.draw(new Ellipse2D.Double(w - 180, -80, 300, 300));
            g2.setColor(new Color(0, 150, 136, 40));
            g2.fill(new Ellipse2D.Double(w - 90, h - 190, 260, 260));

            // Large translucent cross watermark, bottom-right
            g2.setColor(new Color(255, 255, 255, 14));
            drawCross(g2, w - 210, h - 210, 150);

            // Small translucent capsule shapes scattered for texture
            g2.setColor(new Color(255, 255, 255, 16));
            drawCapsule(g2, 60, h - 140, 90, 34, 26);
            drawCapsule(g2, w - 340, 70, 70, 26, 20);

            g2.dispose();
            super.paintComponent(g);
        }

        private void drawCross(Graphics2D g2, int cx, int cy, int size) {
            int arm = size / 3;
            RoundRectangle2D vertical = new RoundRectangle2D.Double(
                    cx - arm / 2.0, cy - size / 2.0, arm, size, 14, 14);
            RoundRectangle2D horizontal = new RoundRectangle2D.Double(
                    cx - size / 2.0, cy - arm / 2.0, size, arm, 14, 14);
            g2.fill(vertical);
            g2.fill(horizontal);
        }

        private void drawCapsule(Graphics2D g2, int x, int y, int len, int thick, int arc) {
            RoundRectangle2D pill = new RoundRectangle2D.Double(x, y, len, thick, arc, arc);
            g2.fill(pill);
        }
    }

    // ------------------------------------------------------------------
    // Centered login card
    // ------------------------------------------------------------------
    private class LoginCard extends RoundedPanel {

        LoginCard() {
            super(22, Color.WHITE);
            setPreferredSize(new Dimension(420, 480));
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(46, 42, 42, 42));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(0, 0, 0, 0);

            JLabel title = new JLabel("Sign In");
            title.setFont(new Font("Segoe UI", Font.BOLD, 26));
            title.setForeground(TEXT_DARK);

            JLabel subtitle = new JLabel("Enter your credentials to access your dashboard");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setForeground(TEXT_MUTED);
            subtitle.setBorder(new EmptyBorder(6, 0, 30, 0));

            JLabel userLabel = fieldLabel("Username");
            RoundedTextField userField = new RoundedTextField();
            userField.setBorder(new EmptyBorder(0, 0, 22, 0));

            JLabel passLabel = fieldLabel("Password");
            RoundedPasswordField passField = new RoundedPasswordField();
            passField.setBorder(new EmptyBorder(0, 0, 6, 0));

            JLabel errorLabel = new JLabel(" ");
            errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            errorLabel.setForeground(new Color(211, 47, 47));
            errorLabel.setBorder(new EmptyBorder(6, 2, 18, 0));

            RoundedButton loginBtn = new RoundedButton("Sign In");
            loginBtn.setBorder(new EmptyBorder(14, 0, 14, 0));

            JLabel footer = new JLabel("Pharmacy POS System \u2022 Secure Staff Login");
            footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            footer.setForeground(TEXT_MUTED);
            footer.setHorizontalAlignment(SwingConstants.CENTER);
            footer.setAlignmentX(Component.CENTER_ALIGNMENT);
            footer.setBorder(new EmptyBorder(26, 0, 0, 0));

            gc.gridy = 0; add(title, gc);
            gc.gridy = 1; add(subtitle, gc);
            gc.gridy = 2; add(userLabel, gc);
            gc.gridy = 3; add(userField, gc);
            gc.gridy = 4; add(passLabel, gc);
            gc.gridy = 5; add(passField, gc);
            gc.gridy = 6; add(errorLabel, gc);
            gc.gridy = 7; add(loginBtn, gc);
            gc.gridy = 8; add(footer, gc);

            Runnable attemptLogin = () -> {
                String role = userDAO.checkLogin(userField.getText(),
                        String.valueOf(passField.getPassword()));
                if (role != null) {
                    loggedInUser = userField.getText();
                    frame.dispose();
                    new HomeFrame(role);
                } else {
                    errorLabel.setText("Invalid username or password. Please try again.");
                }
            };

            loginBtn.addActionListener(e -> attemptLogin.run());
            passField.addActionListener(e -> attemptLogin.run());
        }

        private JLabel fieldLabel(String text) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setForeground(new Color(90, 100, 106));
            l.setBorder(new EmptyBorder(0, 2, 6, 0));
            return l;
        }
    }

    // ------------------------------------------------------------------
    // Reusable modern components
    // ------------------------------------------------------------------

    /** A panel with rounded corners and a flat fill color. */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (radius >= 20) {
                for (int i = 6; i > 0; i--) {
                    g2.setColor(new Color(20, 30, 40, 6));
                    g2.fill(new RoundRectangle2D.Double(i, i + 4, getWidth() - i * 2, getHeight() - i * 2,
                            radius, radius));
                }
            }
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Simple vector cross icon used in the brand badge. */
    private static class CrossIcon extends JComponent {
        private final int size;
        private final Color color;

        CrossIcon(int size, Color color) {
            this.size = size;
            this.color = color;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int arm = size / 3;
            g2.fill(new RoundRectangle2D.Double(cx - arm / 2.0, cy - size / 2.0, arm, size, 4, 4));
            g2.fill(new RoundRectangle2D.Double(cx - size / 2.0, cy - arm / 2.0, size, arm, 4, 4));
            g2.dispose();
        }
    }

    /** Rounded, underline-focus style text field. */
    private static class RoundedTextField extends JTextField {
        private boolean focused = false;

        RoundedTextField() {
            super();
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(new EmptyBorder(13, 18, 11, 14));
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
            setForeground(TEXT_DARK);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override
                public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 200), 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(250, 251, 252));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(focused ? FIELD_FOCUS : FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Double(0.6, 0.6, getWidth() - 1.2, getHeight() - 1.2, 12, 12));
            g2.dispose();
        }
    }

    /** Rounded password field, same treatment as RoundedTextField. */
    private static class RoundedPasswordField extends JPasswordField {
        private boolean focused = false;

        RoundedPasswordField() {
            super();
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(new EmptyBorder(13, 18, 11, 14));
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
            setForeground(TEXT_DARK);
            setEchoChar('\u2022');
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override
                public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 200), 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(250, 251, 252));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(focused ? FIELD_FOCUS : FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.2f));
            g2.draw(new RoundRectangle2D.Double(0.6, 0.6, getWidth() - 1.2, getHeight() - 1.2, 12, 12));
            g2.dispose();
        }
    }

    /** Rounded, gradient-filled call-to-action button with hover feedback. */
    private static class RoundedButton extends JButton {
        private boolean hover = false;

        RoundedButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 200), 48);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c1 = hover ? new Color(0, 170, 152) : TEAL_ACCENT;
            Color c2 = hover ? new Color(20, 130, 190) : BLUE_ACCENT;
            GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
