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
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.working_window.ScenarioController;
import java.util.LinkedHashMap;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class SetUrlParametersDialog extends AskBeforeCloseDialog {
    @FXML private ComboBox parameterNameComboBox;
    @FXML private TextField parameterValueTextField;
    @FXML private Label communicateLabel;
    private ScenarioController controller;
    private WorkflowTask task;
    private LinkedHashMap<String, String> urlParameters;
    
    public SetUrlParametersDialog(ScenarioController controller, WorkflowTask task) {
        super("/fxml/dialogs/SetUrlParametersDialog.fxml");
        
        this.controller = controller;
        this.task = task;
        this.urlParameters = task.getUrlParametersCopy();     
        
        this.setTitle("Ustawienie parametrów w adresie url " + task.getNameWithIndexWithSameNameInScenario());
        
        parameterNameComboBox.setItems(FXCollections.observableArrayList(urlParameters.keySet()));
        parameterNameComboBox.getSelectionModel().selectFirst();
        parameterNameComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    parameterValueTextField.setText(urlParameters.get((String)parameterNameComboBox.getValue()));
                }
        });
        parameterValueTextField.setText(urlParameters.get((String)parameterNameComboBox.getValue()));
        parameterValueTextField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    urlParameters.put((String)parameterNameComboBox.getValue(), parameterValueTextField.getText().trim());
                }
        });
    }
    
    @FXML
    private void saveChangesButtonAction(ActionEvent event) {
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", "Czy rzeczywiscie chcesz zapisać zmiany parametrów w adresie url zadania?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            task.setUrlParameters(urlParameters);
            communicateLabel.setText("Zmiany zostały zapisane");
        }
    }
}
