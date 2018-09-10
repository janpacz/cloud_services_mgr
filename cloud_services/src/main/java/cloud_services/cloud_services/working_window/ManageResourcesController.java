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
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.dialogs.ResourceDialog;
import cloud_services.cloud_services.working_window.dialogs.SetCredentialsDialog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.json.simple.JSONObject;

public class ManageResourcesController extends ManageScenarioDescriptionElementsController {
    private ArrayList<WorkflowTaskCategory> categoriesList;

    @Override
    protected void setGUITexts() {
        titleLabel.setText("Zarządzaj zasobami");
        listTitleLabel.setText("Zasoby:");
        addElementButton.setText("Dodaj zasób");
        editOrCreateCopyButton.setText("Edytuj/stwórz kopię zasobu");
        removeElementButton.setText("Usuń zasób");
    }

    @Override
    protected void getDataFromServer() {
        categoriesList = Commons.getCategoriesFromServer(communicateLabel);
        elementsFromDBList = Commons.getResourcesFromServer(communicateLabel);
    }
    
    @Override
    protected void setGUIAndElementsGotFromServer() {
        if(elementsFromDBList == null)
            addElementButton.setDisable(true);
        else {
            addElementButton.setDisable(false);
            super.setGUIAndElementsGotFromServer();
        }
    }

    @Override
    protected void addElementButtonAction(ActionEvent event) {
        ResourceDialog dialog = new ResourceDialog(null, categoriesList);
        dialog.showAndWait();
        getElementsFromServer();
    }

    @Override
    protected void editOrCreateCopyButtonAction(ActionEvent event) {
        ResourceDialog dialog = new ResourceDialog((WorkflowResource)elementsListView.getSelectionModel().getSelectedItem(), categoriesList);
        dialog.showAndWait();
        getElementsFromServer();
    }

    @Override
    protected void removeElementButtonAction(ActionEvent event) {
        WorkflowResource resourceToRemove = (WorkflowResource)elementsListView.getSelectionModel().getSelectedItem();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie zasobu " + resourceToRemove.getNameAndOwner(), 
                "Czy rzeczywiście chcesz usunąć zasób " + resourceToRemove.getNameAndOwner() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {            
            JSONObject resourceJSONObject = Commons.createLoginJSONObject();
            resourceJSONObject.put(Commons.NAME_JSON_KEY, resourceToRemove.getName());
            try {
                String resultString = Commons.postRequestToServer("/resource/delete", resourceJSONObject, communicateLabel);
                int operationResult = (int)resultString.charAt(0);
                if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                    alert = Commons.createAlert(AlertType.INFORMATION, "", "Zasób został usunięty");
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
        WorkflowResource resource = (WorkflowResource)elementsListView.getSelectionModel().getSelectedItem();
        SetCredentialsDialog dialog = new SetCredentialsDialog(resource);
        dialog.showAndWait();
        getElementsFromServer();
    }
    
}
