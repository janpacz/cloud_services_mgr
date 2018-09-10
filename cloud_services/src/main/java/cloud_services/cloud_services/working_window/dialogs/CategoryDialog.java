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
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.ManageCategoriesController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CategoryDialog extends ScenarioDescriptionElementDialog {    
    private ObservableList<String> urlParametersList;
    @FXML private ListView urlParametersListView;
    @FXML private TextField urlParameterField;
    @FXML private Button saveUrlParameterButton;
    @FXML private Button removeUrlParameterButton;
    @FXML private Button moveUrlParameterUpButton;
    @FXML private Button moveUrlParameterDownButton;
    protected ArrayList<String> inputFilesList;
    protected ObservableList<String> inputFilesListForView;
    @FXML private ListView inputFilesListView;
    @FXML private TextField inputFileParameterNameField;
    @FXML private ComboBox inputFileParameterTypeComboBox;
    @FXML private Button saveInputFileParameterButton;
    @FXML private Button removeInputFileParameterButton;
    @FXML private Button moveInputFileParameterUpButton;
    @FXML private Button moveInputFileParameterDownButton;
    
    private static final int SUCH_CATEGORY_ALREADY_EXISTS = 3;
    
    public CategoryDialog(WorkflowTaskCategory category) {        
        super("/fxml/dialogs/CategoryDialog.fxml");
        
        urlParametersList = FXCollections.observableArrayList();
        urlParametersListView.setItems(urlParametersList);
        urlParametersListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    int selectedIndex = urlParametersListView.getSelectionModel().getSelectedIndex();
                    if(selectedIndex == -1) {
                        urlParameterField.setText("");
                        removeUrlParameterButton.setDisable(true);
                        moveUrlParameterUpButton.setDisable(true);
                        moveUrlParameterDownButton.setDisable(true);
                    }
                    else {
                        urlParameterField.setText(new_val);
                        removeUrlParameterButton.setDisable(false);
                        if(selectedIndex == 0)
                            moveUrlParameterUpButton.setDisable(true);
                        else
                            moveUrlParameterUpButton.setDisable(false);
                        if(selectedIndex == urlParametersList.size() - 1)
                            moveUrlParameterDownButton.setDisable(true);
                        else
                            moveUrlParameterDownButton.setDisable(false);
                    }
                
            }
        });
        urlParameterField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val.trim().isEmpty())
                        saveUrlParameterButton.setDisable(true);
                    else
                        saveUrlParameterButton.setDisable(false);
            }
        });        
        saveUrlParameterButton.setDisable(true);
        removeUrlParameterButton.setDisable(true);
        moveUrlParameterUpButton.setDisable(true);
        moveUrlParameterDownButton.setDisable(true);
        
        inputFilesList = new ArrayList<>();
        inputFilesListForView = FXCollections.observableArrayList();
        inputFilesListView.setItems(inputFilesListForView);
        inputFilesListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    int selectedIndex = inputFilesListView.getSelectionModel().getSelectedIndex();
                    if(selectedIndex == -1) {
                        inputFileParameterNameField.setText("");
                        removeInputFileParameterButton.setDisable(true);
                        moveInputFileParameterUpButton.setDisable(true);
                        moveInputFileParameterDownButton.setDisable(true);
                    } else {
                        String[] inputFileParameterSplited = new_val.split(" - ");
                        inputFileParameterNameField.setText(inputFileParameterSplited[0]);
                        inputFileParameterTypeComboBox.setValue(inputFileParameterSplited[1]);
                        removeInputFileParameterButton.setDisable(false);
                        if(selectedIndex == 0)
                            moveInputFileParameterUpButton.setDisable(true);
                        else
                            moveInputFileParameterUpButton.setDisable(false);
                        if(selectedIndex == inputFilesList.size() - 1)
                            moveInputFileParameterDownButton.setDisable(true);
                        else
                            moveInputFileParameterDownButton.setDisable(false);
                    }
            }
        });
        inputFileParameterNameField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val.trim().isEmpty())
                        saveInputFileParameterButton.setDisable(true);
                    else
                        saveInputFileParameterButton.setDisable(false);
            }
        });
        inputFileParameterTypeComboBox.setItems(FXCollections.observableArrayList(ManageCategoriesController.SINGLE_INPUT_FILE,
                    ManageCategoriesController.MULTIPLE_INPUT_FILES
                ));
        inputFileParameterTypeComboBox.setValue(ManageCategoriesController.SINGLE_INPUT_FILE);
        saveInputFileParameterButton.setDisable(true);
        removeInputFileParameterButton.setDisable(true);
        moveInputFileParameterUpButton.setDisable(true);
        moveInputFileParameterDownButton.setDisable(true);
        
        addElementQuestion = "Czy rzeczywiście chcesz dodać nową kategorię?";
        editElementQuestion = "Czy rzeczywiście chcesz dokonać zmian w tej kategorii?";
        suchElementAlreadyExistsCommunicate = "Posiadasz już inną kategorię o takiej nazwie";    
        addElementUrlString = "/category/add";    
        editElementUrlString = "/category/edit";
        addElementSuccessCommunicate = "Kategoria została utworzona";
        editElementSuccessCommunicate = "Edycja kategorii zakończona sukcesem";
        
        if(category == null) {
            setDialogForElementAddOrCopy("Dodaj nową kategorię");
        } else {
            nameField.setText(category.getName());
            if(category.getDescription() != null)
                descriptionTextArea.setText(category.getDescription());
            for(String urlParameter : category.getUrlParameters())
                urlParametersList.add(urlParameter);
            for(String inputFileInfo : category.getInputFilesInfos().keySet()) {
                inputFilesList.add(inputFileInfo);
                if(category.getInputFilesInfos().get(inputFileInfo).equals(WorkflowTaskCategory.INPUT_FILE_TYPE_SINGLE))
                    inputFilesListForView.add(inputFileInfo + " - " + ManageCategoriesController.SINGLE_INPUT_FILE);
                else
                    inputFilesListForView.add(inputFileInfo + " - " + ManageCategoriesController.MULTIPLE_INPUT_FILES);
            }
            if(category.getOwner().equals(MainApp.getCurrentUser())) {
                setDialogForElementEdition();
            } else {
                setDialogForElementAddOrCopy("Stwórz kopię kategorii " + category.getNameAndOwner());
            }
        }        
    }
    
    @Override
    protected void setDialogForElementEdition() {
        this.setTitle("Edytuj lub stwórz kopię kategorii " + nameField.getText().trim() + "(" + MainApp.getCurrentUser() + ")");
        super.setDialogForElementEdition();
    }
    
    @FXML
    private void saveUrlParameterButtonAction(ActionEvent event) {
        String previousUrlParameterName = (String) urlParametersListView.getSelectionModel().getSelectedItem();
        String urlParameterName = urlParameterField.getText().trim();
        if(urlParametersList.contains(urlParameterName)) {
            if((previousUrlParameterName == null) || (! previousUrlParameterName.equals(urlParameterName))) {
                Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można dodać parametru o takiej nazwie", 
                        "Parametr w adresie url o takiej nazwie już istnieje");
                alert.showAndWait();
            }
        } else if(previousUrlParameterName == null) {
            urlParametersList.add(urlParameterName);
            urlParametersListView.getSelectionModel().selectLast();                      
        } else {
            int selectedIndex = urlParametersListView.getSelectionModel().getSelectedIndex();
            urlParametersList.set(selectedIndex, urlParameterName);    
            urlParametersListView.getSelectionModel().select(selectedIndex);
        }
    }
    
    @FXML
    private void removeUrlParameterButtonAction(ActionEvent event) {
        urlParametersList.remove(urlParametersListView.getSelectionModel().getSelectedIndex()); 
    }
    
    @FXML
    private void moveUrlParameterUpButtonAction(ActionEvent event) {
        int selectedIndex = urlParametersListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(urlParametersList, selectedIndex, selectedIndex - 1);
        urlParametersListView.getSelectionModel().selectPrevious();
    }
    
    @FXML
    private void moveUrlParameterDownButtonAction(ActionEvent event) {
        int selectedIndex = urlParametersListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(urlParametersList, selectedIndex, selectedIndex + 1);
        urlParametersListView.getSelectionModel().selectNext();
    }
    
    @FXML
    private void saveInputFileParameterButtonAction(ActionEvent event) {
        int selectedIndex = inputFilesListView.getSelectionModel().getSelectedIndex();
        String previousInputFileParameterName = null;
        if(selectedIndex != -1)
            previousInputFileParameterName = (String) inputFilesList.get(selectedIndex);
        String inputFileParameterName = inputFileParameterNameField.getText().trim();
        
        if(previousInputFileParameterName == null && ! inputFilesList.contains(inputFileParameterName) ) {
            inputFilesList.add(inputFileParameterName);
            inputFilesListForView.add(inputFileParameterName
                    + " - " + inputFileParameterTypeComboBox.getValue());
            inputFilesListView.getSelectionModel().selectLast();     
        } else if(previousInputFileParameterName != null 
                && ( inputFilesList.contains(inputFileParameterName) == previousInputFileParameterName.equals(inputFileParameterName) )
                ) {
            inputFilesList.set(selectedIndex, inputFileParameterName);
            inputFilesListForView.set(selectedIndex, inputFileParameterName
                    + " - " + inputFileParameterTypeComboBox.getValue());
            inputFilesListView.getSelectionModel().select(selectedIndex);
        } else {
            Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można dodać parametru o takiej nazwie", 
                        "Parametr o takiej nazwie już istnieje");
            alert.showAndWait();            
        }
    }
    
    @FXML
    private void removeInputFileParameterButtonAction(ActionEvent event) {
        inputFilesList.remove(inputFilesListView.getSelectionModel().getSelectedIndex()); 
        inputFilesListForView.remove(inputFilesListView.getSelectionModel().getSelectedIndex()); 
    }
    
    @FXML
    private void moveInputFileParameterUpButtonAction(ActionEvent event) {
        int selectedIndex = inputFilesListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(inputFilesList, selectedIndex, selectedIndex - 1);
        Collections.swap(inputFilesListForView, selectedIndex, selectedIndex - 1);
        inputFilesListView.getSelectionModel().selectPrevious();
    }
    
    @FXML
    private void moveInputFileParameterDownButtonAction(ActionEvent event) {
        int selectedIndex = inputFilesListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(inputFilesList, selectedIndex, selectedIndex + 1);
        Collections.swap(inputFilesListForView, selectedIndex, selectedIndex + 1);
        inputFilesListView.getSelectionModel().selectNext();
    }
    
    @Override
    protected JSONObject createElementJSON() {
        JSONObject categoryJSONObject = null;
        
        if(nameField.getText().trim().isEmpty())
            communicateLabel.setText("Nazwa kategorii nie może być pusta");
        else if(nameField.getText().contains("(") || nameField.getText().contains(")"))
            communicateLabel.setText("Nazwa kategorii nie może zawierać znaków otwarcia i zamknięcia nawiasu");
        else {
            communicateLabel.setText("");
            categoryJSONObject = Commons.createLoginJSONObject();     
            if(!descriptionTextArea.getText().trim().isEmpty())
                categoryJSONObject.put(Commons.DESCRIPTION_JSON_KEY, descriptionTextArea.getText());
            if(!urlParametersList.isEmpty()) {
                JSONArray urlParametersArray = new JSONArray();
                for(String urlParameter : urlParametersList) {
                    urlParametersArray.add(urlParameter);
                }
                categoryJSONObject.put(Commons.URL_PARAMETERS_JSON_KEY, urlParametersArray);
            }
            if(!inputFilesListForView.isEmpty()) {
                JSONArray inputFilesParametersArray = new JSONArray();
                for(String inputFileParameter : inputFilesListForView) {
                    JSONObject inputFileObject = new JSONObject();
                    String[] inputFileParameterSplited = inputFileParameter.split(" - ");
                    inputFileObject.put(Commons.INP_F_PARAMETER_NAME_JSON_KEY, inputFileParameterSplited[0]);
                    inputFileObject.put(Commons.INP_F_PARAMETER_TYPE_JSON_KEY, inputFileParameterSplited[1]);
                    inputFilesParametersArray.add(inputFileObject);
                }
                categoryJSONObject.put(Commons.INPUT_FILES_PARAMETERS_JSON_KEY, inputFilesParametersArray);
            }
        }
        
        return categoryJSONObject;
    }
    
    @Override
    protected boolean saveElement(JSONObject categoryJSONObject, String urlString, String successCommunicate) {
        try {
            String resultString = Commons.postRequestToServer(urlString, categoryJSONObject, communicateLabel);
            int loginResult = (int)resultString.charAt(0);
            switch (loginResult) {
                case Commons.OPERATION_SUCCESSFULL:
                    communicateLabel.setText(successCommunicate);
                    return true;
                case SUCH_CATEGORY_ALREADY_EXISTS:
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
