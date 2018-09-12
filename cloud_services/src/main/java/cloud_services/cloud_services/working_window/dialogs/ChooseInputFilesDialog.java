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
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.working_window.ScenarioController;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

//Dialog setting input files for task - input file can be chosen from disk (than it is represented by String path)
//or can be output file of previous task (than it is represented by Integer index of previous task on previous tasks list).
//If parameter is the list of input files, than it is represented by ArrayList of input files (Strings and Integers - see above).
public class ChooseInputFilesDialog extends AskBeforeCloseDialog {
    @FXML private ComboBox parameterNameComboBox;    
    @FXML private ComboBox inputFileSourceComboBox;
    @FXML private Button chooseFilesButton;
    @FXML private ComboBox precedingTaskComboBox;
    private TreeMap<String, Integer> precedingTaskNameOrder;
    private HashMap<Integer, String> precedingTaskOrderName;
    @FXML private Button addPrecedingTaskToListButton;
    @FXML private Button moveFilesUpButton;
    @FXML private Button moveFilesDownButton;
    @FXML private TextArea chosenFileTextArea;
    @FXML private Label parameterTypeLabel;
    private ObservableList<String> chosenFilesList;
    @FXML private ListView chosenFilesListView;
    @FXML private Button removeFilesButton;
    @FXML private Button saveChangesButton;
    private ScenarioController controller;
    private WorkflowTask task;
    private LinkedHashMap<String, Object> inputFilesInfos;
    
    private static final String INPUT_FILE_SOURCE_DISC = "Wybierz plik z dysku";
    private static final String INPUT_FILE_SOURCE_PREVIOUS_TASK = "Wybierz plik z poprzedniego zadania";
    
    public ChooseInputFilesDialog(ScenarioController controller, WorkflowTask task) {
        super("/fxml/dialogs/ChooseInputFilesDialog.fxml");
        
        this.controller = controller;
        this.task = task;
        inputFilesInfos = task.getInputFilesInfosCopy();
        
        this.setTitle("Wybór plików wejściowych " + task.getNameWithIndexWithSameNameInScenario());
        
        parameterNameComboBox.setItems(FXCollections.observableArrayList(inputFilesInfos.keySet()));
        parameterNameComboBox.getSelectionModel().selectFirst();        
        parameterNameComboBox.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    setGUIComponentsForChosenParameter();
                }
        });        
        
        inputFileSourceComboBox.setItems(FXCollections.observableArrayList(
                INPUT_FILE_SOURCE_DISC
        ));
        
        if(task.getPrecedingTasksAmount() != 0) {
            inputFileSourceComboBox.getItems().add(INPUT_FILE_SOURCE_PREVIOUS_TASK);
        }
        
        inputFileSourceComboBox.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(((String)inputFileSourceComboBox.getValue()).equals(INPUT_FILE_SOURCE_DISC))
                        setGUIComponentsFileFromDisc();
                    else
                        setGUIComponentsFileFromPreviousTask();
                }
        });
        
        precedingTaskNameOrder = new TreeMap(new Comparator<String>() {
            public int compare(String name1, String name2) {
                if(name1.toLowerCase().equals(name2.toLowerCase()))
                    return name1.compareTo(name2);
                else
                    return name1.toLowerCase().compareTo(name2.toLowerCase());
            }
        });
        precedingTaskOrderName = new HashMap();
        Integer[] precedingTasks = task.getPrecedingTasksArray();
        for(int i=0; i<precedingTasks.length; i++) {
            precedingTaskNameOrder.put(controller.getTask(precedingTasks[i]).getNameWithIndexWithSameNameInScenario(), i);
        }
        for(String precedingTaskName : precedingTaskNameOrder.keySet()) {
            precedingTaskComboBox.getItems().add(precedingTaskName);
            precedingTaskOrderName.put(precedingTaskNameOrder.get(precedingTaskName), precedingTaskName);
        }
        
        precedingTaskComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val != null) {
                        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
                        if(chosenInputFileInfo instanceof ArrayList) {
                            addPrecedingTaskToListButton.setDisable(false);
                        } else {
                            inputFilesInfos.put((String)parameterNameComboBox.getValue(), 
                                    precedingTaskNameOrder.get(precedingTaskComboBox.getValue()));
                            setChosenFileText();
                            removeFilesButton.setDisable(false);
                            saveChangesButton.setDisable(false);
                        }
                    } 
                }
        });
        
        chosenFilesList = FXCollections.observableArrayList();
        chosenFilesListView.setItems(chosenFilesList);
        chosenFilesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chosenFilesListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String old_val, String new_val) {
                    if(new_val == null) {
                        moveFilesUpButton.setDisable(true);
                        moveFilesDownButton.setDisable(true);
                        removeFilesButton.setDisable(true);
                    } else {
                        moveFilesUpButton.setDisable(false);
                        moveFilesDownButton.setDisable(false);
                        removeFilesButton.setDisable(false);
                    }
            }
        });
        
        setGUIComponentsForChosenParameter();
        moveFilesUpButton.setDisable(true);
        moveFilesDownButton.setDisable(true);
        saveChangesButton.setDisable(true);
    }
    
    private void setGUIComponentsForChosenParameter(){  
        chosenFilesList.clear();
        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
        if(chosenInputFileInfo instanceof ArrayList) {
            parameterTypeLabel.setText("Typ parametru: Lista plików wejściowych");
            chosenFilesListView.setDisable(false);
            chosenFileTextArea.setText("");
            chosenFileTextArea.setDisable(true);
            removeFilesButton.setDisable(true);
            for(Object inputFile : (ArrayList)chosenInputFileInfo) {
                addComponentToChosenFilesViewList(inputFile);
            }            
            inputFileSourceComboBox.setValue(INPUT_FILE_SOURCE_DISC);
        } else {
            parameterTypeLabel.setText("Typ parametru: Pojedyńczy plik wejściowy");
            chosenFilesListView.setDisable(true);
            chosenFileTextArea.setDisable(false);
            removeFilesButton.setDisable(false);
            addPrecedingTaskToListButton.setDisable(true);
            if(chosenInputFileInfo instanceof Integer) {
                inputFileSourceComboBox.setValue(INPUT_FILE_SOURCE_PREVIOUS_TASK);
                precedingTaskComboBox.getSelectionModel().select(precedingTaskOrderName.get((Integer)chosenInputFileInfo));
            } else {
                inputFileSourceComboBox.setValue(INPUT_FILE_SOURCE_DISC);
                precedingTaskComboBox.getSelectionModel().clearSelection();
                if(chosenInputFileInfo.equals("")) {
                    removeFilesButton.setDisable(true);
                }
            }
            setChosenFileText();
        }
    }
    
    private void setGUIComponentsFileFromDisc() {
        chooseFilesButton.setDisable(false);        
        precedingTaskComboBox.setDisable(true);        
        addPrecedingTaskToListButton.setDisable(true);
    }
    
    private void setGUIComponentsFileFromPreviousTask() {
        chooseFilesButton.setDisable(true);        
        precedingTaskComboBox.setDisable(false);
        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
        if((chosenInputFileInfo instanceof ArrayList) && (! precedingTaskComboBox.getSelectionModel().isEmpty()))
            addPrecedingTaskToListButton.setDisable(false);
    }
    
    private void setChosenFileText() {
        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
        if(chosenInputFileInfo instanceof Integer) {
            chosenFileTextArea.setText("Wybrany plik wyjściowy z zadania " + precedingTaskComboBox.getValue());
        } else {
            if(chosenInputFileInfo.equals("")) {
                chosenFileTextArea.setText("Wybrany plik: brak");
            } else {
                chosenFileTextArea.setText("Wybrany plik: " + chosenInputFileInfo);
            }
        }
    }
    
    private void addComponentToChosenFilesViewList(Object chosenInputFileInfo) {
        if(chosenInputFileInfo instanceof Integer) {
            chosenFilesList.add("Plik wyjściowy z zadania " 
                    + controller.getTask(task.getPrecedingTaskIndexInScenario((Integer)chosenInputFileInfo)).getNameWithIndexWithSameNameInScenario());
        } else {
            chosenFilesList.add((String)chosenInputFileInfo);
        }
    }
    
    @FXML
    private void chooseFilesButtonAction(ActionEvent event) {
        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
        FileChooser fileChooser = new FileChooser();
        if(chosenInputFileInfo instanceof ArrayList) {
            fileChooser.setTitle("Dodaj pliki wejściowe do listy");
            List<File> list = fileChooser.showOpenMultipleDialog(this);
            if (list != null) {
                chosenFilesListView.getSelectionModel().clearSelection();
                saveChangesButton.setDisable(false);
                for (File file : list) {
                    addComponentToChosenFilesViewList(file.getAbsolutePath());                
                    ((ArrayList<Object>) chosenInputFileInfo).add(file.getAbsolutePath());
                    chosenFilesListView.getSelectionModel().selectLast();
                }
            }
        } else {
            fileChooser.setTitle("Wybierz plik wejściowy");
            File file = fileChooser.showOpenDialog(this);
            if(file != null) {
                inputFilesInfos.put((String)parameterNameComboBox.getValue(), file.getAbsolutePath());
                precedingTaskComboBox.getSelectionModel().clearSelection();
                saveChangesButton.setDisable(false);
                setChosenFileText();
                removeFilesButton.setDisable(false);
            }
        }
    }
    
    @FXML
    private void addPrecedingTaskToListButtonAction(ActionEvent event) {
        ArrayList<Object> inputFileParameterList = (ArrayList<Object>) inputFilesInfos.get((String)parameterNameComboBox.getValue());
        int precedingTaskIndex = precedingTaskNameOrder.get(precedingTaskComboBox.getValue());
        addComponentToChosenFilesViewList(precedingTaskIndex);
        inputFileParameterList.add(precedingTaskIndex);
        chosenFilesListView.getSelectionModel().clearSelection();
        chosenFilesListView.getSelectionModel().selectLast();
        saveChangesButton.setDisable(false);
    }
    
    private void switchChosenFiles(int selectedIndex, int notSelectedIndex) {
        ArrayList<Object> inputFilesList = (ArrayList<Object>) inputFilesInfos.get((String)parameterNameComboBox.getValue());
        Object temp = inputFilesList.get(selectedIndex);
        inputFilesList.set(selectedIndex, inputFilesList.get(notSelectedIndex));
        inputFilesList.set(notSelectedIndex, temp);
        temp = chosenFilesList.get(selectedIndex);
        chosenFilesList.set(selectedIndex, chosenFilesList.get(notSelectedIndex));
        chosenFilesList.set(notSelectedIndex, (String) temp);
        chosenFilesListView.getSelectionModel().clearSelection(selectedIndex);
        chosenFilesListView.getSelectionModel().select(notSelectedIndex);
    }
    
    @FXML
    private void moveFilesUpButtonAction(ActionEvent event) {
        for(int i = 1; i < chosenFilesList.size(); i++) {
            if(chosenFilesListView.getSelectionModel().isSelected(i) 
                    && ! chosenFilesListView.getSelectionModel().isSelected(i - 1)) {
                switchChosenFiles(i, i - 1);
            }
        }
    }
    
    @FXML
    private void moveFilesDownButtonAction(ActionEvent event) {
        for(int i = chosenFilesList.size() - 2; i > -1; i--) {
            if(chosenFilesListView.getSelectionModel().isSelected(i) 
                    && ! chosenFilesListView.getSelectionModel().isSelected(i + 1)) {
                switchChosenFiles(i, i + 1);
            }
        }
    }
    
    @FXML
    private void removeFilesButtonAction(ActionEvent event) {
        Object chosenInputFileInfo = inputFilesInfos.get((String)parameterNameComboBox.getValue());
        if(chosenInputFileInfo instanceof ArrayList) {
            ArrayList<Integer> selectedIndexes = new ArrayList();
            selectedIndexes.addAll(chosenFilesListView.getSelectionModel().getSelectedIndices());
            for(int i = chosenFilesList.size() - 1; i > -1; i--) {
                if(selectedIndexes.contains(i)) {
                    ((ArrayList<Object>) chosenInputFileInfo).remove(i);
                    chosenFilesList.remove(i);
                }
            }
            chosenFilesListView.getSelectionModel().clearSelection();            
        } else {
            inputFilesInfos.put((String)parameterNameComboBox.getValue(), "");
            precedingTaskComboBox.getSelectionModel().clearSelection();
            setChosenFileText();
            removeFilesButton.setDisable(true);
        }
        saveChangesButton.setDisable(false);
    }
    
    @FXML
    private void saveChangesButtonAction(ActionEvent event) {
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", 
                        "Czy rzeczywiscie chcesz zapisać zmiany na liście plików wejściowych zadania?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            task.setInputFilesInfos(inputFilesInfos);
            saveChangesButton.setDisable(true);
        }
    }
}
