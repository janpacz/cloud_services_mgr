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
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.json.simple.JSONObject;

public class RegistrationController implements Initializable {
    @FXML private Label communicateLabel;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField repeatPasswordField;
    
    private static final int SUCH_USER_ALREADY_EXISTS = 1;
    
    @FXML
    private void registerButtonAction(ActionEvent event) {
        if(loginField.getText().trim().isEmpty()) {
            communicateLabel.setText("Proszę podać login");
            passwordField.setText("");
            repeatPasswordField.setText("");
        }
        else if(passwordField.getText().isEmpty())
        {
            communicateLabel.setText("Proszę podać hasło");
            repeatPasswordField.setText("");
        }
        else if(repeatPasswordField.getText().isEmpty())
        {
            communicateLabel.setText("Proszę powtórzyć hasło");
            passwordField.setText("");
        }
        else if(!passwordField.getText().equals(repeatPasswordField.getText()))
        {
            communicateLabel.setText("Hasla są różne");
            passwordField.setText("");
            repeatPasswordField.setText("");
        }
        else {
            JSONObject registerJSONObject = new JSONObject();
            registerJSONObject.put(Commons.LOGIN_JSON_KEY, loginField.getText().trim());
            registerJSONObject.put(Commons.PASSWORD_JSON_KEY, passwordField.getText());
            try {
                String resultString = Commons.postRequestToServer("/register", registerJSONObject);
                int registrationResult = (int)resultString.charAt(0);
                switch (registrationResult) {
                    case Commons.OPERATION_SUCCESSFULL:
                        communicateLabel.setText("Użytkownik został zarejestrowany");
                        passwordField.setText("");
                        repeatPasswordField.setText("");
                        break;
                    case SUCH_USER_ALREADY_EXISTS:
                        communicateLabel.setText("Taki użytkownik już istnieje");
                        passwordField.setText("");
                        repeatPasswordField.setText("");
                        break;
                    default:
                        communicateLabel.setText("Błąd serwera");
                        passwordField.setText("");
                        repeatPasswordField.setText("");
                        break;
                }
            } catch (IOException ex) {
                communicateLabel.setText("Błąd połączenia z serwerem");
                passwordField.setText("");
                repeatPasswordField.setText("");
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}
