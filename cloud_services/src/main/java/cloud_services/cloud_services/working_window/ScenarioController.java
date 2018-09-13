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
import cloud_services.cloud_services.scenario_description_objects.DrawingOrderInformation;
import cloud_services.cloud_services.scenario_description_objects.Scenario;
import cloud_services.cloud_services.scenario_description_objects.WorkflowObject;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.working_window.dialogs.ChooseInputFilesDialog;
import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import cloud_services.cloud_services.working_window.dialogs.ManageTaskConnectionsDialog;
import cloud_services.cloud_services.working_window.dialogs.ScenarioDescriptionDialog;
import cloud_services.cloud_services.working_window.dialogs.SetUrlParametersDialog;
import java.awt.MouseInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

//Controller responsible for scenario edition and start of execution
public class ScenarioController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox sortingComboBox;
    @FXML private ListView tasksListView; 
    @FXML private ListView resourcesListView; 
    private ObservableList<WorkflowTask> tasksList; //all tasks retrieved from DB
    private ObservableList<WorkflowResource> resourcesList; //all resources retrieved from DB
    @FXML private Button executeScenarioButton;
    @FXML private Button saveNewScenarioButton;
    @FXML private Button saveChangesButton;
    @FXML private TextArea infoTextArea;
    @FXML private Button chooseWorkflowObjectButton;
    @FXML private Canvas schemeCanvas; // canvas where scenario is drawn
    private ArrayList<WorkflowTask> scenarioTasksList; //tasks belonging to scenario
    private ArrayList<WorkflowResource> scenarioResourcesList; //resources belonging to scenario
    private ArrayList<DrawingOrderInformation> scenarioDrawingOrderList; //order of drawing of scenario objects
    private WorkflowObject movingObject = null; //object currently moved by mouse
    private int beginningConnectionTaskIndex = -1;
    private boolean connectionToPrecedingTask;
    private int previousMousePositionX;
    private int previousMousePositionY;
    private ContextMenu taskContextMenu;
    private ContextMenu resourceContextMenu;
    private ChangeListener<Number> sceneSizeListener;
    private HashMap<String, Integer> sameNamesInScenario; //number of tasks with the same name and owner in scenario
    
    private Scenario scenario;
    private String scenarioPreviousName;
    private String scenarioDescription;
    
    private static final int SUCH_SCENARIO_ALREADY_EXISTS = 3;
    
    public ScenarioController(Scenario scenario) {
        this.scenario = scenario;
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeCollections();
        initializeSchemeCanvas();        
        populateLists();              
    }
    
    private void initializeCollections() {
        tasksList = FXCollections.observableArrayList();
        tasksListView.setItems(tasksList);
        tasksListView.setOnMouseClicked(
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        tasksListViewMouseClicked();
                    }
                }
        );  
        
        resourcesList = FXCollections.observableArrayList();
        resourcesListView.setItems(resourcesList);
        resourcesListView.setOnMouseClicked(
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        resourcesListViewMouseClicked();
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
                Commons.sortElements(new_val, tasksList); 
                Commons.sortElements(new_val, resourcesList);
            }
        });
        
        scenarioTasksList = new ArrayList<>();
        scenarioResourcesList = new ArrayList<>();
        scenarioDrawingOrderList = new ArrayList<>();
        sameNamesInScenario = new HashMap<>();
    }
    
    private void initializeSchemeCanvas() {
        
        schemeCanvas.setOnMousePressed(
                new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        schemePaneMousePressed(event);
                    }
                }
        );
        schemeCanvas.setOnMouseDragged(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        schemePaneMouseDragged(event);
                    }
                }
        );
        schemeCanvas.setOnMouseReleased(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        schemePaneMouseReleased(event);
                    }
                }
        );
        schemeCanvas.setOnMouseMoved(new EventHandler<MouseEvent>() {

                    @Override
                    public void handle(MouseEvent event) {
                        schemePaneMouseMoved(event);
                    }
                }
        );  
    }
    
    private void populateLists() {
        executeScenarioButton.setDisable(true);
        saveNewScenarioButton.setDisable(true);
        saveChangesButton.setDisable(true);
        chooseWorkflowObjectButton.setDisable(true);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<WorkflowTask> tasksArrayList = Commons.getTasksFromServer();   
                final ArrayList<WorkflowResource> resourcesArrayList = Commons.getResourcesFromServer();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        tasksList.addAll(tasksArrayList);
                        resourcesList.addAll(resourcesArrayList);
                        Commons.sortElements((String)sortingComboBox.getValue(), tasksList); 
                        Commons.sortElements((String)sortingComboBox.getValue(), resourcesList);
                        
                        setSceneForScenario();
                        drawSchemeCanvas();
                        
                        executeScenarioButton.setDisable(false);
                        saveNewScenarioButton.setDisable(false);
                        chooseWorkflowObjectButton.setDisable(false);
                    }                    
                });
            }
        }).start();    
    }
    
    private void setSceneForScenarioAddOrCopy(String title) {
        titleLabel.setText(title);
        scenarioPreviousName = "";
        saveChangesButton.setDisable(true);
    }
    
    private void setSceneForScenarioEdition() {
        titleLabel.setText("Edytuj lub stwórz kopię scenariusza " + nameField.getText().trim() + "(" + MainApp.getCurrentUser() + ")");
        scenarioPreviousName = nameField.getText().trim();
        saveChangesButton.setDisable(false);
    }
    
    //gets workflow object's index in list given workflow object's name and order
    private int getWorkflowObjectIndex(List<? extends WorkflowObject> workflowObjectsList, String nameAndOwner) {
        for(int i=0; i<workflowObjectsList.size(); i++) {
            if(workflowObjectsList.get(i).getNameAndOwner().equals(nameAndOwner))
                return i;
        }
        return -1;
    } 
    
    private void setSceneForScenario() {        
        if(scenario == null) {
            setSceneForScenarioAddOrCopy("Dodaj nowy scenariusz");
            scenarioDescription = "";
        } else {
            nameField.setText(scenario.getName());
            if(scenario.getDescription() == null)
                scenarioDescription = "";
            else
                scenarioDescription = scenario.getDescription();
            //set scene for scenario got from DB
            JSONObject workflowObjectsJSONObject = scenario.getWorkflowObjectsJSONObject();
            ArrayList<String> tasksNotHavingCredentialsToNamesAndOwners = new ArrayList<>();
            ArrayList<String> resourcesNotHavingCredentialsToNamesAndOwners = new ArrayList<>();
            
            if(workflowObjectsJSONObject.containsKey(Commons.TASKS_JSON_KEY)) {
                JSONArray tasksJSONArray = (JSONArray) workflowObjectsJSONObject.get(Commons.TASKS_JSON_KEY);
                HashMap<Integer, Integer> newIndexToOldMappings = new HashMap();
                Long removedTasksWithLowerIndex = 0L;
                for(int i=0; i<tasksJSONArray.size(); i++) {
                    JSONObject taskJSONObject = (JSONObject) tasksJSONArray.get(i);
                    String taskNameAndOwner = taskJSONObject.get(Commons.WORKFLOW_OBJECT_NAME_JSON_KEY) + "(" + taskJSONObject.get(Commons.WORKFLOW_OBJECT_OWNER_JSON_KEY) + ")";
                    int taskIndexInCurrentDBTasksList = getWorkflowObjectIndex(tasksList, taskNameAndOwner);         
                    if(taskIndexInCurrentDBTasksList == -1) {
                        if(! tasksNotHavingCredentialsToNamesAndOwners.contains(taskNameAndOwner))
                            tasksNotHavingCredentialsToNamesAndOwners.add(taskNameAndOwner);
                        removedTasksWithLowerIndex++;

                        JSONArray precedingTasksJSONArray = (JSONArray) taskJSONObject.get(Commons.PRECEDING_TASKS_JSON_KEY);
                        for(Object obj : precedingTasksJSONArray) {
                            Integer precedingTaskIndex = ((Long) obj).intValue();
                            JSONObject precedingTaskJSONObject = (JSONObject) tasksJSONArray.get(precedingTaskIndex);
                            JSONArray folTasksJSONArrayOfPrecTask = (JSONArray) precedingTaskJSONObject.get(Commons.FOLLOWING_TASKS_JSON_KEY);
                            folTasksJSONArrayOfPrecTask.remove(((Integer) i).longValue());
                        }

                        JSONArray followingTasksJSONArray = (JSONArray) taskJSONObject.get(Commons.FOLLOWING_TASKS_JSON_KEY);
                        for(Object obj : followingTasksJSONArray) {
                            Integer followingTaskIndex = ((Long) obj).intValue();
                            JSONObject followingTaskJSONObject = (JSONObject) tasksJSONArray.get(followingTaskIndex);
                            JSONArray precTasksJSONArrayOfFolTask = (JSONArray) followingTaskJSONObject.get(Commons.PRECEDING_TASKS_JSON_KEY);
                            Integer indexOfRemovedPrecTask = precTasksJSONArrayOfFolTask.indexOf(((Integer) i).longValue());
                            precTasksJSONArrayOfFolTask.remove(((Integer) i).longValue());

                            JSONArray inputFileParametersJSONArray = (JSONArray) followingTaskJSONObject.get(Commons.INPUT_FILES_PARAMETERS_JSON_KEY);
                            for(Object obj1 : inputFileParametersJSONArray) {
                                JSONObject inputFileParameterJSONObject = (JSONObject) obj1;
                                Object inputFileParameterValue = inputFileParameterJSONObject.get(Commons.INP_F_PARAMETER_VALUE_JSON_KEY);
                                if(inputFileParameterValue instanceof JSONArray) {
                                    JSONArray inputFilesArray = (JSONArray) inputFileParameterValue;
                                    for(int j=0; j<inputFilesArray.size(); j++) {
                                        if(inputFilesArray.get(j) instanceof Long) {
                                            Integer taskIndexInPrecedingTasksList = ((Long) inputFilesArray.get(j)).intValue();
                                            if(taskIndexInPrecedingTasksList == indexOfRemovedPrecTask) {
                                                inputFilesArray.remove(j);
                                                j--;
                                            } else if(taskIndexInPrecedingTasksList > indexOfRemovedPrecTask) {
                                                inputFilesArray.set(j, (Long) inputFilesArray.get(j) - 1);
                                            }
                                        }                                    
                                    }
                                } else if(inputFileParameterValue instanceof Long) {
                                    Integer taskIndexInPrecedingTasksList = ((Long) inputFileParameterValue).intValue();
                                    if(taskIndexInPrecedingTasksList == indexOfRemovedPrecTask) {
                                        inputFileParameterJSONObject.put(Commons.INP_F_PARAMETER_VALUE_JSON_KEY, "");
                                    } else if(taskIndexInPrecedingTasksList > indexOfRemovedPrecTask) {
                                        inputFileParameterJSONObject.put(Commons.INP_F_PARAMETER_VALUE_JSON_KEY, (Long) inputFileParameterValue - 1);
                                    }
                                }
                            }
                        }
                    } else if(removedTasksWithLowerIndex != 0L) {
                        Long oldTaskIndexInScenario = ((Integer) i).longValue();
                        Long newTaskIndexInScenario = oldTaskIndexInScenario - removedTasksWithLowerIndex;
                        newIndexToOldMappings.put(newTaskIndexInScenario.intValue(), i);

                        JSONArray precedingTasksJSONArray = (JSONArray) taskJSONObject.get(Commons.PRECEDING_TASKS_JSON_KEY);
                        for(Object obj : precedingTasksJSONArray) {                            
                            Integer precedingTaskIndex = ((Long) obj).intValue();
                            if(precedingTaskIndex < i)
                                precedingTaskIndex = newIndexToOldMappings.get(precedingTaskIndex);                            
                            JSONObject precedingTaskJSONObject = (JSONObject) tasksJSONArray.get(precedingTaskIndex);
                            JSONArray folTasksJSONArrayOfPrecTask = (JSONArray) precedingTaskJSONObject.get(Commons.FOLLOWING_TASKS_JSON_KEY);
                            folTasksJSONArrayOfPrecTask.set(folTasksJSONArrayOfPrecTask.indexOf(oldTaskIndexInScenario), newTaskIndexInScenario);
                        }

                        JSONArray followingTasksJSONArray = (JSONArray) taskJSONObject.get(Commons.FOLLOWING_TASKS_JSON_KEY);
                        for(Object obj : followingTasksJSONArray) {
                            Integer followingTaskIndex = ((Long) obj).intValue();
                            if(followingTaskIndex < i)
                                followingTaskIndex = newIndexToOldMappings.get(followingTaskIndex);
                            JSONObject followingTaskJSONObject = (JSONObject) tasksJSONArray.get(followingTaskIndex);
                            JSONArray precTasksJSONArrayOfFolTask = (JSONArray) followingTaskJSONObject.get(Commons.PRECEDING_TASKS_JSON_KEY);
                            precTasksJSONArrayOfFolTask.set(precTasksJSONArrayOfFolTask.indexOf(oldTaskIndexInScenario), newTaskIndexInScenario);
                        }
                    } else {
                        newIndexToOldMappings.put(i, i);
                    }
                }
                for(Object obj : tasksJSONArray) {  
                    JSONObject taskJSONObject = (JSONObject) obj;
                    String taskNameAndOwner = taskJSONObject.get(Commons.WORKFLOW_OBJECT_NAME_JSON_KEY) + "(" + taskJSONObject.get(Commons.WORKFLOW_OBJECT_OWNER_JSON_KEY) + ")";
                    int taskIndexInCurrentDBTasksList = getWorkflowObjectIndex(tasksList, taskNameAndOwner);
                    if(taskIndexInCurrentDBTasksList != -1) {
                        WorkflowTask taskFromCurrentDB = tasksList.get(taskIndexInCurrentDBTasksList);

                        LinkedHashMap<String,String> urlParametersMap = taskFromCurrentDB.getUrlParametersCopy();
                        JSONArray urlParametersJSONArray = (JSONArray) taskJSONObject.get(Commons.URL_PARAMETERS_JSON_KEY);
                        for(Object obj1 : urlParametersJSONArray) {
                            JSONObject urlParameterJSONObject = (JSONObject) obj1;
                            if(urlParametersMap.containsKey(urlParameterJSONObject.get(Commons.URL_PARAMETER_NAME_JSON_KEY))) {
                                urlParametersMap.put((String)urlParameterJSONObject.get(Commons.URL_PARAMETER_NAME_JSON_KEY),
                                        (String)urlParameterJSONObject.get(Commons.URL_PARAMETER_VALUE_JSON_KEY));
                            }
                        }

                        LinkedHashMap<String, Object> inputFileParametersMap =  taskFromCurrentDB.getInputFilesInfosCopy();
                        JSONArray inputFileParametersJSONArray = (JSONArray) taskJSONObject.get(Commons.INPUT_FILES_PARAMETERS_JSON_KEY);
                        for(Object obj1 : inputFileParametersJSONArray) {
                            JSONObject inputFileParameterJSONObject = (JSONObject) obj1;
                            String inputFileParameterName = (String) inputFileParameterJSONObject.get(Commons.INP_F_PARAMETER_NAME_JSON_KEY);
                            if(inputFileParametersMap.containsKey(inputFileParameterName)) {
                                Object inputFileValueFromMap = inputFileParametersMap.get(inputFileParameterName);
                                Object inputFileValueFromScenario = inputFileParameterJSONObject.get(Commons.INP_F_PARAMETER_VALUE_JSON_KEY);
                                if((inputFileValueFromMap instanceof ArrayList) && (inputFileValueFromScenario instanceof JSONArray)) {
                                    ArrayList inputFilesArrayList = (ArrayList) inputFileValueFromMap;
                                    for(Object obj2 : (JSONArray) inputFileValueFromScenario) {
                                        if(obj2 instanceof String)
                                            inputFilesArrayList.add(obj2);
                                        else
                                            inputFilesArrayList.add(((Long)obj2).intValue());
                                    }
                                } else if((inputFileValueFromMap instanceof String) && ( ! (inputFileValueFromScenario instanceof JSONArray) )) {
                                    if(inputFileValueFromScenario instanceof String)
                                        inputFileParametersMap.put(inputFileParameterName, inputFileValueFromScenario);
                                    else
                                        inputFileParametersMap.put(inputFileParameterName, ((Long)inputFileValueFromScenario).intValue());
                                }                        
                            }
                        }

                        addTaskToScenario(taskFromCurrentDB);

                        WorkflowTask taskAddedToScenario = scenarioTasksList.get(scenarioTasksList.size() - 1);
                        taskAddedToScenario.setUrlParameters(urlParametersMap);
                        taskAddedToScenario.setInputFilesInfos(inputFileParametersMap);
                        for(Object obj1 : (JSONArray) taskJSONObject.get(Commons.PRECEDING_TASKS_JSON_KEY)) {
                            taskAddedToScenario.addPrecedingTask(((Long)obj1).intValue());
                        }
                        for(Object obj1 : (JSONArray) taskJSONObject.get(Commons.FOLLOWING_TASKS_JSON_KEY)) {
                            taskAddedToScenario.addFollowingTask(((Long)obj1).intValue());
                        }
                        taskAddedToScenario.setPosition(((Long)taskJSONObject.get(Commons.POSITION_X_JSON_KEY)).intValue(),
                                ((Long)taskJSONObject.get(Commons.POSITION_Y_JSON_KEY)).intValue());
                    }
                }
            }
            
            if(workflowObjectsJSONObject.containsKey(Commons.RESOURCES_JSON_KEY)) {
                JSONArray resourcesJSONArray = (JSONArray) workflowObjectsJSONObject.get(Commons.RESOURCES_JSON_KEY);                
                for(Object obj : resourcesJSONArray) {
                    JSONObject resourceJSONObject = (JSONObject) obj;
                    String resourceNameAndOwner = resourceJSONObject.get(Commons.WORKFLOW_OBJECT_NAME_JSON_KEY) + "(" + resourceJSONObject.get(Commons.WORKFLOW_OBJECT_OWNER_JSON_KEY) + ")";
                    int resourceIndexInCurrentDBTasksList = getWorkflowObjectIndex(resourcesList, resourceNameAndOwner);         
                    if(resourceIndexInCurrentDBTasksList == -1) {
                        if(! resourcesNotHavingCredentialsToNamesAndOwners.contains(resourceNameAndOwner))
                            resourcesNotHavingCredentialsToNamesAndOwners.add(resourceNameAndOwner);
                    } else {
                        addResourceToScenario(resourcesList.get(resourceIndexInCurrentDBTasksList));
                        WorkflowResource resourceAddedToScenario = scenarioResourcesList.get(scenarioResourcesList.size() - 1);
                        resourceAddedToScenario.setPosition(((Long)resourceJSONObject.get(Commons.POSITION_X_JSON_KEY)).intValue(),
                                ((Long)resourceJSONObject.get(Commons.POSITION_Y_JSON_KEY)).intValue());
                    }
                }
            }
            
            if((! tasksNotHavingCredentialsToNamesAndOwners.isEmpty()) || (! resourcesNotHavingCredentialsToNamesAndOwners.isEmpty())) {
                String alertText = "";
                if(! tasksNotHavingCredentialsToNamesAndOwners.isEmpty()) {
                    alertText = "Nie masz uprawnień do korzystania z zadań:";
                    for(String taskNameAndOwner : tasksNotHavingCredentialsToNamesAndOwners) {
                        alertText += "\n" + taskNameAndOwner;
                    }
                    if(! resourcesNotHavingCredentialsToNamesAndOwners.isEmpty()) {
                        alertText += "\noraz z zasobów:";
                        for(String resourceNameAndOwner : resourcesNotHavingCredentialsToNamesAndOwners) {
                            alertText += "\n" + resourceNameAndOwner;
                        }
                    }
                } else if(! resourcesNotHavingCredentialsToNamesAndOwners.isEmpty()) {
                    alertText = "Nie masz uprawnień do korzystania z zasobów:";
                    for(String resourceNameAndOwner : resourcesNotHavingCredentialsToNamesAndOwners) {
                        alertText += "\n" + resourceNameAndOwner;
                    }
                }
                alertText += ".\nTworząc scenariusz w oparciu o " + scenario.getNameAndOwner()
                        + "\nmożesz korzystać tylko z zadań i zasobów, do których posiadasz uprawnienia.";
                Alert alert = Commons.createAlert(AlertType.INFORMATION, "Brak uprawnień", alertText);
                alert.showAndWait();
            }
            
            if(scenario.getOwner().equals(MainApp.getCurrentUser())) {
                setSceneForScenarioEdition();
            } else {
                setSceneForScenarioAddOrCopy("Stwórz kopię scenariusza " + scenario.getNameAndOwner());
            }
        }
    }   
    
    public void setSchemeCanvasSize() {
        if(schemeCanvas.getScene() != null) {
            if(sceneSizeListener == null) {
                sceneSizeListener = new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                        setSchemeCanvasSize(); 
                    }
                };
                schemeCanvas.getScene().widthProperty().addListener(sceneSizeListener);
                schemeCanvas.getScene().heightProperty().addListener(sceneSizeListener); 
            }        
        
            schemeCanvas.setWidth(schemeCanvas.getScene().getWidth() - schemeCanvas.getLayoutX());
            schemeCanvas.setHeight(schemeCanvas.getScene().getHeight() - schemeCanvas.getLayoutY());
            drawSchemeCanvas();
        }
    }   
    
    private void addTaskToScenario(WorkflowTask task) {
        if(sameNamesInScenario.containsKey(task.getNameAndOwner())) {
            Integer indexWithSameNameInScenario = sameNamesInScenario.get(task.getNameAndOwner());
            sameNamesInScenario.put(task.getNameAndOwner(), indexWithSameNameInScenario + 1);
            scenarioTasksList.add(new WorkflowTask(task, this, scenarioTasksList.size(), indexWithSameNameInScenario));
        } else {
            sameNamesInScenario.put(task.getNameAndOwner(), 1);
            scenarioTasksList.add(new WorkflowTask(task, this, scenarioTasksList.size(), 0));
        }                       
        scenarioDrawingOrderList.add(new DrawingOrderInformation(DrawingOrderInformation.TASK, scenarioTasksList.size()-1));
    }
    
    private void addResourceToScenario(WorkflowResource resource) {
        scenarioResourcesList.add(new WorkflowResource(resource, scenarioResourcesList.size()));            
        scenarioDrawingOrderList.add(new DrawingOrderInformation(DrawingOrderInformation.RESOURCE, scenarioResourcesList.size()-1));
    }
    
    @FXML
    private void scenarioDescriptionButtonAction(ActionEvent event) {
        ScenarioDescriptionDialog dialog = new ScenarioDescriptionDialog(this);
        dialog.showAndWait();
    }

    public String getScenarioDescription() {
        return scenarioDescription;
    }

    public void setScenarioDescription(String scenarioDescription) {
        this.scenarioDescription = scenarioDescription;
    }
    
    private void tasksListViewMouseClicked() {
        resourcesListView.getSelectionModel().clearSelection();
        WorkflowTask chosenTask = (WorkflowTask)tasksListView.getSelectionModel().getSelectedItem();
        if(chosenTask != null) {
            infoTextArea.setText(chosenTask.info());
        }
        else {
            infoTextArea.setText("");
        }
    }
    
    private void resourcesListViewMouseClicked() {
        tasksListView.getSelectionModel().clearSelection();
        WorkflowResource chosenResource = (WorkflowResource)resourcesListView.getSelectionModel().getSelectedItem();
        if(chosenResource != null) {
            infoTextArea.setText(chosenResource.info());
        }
        else {
            infoTextArea.setText("");
        }
    }
    
    private void schemePaneMousePressed(MouseEvent event) {
        if(taskContextMenu != null)
            taskContextMenu.hide();
        if(resourceContextMenu != null)
            resourceContextMenu.hide();
        WorkflowObject chosenObject = null;
        WorkflowObject workflowObject;
        //currently clicked object will be now drawn last (on top)
        for(int i=scenarioDrawingOrderList.size()-1; i>=0; i--) {
            if(scenarioDrawingOrderList.get(i).getType().equals(DrawingOrderInformation.TASK)) {
                workflowObject = scenarioTasksList.get(scenarioDrawingOrderList.get(i).getIndexInScenario());
            } else {
                workflowObject = scenarioResourcesList.get(scenarioDrawingOrderList.get(i).getIndexInScenario());
            }
            if (event.getX() >= workflowObject.getPositionX()
                && (event.getX() <= workflowObject.getPositionX() + workflowObject.getSizeOnSchemeX())
                && (event.getY() >= workflowObject.getPositionY())
                && (event.getY() <= workflowObject.getPositionY() + workflowObject.getSizeOnSchemeY())) {
                
                chosenObject = workflowObject;
                if(i != scenarioDrawingOrderList.size()-1) {
                    
                    DrawingOrderInformation chosenTuple = scenarioDrawingOrderList.remove(i);
                    scenarioDrawingOrderList.add(chosenTuple);
                }
                break;
            }
        }
        if(chosenObject != null) {
            infoTextArea.setText(chosenObject.info());
            if(event.getButton()==MouseButton.PRIMARY) {
                //left mouse button - start of object's movement
                movingObject = chosenObject;              
                previousMousePositionX = (int) event.getX();
                previousMousePositionY = (int) event.getY();                
            }
            if(chosenObject.getClass().getSimpleName().equals("WorkflowResource")) {
                if(event.getButton()==MouseButton.SECONDARY) {
                    //right mouse button - show context menu
                    showResourceContextMenu(event, (WorkflowResource) chosenObject);
                }
            } else {
                WorkflowTask chosenTask = (WorkflowTask) chosenObject;
                if(event.getButton()==MouseButton.SECONDARY) {
                    //right mouse button - show context menu
                    showTaskContextMenu(event, chosenTask);
                }
                
                if(beginningConnectionTaskIndex != -1 && beginningConnectionTaskIndex != chosenTask.getIndexInScenario()) {
                    //new task connection establishment
                    if(connectionToPrecedingTask) {
                        scenarioTasksList.get(beginningConnectionTaskIndex).addPrecedingTask(chosenTask.getIndexInScenario());
                        chosenTask.addFollowingTask(beginningConnectionTaskIndex);
                    }
                    else {
                        scenarioTasksList.get(beginningConnectionTaskIndex).addFollowingTask(chosenTask.getIndexInScenario());
                        chosenTask.addPrecedingTask(beginningConnectionTaskIndex);
                    }
                }
            }            
        }
        beginningConnectionTaskIndex = -1;
        drawSchemeCanvas();
    }
    
    private void schemePaneMouseDragged(MouseEvent event) {
        if(movingObject != null) {
            //object's movment if mouse button is pressed
            int mouseMovementX = (int)event.getX() - previousMousePositionX;
            int mouseMovementY = (int)event.getY() - previousMousePositionY;
            int newResourcePositionX = movingObject.getPositionX() + mouseMovementX;
            int newResourcePositionY = movingObject.getPositionY() + mouseMovementY;
            if(newResourcePositionX >=0 && newResourcePositionY >=0)
            {
                movingObject.setPosition(newResourcePositionX, newResourcePositionY);              
                drawSchemeCanvas();
            }
            previousMousePositionX = (int)event.getX();
            previousMousePositionY = (int)event.getY();
        }
    }
    
    private void schemePaneMouseReleased(MouseEvent event) {
        if(event.getButton()==MouseButton.PRIMARY) {
            //stop of object's movement
            movingObject = null;             
        }
    }
    
    private void schemePaneMouseMoved(MouseEvent event) {
        if(beginningConnectionTaskIndex != -1) {
            //tasks connection drawing
            previousMousePositionX = (int)event.getX();
            previousMousePositionY = (int)event.getY();
            drawSchemeCanvas();
        }
    }
    
    private void showResourceContextMenu(MouseEvent event, final WorkflowResource resource) {
        resourceContextMenu = new ContextMenu();
        MenuItem infoMenuItem, removeResourceMenuItem;
        
        infoMenuItem = new MenuItem("Informacje");
        infoMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Alert alert = Commons.createAlert(AlertType.INFORMATION, resource.getNameAndOwner(), resource.info());
                    alert.initModality(Modality.NONE);
                    alert.show();
                }
            }
        );
        
        removeResourceMenuItem = new MenuItem("Usuń zasób");
        removeResourceMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie " + resource.getNameAndOwner(), 
                            "Czy rzeczywiscie chcesz usunąć zasób " + resource.getNameAndOwner() + " ze scenariusza?");
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        scenarioDrawingOrderList.remove(scenarioDrawingOrderList.size()-1);
                        scenarioResourcesList.remove(resource.getIndexInScenario());
                        for(int i=0; i<scenarioDrawingOrderList.size(); i++)
                        {
                            if(scenarioDrawingOrderList.get(i).getType().equals(DrawingOrderInformation.RESOURCE) 
                                    && scenarioDrawingOrderList.get(i).getIndexInScenario() > resource.getIndexInScenario())
                                scenarioDrawingOrderList.get(i).setIndexInScenario(scenarioDrawingOrderList.get(i).getIndexInScenario() - 1);
                        }
                        for(WorkflowResource r : scenarioResourcesList)
                        {
                            if(r.getIndexInScenario() > resource.getIndexInScenario())
                                r.setIndexInScenario(r.getIndexInScenario() - 1);
                        }
                        drawSchemeCanvas();
                    }
                }
            }
        );
        
        resourceContextMenu.getItems().addAll(infoMenuItem, removeResourceMenuItem);
        resourceContextMenu.show(schemeCanvas, event.getScreenX(), event.getScreenY());
    }
    
    private void showTaskContextMenu(MouseEvent event, final WorkflowTask task) {
        taskContextMenu = new ContextMenu();
        MenuItem infoMenuItem, connectToPrecedingTaskMenuItem, connectToFollowingTaskMenuItem, 
                manageConnecionsMenuItem, removeTaskMenuItem, setUrlParametersMenuItem, 
                chooseInputFilesMenuItem, saveOutputFileMenuItem, showFailReasonMenuItem;
        
        infoMenuItem = new MenuItem("Informacje");
        infoMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Alert alert = Commons.createAlert(AlertType.INFORMATION, task.getNameAndOwner(), task.info());                   
                    alert.initModality(Modality.NONE);                    
                    alert.show();
                }
            }
        );
        
        connectToPrecedingTaskMenuItem = new MenuItem("Połącz z poprzedzającym zadaniem w przepływie");
        connectToPrecedingTaskMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    beginTaskConnection(true, task.getIndexInScenario());
                }
            }
        );
        
        connectToFollowingTaskMenuItem = new MenuItem("Połącz z zadaniem kolejnym w przepływie");
        connectToFollowingTaskMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    beginTaskConnection(false, task.getIndexInScenario());
                }
            }
        );
        
        manageConnecionsMenuItem = new MenuItem("Zarządzaj połączonymi zadaniami");
        manageConnecionsMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    showManageTaskConnectionsDialog(task);
                }
            }
        ); 
        
        removeTaskMenuItem = new MenuItem("Usuń zadanie");
        removeTaskMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Usunięcie " + task.getNameWithIndexWithSameNameInScenario(),
                            "Czy rzeczywiscie chcesz usunąć zadanie " + task.getNameWithIndexWithSameNameInScenario() + " ze scenariusza?");
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        for(int index : task.getPrecedingTasksArray())
                        {
                            removeTaskConnection(index, task.getIndexInScenario());
                        }
                        for(int index : task.getFollowingTasksArray())
                        {
                            removeTaskConnection(task.getIndexInScenario(), index);
                        }
                        scenarioDrawingOrderList.remove(scenarioDrawingOrderList.size()-1);
                        scenarioTasksList.remove(task.getIndexInScenario());
                        for(int i=0; i<scenarioDrawingOrderList.size(); i++)
                        {
                            if(scenarioDrawingOrderList.get(i).getType().equals(DrawingOrderInformation.TASK) 
                                    && scenarioDrawingOrderList.get(i).getIndexInScenario() > task.getIndexInScenario())
                                scenarioDrawingOrderList.get(i).setIndexInScenario(scenarioDrawingOrderList.get(i).getIndexInScenario() - 1);
                        }
                        for(WorkflowTask t : scenarioTasksList)
                        {
                            t.actualizationAfterAnotherTaskRemoval(task);
                        }
                        sameNamesInScenario.put(task.getNameAndOwner(), sameNamesInScenario.get(task.getNameAndOwner()) - 1);
                        drawSchemeCanvas();
                    }
                }
            }
        );
        
        taskContextMenu.getItems().addAll(infoMenuItem, connectToPrecedingTaskMenuItem, 
                connectToFollowingTaskMenuItem, manageConnecionsMenuItem, removeTaskMenuItem);     
        
        if(!task.getCategory().getUrlParameters().isEmpty()) {
            setUrlParametersMenuItem = new MenuItem("Ustaw parametry w adresie url");
            setUrlParametersMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        showSetUrlParametersDialog(task);
                    }
                }
            );
            taskContextMenu.getItems().add(setUrlParametersMenuItem);
        }
        
        if(!task.getCategory().getInputFilesInfos().isEmpty()) {
            chooseInputFilesMenuItem = new MenuItem("Wybierz pliki wejściowe");
            chooseInputFilesMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        showChooseInputFilesDialog(task);
                    }
                }
            );
            taskContextMenu.getItems().add(chooseInputFilesMenuItem);
        }
        
        if(task.getExecutionState() == WorkflowTask.TASK_EXECUTED) {
            saveOutputFileMenuItem = new MenuItem("Zapisz plik wynikowy");
            saveOutputFileMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Zapisz plik wynikowy");      
                        File fileToSaveTo = fileChooser.showSaveDialog(tasksListView.getScene().getWindow());
                        if (fileToSaveTo != null) {
                            File taskOutputFile = new File(task.getTaskOutputFileName());
                            try {
                                int readedByte;
                                try (FileInputStream fis = new FileInputStream(taskOutputFile);FileOutputStream fos = new FileOutputStream(fileToSaveTo)) {
                                    while((readedByte=fis.read())!=-1) {
                                        fos.write(readedByte);
                                    }
                                }
                            } catch (IOException ex) {
                                Alert alert = Commons.createAlert(AlertType.ERROR, "Błąd podczas zapisywania pliku", "Błąd podczas zapisywania pliku");
                                alert.show(); 
                            }

                        }                       
                    }
                }
            );
            taskContextMenu.getItems().add(saveOutputFileMenuItem);
        }
        
        if(task.getExecutionState() == WorkflowTask.TASK_FAILED) {
            showFailReasonMenuItem = new MenuItem("Pokaż przyczyny błędu");
            showFailReasonMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        String failReasons = "";
                        TreeMap<Integer, String> failedTasks = task.getPrecedingTasksThatFailed();
                        for(int failedTaskIndex : failedTasks.keySet()) {
                            WorkflowTask failedTask = getTask(failedTaskIndex);
                            failReasons += "\n" + failedTask.getNameWithIndexWithSameNameInScenario() 
                                    + ": " + failedTasks.get(failedTaskIndex) + "\n";
                        }
                        Alert alert = Commons.createAlert(AlertType.ERROR, 
                                "Przyczyny błędu w zadaniu " + task.getNameWithIndexWithSameNameInScenario(), failReasons);
                        alert.initModality(Modality.NONE);
                        alert.show();                      
                    }
                }
            );
            taskContextMenu.getItems().add(showFailReasonMenuItem);
        }
        
        taskContextMenu.show(schemeCanvas, event.getScreenX(), event.getScreenY());
    }
    
    public WorkflowTask getTask(int taskIndex)
    {
        return scenarioTasksList.get(taskIndex);
    }
    
    public ArrayList<WorkflowTask> getTasksList()
    {
        return scenarioTasksList;
    }
    
    public ArrayList<WorkflowResource> getResourcesList()
    {
        return scenarioResourcesList;
    }
           
    private void beginTaskConnection(boolean connectionToPrecedingTask, int taskIndex)
    {
        this.connectionToPrecedingTask = connectionToPrecedingTask;
        beginningConnectionTaskIndex = taskIndex;
        int schemePaneLocationOnScreenX = (int)schemeCanvas.getScene().getWindow().getX() + (int)schemeCanvas.getScene().getX() + (int)schemeCanvas.getLayoutX();
        int schemePaneLocationOnScreenY = (int)schemeCanvas.getScene().getWindow().getY() + (int)schemeCanvas.getScene().getY() + (int)schemeCanvas.getLayoutY();
        previousMousePositionX = MouseInfo.getPointerInfo().getLocation().x - schemePaneLocationOnScreenX;
        previousMousePositionY = MouseInfo.getPointerInfo().getLocation().y - schemePaneLocationOnScreenY;
        drawSchemeCanvas();
    }
    
    private void showManageTaskConnectionsDialog(WorkflowTask task) {
        ManageTaskConnectionsDialog dialog = new ManageTaskConnectionsDialog(this, task);
        dialog.showAndWait();
    }
    
    public void removeTaskConnection(int taskIndex1, int taskIndex2)
    {
        WorkflowTask task1 = scenarioTasksList.get(taskIndex1);
        WorkflowTask task2 = scenarioTasksList.get(taskIndex2);
        task1.removeFollowingTask(taskIndex2);
        task2.removePrecedingTask(taskIndex1);
        drawSchemeCanvas();
    }
    
    private void showSetUrlParametersDialog(WorkflowTask task) {
        SetUrlParametersDialog dialog = new SetUrlParametersDialog(this, task);
        dialog.showAndWait();
    }
    
    private void showChooseInputFilesDialog(WorkflowTask task) {
        ChooseInputFilesDialog dialog = new ChooseInputFilesDialog(this, task);
        dialog.showAndWait();
    }
    
    //method draws cannection while it is created, second task not yet chosen
    private void drawTaskArrow(int taskIndex)
    {
        GraphicsContext gc = schemeCanvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        WorkflowTask task = scenarioTasksList.get(taskIndex);
        if(connectionToPrecedingTask)
        {
            gc.strokeLine(task.getLeftMiddlePointX(), 
                task.getLeftMiddlePointY(), 
                previousMousePositionX, 
                previousMousePositionY);
        }
        else
        {
            gc.strokeLine(task.getRightMiddlePointX(), 
                task.getRightMiddlePointY(), 
                previousMousePositionX, 
                previousMousePositionY);
        }
    }
    
    //method draws established connection between two tasks
    private void drawTaskArrow(int taskIndex1, int taskIndex2)
    {        
        WorkflowTask task1 = scenarioTasksList.get(taskIndex1);
        WorkflowTask task2 = scenarioTasksList.get(taskIndex2);
        GraphicsContext gc = schemeCanvas.getGraphicsContext2D();
        gc.setStroke(task1.getTaskColor());
        gc.strokeLine(task1.getRightMiddlePointX(), 
                task1.getRightMiddlePointY(), 
                task2.getLeftMiddlePointX(), 
                task2.getLeftMiddlePointY());
    }
    
    //method draws assingment of task to resource during scenario execution
    private void drawTaskResourceArrow(int taskIndex, int resourceIndex)
    {
        WorkflowTask task = scenarioTasksList.get(taskIndex);
        WorkflowResource resource = scenarioResourcesList.get(resourceIndex);
        GraphicsContext gc = schemeCanvas.getGraphicsContext2D();
        gc.setStroke(task.getTaskColor());
        if(task.getPositionY() > resource.getPositionY())
        {
            gc.strokeLine(task.getUpMiddlePointX(), 
                    task.getUpMiddlePointY(), 
                    resource.getDownMiddlePointX(), 
                    resource.getDownMiddlePointY());
        }
        else
        {
            gc.strokeLine(task.getDownMiddlePointX(), 
                    task.getDownMiddlePointY(), 
                    resource.getUpMiddlePointX(), 
                    resource.getUpMiddlePointY());
        }
    }
    
    public void drawSchemeCanvas() {
        GraphicsContext gc = schemeCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, schemeCanvas.getWidth(), schemeCanvas.getHeight());
        for(Object t : scenarioDrawingOrderList.toArray()) {
            DrawingOrderInformation tuple = (DrawingOrderInformation) t;
            if(tuple.getType().equals(DrawingOrderInformation.RESOURCE))
                scenarioResourcesList.get(tuple.getIndexInScenario()).draw(gc);
            else {
                WorkflowTask task = scenarioTasksList.get(tuple.getIndexInScenario());
                task.draw(gc);
                for(Integer followingTaskIndex : task.getFollowingTasksArray())
                {
                    drawTaskArrow(task.getIndexInScenario(),followingTaskIndex);
                }
                if(task.getResourceIndex() != -1)
                {
                    drawTaskResourceArrow(task.getIndexInScenario(), task.getResourceIndex());
                }
            }
        }
        if(beginningConnectionTaskIndex != -1)
            drawTaskArrow(beginningConnectionTaskIndex);
    }
    
    private boolean isScenarioCyclicSearch(int startingTaskIndex, int[] cycleSearchTaskStatus) {
        cycleSearchTaskStatus[startingTaskIndex] = 1;
        WorkflowTask task = scenarioTasksList.get(startingTaskIndex);
        for(Integer followingTaskIndex : task.getFollowingTasksArray()) {
            if(cycleSearchTaskStatus[followingTaskIndex] != 2) {
                if(cycleSearchTaskStatus[followingTaskIndex] == 1)
                    return true;
                if(isScenarioCyclicSearch(followingTaskIndex, cycleSearchTaskStatus))
                    return true;
            }
        }
        cycleSearchTaskStatus[startingTaskIndex] = 2;
        return false;
    }
    
    private boolean isScenarioCyclic() {
        int[] cycleSearchTaskStatus = new int[scenarioTasksList.size()];
        for(int i = 0; i < cycleSearchTaskStatus.length; i++) {
            cycleSearchTaskStatus[i] = 0;
        }
        for(int i = 0; i < cycleSearchTaskStatus.length; i++) {
            if(cycleSearchTaskStatus[i] == 0) {
                if(isScenarioCyclicSearch(i, cycleSearchTaskStatus))
                    return true;
            }
        }
        return false;
    }
    
    @FXML
    private void chooseWorkflowObjectButtonAction(ActionEvent event) {
        WorkflowTask chosenTask = (WorkflowTask)tasksListView.getSelectionModel().getSelectedItem();
        WorkflowResource chosenResource = (WorkflowResource)resourcesListView.getSelectionModel().getSelectedItem();
        if(chosenTask != null) {
            addTaskToScenario(chosenTask);
            drawSchemeCanvas();
        } else if(chosenResource != null) {
            addResourceToScenario(chosenResource);
            drawSchemeCanvas();
        }
    }
    
    @FXML
    private void executeScenarioButtonAction(ActionEvent event) {
        String failReason = "";     
        if(scenarioResourcesList.isEmpty()) {
            failReason = "Należy dodać przynajmniej jeden zasób";
        } else if(scenarioTasksList.isEmpty()) {
            failReason = "Należy dodać przynajmniej jedno zadanie";
        } else {
            for(WorkflowTask task : scenarioTasksList) {
                if(!task.getCategory().getUrlParameters().isEmpty()) {
                    LinkedHashMap<String, String> urlParameters = task.getUrlParametersCopy();
                    for(String parameter : urlParameters.keySet()) {
                        if(urlParameters.get(parameter).equals("")) {
                             failReason = "Parametr w adresie url " + parameter + " \nw zadaniu " 
                                + task.getNameWithIndexWithSameNameInScenario()
                                + " \nnie został ustawiony";
                            break;
                        }
                    }
                }
                if(!failReason.equals(""))
                    break;
                if(!task.getCategory().getInputFilesInfos().isEmpty()) {
                    LinkedHashMap<String, Object> inputFileInfos = task.getInputFilesInfosCopy();
                    for(String parameter : inputFileInfos.keySet()) {
                        if((inputFileInfos.get(parameter) instanceof ArrayList)
                                && (((ArrayList)inputFileInfos.get(parameter)).isEmpty())) {
                                                                                
                            failReason = "Lista plików wejściowych " + parameter + " \nw zadaniu " 
                                + task.getNameWithIndexWithSameNameInScenario()
                                + " \njest pusta";
                            break;
                        } else if(inputFileInfos.get(parameter).equals("")) {
                                                                                        
                            failReason = "Plik wejściowy " + parameter + " \nw zadaniu " 
                                    + task.getNameWithIndexWithSameNameInScenario()
                                    + " \nnie został wybrany";
                            break;
                        } 
                    }
                }
                if(!failReason.equals(""))
                    break;
                boolean resorceAcceptingCurrentTaskCategoryFound = false;
                for(WorkflowResource resource : scenarioResourcesList) {
                    if(resource.acceptsCategory(task.getCategory())) {
                        resorceAcceptingCurrentTaskCategoryFound = true;
                        break;
                    }
                }
                if(!resorceAcceptingCurrentTaskCategoryFound) {
                    failReason = "Żaden zasób nie obsługuje kategorii " + task.getCategory().getNameAndOwner() +
                            " \nzadania " + task.getNameWithIndexWithSameNameInScenario();
                    break;
                }
            }
            if(failReason.equals("") && isScenarioCyclic())
                failReason = "Scenariusz zawiera cykl zależności";
        }
        if(failReason.equals("")) {
            ExecuteScenarioDialog dialog = new ExecuteScenarioDialog(this);
            dialog.showAndWait();
        } else {
            Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można wykonać scenariusza", failReason);
            alert.showAndWait();
        }
    }
    
    private JSONObject createScenarioJSON() {
        JSONObject scenarioJSONObject = null, workflowObjectsJSONObject;
        
        if(nameField.getText().trim().isEmpty()) {
            Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można zapisać scenariusza", "Nazwa scenariusza nie może być pusta");
            alert.showAndWait();
        } else {
            scenarioJSONObject = Commons.createLoginJSONObject(); 
            if(! scenarioDescription.equals("")) {
                scenarioJSONObject.put(Commons.DESCRIPTION_JSON_KEY, scenarioDescription);
            }
            workflowObjectsJSONObject = new JSONObject();
            if(!scenarioTasksList.isEmpty()) {            
                JSONArray scenarioTasksArray = new JSONArray();
                for(WorkflowTask task : scenarioTasksList) {                
                    JSONObject taskJSONObject = new JSONObject();
                    taskJSONObject.put(Commons.WORKFLOW_OBJECT_OWNER_JSON_KEY, task.getOwner());
                    taskJSONObject.put(Commons.WORKFLOW_OBJECT_NAME_JSON_KEY, task.getName());
                    taskJSONObject.put(Commons.POSITION_X_JSON_KEY, task.getPositionX());
                    taskJSONObject.put(Commons.POSITION_Y_JSON_KEY, task.getPositionY());
                    JSONArray precedingTasksJSONArray = new JSONArray();
                    for(Integer precedingTaskIndex : task.getPrecedingTasksArray()) {
                        precedingTasksJSONArray.add(precedingTaskIndex);
                    }
                    taskJSONObject.put(Commons.PRECEDING_TASKS_JSON_KEY, precedingTasksJSONArray);
                    JSONArray followingTasksJSONArray = new JSONArray();
                    for(Integer followingTaskIndex : task.getFollowingTasksArray()) {
                        followingTasksJSONArray.add(followingTaskIndex);
                    }
                    taskJSONObject.put(Commons.FOLLOWING_TASKS_JSON_KEY, followingTasksJSONArray);
                    
                    JSONArray urlParametersJSONArray = new JSONArray();
                    LinkedHashMap <String, String> urlParameters = task.getUrlParametersCopy();
                    for(String urlParameterKey : urlParameters.keySet()) {
                        JSONObject urlParameterJSONObject = new JSONObject();
                        urlParameterJSONObject.put(Commons.URL_PARAMETER_NAME_JSON_KEY, urlParameterKey);
                        urlParameterJSONObject.put(Commons.URL_PARAMETER_VALUE_JSON_KEY, urlParameters.get(urlParameterKey));
                        urlParametersJSONArray.add(urlParameterJSONObject);
                    }
                    taskJSONObject.put(Commons.URL_PARAMETERS_JSON_KEY, urlParametersJSONArray);
                    
                    JSONArray inputFilesParametersJSONArray = new JSONArray();
                    LinkedHashMap <String, Object> inputFilesParameters = task.getInputFilesInfosCopy();
                    for(String inputFileParameterKey : inputFilesParameters.keySet()) {
                        JSONObject inputFileParameterJSONObject = new JSONObject();
                        inputFileParameterJSONObject.put(Commons.INP_F_PARAMETER_NAME_JSON_KEY, inputFileParameterKey);
                        Object inputFileParameterValue = inputFilesParameters.get(inputFileParameterKey);
                        if(inputFileParameterValue instanceof ArrayList) {
                            JSONArray oneParameterJSONArray = new JSONArray();
                            for(Object obj : (ArrayList)inputFileParameterValue) {
                                oneParameterJSONArray.add(obj);
                            }
                            inputFileParameterJSONObject.put(Commons.INP_F_PARAMETER_VALUE_JSON_KEY, oneParameterJSONArray);
                        } else {
                            inputFileParameterJSONObject.put(Commons.INP_F_PARAMETER_VALUE_JSON_KEY, inputFileParameterValue);
                        }
                        inputFilesParametersJSONArray.add(inputFileParameterJSONObject);
                    }
                    taskJSONObject.put(Commons.INPUT_FILES_PARAMETERS_JSON_KEY, inputFilesParametersJSONArray);

                    scenarioTasksArray.add(taskJSONObject);
                }
                workflowObjectsJSONObject.put(Commons.TASKS_JSON_KEY, scenarioTasksArray);
            }
            
            if(!scenarioResourcesList.isEmpty()) {
                JSONArray scenarioResourcesArray = new JSONArray();
                for(WorkflowResource resource : scenarioResourcesList) {         
                    JSONObject resourceJSONObject = new JSONObject();
                    resourceJSONObject.put(Commons.WORKFLOW_OBJECT_OWNER_JSON_KEY, resource.getOwner());
                    resourceJSONObject.put(Commons.WORKFLOW_OBJECT_NAME_JSON_KEY, resource.getName());
                    resourceJSONObject.put(Commons.POSITION_X_JSON_KEY, resource.getPositionX());
                    resourceJSONObject.put(Commons.POSITION_Y_JSON_KEY, resource.getPositionY());
                    scenarioResourcesArray.add(resourceJSONObject);
                }            
                workflowObjectsJSONObject.put(Commons.RESOURCES_JSON_KEY, scenarioResourcesArray);
            }
            scenarioJSONObject.put(Commons.WORKFLOW_OBJECTS_JSON_KEY, workflowObjectsJSONObject);
        } 
        return scenarioJSONObject;
    }
    
    private boolean saveScenario(JSONObject scenarioJSONObject, String urlString, String successCommunicate) {
        try {
            String resultString = Commons.postRequestToServer(urlString, scenarioJSONObject);
            int loginResult = (int)resultString.charAt(0);
            switch (loginResult) {
                case Commons.OPERATION_SUCCESSFULL:
                    Alert alert = Commons.createAlert(AlertType.INFORMATION, "", successCommunicate);
                    alert.showAndWait();
                    return true;
                case SUCH_SCENARIO_ALREADY_EXISTS:
                    alert = Commons.createAlert(AlertType.ERROR, "Nie można zapisać scenariusza", "Posiadasz już inny scenariusz o takiej nazwie");
                    alert.showAndWait();
                    break;
                default:
                    alert = Commons.createAlert(AlertType.ERROR, "Nie można zapisać scenariusza", "Błąd połączenia z serwerem");
                    alert.showAndWait();
                    break;
            }
        } catch (IOException ex) {
            Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można zapisać scenariusza", "Błąd serwera");
            alert.showAndWait();
        }
        return false;
    }
    
    
    @FXML
    private void saveNewScenarioButtonAction(ActionEvent event) {
        JSONObject scenarioJSONObject = createScenarioJSON();
        if(scenarioJSONObject != null) {
            if(nameField.getText().trim().equals(scenarioPreviousName)) {
                Alert alert = Commons.createAlert(AlertType.ERROR, "Nie można zapisać scenariusza", "Musisz wybrać nową nazwę scenariusza");
                alert.showAndWait();           
            } else {
                Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", "Czy rzeczywiście chcesz dodać nowy scenariusz?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    scenarioJSONObject.put(Commons.NAME_JSON_KEY, nameField.getText().trim());
                    boolean scenarioSuccessfullySaved = saveScenario(scenarioJSONObject, "/scenario/add", "Utworzony został nowy scenarusz");
                    if(scenarioSuccessfullySaved)
                        setSceneForScenarioEdition();
                }
            }
        }
    }
    
    @FXML
    private void saveChangesButtonAction(ActionEvent event) {
        JSONObject scenarioJSONObject = createScenarioJSON();
        if(scenarioJSONObject != null) {
            Alert alert = Commons.createAlert(AlertType.CONFIRMATION, "Zapisanie zmian", "Czy rzeczywiście chcesz zapisać zmiany w scenariuszu?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                if(nameField.getText().trim().equals(scenarioPreviousName)) {
                    scenarioJSONObject.put(Commons.NAME_JSON_KEY, nameField.getText().trim());                
                } else {
                    scenarioJSONObject.put(Commons.OLD_NAME_JSON_KEY, scenarioPreviousName);
                    scenarioJSONObject.put(Commons.NEW_NAME_JSON_KEY, nameField.getText().trim());
                }
                boolean scenarioSuccessfullySaved = saveScenario(scenarioJSONObject, "/scenario/edit", "Zmiany w scenariuszu zostały zapisane");
                if(( ! nameField.getText().trim().equals(scenarioPreviousName)) && scenarioSuccessfullySaved)
                    setSceneForScenarioEdition();
            }
        }
    }
}
