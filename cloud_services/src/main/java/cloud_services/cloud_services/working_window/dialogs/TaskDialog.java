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
import cloud_services.cloud_services.MainApp;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import java.io.IOException;
import java.util.ArrayList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import org.json.simple.JSONObject;

public class TaskDialog extends WorkflowObjectDialog {
    @FXML private TextField dataSizeField;
    @FXML private TextField operationsAmountField;
    private WorkflowTaskCategory chosenCategory;
    
    private static final int SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST = 3;    
    private static final int SUCH_TASK_ALREADY_EXISTS = 4;
    
    public TaskDialog(WorkflowTask task, ArrayList<WorkflowTaskCategory> categoriesHavingCredentialsToList) {
        super("/fxml/dialogs/TaskDialog.fxml");
        
        notChosenCategoriesList.addAll(categoriesHavingCredentialsToList);
        
        addElementQuestion = "Czy rzeczywiście chcesz dodać nowe zadanie?";
        editElementQuestion = "Czy rzeczywiście chcesz dokonać zmian w tym zadaniu?";
        suchElementAlreadyExistsCommunicate = "Posiadasz już inne zadanie o takiej nazwie";    
        addElementUrlString = "/task/add";    
        editElementUrlString = "/task/edit";
        addElementSuccessCommunicate = "Zadanie została utworzone";
        editElementSuccessCommunicate = "Edycja zadania zakończona sukcesem";
        
        if(task == null) {
            chosenCategory = null;
            setDialogForElementAddOrCopy("Dodaj nowe zadanie");
        } else {
            nameField.setText(task.getName());            
            dataSizeField.setText(Integer.toString(task.getDataToSendSize()));
            operationsAmountField.setText(Integer.toString(task.getOperationsAmount()));
            if(task.getDescription() != null)
                descriptionTextArea.setText(task.getDescription());
            WorkflowTaskCategory taskCategory = task.getCategory();
            int categoryIndex = Commons.getCategoryIndex(categoriesHavingCredentialsToList, taskCategory.getNameAndOwner());
            if(categoryIndex == -1) {
                chosenCategory = null;
                String alertText = "Nie masz uprawnień do korzystania z kategorii " + taskCategory.getNameAndOwner()
                        + ".\nAby stworzyć zadanie w oparciu o " + task.getNameAndOwner()
                        + "\nmusisz wybrać kategorię spośród tych, do których posiadasz uprawnienia.";
                Alert alert = Commons.createAlert(AlertType.INFORMATION, "Brak uprawnień do kategorii", alertText);
                alert.showAndWait();
            } else {
                chosenCategory = taskCategory;
                notChosenCategoriesList.remove(taskCategory);
                categoryInfoTextArea.setText(chosenCategory.info());
            }
            if(task.getOwner().equals(MainApp.getCurrentUser())) {
                setDialogForElementEdition();
            } else {
                setDialogForElementAddOrCopy("Stwórz kopię zadania " + task.getNameAndOwner());
            }
        }
    }
    
    @Override
    protected void setDialogForElementEdition() {
        this.setTitle("Edytuj lub stwórz kopię zadania " + nameField.getText().trim() + "(" + MainApp.getCurrentUser() + ")");
        super.setDialogForElementEdition();
    }
    
    public void setCategory(WorkflowTaskCategory category) {
        if(chosenCategory != null)
            notChosenCategoriesList.add(chosenCategory);
        notChosenCategoriesList.remove(category);
        chosenCategory = category;
        categoryInfoTextArea.setText(chosenCategory.info());
    }
    
    @FXML
    private void chooseCategoryButtonAction(ActionEvent event) {
        ChooseTaskCategoryDialog dialog = new ChooseTaskCategoryDialog(this);
        dialog.showAndWait();
    }
    
     @Override
    protected JSONObject createElementJSON() {
        JSONObject taskJSONObject = null;
        Integer dataToSendSize = -1, operationsAmount = -1;        
        
        if(nameField.getText().trim().isEmpty())
            communicateLabel.setText("Nazwa zadania nie może być pusta");
        else if(nameField.getText().contains("(") || nameField.getText().contains(")"))
            communicateLabel.setText("Nazwa zadania nie może zawierać znaków otwarcia i zamknięcia nawiasu");
        else if(dataSizeField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać rozmiar danych do wysłania");
        else if(operationsAmountField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać ilość operacji do wykonania");
        else if(chosenCategory == null)
            communicateLabel.setText("Musisz wybrać kategorię zadania");
        else {
            try {
                dataToSendSize = Integer.parseUnsignedInt(dataSizeField.getText().trim());
            } catch(NumberFormatException ex) {
                communicateLabel.setText("Rozmiar danych do wysłania musi być liczbą całkowitą dodatnią lub wynosić zero");
            }
            if(dataToSendSize != -1) {
                try {
                    operationsAmount = Integer.parseUnsignedInt(operationsAmountField.getText().trim());
                    if(operationsAmount == 0)
                        throw new NumberFormatException();
                } catch(NumberFormatException ex) {
                    operationsAmount = -1;
                    communicateLabel.setText("Ilość operacji musi być liczbą całkowitą dodatnią");
                }
            }
            if(dataToSendSize != -1 && operationsAmount != -1) {
                communicateLabel.setText("");
                taskJSONObject = Commons.createLoginJSONObject();
                taskJSONObject.put(Commons.DATA_TO_SEND_SIZE_JSON_KEY, dataToSendSize);
                taskJSONObject.put(Commons.OPERATIONS_AMOUNT_JSON_KEY, operationsAmount);
                String categoryName = chosenCategory.getName();
                String categoryOwner = chosenCategory.getOwner();
                taskJSONObject.put(Commons.CATEGORY_OWNER_JSON_KEY, categoryOwner);
                taskJSONObject.put(Commons.CATEGORY_NAME_JSON_KEY, categoryName);
                if(!descriptionTextArea.getText().trim().isEmpty())
                    taskJSONObject.put(Commons.DESCRIPTION_JSON_KEY, descriptionTextArea.getText());                    
            }
        }
        
        return taskJSONObject;
    }
    
    @Override
    protected boolean saveElement(JSONObject taskJSONObject, String urlString, String successCommunicate) {
        try {
            String resultString = Commons.postRequestToServer(urlString, taskJSONObject, communicateLabel);
            int loginResult = (int)resultString.charAt(0);
            switch (loginResult) {
                case Commons.OPERATION_SUCCESSFULL:
                    communicateLabel.setText(successCommunicate);
                    return true;
                case SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST:
                    communicateLabel.setText("Brak kategorii " + chosenCategory.getNameAndOwner() + " lub Twoich uprawnień do niej w bazie danych");
                    break;
                case SUCH_TASK_ALREADY_EXISTS:
                    communicateLabel.setText(suchElementAlreadyExistsCommunicate);
                    break;
                default:
                    communicateLabel.setText("Błąd połączenia lub po stronie serwera");
                    break;
            }                        
        } catch (IOException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
        return false;
    }
}
