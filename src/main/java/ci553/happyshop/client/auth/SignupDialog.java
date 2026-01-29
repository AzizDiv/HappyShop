package ci553.happyshop.client.auth;

import ci553.happyshop.security.AuthService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SignupDialog {
    private final AuthService authService;

    public SignupDialog(AuthService authService) {
        this.authService = authService;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Signup");

        TextField tfUser = new TextField();
        tfUser.setPromptText("username");

        PasswordField pf = new PasswordField();
        pf.setPromptText("password");

        PasswordField pf2 = new PasswordField();
        pf2.setPromptText("confirm password");

        Label lbMsg = new Label();

        Button btnCreate = new Button("Create");
        Button btnCancel = new Button("Cancel");

        btnCreate.setOnAction(e -> {
            String u = tfUser.getText();
            String p = pf.getText();
            String p2 = pf2.getText();
            if (!p.equals(p2)) {
                lbMsg.setText("Passwords do not match");
                return;
            }
            try {
                boolean ok = authService.signup(u, p, "CUSTOMER");
                if (ok) {
                    lbMsg.setText("Account created. You can now login.");
                    // optionally close
                    // stage.close();
                } else {
                    lbMsg.setText("Username already exists.");
                }
            } catch (Exception ex) {
                lbMsg.setText("Error: " + ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> stage.close());

        HBox hbBtns = new HBox(10, btnCreate, btnCancel);
        hbBtns.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10, tfUser, pf, pf2, lbMsg, hbBtns);
        vb.setAlignment(Pos.CENTER);
        vb.setStyle("-fx-padding:20;");
        stage.setScene(new Scene(vb));
        stage.showAndWait();
    }
}
