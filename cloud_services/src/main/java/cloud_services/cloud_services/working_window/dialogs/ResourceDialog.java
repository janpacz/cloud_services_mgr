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
import cloud_services.cloud_services.scenario_description_objects.ResourceUrlParameter;
import cloud_services.cloud_services.scenario_description_objects.RestTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.SoapTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.TaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ResourceDialog extends WorkflowObjectDialog {
    @FXML private TextField timePerOperationField;
    @FXML private TextField costPerOperationField;
    @FXML private TextField timePerDataToSendUnitField;
    @FXML private TextField taskCapacityField;
    private ArrayList<WorkflowTaskCategory> chosenCategoriesList;
    private ObservableList<String> categoriesNamesAndOwnersList;
    private ArrayList<TaskExecutionTechnology> categoriesTechnologiesList;
    @FXML private ListView categoriesListView;
    @FXML private TextArea technologyInfoTextArea;
    @FXML private Button moveCategoryUpButton;
    @FXML private Button moveCategoryDownButton;
    @FXML private Button removeCategoryButton;
    @FXML private Button editTechnologyButton;

    private static final int SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST = 3;    
    private static final int SUCH_RESOURCE_ALREADY_EXISTS = 4;
    
    public ResourceDialog(WorkflowResource resource, ArrayList<WorkflowTaskCategory> categoriesHavingCredentialsToList) {
        super("/fxml/dialogs/ResourceDialog.fxml");        
        
        chosenCategoriesList = new ArrayList<>();
        categoriesNamesAndOwnersList = FXCollections.observableArrayList();
        categoriesTechnologiesList = new ArrayList<>();
        categoriesListView.setItems(categoriesNamesAndOwnersList);
        categoriesListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    int selectedIndex = categoriesListView.getSelectionModel().getSelectedIndex();
                    if(selectedIndex == -1) {
                        categoryInfoTextArea.setText("");
                        technologyInfoTextArea.setText("");
                        moveCategoryUpButton.setDisable(true);
                        moveCategoryDownButton.setDisable(true);
                        removeCategoryButton.setDisable(true);
                        editTechnologyButton.setDisable(true);
                    } else {
                        int categoryIndex = Commons.getCategoryIndex(chosenCategoriesList, categoriesNamesAndOwnersList.get(selectedIndex));
                        WorkflowTaskCategory category = chosenCategoriesList.get(categoryIndex);
                        categoryInfoTextArea.setText(category.info());
                        if(categoriesTechnologiesList.get(selectedIndex) != null)
                            technologyInfoTextArea.setText(categoriesTechnologiesList.get(selectedIndex).info());
                        else
                            technologyInfoTextArea.setText("");
                        removeCategoryButton.setDisable(false);
                        editTechnologyButton.setDisable(false);
                        if(selectedIndex == 0)
                            moveCategoryUpButton.setDisable(true);
                        else
                            moveCategoryUpButton.setDisable(false);
                        if(selectedIndex == categoriesNamesAndOwnersList.size() - 1)
                            moveCategoryDownButton.setDisable(true);
                        else
                            moveCategoryDownButton.setDisable(false);
                    }
                }
            }
        );
        
        moveCategoryUpButton.setDisable(true);
        moveCategoryDownButton.setDisable(true);
        removeCategoryButton.setDisable(true);
        editTechnologyButton.setDisable(true);
        
        addElementQuestion = "Czy rzeczywiście chcesz dodać nowy zasób?";
        editElementQuestion = "Czy rzeczywiście chcesz dokonać zmian w tym zasobie?";
        suchElementAlreadyExistsCommunicate = "Posiadasz już inny zasób o takiej nazwie";    
        addElementUrlString = "/resource/add";    
        editElementUrlString = "/resource/edit";
        addElementSuccessCommunicate = "Zasób został utworzony";
        editElementSuccessCommunicate = "Edycja zasobu zakończona sukcesem";
        
        notChosenCategoriesList.addAll(categoriesHavingCredentialsToList);
        if(resource == null) {            
            setDialogForElementAddOrCopy("Dodaj nowy zasób");
        } else {
            nameField.setText(resource.getName());
            timePerOperationField.setText(Long.toString(resource.getTimePerOperation()));
            costPerOperationField.setText(Integer.toString(resource.getCostPerOperation()));
            timePerDataToSendUnitField.setText(Long.toString(resource.getTimePerDataToSendUnit()));
            taskCapacityField.setText(Integer.toString(resource.getTasksCapacity()));
            if(resource.getDescription() != null)
                descriptionTextArea.setText(resource.getDescription());
            LinkedHashMap<String, TaskExecutionTechnology> categoriesNamesAndTechnologies = resource.getCategoriesNamesAndTechnologies();
            ArrayList<String> categoriesNotHavingCredentialsToList = new ArrayList<>();
            for(String categoryNameAndOwner : categoriesNamesAndTechnologies.keySet()) {
                int categoryIndex = Commons.getCategoryIndex(categoriesHavingCredentialsToList, categoryNameAndOwner);
                if(categoryIndex == -1) {
                    categoriesNotHavingCredentialsToList.add(categoryNameAndOwner);
                } else {
                    categoriesNamesAndOwnersList.add(categoryNameAndOwner);
                    categoriesTechnologiesList.add(categoriesNamesAndTechnologies.get(categoryNameAndOwner));
                    WorkflowTaskCategory category = categoriesHavingCredentialsToList.get(categoryIndex);
                    chosenCategoriesList.add(category);
                    notChosenCategoriesList.remove(category);
                }
            }
            if(! categoriesNotHavingCredentialsToList.isEmpty()) {
                String alertText = "Nie masz uprawnień do korzystania z kategorii:";
                for(String categoryNameAndOwner : categoriesNotHavingCredentialsToList) {
                    alertText += "\n" + categoryNameAndOwner;
                }
                alertText += ".\nTworząc zasób w oparciu o " + resource.getNameAndOwner()
                        + "\nmożesz korzystać tylko z kategorii, do których posiadasz uprawnienia.";
                Alert alert = Commons.createAlert(AlertType.INFORMATION, "Brak uprawnień do kategorii", alertText);
                alert.showAndWait();
            }
            if(resource.getOwner().equals(MainApp.getCurrentUser())) {
                setDialogForElementEdition();
            } else {
                setDialogForElementAddOrCopy("Stwórz kopię zasobu " + resource.getNameAndOwner());
            }
        }
    }
    
    @Override
    protected void setDialogForElementEdition() {
        this.setTitle("Edytuj lub stwórz kopię zasobu " + nameField.getText().trim() + "(" + MainApp.getCurrentUser() + ")");
        super.setDialogForElementEdition();
    }
    
    public void addCategory(WorkflowTaskCategory category) {
        chosenCategoriesList.add(category);
        categoriesNamesAndOwnersList.add(category.getNameAndOwner());
        categoriesTechnologiesList.add(null);
        notChosenCategoriesList.remove(category);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {                
                categoriesListView.getSelectionModel().selectLast();
            }
        });
    }
    
    public TaskExecutionTechnology getCategoryTechnology(int index) {
        return categoriesTechnologiesList.get(index);
    }
    
    public void setCategoryTechnology(int index, TaskExecutionTechnology technology) {
        categoriesTechnologiesList.set(index, technology);
    }
    
    @FXML
    private void moveCategoryUpButtonAction(ActionEvent event) {
        int selectedIndex = categoriesListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(categoriesNamesAndOwnersList, selectedIndex, selectedIndex - 1);
        Collections.swap(categoriesTechnologiesList, selectedIndex, selectedIndex - 1);
        categoriesListView.getSelectionModel().selectPrevious();
    }
    
    @FXML
    private void moveCategoryDownButtonAction(ActionEvent event) {
        int selectedIndex = categoriesListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(categoriesNamesAndOwnersList, selectedIndex, selectedIndex + 1);
        Collections.swap(categoriesTechnologiesList, selectedIndex, selectedIndex + 1);
        categoriesListView.getSelectionModel().selectNext();
    }
    
    @FXML
    private void addCategoryButtonAction(ActionEvent event) {
        ChooseResourceCategoriesDialog dialog = new ChooseResourceCategoriesDialog(this);
        dialog.showAndWait();        
    }
    
    @FXML
    private void removeCategoryButtonAction(ActionEvent event) {
        String categoryNameAndOwner = (String)categoriesListView.getSelectionModel().getSelectedItem();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie kategorii " + categoryNameAndOwner, 
                "Czy rzeczywiście chcesz usunąć kategorię " + categoryNameAndOwner + "?");
        Optional<ButtonType> result = alert.showAndWait(); 
        if (result.get() == ButtonType.OK) {
            WorkflowTaskCategory category = null;
            for(WorkflowTaskCategory c : chosenCategoriesList) {
                if(c.getNameAndOwner().equals(categoryNameAndOwner)) {
                    category = c;
                    break;
                }
            }        
            categoriesTechnologiesList.remove(categoriesListView.getSelectionModel().getSelectedIndex()); 
            categoriesNamesAndOwnersList.remove(categoriesListView.getSelectionModel().getSelectedIndex()); 
            chosenCategoriesList.remove(category);
            notChosenCategoriesList.add(category);
        }
    }
    
    @FXML
    private void editTechnologyButtonAction(ActionEvent event) {
        int selectedIndex = categoriesListView.getSelectionModel().getSelectedIndex();
        int categoryIndex = Commons.getCategoryIndex(chosenCategoriesList, categoriesNamesAndOwnersList.get(selectedIndex));
        WorkflowTaskCategory category = chosenCategoriesList.get(categoryIndex);
        EditTechnologyDialog dialog = new EditTechnologyDialog(this, category, selectedIndex);
        dialog.showAndWait(); 
        if(categoriesTechnologiesList.get(selectedIndex) != null)
            technologyInfoTextArea.setText(categoriesTechnologiesList.get(selectedIndex).info());
    }
    
    @Override
    protected JSONObject createElementJSON() {
        JSONObject resourceJSONObject = null;
        Integer costPerOperation = -1, tasksCapacity = -1;
        Long timePerOperation= -1L, timePerDataToSendUnit= -1L;
        
        if(nameField.getText().trim().isEmpty())
            communicateLabel.setText("Nazwa zasobu nie może być pusta");
        else if(nameField.getText().contains("(") || nameField.getText().contains(")"))
            communicateLabel.setText("Nazwa zasobu nie może zawierać znaków otwarcia i zamknięcia nawiasu");
        else if(timePerOperationField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać czas wykonania operacji na zasobie");
        else if(costPerOperationField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać koszt wykonania operacji na zasobie");
        else if(timePerDataToSendUnitField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać czas wysłania jednostki danych do zasobu");
        else if(taskCapacityField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać ilość zadań wykonywanych jednocześnie");
        else {
            try {
                timePerOperation = Long.parseUnsignedLong(timePerOperationField.getText().trim());
                if(timePerOperation == 0L)
                    throw new NumberFormatException();
            } catch(NumberFormatException ex) {
                timePerOperation = -1L;
                communicateLabel.setText("Czas wykonania operacji musi być liczbą całkowitą dodatnią");
            }
            if(timePerOperation != -1L) {
                try {
                    costPerOperation = Integer.parseUnsignedInt(costPerOperationField.getText().trim());
                } catch(NumberFormatException ex) {
                    costPerOperation = -1;
                    communicateLabel.setText("Koszt wykonania operacji musi być liczbą całkowitą dodatnią lub wynosić zero");
                }
                if(costPerOperation != -1) {
                    try {
                        timePerDataToSendUnit = Long.parseUnsignedLong(timePerDataToSendUnitField.getText().trim());
                        if(timePerDataToSendUnit == 0L)
                            throw new NumberFormatException();
                    } catch(NumberFormatException ex) {
                        timePerDataToSendUnit = -1L;
                        communicateLabel.setText("Czas wysłania jednostki danych do zasobu musi być liczbą całkowitą dodatnią");
                    }
                    if(timePerDataToSendUnit != -1L) {
                        try {
                            tasksCapacity = Integer.parseUnsignedInt(taskCapacityField.getText().trim());
                            if(tasksCapacity == 0)
                                throw new NumberFormatException();
                        } catch(NumberFormatException ex) {
                            tasksCapacity = -1;
                            communicateLabel.setText("Ilość zadań wykonywanych jednocześnie musi być liczbą całkowitą dodatnią");
                        }
                    }
                }
            }
            if((timePerOperation != -1L) && (costPerOperation != -1) && (timePerDataToSendUnit != -1L) && (tasksCapacity != -1)) {
                communicateLabel.setText("");
                resourceJSONObject = Commons.createLoginJSONObject();
                resourceJSONObject.put(Commons.OPERATION_TIME_JSON_KEY, timePerOperation);
                resourceJSONObject.put(Commons.OPERATION_COST_JSON_KEY, costPerOperation);
                resourceJSONObject.put(Commons.DATA_SEND_TIME_JSON_KEY, timePerDataToSendUnit);
                resourceJSONObject.put(Commons.TASKS_AT_ONCE_AMOUNT_JSON_KEY, tasksCapacity);
                if(!descriptionTextArea.getText().trim().isEmpty())
                    resourceJSONObject.put(Commons.DESCRIPTION_JSON_KEY, descriptionTextArea.getText());
                if(!categoriesNamesAndOwnersList.isEmpty()) {
                    JSONArray categoriesJSONArray = new JSONArray();
                    for(int i=0; i<categoriesNamesAndOwnersList.size(); i++) {
                        if(categoriesTechnologiesList.get(i) == null) {
                            communicateLabel.setText("Kategoria " + categoriesNamesAndOwnersList.get(i) + " nie ma ustawionej technologii");
                            return null;
                        } else {
                            JSONObject categoryJSONObject = new JSONObject();
                            String categoryNameAndOwner = categoriesNamesAndOwnersList.get(i);
                            String categoryName = categoryNameAndOwner.substring(0, categoryNameAndOwner.indexOf("("));
                            String categoryOwner = categoryNameAndOwner.substring(categoryNameAndOwner.indexOf("(") + 1, categoryNameAndOwner.indexOf(")"));
                            categoryJSONObject.put(Commons.CATEGORY_OWNER_JSON_KEY, categoryOwner);
                            categoryJSONObject.put(Commons.CATEGORY_NAME_JSON_KEY, categoryName);
                            JSONObject categoryTechnologyJSONObject = new JSONObject();
                            TaskExecutionTechnology technology = categoriesTechnologiesList.get(i);
                            categoryTechnologyJSONObject.put(Commons.TECHNOLOGY_URL_JSON_KEY, technology.getUrl());
                            JSONArray urlParametersJSONArray = new JSONArray();
                            LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder = technology.getUrlParametersNamesAndOrder();
                            for(String resourceUrlParameterName : urlParametersNamesAndOrder.keySet()) {
                                JSONObject urlParameterJSONObject = new JSONObject();
                                urlParameterJSONObject.put(Commons.TECHNOLOGY_URL_PARAMETER_NAME, resourceUrlParameterName);
                                urlParameterJSONObject.put(Commons.TECHNOLOGY_URL_PARAMETER_TYPE, urlParametersNamesAndOrder.get(resourceUrlParameterName).getType());
                                urlParameterJSONObject.put(Commons.TECHNOLOGY_URL_PARAMETER_VALUE, urlParametersNamesAndOrder.get(resourceUrlParameterName).getValue());
                                urlParametersJSONArray.add(urlParameterJSONObject);
                            }
                            categoryTechnologyJSONObject.put(Commons.TECHNOLOGY_URL_PARAMETERS_JSON_KEY, urlParametersJSONArray);
                            if(technology.getTechnologyName().equals(RestTaskExecutionTechnology.REST_TECHNOLOGY_NAME)) {
                                categoryTechnologyJSONObject.put(Commons.TECHNOLOGY_TYPE_JSON_KEY, Commons.REST_TECHNOLOGY_TYPE);
                                categoryTechnologyJSONObject.put(Commons.REST_EXPECTED_RESPONSE_CODE, ((RestTaskExecutionTechnology)technology).getExpectedResponseCode());
                            } else {
                                categoryTechnologyJSONObject.put(Commons.TECHNOLOGY_TYPE_JSON_KEY, Commons.SOAP_TECHNOLOGY_TYPE);
                                SoapTaskExecutionTechnology soapTechnology = (SoapTaskExecutionTechnology) technology;
                                categoryTechnologyJSONObject.put(Commons.SOAP_METHOD_NAME, soapTechnology.getMethodName());
                                categoryTechnologyJSONObject.put(Commons.SOAP_NAMESPACE_PREFIX, soapTechnology.getNamespacePrefix());
                                categoryTechnologyJSONObject.put(Commons.SOAP_NAMESPACE_URI, soapTechnology.getNamespaceUri());
                            }
                            categoryJSONObject.put(Commons.CATEGORY_TECHNOLOGY_JSON_KEY, categoryTechnologyJSONObject);
                            categoriesJSONArray.add(categoryJSONObject);
                        }
                    }
                    resourceJSONObject.put(Commons.CATEGORIES_JSON_KEY, categoriesJSONArray);
                }                    
            }
        }
        
        return resourceJSONObject;
    }
    
    @Override
    protected boolean saveElement(JSONObject resourceJSONObject, String urlString, String successCommunicate) {
        try {
            String resultString = Commons.postRequestToServer(urlString, resourceJSONObject, communicateLabel);
            int loginResult = (int)resultString.charAt(0);
            switch (loginResult) {
                case Commons.OPERATION_SUCCESSFULL:
                    communicateLabel.setText(successCommunicate);
                    return true;
                case SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST:
                    communicateLabel.setText("Brak kategorii lub uprawnień do nich w bazie");                                    
                    String displayString = "W bazie danych brak następujących kategorii lub Twoich uprawnień do nich:";
                    resultString = resultString.substring(1);
                    JSONParser parser = new JSONParser();
                    JSONArray notFoundCategoriesJSONArray = (JSONArray)parser.parse(resultString);
                    for(Object obj : notFoundCategoriesJSONArray) {
                        JSONObject notFoundCategoryJSONObject = (JSONObject) obj;
                        displayString += "\n" + notFoundCategoryJSONObject.get(Commons.CATEGORY_NAME_JSON_KEY)
                                + "(" + notFoundCategoryJSONObject.get(Commons.CATEGORY_OWNER_JSON_KEY) + ")";
                    }
                    Alert alert = Commons.createAlert(AlertType.ERROR, "Brak kategorii lub uprawnień w bazie", displayString);
                    alert.showAndWait(); 
                    break;
                case SUCH_RESOURCE_ALREADY_EXISTS:
                    communicateLabel.setText(suchElementAlreadyExistsCommunicate);
                    break;
                default:
                    communicateLabel.setText("Błąd połączenia lub po stronie serwera");
                    break;
            }                            
        } catch (IOException | ParseException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
        return false;
    }
}
