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
import cloud_services.cloud_services.scenario_description_objects.ResourceUrlParameter;
import cloud_services.cloud_services.scenario_description_objects.RestTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.SoapTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.TaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

//Dialog setting execution technology  of tasks of specified category on specified resource
public class EditTechnologyDialog extends AskBeforeCloseDialog {
    @FXML private Label titleLabel;
    @FXML private TextField urlField;
    
    //controls responsible for list of url parameters specified by category
    private ObservableList<String> categoryParametersList;
    private LinkedHashMap<String,String> categoryResourceParametersMap;
    @FXML private ListView categoryParametersListView;
    @FXML private TextField categoryParameterNameField;
    @FXML private Button categoryParameterNameButton;
    
    //controls responsible for list of url parameters specified by resource service
    private ObservableList<String> resourceParametersNamesList;
    private ArrayList<ResourceUrlParameter> resourceParametersValuesList;
    @FXML private ListView resourceParametersListView;
    @FXML private TextField resourceParameterNameField;
    @FXML private Button resourceParameterNameButton;
    @FXML private Label resourceCategoryParameterLabel;
    @FXML private TextField resourceParameterValueField;
    @FXML private Button resourceParameterValueButton;
    @FXML private Button moveResourceParameterUpButton;
    @FXML private Button moveResourceParameterDownButton;
    @FXML private Button removeResourceParameterButton;
    
    @FXML private ComboBox technologyComboBox; // REST/SOAP choise
    @FXML private TextField expectedResponseCodeField;
    @FXML private TextField methodNameField;
    @FXML private TextField namespacePrefixField;
    @FXML private TextField namespaceUriField;
    
    @FXML private Label communicateLabel;
    
    private ResourceDialog dialog;
    private int technologyIndex;
    
    private boolean changeSelection = true;
    
    private static final String REST = RestTaskExecutionTechnology.REST_TECHNOLOGY_NAME;
    private static final String SOAP = SoapTaskExecutionTechnology.SOAP_TECHNOLOGY_NAME;
    
    public EditTechnologyDialog(
            ResourceDialog dialog,
            final WorkflowTaskCategory category,
            int technologyIndex
    ) {
        super("/fxml/dialogs/EditTechnologyDialog.fxml");
        
        this.dialog = dialog;
        this.technologyIndex = technologyIndex;
        TaskExecutionTechnology technology = dialog.getCategoryTechnology(technologyIndex);
        
        String title = "Edycja technologii wykonania dla kategorii " + category.getNameAndOwner();
        this.setTitle(title);
        titleLabel.setText(title);
        
        if(technology != null)
            urlField.setText(technology.getUrl());
        
        categoryParametersList = FXCollections.observableArrayList();
        categoryResourceParametersMap = new LinkedHashMap<>();
        for(String categoryParameter : category.getUrlParameters()) {
            categoryParametersList.add(categoryParameter);
            categoryResourceParametersMap.put(categoryParameter, null);
        }
        categoryParametersListView.setItems(categoryParametersList);
        categoryParameterNameField.setDisable(true);
        categoryParameterNameButton.setDisable(true);
        categoryParameterNameField.textProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val.trim().isEmpty())
                        categoryParameterNameButton.setDisable(true);
                    else
                        categoryParameterNameButton.setDisable(false);
            }
        });        
        categoryParametersListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                        String selectedName = (String)categoryParametersListView.getSelectionModel().getSelectedItem();
                        if(selectedName == null) {
                            categoryParameterNameField.setText("");
                            categoryParameterNameField.setDisable(true);
                            if(changeSelection) {
                                if(resourceParametersListView.getSelectionModel().getSelectedIndex() != -1) {
                                    changeSelection = false;
                                    resourceParametersListView.getSelectionModel().clearSelection();    
                                } else {
                                    resourceParameterNameField.setDisable(false);
                                    resourceParameterNameField.setText("");
                                }
                            } else
                                changeSelection = true;
                        } else {
                            if(categoryResourceParametersMap.get(selectedName) == null) {
                                categoryParameterNameField.setText("");
                                if(resourceParametersListView.getSelectionModel().getSelectedIndex() != -1) {
                                    changeSelection = false;
                                    resourceParametersListView.getSelectionModel().clearSelection();
                                }
                                resourceParameterNameField.setDisable(true);
                                resourceParameterNameField.setText("");
                            }
                            else {
                                categoryParameterNameField.setText(categoryResourceParametersMap.get(selectedName)); 
                                if(changeSelection) {
                                    if(resourceParametersListView.getSelectionModel().getSelectedItem() == null
                                            || ! resourceParametersListView.getSelectionModel().getSelectedItem().equals(categoryResourceParametersMap.get(selectedName))) {
                                        changeSelection = false;
                                        resourceParametersListView.getSelectionModel().select(categoryResourceParametersMap.get(selectedName));
                                    }
                                } else
                                    changeSelection = true;                                
                            }
                            categoryParameterNameField.setDisable(false);
                        }
                    }
                }
        );
        
        resourceParametersNamesList = FXCollections.observableArrayList();
        resourceParametersValuesList = new ArrayList<>();
        if(technology != null) {
            LinkedHashMap<String, ResourceUrlParameter> resourceParameters = technology.getUrlParametersNamesAndOrder();
            for(String parameterName : resourceParameters.keySet()) {
                resourceParametersNamesList.add(parameterName);
                ResourceUrlParameter parameterValue = resourceParameters.get(parameterName);
                resourceParametersValuesList.add(parameterValue);
                if(parameterValue.getType().equals(ResourceUrlParameter.TASK))
                    categoryResourceParametersMap.put(parameterValue.getValue(), parameterName);
            }
        }
        resourceParametersListView.setItems(resourceParametersNamesList);
        resourceParameterNameButton.setDisable(true);
        setGUIResourceParameterValueDisable(true);
        setResourceParametersButtonsDisable(true);
        resourceParameterNameField.textProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val.trim().isEmpty())
                        resourceParameterNameButton.setDisable(true);
                    else
                        resourceParameterNameButton.setDisable(false);
            }
        });
        resourceParametersListView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                        setGUIForResourceParameter();
                    }
                }
        );
        
        technologyComboBox.setItems(FXCollections.observableArrayList(
                REST,
                SOAP
        ));        
        technologyComboBox.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(((String)technologyComboBox.getValue()).equals(REST)) {
                        setGUIForREST();
                    } else {
                        setGUIForSOAP();
                    }
                }
        });
        
        if(technology != null) {
            if(technology.getTechnologyName().equals(REST)) {
                technologyComboBox.getSelectionModel().selectFirst();
                RestTaskExecutionTechnology restTechnology = (RestTaskExecutionTechnology) technology;
                expectedResponseCodeField.setText(Integer.toString(restTechnology.getExpectedResponseCode()));
            } else {
                technologyComboBox.getSelectionModel().selectLast();
                SoapTaskExecutionTechnology soapTechnology = (SoapTaskExecutionTechnology) technology;
                methodNameField.setText(soapTechnology.getMethodName());
                namespacePrefixField.setText(soapTechnology.getNamespacePrefix());
                namespaceUriField.setText(soapTechnology.getNamespaceUri());
            }
        } else {
            technologyComboBox.getSelectionModel().selectFirst();
        }
    }
    
    private void setGUIResourceParameterValueDisable(boolean disable) {
        resourceParameterValueField.setDisable(disable);
        resourceParameterValueButton.setDisable(disable);
    }
    
    private void setResourceParametersButtonsDisable(boolean disable) {
        moveResourceParameterUpButton.setDisable(disable);
        moveResourceParameterDownButton.setDisable(disable);
        removeResourceParameterButton.setDisable(disable);
    }
    
    private void setGUIForREST() {
        expectedResponseCodeField.setDisable(false);
        methodNameField.setDisable(true);
        namespacePrefixField.setDisable(true);
        namespaceUriField.setDisable(true);
    }
    
    private void setGUIForSOAP() {
        expectedResponseCodeField.setDisable(true);
        methodNameField.setDisable(false);
        namespacePrefixField.setDisable(false);
        namespaceUriField.setDisable(false);
    }
    
    private void setGUIForResourceParameter()
    {
        int selectedIndex = resourceParametersListView.getSelectionModel().getSelectedIndex();
        if(selectedIndex == -1) {
            resourceParameterValueField.setText("");
            setGUIResourceParameterValueDisable(true);
            resourceCategoryParameterLabel.setText("");                            
            setResourceParametersButtonsDisable(true);
            if(changeSelection) {
                if(categoryParametersListView.getSelectionModel().getSelectedIndex() != -1) {
                    changeSelection = false;
                    categoryParametersListView.getSelectionModel().clearSelection(); 
                }
            } else
                changeSelection = true;
            resourceParameterNameField.setDisable(false);
            resourceParameterNameField.setText("");
        } else {            
            ResourceUrlParameter selectedParameter = resourceParametersValuesList.get(selectedIndex);
            if(selectedParameter.getType().equals(ResourceUrlParameter.TASK)) {
                resourceParameterNameField.setDisable(true);
                resourceParameterNameField.setText("");
                resourceParameterValueField.setText("");
                setGUIResourceParameterValueDisable(true);
                resourceCategoryParameterLabel.setText("Parametr kategorii: " + selectedParameter.getValue());
                if(changeSelection) {
                    if(categoryParametersListView.getSelectionModel().getSelectedItem() == null
                            || ! categoryParametersListView.getSelectionModel().getSelectedItem().equals(selectedParameter.getValue())) {
                        changeSelection = false;
                        categoryParametersListView.getSelectionModel().select(selectedParameter.getValue());
                    }
                } else
                    changeSelection = true;
            } else {
                resourceParameterNameField.setDisable(false);
                resourceParameterNameField.setText(resourceParametersNamesList.get(selectedIndex));
                resourceParameterValueField.setText(selectedParameter.getValue());
                setGUIResourceParameterValueDisable(false);
                resourceCategoryParameterLabel.setText("");
                if(categoryParametersListView.getSelectionModel().getSelectedIndex() != -1) {
                    changeSelection = false;
                    categoryParametersListView.getSelectionModel().clearSelection();
                }
            }
            if(selectedIndex == 0)
                moveResourceParameterUpButton.setDisable(true);
            else
                moveResourceParameterUpButton.setDisable(false);
            if(selectedIndex == resourceParametersNamesList.size() - 1)
                moveResourceParameterDownButton.setDisable(true);
            else
                moveResourceParameterDownButton.setDisable(false);
            removeResourceParameterButton.setDisable(false);
        }
    }

    @FXML
    private void categoryParameterNameButtonAction(ActionEvent event) {
        String categoryParameterName = (String)categoryParametersListView.getSelectionModel().getSelectedItem();
        String resourceParameterName = categoryParameterNameField.getText().trim();
        String previousResourceParameterName = categoryResourceParametersMap.get(categoryParameterName);
        if(resourceParametersNamesList.contains(resourceParameterName)) {
            communicateLabel.setText("Istnieje już parametr w adresie url zasobu o takiej nazwie");
        } else { 
            categoryResourceParametersMap.put(categoryParameterName, resourceParameterName);            
            if(previousResourceParameterName == null) {            
                resourceParametersNamesList.add(resourceParameterName);
                resourceParametersValuesList.add(new ResourceUrlParameter(ResourceUrlParameter.TASK, categoryParameterName));
                resourceParametersListView.getSelectionModel().selectLast();
            } else {
                int resourceParameterNameIndex = resourceParametersNamesList.indexOf(previousResourceParameterName);
                resourceParametersNamesList.set(resourceParameterNameIndex, resourceParameterName);
                resourceParametersListView.getSelectionModel().select(resourceParameterNameIndex);
            }
            communicateLabel.setText("");
        }
    }
    
    @FXML
    private void resourceParameterNameButtonAction(ActionEvent event) {
        String previousResourceParameterName = (String) resourceParametersListView.getSelectionModel().getSelectedItem();
        String resourceParameterName = resourceParameterNameField.getText().trim();
        communicateLabel.setText("");
        if(resourceParametersNamesList.contains(resourceParameterName)) {
            if((previousResourceParameterName == null) || (! previousResourceParameterName.equals(resourceParameterName)))
                communicateLabel.setText("Istnieje już parametr w adresie url zasobu o takiej nazwie");
        } else if(previousResourceParameterName == null) {
            resourceParametersNamesList.add(resourceParameterName);            
            resourceParametersValuesList.add(new ResourceUrlParameter(ResourceUrlParameter.RESOURCE, ""));
            resourceParametersListView.getSelectionModel().selectLast();            
        } else {
            int selectedIndex = resourceParametersListView.getSelectionModel().getSelectedIndex();
            resourceParametersNamesList.set(selectedIndex, resourceParameterName);
            resourceParametersListView.getSelectionModel().select(selectedIndex);
        }
    }
    
    @FXML
    private void resourceParameterValueButtonAction(ActionEvent event) {
        int resourceParameterIndex = resourceParametersListView.getSelectionModel().getSelectedIndex();
        String resourceParameterValue = resourceParameterValueField.getText().trim();
        resourceParametersValuesList.get(resourceParameterIndex).setValue(resourceParameterValue);
    }
    
    @FXML
    private void moveResourceParameterUpButtonAction(ActionEvent event) {
        int selectedIndex = resourceParametersListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(resourceParametersNamesList, selectedIndex, selectedIndex - 1);
        Collections.swap(resourceParametersValuesList, selectedIndex, selectedIndex - 1);
        resourceParametersListView.getSelectionModel().selectPrevious();
    }
    
    @FXML
    private void moveResourceParameterDownButtonAction(ActionEvent event) {
        int selectedIndex = resourceParametersListView.getSelectionModel().getSelectedIndex(); 
        Collections.swap(resourceParametersNamesList, selectedIndex, selectedIndex + 1);
        Collections.swap(resourceParametersValuesList, selectedIndex, selectedIndex + 1);
        resourceParametersListView.getSelectionModel().selectNext();
    }
    
    @FXML
    private void removeResourceParameterButtonAction(ActionEvent event) {
        String parameterName = (String)resourceParametersListView.getSelectionModel().getSelectedItem();
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie parametru " + parameterName,
                "Czy rzeczywiście chcesz usunąć parametr " + parameterName + "?");
        Optional<ButtonType> result = alert.showAndWait(); 
        if (result.get() == ButtonType.OK) {
            int resourceIndex = resourceParametersListView.getSelectionModel().getSelectedIndex();
            resourceParametersNamesList.remove(resourceIndex);
            ResourceUrlParameter parameterToRemove = resourceParametersValuesList.get(resourceIndex);
            if(parameterToRemove.getType().equals(ResourceUrlParameter.TASK)) {
                categoryResourceParametersMap.put(parameterToRemove.getValue(), null);
            }
            resourceParametersValuesList.remove(resourceIndex); 
            setGUIForResourceParameter();
        }
    }
    
    @FXML
    private void saveTechnologyButtonAction(ActionEvent event) {        
        boolean validationPassed = false;
        int expectedResponseCode = -1;
        if(urlField.getText().trim().isEmpty())
            communicateLabel.setText("Musisz podać adres url");
        else {
            try {
                // URL validation
                URL url = new URL(urlField.getText().trim());
                url.toURI();
                if(technologyComboBox.getValue().equals(REST)) {
                    if(expectedResponseCodeField.getText().trim().isEmpty())
                        communicateLabel.setText("Musisz podać przewidywany kod odpowiedzi");
                    else {
                        try {
                            expectedResponseCode = Integer.parseUnsignedInt(expectedResponseCodeField.getText().trim());
                            if(expectedResponseCode == 0)
                                throw new NumberFormatException();
                            validationPassed = true;
                        } catch(NumberFormatException ex) {
                            communicateLabel.setText("Przewidywany kod odpowiedzi musi być liczbą całkowitą dodatnią");
                        }
                    }
                } else {
                    if(methodNameField.getText().trim().isEmpty())
                        communicateLabel.setText("Musisz podać nazwę metody");
                    else if(namespacePrefixField.getText().trim().isEmpty())
                        communicateLabel.setText("Musisz podać prefix przestrzeni nazw");
                    else if(namespaceUriField.getText().trim().isEmpty())
                        communicateLabel.setText("Musisz podać uri przestrzeni nazw");
                    else
                        validationPassed = true;
                }
            } catch (MalformedURLException | URISyntaxException ex) {
                communicateLabel.setText("Podany adres url jest nieprawidłowy");
            }
        }
        if(validationPassed && (!categoryResourceParametersMap.isEmpty())) {
            for(String categoryParameter : categoryResourceParametersMap.keySet()) {
                if(categoryResourceParametersMap.get(categoryParameter) == null) {
                    validationPassed = false;
                    communicateLabel.setText("Parametr w adresie url " + categoryParameter 
                            + " kategorii nie ma odpowiadającego sobie parametru w adresie url zasobu");
                    break;
                }
            }
        }
        if(validationPassed) {
            Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian",
                    "Czy rzeczywiście chcesz zapisać tą technologię?");
            Optional<ButtonType> result = alert.showAndWait();        
            if (result.get() == ButtonType.OK) {
                LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder = new LinkedHashMap<>();
                for(int i=0; i<resourceParametersNamesList.size(); i++)
                    urlParametersNamesAndOrder.put(resourceParametersNamesList.get(i), resourceParametersValuesList.get(i));
                if(technologyComboBox.getValue().equals(REST)) {
                    dialog.setCategoryTechnology(technologyIndex, 
                            new RestTaskExecutionTechnology(
                                    urlField.getText().trim(),
                                    urlParametersNamesAndOrder,
                                    expectedResponseCode
                            ));
                } else {
                    dialog.setCategoryTechnology(technologyIndex, 
                            new SoapTaskExecutionTechnology(
                                    urlField.getText().trim(),
                                    urlParametersNamesAndOrder,
                                    methodNameField.getText().trim(),
                                    namespacePrefixField.getText().trim(),
                                    namespaceUriField.getText().trim()
                            ));
                }
                communicateLabel.setText("Technologia została zapisana");
            }
        }        
    }
}
