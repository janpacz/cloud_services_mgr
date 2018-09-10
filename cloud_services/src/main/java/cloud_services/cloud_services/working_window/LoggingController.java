/*
 * Copyright (c) 2018, Jan Pączkowski
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package cloud_services.cloud_services.working_window;

import cloud_services.cloud_services.Commons;
import cloud_services.cloud_services.MainApp;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.simple.JSONObject;

public class LoggingController implements Initializable {    
    @FXML private Label communicateLabel;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField; 
    
    @FXML
    private void loginButtonAction(ActionEvent event) {
        if(loginField.getText().trim().isEmpty())
        {
            communicateLabel.setText("Proszę podać login");
            passwordField.setText("");
        }
        else if(passwordField.getText().isEmpty())
        {
            communicateLabel.setText("Proszę podać hasło");
        }
        else
        {
            JSONObject loginJSONObject = new JSONObject();
            loginJSONObject.put(Commons.LOGIN_JSON_KEY, loginField.getText().trim());
            loginJSONObject.put(Commons.PASSWORD_JSON_KEY, passwordField.getText());
            try {
                String resultString = Commons.postRequestToServer("/login", loginJSONObject);
                int loginResult = (int)resultString.charAt(0);  
                switch (loginResult) {
                    case Commons.OPERATION_SUCCESSFULL:
                        MainApp.setCurrentUser(loginField.getText().trim());
                        MainApp.setCurrentPassword(passwordField.getText());
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ManageScenarioDescriptionElementsScene.fxml"));
                            fxmlLoader.setController(new ManageCategoriesController());
                            Parent root = fxmlLoader.load();
                            loginField.getScene().setRoot(root);
                        } catch (IOException ex) {
                            Logger.getLogger(LoggingController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case Commons.USER_DOES_NOT_EXIST:
                        communicateLabel.setText("Nie ma takiego użytkownika");
                        passwordField.setText("");
                        break;
                    case Commons.WRONG_PASSWORD:
                        communicateLabel.setText("Niepoprawne hasło");
                        passwordField.setText("");
                        break;
                    default:
                        communicateLabel.setText("Błąd serwera");
                        passwordField.setText("");
                        break;
                }     
            } catch (IOException ex) {
                communicateLabel.setText("Błąd połączenia z serwerem");
                passwordField.setText("");
            }
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
}
