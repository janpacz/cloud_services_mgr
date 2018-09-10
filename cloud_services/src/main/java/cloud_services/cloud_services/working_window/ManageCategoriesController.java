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
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.dialogs.CategoryDialog;
import cloud_services.cloud_services.working_window.dialogs.SetCredentialsDialog;
import java.io.IOException;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import org.json.simple.JSONObject;

public class ManageCategoriesController extends ManageScenarioDescriptionElementsController {    
    public static final String SINGLE_INPUT_FILE = "Pojedynczy plik";
    public static final String MULTIPLE_INPUT_FILES = "Lista plików";    
    
    @Override
    protected void setGUITexts() {
        titleLabel.setText("Zarządzaj kategoriami");
        listTitleLabel.setText("Kategorie:");
        addElementButton.setText("Dodaj kategorię");
        editOrCreateCopyButton.setText("Edytuj/stwórz kopię kategorii");
        removeElementButton.setText("Usuń kategorię");
    }
    
    @Override
    protected void getDataFromServer() {
        elementsFromDBList = Commons.getCategoriesFromServer(communicateLabel);
    }
    
    @FXML
    @Override
    protected void addElementButtonAction(ActionEvent event) {
        CategoryDialog dialog = new CategoryDialog(null);
        dialog.showAndWait();
        getElementsFromServer();
    }
    
    @FXML
    @Override
    protected void editOrCreateCopyButtonAction(ActionEvent event) {
        CategoryDialog dialog = new CategoryDialog((WorkflowTaskCategory)elementsListView.getSelectionModel().getSelectedItem());
        dialog.showAndWait();
        getElementsFromServer();
    }
    
    @FXML
    @Override
    protected void removeElementButtonAction(ActionEvent event) {
        WorkflowTaskCategory categoryToRemove = (WorkflowTaskCategory)elementsListView.getSelectionModel().getSelectedItem();
        String categoryToRemoveName = categoryToRemove.getName();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie kategorii " + categoryToRemoveName, 
                "Czy rzeczywiście chcesz usunąć kategorię " + categoryToRemoveName + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            JSONObject categoryJSONObject = Commons.createLoginJSONObject();
            categoryJSONObject.put(Commons.NAME_JSON_KEY, categoryToRemoveName);
            try {
                String resultString = Commons.postRequestToServer("/category/delete", categoryJSONObject, communicateLabel);
                int operationResult = (int)resultString.charAt(0);
                if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                    alert = Commons.createAlert(AlertType.INFORMATION, "", "Kategoria została usunięta");
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
        WorkflowTaskCategory category = (WorkflowTaskCategory)elementsListView.getSelectionModel().getSelectedItem();
        SetCredentialsDialog dialog = new SetCredentialsDialog(category);
        dialog.showAndWait();
        getElementsFromServer();
    }
}
