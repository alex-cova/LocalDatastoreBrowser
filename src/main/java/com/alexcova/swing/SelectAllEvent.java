package com.alexcova.swing;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SelectAllEvent extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
        var field = ((JTextField) e.getSource());
        field.setSelectionStart(0);
        field.setSelectionEnd(field.getText().length());
    }
}
