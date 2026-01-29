package ci553.happyshop.client.customer;

import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * A standalone Customer Client that can be run independently without launching the full system.
 * Designed for early-stage testing, though full functionality may require other clients to be active.
 */

public class CustomerClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Creates the Model, View, and Controller objects and links them together for communication.
     * It also creates the DatabaseRW instance via the DatabaseRWFactory and injects it into the CustomerModel.
     * Once the components are linked, the customer interface (view) is started.
     *
     * Also creates the RemoveProductNotifier, which tracks the position of the Customer View
     * and is triggered by the Customer Model when needed.
     */
    @Override
    public void start(Stage window) {
        DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();
        ci553.happyshop.security.AuthService authService = new ci553.happyshop.security.AuthService(databaseRW);

        // Show login dialog first
        ci553.happyshop.client.auth.LoginDialog loginDialog = new ci553.happyshop.client.auth.LoginDialog(authService);
        ci553.happyshop.security.User user = loginDialog.showAndWait();
        if (user == null) {
            System.out.println("Login cancelled or failed. Exiting.");
            return;
        }

        // Save session
        ci553.happyshop.security.UserSession.get().setUser(user);

        // Proceed as before
        CustomerView cusView = new CustomerView();
        CustomerController cusController = new CustomerController();
        CustomerModel cusModel = new CustomerModel();

        cusView.cusController = cusController;
        cusController.cusModel = cusModel;
        cusModel.cusView = cusView;
        cusModel.databaseRW = databaseRW;

        RemoveProductNotifier removeProductNotifier = new RemoveProductNotifier();
        removeProductNotifier.cusView = cusView;
        cusModel.removeProductNotifier = removeProductNotifier;

        // Optionally show username in window title
        window.setTitle("🛒 HappyShop Customer Client - Logged in as: " + user.getUsername());

        cusView.start(window);
    }


}