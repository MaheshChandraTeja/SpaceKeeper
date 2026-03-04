package com.spacekeeperfx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public final class CreateVaultDialog {
    public record Result(String name, Path file, char[] password) {}

    private CreateVaultDialog() {}

    public static Optional<Result> show(Window owner, Path initialDir) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("New Vault");

        TextField name = new TextField();
        name.setPromptText("Vault name");

        TextField pathField = new TextField();
        Button browse = new Button("Browse…");
        browse.getStyleClass().add("ghost");

        PasswordField pwd = new PasswordField();
        pwd.setPromptText("Password (optional)");
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Confirm password");

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB (*.db)", "*.db"));
        if (initialDir != null && initialDir.toFile().isDirectory()) chooser.setInitialDirectory(initialDir.toFile());
        browse.setOnAction(e -> {
            if (!name.getText().isBlank()) chooser.setInitialFileName(sanitize(name.getText()) + ".db");
            File f = chooser.showSaveDialog(stage);
            if (f != null) pathField.setText(f.getAbsolutePath());
        });

        Button ok = new Button("Create");
        ok.getStyleClass().add("primary");
        ok.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("ghost");
        cancel.setCancelButton(true);

        ok.setOnAction(e -> stage.close());
        cancel.setOnAction(e -> {
            pathField.setUserData("CANCEL");
            stage.close();
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name"), 0, 0); grid.add(name, 1, 0);
        HBox pathBox = new HBox(8, pathField, browse);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        grid.add(new Label("Location"), 0, 1); grid.add(pathBox, 1, 1);
        grid.add(new Label("Password"), 0, 2); grid.add(pwd, 1, 2);
        grid.add(new Label("Confirm"), 0, 3); grid.add(confirm, 1, 3);

        HBox buttons = new HBox(8, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 1, 4);

        stage.setScene(new Scene(grid));
        stage.showAndWait();

        if ("CANCEL".equals(pathField.getUserData())) return Optional.empty();
        String n = name.getText() == null ? "" : name.getText().trim();
        String loc = pathField.getText() == null ? "" : pathField.getText().trim();
        if (n.isBlank() || loc.isBlank()) return Optional.empty();
        char[] pw = null;
        if (!pwd.getText().isBlank() || !confirm.getText().isBlank()) {
            if (!pwd.getText().equals(confirm.getText())) return Optional.empty();
            pw = pwd.getText().toCharArray();
        }
        return Optional.of(new Result(n, Path.of(loc), pw));
    }

    private static String sanitize(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
