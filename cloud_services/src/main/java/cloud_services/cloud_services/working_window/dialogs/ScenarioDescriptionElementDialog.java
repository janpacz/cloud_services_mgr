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

package cloud_services.cloud_services.working_window.dialogs;

import cloud_services.cloud_services.Commons;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.simple.JSONObject;

public abstract class ScenarioDescriptionElementDialog extends AskBeforeCloseDialog {
    @FXML protected TextField nameField;
    @FXML protected TextArea descriptionTextArea;
    @FXML protected Label communicateLabel;
    @FXML protected Button saveChangesInElementButton;
    
    protected String addElementQuestion;
    protected String editElementQuestion;
    protected String suchElementAlreadyExistsCommunicate;    
    protected String addElementUrlString;    
    protected String editElementUrlString;
    protected String addElementSuccessCommunicate;
    protected String editElementSuccessCommunicate;
    protected String elementPreviousName;
    
    protected ScenarioDescriptionElementDialog(String fxmlPath) {
        super(fxmlPath);
    }
    
    protected void setDialogForElementAddOrCopy(String title) {
        this.setTitle(title);
        elementPreviousName = "";
        saveChangesInElementButton.setDisable(true);
    }
    
    protected void setDialogForElementEdition() {
        elementPreviousName = nameField.getText().trim();
        saveChangesInElementButton.setDisable(false);
    }
    
    protected abstract JSONObject createElementJSON();
    protected abstract boolean saveElement(JSONObject elementJSONObject, String urlString, String successCommunicate);
    
    @FXML private void saveNewElementButtonAction(ActionEvent event) {
        JSONObject elementJSONObject = createElementJSON();
        if(elementJSONObject != null) {
            if(nameField.getText().trim().equals(elementPreviousName)) {
                communicateLabel.setText(suchElementAlreadyExistsCommunicate);               
            } else {
                Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", addElementQuestion);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    elementJSONObject.put(Commons.NAME_JSON_KEY, nameField.getText().trim());
                    boolean elementSuccessfullySaved = saveElement(elementJSONObject, addElementUrlString, addElementSuccessCommunicate);
                    if(elementSuccessfullySaved)
                        setDialogForElementEdition();
                }
            }
        }
    }
    
    @FXML private void saveChangesInElementButtonAction(ActionEvent event) {
        JSONObject elementJSONObject = createElementJSON();
        if(elementJSONObject != null) {
            Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", editElementQuestion);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                if(nameField.getText().trim().equals(elementPreviousName)) {
                    elementJSONObject.put(Commons.NAME_JSON_KEY, nameField.getText().trim());                
                } else {
                    elementJSONObject.put(Commons.OLD_NAME_JSON_KEY, elementPreviousName);
                    elementJSONObject.put(Commons.NEW_NAME_JSON_KEY, nameField.getText().trim());
                }
                boolean elementSuccessfullySaved = saveElement(elementJSONObject, editElementUrlString, editElementSuccessCommunicate);
                if(( ! nameField.getText().trim().equals(elementPreviousName)) && elementSuccessfullySaved)
                    setDialogForElementEdition();
            }
        }
    }
}
