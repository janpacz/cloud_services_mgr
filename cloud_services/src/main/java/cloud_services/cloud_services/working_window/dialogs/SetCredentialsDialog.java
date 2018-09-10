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
import cloud_services.cloud_services.scenario_description_objects.ScenarioDescriptionElement;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SetCredentialsDialog extends AskBeforeCloseDialog {
    private ArrayList<String> usersWithCredentialsInDBList;
    private ObservableList<String> usersWithCredentialsList;
    @FXML private ListView usersWithCredentialsListView;
    @FXML private Button addCredentialsButton;
    @FXML private Button moveUserUpButton;
    @FXML private Button moveUserDownButton;
    @FXML private Button removeCredentialsButton;
    @FXML private TextField searchUsersWithoutCredentialsField;
    @FXML private Button searchUsersWithoutCredentialsButton;
    private ObservableList<String> searchUsersWithoutCredentialsList;
    @FXML private ListView searchUsersWithoutCredentialsListView;
    private ObservableList<String> usersWithCredentialsBeingRemovedList;
    @FXML private ListView usersWithCredentialsBeingRemovedListView;
    @FXML private Label communicateLabel;
    
    private ScenarioDescriptionElement scenarioDescriptionElement;
    
    private static final int SUCH_USER_DOES_NOT_EXIST = 3;
    
    public SetCredentialsDialog(ScenarioDescriptionElement scenarioDescriptionElement) {
        super("/fxml/dialogs/SetCredentialsDialog.fxml");
        
        this.scenarioDescriptionElement = scenarioDescriptionElement;        
        
        String title = "Ustawienie uprawnień użytkowników dla ";
        if(scenarioDescriptionElement instanceof WorkflowTask)
            title += "zadania " + scenarioDescriptionElement.getNameAndOwner();
        else if(scenarioDescriptionElement instanceof WorkflowResource)
            title += "zasobu " + scenarioDescriptionElement.getNameAndOwner();
        else if(scenarioDescriptionElement instanceof WorkflowTaskCategory)
            title += "kategorii " + scenarioDescriptionElement.getNameAndOwner();
        else
            title += "scenariusza " + scenarioDescriptionElement.getNameAndOwner();
        this.setTitle(title);
        
        usersWithCredentialsInDBList = new ArrayList<>();
        usersWithCredentialsList = FXCollections.observableArrayList();
        usersWithCredentialsListView.setItems(usersWithCredentialsList);
        usersWithCredentialsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                int selectedIndex = usersWithCredentialsListView.getSelectionModel().getSelectedIndex();
                if(selectedIndex == -1) {
                    moveUserUpButton.setDisable(true);
                    moveUserDownButton.setDisable(true);
                    removeCredentialsButton.setDisable(true);
                } else {
                    if(selectedIndex == 0)
                        moveUserUpButton.setDisable(true);
                    else
                        moveUserUpButton.setDisable(false);
                    if(selectedIndex == usersWithCredentialsList.size()-1)
                        moveUserDownButton.setDisable(true);
                    else
                        moveUserDownButton.setDisable(false);
                    removeCredentialsButton.setDisable(false);
                    searchUsersWithoutCredentialsListView.getSelectionModel().clearSelection();
                    usersWithCredentialsBeingRemovedListView.getSelectionModel().clearSelection();
                }
            }
        });
        
        searchUsersWithoutCredentialsField.textProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val.trim().isEmpty())
                        searchUsersWithoutCredentialsButton.setDisable(true);
                    else
                        searchUsersWithoutCredentialsButton.setDisable(false);
            }
        });        
        
        searchUsersWithoutCredentialsList = FXCollections.observableArrayList();
        searchUsersWithoutCredentialsListView.setItems(searchUsersWithoutCredentialsList);
        searchUsersWithoutCredentialsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                usersWithoutCredentialsListViewValueChanged(new_val, usersWithCredentialsBeingRemovedListView);                
            }
        });
        
        usersWithCredentialsBeingRemovedList = FXCollections.observableArrayList();
        usersWithCredentialsBeingRemovedListView.setItems(usersWithCredentialsBeingRemovedList);
        usersWithCredentialsBeingRemovedListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                usersWithoutCredentialsListViewValueChanged(new_val, searchUsersWithoutCredentialsListView);                
            }
        });
        
        addCredentialsButton.setDisable(true);
        moveUserUpButton.setDisable(true);
        moveUserDownButton.setDisable(true);
        removeCredentialsButton.setDisable(true);
        searchUsersWithoutCredentialsButton.setDisable(true);
        
        getUsersWithCredentialsFromServer("");
    }
    
    private void usersWithoutCredentialsListViewValueChanged(String new_val, ListView otherUsersWithoutCredentialsListView) {
        if(new_val == null) {
            addCredentialsButton.setDisable(true);
        } else {            
            usersWithCredentialsListView.getSelectionModel().clearSelection();
            otherUsersWithoutCredentialsListView.getSelectionModel().clearSelection();
            addCredentialsButton.setDisable(false);
        }
    }
    
    private void getUsersWithCredentialsFromServer(String successCommunicate) {
        usersWithCredentialsInDBList.clear();
        usersWithCredentialsList.clear(); 
        usersWithCredentialsBeingRemovedList.clear();
        JSONObject scenarioDescriptionElementJSON = Commons.createLoginJSONObject();
        scenarioDescriptionElementJSON.put(Commons.NAME_JSON_KEY, scenarioDescriptionElement.getName());
        if(scenarioDescriptionElement instanceof WorkflowTask)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY);
        else if(scenarioDescriptionElement instanceof WorkflowResource)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY);
        else if(scenarioDescriptionElement instanceof WorkflowTaskCategory)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY);
        else
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY);
        try {
            String resultString = Commons.postRequestToServer("/users/get/with_credentials", scenarioDescriptionElementJSON, communicateLabel);
            int operationResult = (int)resultString.charAt(0);
            if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                communicateLabel.setText(successCommunicate);
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray usersWithCredentialsJSONArray = (JSONArray) parser.parse(resultString);
                Collections.sort(usersWithCredentialsJSONArray, Commons.orderJSONKeyComparator);
                for(Object obj : usersWithCredentialsJSONArray) {
                    JSONObject userJSONObject = (JSONObject) obj;
                    usersWithCredentialsInDBList.add((String) userJSONObject.get(Commons.USER_JSON_KEY));
                    usersWithCredentialsList.add((String) userJSONObject.get(Commons.USER_JSON_KEY));
                }
            } else {
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        } catch (IOException | ParseException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
    }
    
    @FXML
    private void addCredentialsButtonAction(ActionEvent event) {
        String userToAdd;
        if(searchUsersWithoutCredentialsListView.getSelectionModel().getSelectedItem() == null) {            
            userToAdd = (String) usersWithCredentialsBeingRemovedListView.getSelectionModel().getSelectedItem();
            usersWithCredentialsBeingRemovedList.remove(userToAdd);
        } else {
            userToAdd = (String) searchUsersWithoutCredentialsListView.getSelectionModel().getSelectedItem();
            searchUsersWithoutCredentialsList.remove(userToAdd);
        }
        usersWithCredentialsList.add(userToAdd);
    }
    
    @FXML
    private void moveUserUpButtonAction(ActionEvent event) {
        int selectedIndex = usersWithCredentialsListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(usersWithCredentialsList, selectedIndex, selectedIndex - 1);
        usersWithCredentialsListView.getSelectionModel().selectPrevious();
    }
    
    @FXML
    private void moveUserDownButtonAction(ActionEvent event) {
        int selectedIndex = usersWithCredentialsListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(usersWithCredentialsList, selectedIndex, selectedIndex + 1);
        usersWithCredentialsListView.getSelectionModel().selectNext();
    }
    
    @FXML
    private void removeCredentialsButtonAction(ActionEvent event) {
        String userToRemove = (String) usersWithCredentialsListView.getSelectionModel().getSelectedItem();
        if(usersWithCredentialsInDBList.contains(userToRemove))
            usersWithCredentialsBeingRemovedList.add(userToRemove);
        else if( ! searchUsersWithoutCredentialsField.getText().trim().isEmpty() 
                && userToRemove.contains(searchUsersWithoutCredentialsField.getText().trim()))
            searchUsersWithoutCredentialsList.add(userToRemove);
        usersWithCredentialsList.remove(userToRemove);
    }
    
    @FXML
    private void refreshUsersWithCredentialsButtonAction(ActionEvent event) {
        getUsersWithCredentialsFromServer("");
    }
    
    @FXML
    private void searchUsersWithoutCredentialsButtonAction(ActionEvent event) {
        searchUsersWithoutCredentialsList.clear();
        JSONObject scenarioDescriptionElementJSON = Commons.createLoginJSONObject();
        scenarioDescriptionElementJSON.put(Commons.NAME_JSON_KEY, scenarioDescriptionElement.getName());
        if(scenarioDescriptionElement instanceof WorkflowTask)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY);
        else if(scenarioDescriptionElement instanceof WorkflowResource)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY);
        else if(scenarioDescriptionElement instanceof WorkflowTaskCategory)
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY);
        else
            scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY);
        scenarioDescriptionElementJSON.put(Commons.USERS_SEARCH_STRING, searchUsersWithoutCredentialsField.getText().trim());
        try {
            String resultString = Commons.postRequestToServer("/users/get/without_credentials", scenarioDescriptionElementJSON, communicateLabel);
            int operationResult = (int)resultString.charAt(0);
            if(operationResult == Commons.OPERATION_SUCCESSFULL) {
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray usersWithoutCredentialsJSONArray = (JSONArray) parser.parse(resultString);
                for(Object obj : usersWithoutCredentialsJSONArray) {
                    String user = (String) obj;
                    if(!usersWithCredentialsList.contains(user))
                        searchUsersWithoutCredentialsList.add(user);
                }
            } else {
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        } catch (IOException | ParseException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
    }
    
    @FXML
    private void saveChangesButtonAction(ActionEvent event) {
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", "Czy rzeczywiście chcesz zapisać zmiany w uprawnieniach użytkowników?");
        Optional<ButtonType> result = alert.showAndWait();        
        if (result.get() == ButtonType.OK) {
            communicateLabel.setText("");
            JSONObject scenarioDescriptionElementJSON = Commons.createLoginJSONObject();
            scenarioDescriptionElementJSON.put(Commons.NAME_JSON_KEY, scenarioDescriptionElement.getName());
            if(scenarioDescriptionElement instanceof WorkflowTask)
                scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY);
            else if(scenarioDescriptionElement instanceof WorkflowResource)
                scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY);
            else if(scenarioDescriptionElement instanceof WorkflowTaskCategory)
                scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY);
            else
                scenarioDescriptionElementJSON.put(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY, Commons.SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY);
            JSONArray usersHavingCredentialsJSONArray = new JSONArray();
            JSONArray usersReceivingCredentialsJSONArray = new JSONArray();
            JSONArray usersToRemoveJSONArray = new JSONArray();
            for(int i=0; i<usersWithCredentialsList.size(); i++) {
                String user = usersWithCredentialsList.get(i);
                JSONObject userJSONObject = new JSONObject();
                userJSONObject.put(Commons.USER_JSON_KEY, user);
                userJSONObject.put(Commons.ORDER_JSON_KEY, i);
                if(usersWithCredentialsInDBList.contains(user))
                    usersHavingCredentialsJSONArray.add(userJSONObject);
                else
                    usersReceivingCredentialsJSONArray.add(userJSONObject);
            }
            for(String user : usersWithCredentialsBeingRemovedList) 
                usersToRemoveJSONArray.add(user);
            scenarioDescriptionElementJSON.put(Commons.USERS_HAVING_CREDENTIALS_JSON_KEY, usersHavingCredentialsJSONArray);
            scenarioDescriptionElementJSON.put(Commons.USERS_RECEIVING_CREDENTIALS_JSON_KEY, usersReceivingCredentialsJSONArray);
            scenarioDescriptionElementJSON.put(Commons.USERS_WITHOUT_CREDENTIALS_JSON_KEY, usersToRemoveJSONArray);
            try {
                String resultString = Commons.postRequestToServer("/users/set", scenarioDescriptionElementJSON, communicateLabel);
                int loginResult = (int)resultString.charAt(0);
                switch (loginResult) {
                    case Commons.OPERATION_SUCCESSFULL:
                        getUsersWithCredentialsFromServer("Zmiany zostały zapisane");
                        break;
                    case SUCH_USER_DOES_NOT_EXIST:
                        communicateLabel.setText("Brak użytkowników w bazie");
                        String displayString = "Następujący użytkownicy nie zostali odnalezieni w bazie danych:";
                        resultString = resultString.substring(1);
                        JSONParser parser = new JSONParser();
                        JSONArray notFoundUsersJSONArray = (JSONArray)parser.parse(resultString);
                        for(Object obj : notFoundUsersJSONArray) {
                            String notFoundUser = (String) obj;
                            displayString += "\n" + notFoundUser;
                        }
                        alert = Commons.createAlert(AlertType.ERROR, "Brak użytkowników w bazie", displayString);
                        alert.showAndWait();
                        break;
                    default:
                        communicateLabel.setText("Błąd połączenia lub po stronie serwera");
                        break;
                }
            } catch (IOException | ParseException ex) {
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        }
    }
}
