module com.spacekeeperfx {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // transitively pulled by controls, but explicit is fine

    // JDBC / SQLite
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Desktop helpers (optional; FileChooser/Image IO interop, etc.)
    requires java.desktop;

    // Open controller packages to FXMLLoader
    opens com.spacekeeperfx.ui to javafx.fxml;
    opens com.spacekeeperfx.ui.components to javafx.fxml;

    // Export core app packages
    exports com.spacekeeperfx;
    exports com.spacekeeperfx.config;
    exports com.spacekeeperfx.model;
    exports com.spacekeeperfx.service;
    exports com.spacekeeperfx.repository;
    exports com.spacekeeperfx.persistence;
    exports com.spacekeeperfx.persistence.dao;
    exports com.spacekeeperfx.persistence.mapper;
    exports com.spacekeeperfx.util;
    exports com.spacekeeperfx.ui; // if other modules/app code need to refer to controllers
}
