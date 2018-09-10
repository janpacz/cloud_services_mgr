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
import cloud_services.cloud_services.scenario_description_objects.ScenarioDescriptionElement;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.dialogs.SetCredentialsDialog;
import cloud_services.cloud_services.working_window.dialogs.TaskDialog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.json.simple.JSONObject;

public class ManageTasksController extends ManageScenarioDescriptionElementsController {
    private ArrayList<WorkflowTaskCategory> categoriesList;
    
    @Override
    protected void setGUITexts() {
        titleLabel.setText("Zarządzaj zadaniami");
        listTitleLabel.setText("Zadania:");
        addElementButton.setText("Dodaj zadanie");
        editOrCreateCopyButton.setText("Edytuj/stwórz kopię zadania");
        removeElementButton.setText("Usuń zadanie");
    }
    
    @Override
    protected void getDataFromServer() {
        categoriesList = Commons.getCategoriesFromServer(communicateLabel);
        elementsFromDBList = Commons.getTasksFromServer(communicateLabel);
    }
    
    @Override
    protected void setGUIAndElementsGotFromServer() {
        if(elementsFromDBList == null)
            addElementButton.setDisable(true);
        else {
            addElementButton.setDisable(false);
            for(ScenarioDescriptionElement element : elementsFromDBList) {
                WorkflowTask task = (WorkflowTask) element;
                WorkflowTaskCategory taskCategory = task.getCategory();
                if(! taskCategory.getOwner().equals(MainApp.getCurrentUser())) {
                    int categoryIndex = Commons.getCategoryIndex(categoriesList, taskCategory.getNameAndOwner());
                    if(categoryIndex != -1) {
                        categoriesList.remove(categoryIndex);
                        categoriesList.add(taskCategory);
                    }
                }
            }
            super.setGUIAndElementsGotFromServer();
        }
    }
    
    @Override
    protected void addElementButtonAction(ActionEvent event) {
        TaskDialog dialog = new TaskDialog(null, categoriesList);
        dialog.showAndWait();
        getElementsFromServer();
    }
    
    @Override
    protected void editOrCreateCopyButtonAction(ActionEvent event) {
        TaskDialog dialog = new TaskDialog((WorkflowTask)elementsListView.getSelectionModel().getSelectedItem(), categoriesList);
        dialog.showAndWait();
        getElementsFromServer();
    }
    
    @Override
    protected void removeElementButtonAction(ActionEvent event) {
        WorkflowTask taskToRemove = (WorkflowTask)elementsListView.getSelectionModel().getSelectedItem();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie zadania " + taskToRemove.getNameAndOwner(), 
                "Czy rzeczywiście chcesz usunąć zadanie " + taskToRemove.getNameAndOwner() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            JSONObject taskJSONObject = Commons.createLoginJSONObject();
            taskJSONObject.put(Commons.NAME_JSON_KEY, taskToRemove.getName());
            try {
                String resultString = Commons.postRequestToServer("/task/delete", taskJSONObject, communicateLabel);
                int operationResult = (int)resultString.charAt(0);
                if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                    alert = Commons.createAlert(AlertType.INFORMATION, "", "Zadanie zostało usunięte");
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
        WorkflowTask task = (WorkflowTask)elementsListView.getSelectionModel().getSelectedItem();
        SetCredentialsDialog dialog = new SetCredentialsDialog(task);
        dialog.showAndWait();
        getElementsFromServer();
    }
}
