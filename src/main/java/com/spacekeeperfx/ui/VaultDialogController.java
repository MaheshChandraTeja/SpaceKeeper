package com.spacekeeperfx.ui;

import com.spacekeeperfx.model.Vault;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Minimal dialog helper for creating/renaming a Vault.
 * No FXML required; returns Optional<Vault>.
 */
public final class VaultDialogController {

    private VaultDialogController() {}

    public static Optional<Vault> createNewVault(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Create Vault");

        TextField nameField = new TextField();
        nameField.setPromptText("Vault name");
        nameField.setPrefColumnCount(24);

        Button ok = new Button("Create");
        ok.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);

        ok.setOnAction(e -> stage.close());
        cancel.setOnAction(e -> {
            nameField.setUserData("CANCEL");
            stage.close();
        });

        GridPane gp = new GridPane();
        gp.setPadding(new Insets(12));
        gp.setHgap(8);
        gp.setVgap(8);
        gp.addRow(0, new Label("Name:"), nameField);
        gp.addRow(1, new Label(), new javafx.scene.layout.HBox(8, ok, cancel));

        stage.setScene(new Scene(gp));
        stage.showAndWait();

        if ("CANCEL".equals(nameField.getUserData())) return Optional.empty();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isBlank()) return Optional.empty();
        return Optional.of(new Vault(name));
    }

    public static Optional<String> renameVault(Window owner, String currentName) {
        TextInputDialog dlg = new TextInputDialog(currentName == null ? "" : currentName);
        dlg.initOwner(owner);
        dlg.setHeaderText(null);
        dlg.setTitle("Rename Vault");
        dlg.setContentText("New name:");
        return dlg.showAndWait().map(String::trim).filter(s -> !s.isBlank());
    }
}
