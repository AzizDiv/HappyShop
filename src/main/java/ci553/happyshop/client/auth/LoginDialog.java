package ci553.happyshop.client.auth;

import ci553.happyshop.security.AuthService;
import ci553.happyshop.security.User;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginDialog {
    private final AuthService authService;

    public LoginDialog(AuthService authService) {
        this.authService = authService;
    }

    public User showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Login");

        TextField tfUser = new TextField();
        tfUser.setPromptText("username");

        PasswordField pf = new PasswordField();
        pf.setPromptText("password");

        Label lbMsg = new Label();

        Button btnLogin = new Button("Login");
        Button btnSignup = new Button("Signup");

        btnLogin.setOnAction(e -> {
            try {
                var user = authService.login(tfUser.getText(), pf.getText());
                if (user != null) {
                    stage.setUserData(user);
                    stage.close();
                } else {
                    lbMsg.setText("Login failed. Check username/password.");
                }
            } catch (Exception ex) {
                lbMsg.setText("Error: " + ex.getMessage());
            }
        });

        btnSignup.setOnAction(e -> {
            SignupDialog signup = new SignupDialog(authService);
            signup.showAndWait();
        });

        HBox hbBtns = new HBox(10, btnLogin, btnSignup);
        hbBtns.setAlignment(Pos.CENTER);

        VBox vb = new VBox(10, tfUser, pf, lbMsg, hbBtns);
        vb.setAlignment(Pos.CENTER);
        vb.setStyle("-fx-padding:20;");
        stage.setScene(new Scene(vb));
        stage.showAndWait();

        Object data = stage.getUserData();
        if (data instanceof User) return (User) data;
        return null;
    }
}
