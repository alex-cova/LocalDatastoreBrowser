package com.alexcova.swing;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class RendererUtil {
    public static void installRenderer(JTable table, TableCellRenderer render) {
        table.setDefaultRenderer(Number.class, render);
        table.setDefaultRenderer(Boolean.class, render);

        table.setDefaultRenderer(Component.class, render);

        table.setDefaultRenderer(Double.class, render);
        table.setDefaultRenderer(Date.class, render);

        table.setDefaultRenderer(Float.class, render);

        table.setDefaultRenderer(Integer.class, render);
        table.setDefaultRenderer(Icon.class, render);
        table.setDefaultRenderer(ImageIcon.class, render);

        table.setDefaultRenderer(JComponent.class, render);

        table.setDefaultRenderer(LocalDate.class, render);
        table.setDefaultRenderer(LocalDateTime.class, render);
        table.setDefaultRenderer(LocalTime.class, render);
        table.setDefaultRenderer(Long.class, render);

        table.setDefaultRenderer(Object.class, render);

        table.setDefaultRenderer(String.class, render);
    }
}
