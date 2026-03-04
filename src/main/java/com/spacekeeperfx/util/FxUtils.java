package com.spacekeeperfx.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Small helper utilities for JavaFX.
 */
public final class FxUtils {

    private FxUtils() {}

    /** Ensure a runnable executes on the JavaFX application thread. */
    public static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /** Show a simple info dialog. */
    public static void info(String content) {
        runFx(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText(null);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    /** Show an error dialog. */
    public static void error(String content) {
        runFx(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(null);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    /** Open a modal stage with a given root node. */
    public static Stage openModal(Stage owner, String title, Parent root, int width, int height) {
        Stage s = new Stage();
        if (owner != null) s.initOwner(owner);
        s.initModality(Modality.WINDOW_MODAL);
        s.setTitle(title == null ? "" : title);
        s.setScene(new Scene(root, width, height));
        s.show();
        return s;
    }

    /** Load an FXML relative to a class (returns the FXMLLoader for controller access). */
    public static FXMLLoader loadFxml(Class<?> resourceAnchor, String resourcePath) {
        Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        Objects.requireNonNull(resourcePath, "resourcePath");
        URL url = resourceAnchor.getResource(resourcePath);
        if (url == null) throw new IllegalArgumentException("FXML not found: " + resourcePath);
        return new FXMLLoader(url);
    }

    /** Basic autosize of columns based on header and a few sample rows. */
    public static void autosizeColumns(TableView<?> table, int sampleRows) {
        if (table == null) return;
        runFx(() -> {
            for (Object objCol : table.getColumns()) {
                @SuppressWarnings("unchecked")
                TableColumn<Object, ?> col = (TableColumn<Object, ?>) objCol;
                col.setPrefWidth(usefulColumnWidth(col, table, sampleRows));
            }
        });
    }

    private static double usefulColumnWidth(TableColumn<Object, ?> col, TableView<?> table, int sampleRows) {
        double header = stringWidth(col.getText());
        double max = header + 32; // padding
        int limit = Math.min(sampleRows, table.getItems().size());
        for (int i = 0; i < limit; i++) {
            Object item = table.getItems().get(i);
            Object cellData = col.getCellObservableValue(item) == null ? null : col.getCellObservableValue(item).getValue();
            String s = cellData == null ? "" : String.valueOf(cellData);
            max = Math.max(max, stringWidth(s) + 24);
        }
        return Math.min(Math.max(max, 80), 420);
    }

    // crude string width estimate; good enough for initial sizing
    private static double stringWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        return Math.min(8 * s.length(), 360);
    }

    /** File chooser for images. */
    public static File chooseImage(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        return fc.showOpenDialog(owner);
    }

    /** Run a task then call back on FX thread with its result. */
    public static <T> void runAsync(Supplier<T> supplier, Consumer<T> onFx) {
        CompletableFuture
                .supplyAsync(supplier)
                .thenAccept(result -> runFx(() -> onFx.accept(result)));
    }

    public static void runAsync(Runnable background, Runnable onFx) {
        CompletableFuture
                .runAsync(background)
                .thenRun(() -> runFx(onFx));
    }
}
