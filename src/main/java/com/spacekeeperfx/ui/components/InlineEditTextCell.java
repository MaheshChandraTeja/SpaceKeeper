package com.spacekeeperfx.ui.components;

import com.spacekeeperfx.model.Record;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

/**
 * Simple inline-editing cell for TEXT columns.
 * Commits on ENTER or focus lost, cancels on ESC.
 */
public class InlineEditTextCell extends TableCell<Record, String> {

    private TextField textField;

    public InlineEditTextCell() {}

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createTextFieldIfNeeded();
            setText(null);
            setGraphic(textField);
            textField.setText(getItem());
            textField.selectAll();
            textField.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItem());
        setGraphic(null);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item);
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item);
                setGraphic(null);
            }
        }
    }

    private void createTextFieldIfNeeded() {
        if (textField != null) return;
        textField = new TextField(getItem());
        textField.setOnAction(e -> commit());
        textField.focusedProperty().addListener((obs, was, is) -> {
            if (!is) commit();
        });
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                e.consume();
            }
        });
    }

    private void commit() {
        String newVal = textField.getText();
        commitEdit(newVal);
    }
}
