package com.alexcova.swing;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class EntitiesRenderer extends JLabel implements TableCellRenderer {

    static final Color SELECTION_COLOR = new Color(27, 166, 214);
    static final Color ZEBRA_COLOR = new Color(240, 240, 240);

    static final Color BORDER_COLOR = new Color(230, 230, 230);

    static final MatteBorder MATTE_BORDER_RIGHT = new MatteBorder(0, 0, 1, 1, BORDER_COLOR);
    static final MatteBorder MATTE_BORDER_LEFT = new MatteBorder(0, 0, 1, 0, BORDER_COLOR);

    static final EmptyBorder EMPTY_BORDER_RIGHT = new EmptyBorder(5, 5, 0, 5);
    static final EmptyBorder EMPTY_BORDER_LEFT = new EmptyBorder(5, 5, 0, 5);

    static final CompoundBorder BORDER_RIGHT = new CompoundBorder(MATTE_BORDER_RIGHT, EMPTY_BORDER_RIGHT);
    static final CompoundBorder BORDER_LEFT = new CompoundBorder(MATTE_BORDER_LEFT, EMPTY_BORDER_LEFT);


    private final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public EntitiesRenderer() {
        setOpaque(true);
        setFont(new Font("Arial", Font.PLAIN, 13));
        setMinimumSize(new Dimension(150, 35));
        setHorizontalTextPosition(SwingConstants.LEADING);
        setHorizontalAlignment(SwingConstants.LEADING);


    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        if (table.getRowHeight() < 35) {
            table.setRowHeight(35);
        }

        if (isSelected) {
            setBackground(SELECTION_COLOR);
            setForeground(Color.WHITE);
        } else {
            setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA_COLOR);
            setForeground(Color.DARK_GRAY);
        }

        setBorder((column < table.getColumnCount() - 1) ? BORDER_RIGHT : BORDER_LEFT);

        if (value == null) {
            setText("");
        } else {
            if (value instanceof FullEntity<?> entity) {
                setText(toJson(entity));
            } else if (value instanceof List<?> list) {
                var t = list.stream()
                        .map(a -> {
                            if (a instanceof Value vl) {
                                return vl.get().toString();
                            }

                            return a.toString();
                        }).collect(Collectors.joining(", ", "[", "]"));

                setText(t);
            } else if (value instanceof Timestamp date) {
                setText(dt.format(date.toDate()));
            } else if (value instanceof Boolean val) {
                var check = new JCheckBox("", val);

                check.setHorizontalAlignment(SwingConstants.CENTER);
                check.setHorizontalTextPosition(SwingConstants.CENTER);
                check.setVerticalAlignment(SwingConstants.CENTER);

                if (isSelected) {
                    check.setBackground(SELECTION_COLOR);
                    check.setForeground(Color.WHITE);
                } else {
                    check.setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA_COLOR);
                    check.setForeground(Color.DARK_GRAY);
                }

                check.setBorderPainted(true);
                check.setBorder((column < table.getColumnCount() - 1) ? BORDER_RIGHT : BORDER_LEFT);

                return check;
            } else {
                setText(value.toString());
            }
        }

        return applyBounds(this, table, column, row);
    }


    private String toJson(FullEntity<?> entity) {
        return EntitySerializer.serialize(entity);
    }

    public final JComponent applyBounds(JComponent component, JTable table, int column, int row) {

        if (table.getColumnCount() < column) return component;

        TableColumn tc = table.getColumnModel().getColumn(column);

        if (tc.getHeaderValue().toString().equals("Name/ID")) {
            component.setPreferredSize(new Dimension(250, table.getRowHeight(row)));
            component.setMinimumSize(component.getPreferredSize());
            component.setMaximumSize(component.getPreferredSize());
            component.setSize(component.getPreferredSize());

            return component;
        }

        component.setPreferredSize(new Dimension(Math.max(150, tc.getPreferredWidth()), table.getRowHeight(row)));
        component.setMinimumSize(new Dimension(Math.max(150, tc.getMinWidth()), table.getRowHeight(row)));
        component.setMaximumSize(new Dimension(Math.max(150, tc.getMaxWidth()), table.getRowHeight(row)));

        return component;
    }


}
