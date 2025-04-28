module code.laticegasmodel.simulation.latticegasmodel {
    requires javafx.controls;
    requires javafx.fxml;


    opens code.simulation.latticegasmodel to javafx.fxml;
    exports code.simulation.latticegasmodel;
}