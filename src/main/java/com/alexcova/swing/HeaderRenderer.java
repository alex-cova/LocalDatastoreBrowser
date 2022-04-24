package com.alexcova.swing;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class HeaderRenderer extends JLabel implements TableCellRenderer {

    public HeaderRenderer() {
        super.setMinimumSize(new Dimension(20, 30));
        super.setPreferredSize(new Dimension(super.getWidth(), 30));
        super.setOpaque(true);
        super.setHorizontalAlignment(SwingConstants.CENTER);
        super.setFont(new Font("Arial", Font.PLAIN, 14));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        setBackground(Color.LIGHT_GRAY);
        setForeground(Color.DARK_GRAY);
        setText(value.toString());

        return this;
    }

    @Override
    public void validate() {
    }

    @Override
    public void revalidate() {
    }

    @Override
    public void invalidate() {
    }

    @Override
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    @Override
    public void repaint(Rectangle r) {
    }

    @Override
    public void repaint() {
    }

    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        // Strings get interned...
        if ("text".equals(propertyName)
                || "labelFor".equals(propertyName)
                || "displayedMnemonic".equals(propertyName)
                || (("font".equals(propertyName) || "foreground".equals(propertyName))
                && oldValue != newValue
                && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {

            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    @Override
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}
