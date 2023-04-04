module checkout {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires java.logging;
    requires org.joda.time;
    requires javafx.swing;
    requires itextpdf;

    opens application to javafx.graphics;
    opens controller to javafx.fxml;
}