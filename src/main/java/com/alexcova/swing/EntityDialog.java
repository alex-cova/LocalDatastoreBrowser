package com.alexcova.swing;

import com.google.cloud.datastore.FullEntity;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class EntityDialog extends JDialog {

    private final JEditorPane editor = new JEditorPane();

    public EntityDialog(JFrame parent) {
        super(parent);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(editor);
        add(scrollPane);
        setSize(450, 450);

    }

    public EntityDialog setEntity(@NotNull FullEntity<?> entity) {

        var json = EntitySerializer.serializeBeauty(entity);
        setTitle(entity.getKey().getKind());
        editor.setText(json);
        return this;
    }

    public static void show(JFrame parent, @NotNull FullEntity<?> entity) {
        new EntityDialog(parent)
                .setEntity(entity)
                .setVisible(true);
    }
}
