package com.spacekeeperfx.service;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowResizer {
    private final Stage stage;
    private final Region root;   // use your VBox/BorderPane here
    private int border = 6;
    private boolean resizing = false;

    private static final double MIN_W = 600;
    private static final double MIN_H = 420;

    public WindowResizer(Stage stage, Region root) {
        this.stage = stage;
        this.root  = root;

        root.addEventFilter(MouseEvent.MOUSE_MOVED, this::updateCursor);
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> resizing = root.getCursor() != Cursor.DEFAULT);
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::resizeDrag);
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> { resizing = false; root.setCursor(Cursor.DEFAULT); });
    }

    public void setBorderWidth(int px) { this.border = px; }

    private void updateCursor(MouseEvent e) {
        double x = e.getX(), y = e.getY();
        double w = root.getWidth(), h = root.getHeight(); // Region gives width/height

        Cursor c = Cursor.DEFAULT;
        boolean L = x < border, R = x > w - border, T = y < border, B = y > h - border;

        if (L && T) c = Cursor.NW_RESIZE;
        else if (R && T) c = Cursor.NE_RESIZE;
        else if (L && B) c = Cursor.SW_RESIZE;
        else if (R && B) c = Cursor.SE_RESIZE;
        else if (L) c = Cursor.W_RESIZE;
        else if (R) c = Cursor.E_RESIZE;
        else if (T) c = Cursor.N_RESIZE;
        else if (B) c = Cursor.S_RESIZE;

        root.setCursor(c);
    }

    private void resizeDrag(MouseEvent e) {
        if (!resizing || stage.isMaximized()) return;

        Cursor c = root.getCursor();
        double sx = e.getScreenX(), sy = e.getScreenY();

        double x = stage.getX(), y = stage.getY();
        double w = stage.getWidth(), h = stage.getHeight();

        // Horizontal edges
        if (c == Cursor.W_RESIZE || c == Cursor.NW_RESIZE || c == Cursor.SW_RESIZE) {
            double nx = sx;
            w += x - nx; x = nx;
        } else if (c == Cursor.E_RESIZE || c == Cursor.NE_RESIZE || c == Cursor.SE_RESIZE) {
            w = sx - x;
        }

        // Vertical edges
        if (c == Cursor.N_RESIZE || c == Cursor.NE_RESIZE || c == Cursor.NW_RESIZE) {
            double ny = sy;
            h += y - ny; y = ny;
        } else if (c == Cursor.S_RESIZE || c == Cursor.SE_RESIZE || c == Cursor.SW_RESIZE) {
            h = sy - y;
        }

        stage.setX(x); stage.setY(y);
        stage.setWidth(Math.max(w, MIN_W));
        stage.setHeight(Math.max(h, MIN_H));
    }
}
