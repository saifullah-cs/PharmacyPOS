package com.mycompany.pharmacypos;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * These screens use absolute positioning (setLayout(null) + setBounds(...))
 * rather than layout managers, so by default resizing or maximizing the
 * window does nothing to the components inside it - the table/panel just
 * stays its original size with empty space around it.
 *
 * This is a small, targeted fix rather than a full rewrite to layout
 * managers: it keeps one designated "main area" component (usually the
 * JScrollPane holding the screen's table) stretched to fill the window as
 * it's resized, and optionally keeps a bottom-anchored component (like a
 * totals label) pinned to the bottom edge. Existing bounds/behavior are
 * unchanged until the user actually resizes or maximizes the window.
 */
public class ResponsiveUtil {

    /**
     * @param frame        the screen's window
     * @param mainArea     the component (e.g. a JScrollPane) that should grow to fill the window
     * @param rightMargin  space to leave on the right of mainArea
     * @param bottomMargin space to leave below mainArea (e.g. for a totals label)
     * @param bottomAnchored optional component(s) that should stay pinned to the bottom
     *                       of the window (e.g. a "Total Sales" label) instead of growing
     */
    public static void makeResponsive(JFrame frame, JComponent mainArea, int rightMargin, int bottomMargin,
                                       JComponent... bottomAnchored) {
        frame.setResizable(true);

        int initialFrameHeight = frame.getHeight();
        int[] initialOffsets = new int[bottomAnchored.length];
        for (int i = 0; i < bottomAnchored.length; i++) {
            initialOffsets[i] = initialFrameHeight - bottomAnchored[i].getY();
        }

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int contentWidth = frame.getContentPane().getWidth();
                int contentHeight = frame.getContentPane().getHeight();

                int newWidth = Math.max(100, contentWidth - mainArea.getX() - rightMargin);
                int newHeight = Math.max(100, contentHeight - mainArea.getY() - bottomMargin);
                mainArea.setBounds(mainArea.getX(), mainArea.getY(), newWidth, newHeight);

                for (int i = 0; i < bottomAnchored.length; i++) {
                    JComponent anchored = bottomAnchored[i];
                    anchored.setLocation(anchored.getX(), contentHeight - initialOffsets[i]);
                }

                frame.revalidate();
                frame.repaint();
            }
        });
    }
}
