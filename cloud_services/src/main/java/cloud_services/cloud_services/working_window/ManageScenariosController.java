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
import cloud_services.cloud_services.scenario_description_objects.Scenario;
import cloud_services.cloud_services.working_window.dialogs.SetCredentialsDialog;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.json.simple.JSONObject;

public class ManageScenariosController extends ManageScenarioDescriptionElementsController {
    
    @Override
    protected void setGUITexts() {
        titleLabel.setText("Zarządzaj scenariuszami");
        listTitleLabel.setText("Scenariusze:");
        addElementButton.setText("Dodaj i wykonaj scenariusz");
        editOrCreateCopyButton.setText("Edytuj/wykonaj scenariusz");
        removeElementButton.setText("Usuń scenariusz");
    }
    
    @Override
    protected void getDataFromServer() {
        elementsFromDBList = Commons.getScenariosFromServer(communicateLabel);
    }
    
    @Override
    protected void addElementButtonAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ScenarioScene.fxml"));
            ScenarioController controller = new ScenarioController(null);
            fxmlLoader.setController(controller);
            Parent root = fxmlLoader.load();

            titleLabel.getScene().setRoot(root);
            controller.setSchemeCanvasSize();
        } catch (IOException ex) {
            Logger.getLogger(ManageScenariosController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    protected void editOrCreateCopyButtonAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ScenarioScene.fxml"));
            ScenarioController controller = new ScenarioController((Scenario)elementsListView.getSelectionModel().getSelectedItem());
            fxmlLoader.setController(controller);
            Parent root = fxmlLoader.load();

            titleLabel.getScene().setRoot(root);
            controller.setSchemeCanvasSize();
        } catch (IOException ex) {
            Logger.getLogger(ManageScenariosController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    protected void removeElementButtonAction(ActionEvent event) {
        Scenario scenarioToRemove = (Scenario)elementsListView.getSelectionModel().getSelectedItem();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie scenariusza " + scenarioToRemove.getNameAndOwner(), 
                "Czy rzeczywiście chcesz usunąć scenariusz " + scenarioToRemove.getNameAndOwner() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            JSONObject scenarioJSONObject = Commons.createLoginJSONObject();
            scenarioJSONObject.put(Commons.NAME_JSON_KEY, scenarioToRemove.getName());
            try {
                String resultString = Commons.postRequestToServer("/scenario/delete", scenarioJSONObject, communicateLabel);
                int operationResult = (int)resultString.charAt(0);
                if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                    alert = Commons.createAlert(AlertType.INFORMATION, "", "Scenariusz został usunięty");
                    alert.showAndWait();
                } else {
                    alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                    alert.showAndWait();
                }
            } catch (IOException ex){
                alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                alert.showAndWait();
            }
            getElementsFromServer();
        }        
    }
    
    @Override
    protected void setCredentialsButtonAction(ActionEvent event) {
        Scenario scenario = (Scenario)elementsListView.getSelectionModel().getSelectedItem();
        SetCredentialsDialog dialog = new SetCredentialsDialog(scenario);
        dialog.showAndWait();
        getElementsFromServer();
    }
}
