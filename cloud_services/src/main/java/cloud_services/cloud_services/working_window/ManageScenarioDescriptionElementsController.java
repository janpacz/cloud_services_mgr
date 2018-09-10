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
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public abstract class ManageScenarioDescriptionElementsController implements Initializable {
    @FXML protected Label titleLabel;
    @FXML protected Label listTitleLabel;    
    protected ObservableList<ScenarioDescriptionElement> elementsList;
    @FXML protected ListView elementsListView;
    @FXML protected TextArea infoTextArea;
    @FXML protected Label communicateLabel;
    @FXML protected ComboBox sortingComboBox;
    @FXML private Button refreshButton;
    @FXML protected Button addElementButton;
    @FXML protected Button editOrCreateCopyButton;
    @FXML protected Button removeElementButton;
    @FXML private Button setCredentialsButton;
    
    protected ArrayList<? extends ScenarioDescriptionElement> elementsFromDBList;
        
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        elementsList = FXCollections.observableArrayList();
        elementsListView.setItems(elementsList);
        
        elementsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ScenarioDescriptionElement>() {
                @Override
                public void changed(ObservableValue<? extends ScenarioDescriptionElement> ov, ScenarioDescriptionElement old_val, ScenarioDescriptionElement new_val) {
                    if(new_val == null) {
                        infoTextArea.setText("");
                        editOrCreateCopyButton.setDisable(true);
                        setGUIIsElementOwner(false);
                    } else {
                        infoTextArea.setText(new_val.info());
                        editOrCreateCopyButton.setDisable(false);
                        if(new_val.getOwner().equals(MainApp.getCurrentUser())) {
                            setGUIIsElementOwner(true);
                        } else {
                            setGUIIsElementOwner(false);
                        }
                    }
                }
            }
        );
        
        sortingComboBox.setItems(FXCollections.observableArrayList(
                    Commons.SORTING_NAME_ASCENDING,
                    Commons.SORTING_NAME_DESCENDING
                ));
        sortingComboBox.setValue(Commons.SORTING_NAME_ASCENDING);
        sortingComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String old_val, String new_val) {
                Commons.sortElements(new_val, elementsList);              
            }
        });
        
        editOrCreateCopyButton.setDisable(true);
        setGUIIsElementOwner(false);        
        getElementsFromServer();
        setGUITexts();
    }
    
    protected void getElementsFromServer() {
        final String gettingDataCommunicate = "Pobieranie danych z bazy";
        
        refreshButton.setDisable(true);
        addElementButton.setDisable(true);
        elementsList.clear();
        communicateLabel.setText(gettingDataCommunicate);
        new Thread(new Runnable() {
            @Override
            public void run() {
                getDataFromServer();
                Platform.runLater(new Runnable() {                    
                    @Override
                    public void run() {
                        setGUIAndElementsGotFromServer();
                        refreshButton.setDisable(false);
                        addElementButton.setDisable(false);
                        if(communicateLabel.getText().equals(gettingDataCommunicate))
                            communicateLabel.setText("");
                    }
                });   
            }           
        }).start();
    }
    
    protected abstract void setGUITexts();
    protected abstract void getDataFromServer();
    
    protected void setGUIAndElementsGotFromServer() {
        elementsList.addAll(elementsFromDBList);
        Commons.sortElements((String)sortingComboBox.getValue(), elementsList);
    }
    
    private void setGUIIsElementOwner(boolean isElementOwner) {
        removeElementButton.setDisable(!isElementOwner);
        setCredentialsButton.setDisable(!isElementOwner);
    }
    
    @FXML
    private void refreshButtonAction(ActionEvent event) {
        getElementsFromServer();
    }
    
    @FXML protected abstract void addElementButtonAction(ActionEvent event);
    @FXML protected abstract void editOrCreateCopyButtonAction(ActionEvent event);
    @FXML protected abstract void removeElementButtonAction(ActionEvent event);
    @FXML protected abstract void setCredentialsButtonAction(ActionEvent event);
}
