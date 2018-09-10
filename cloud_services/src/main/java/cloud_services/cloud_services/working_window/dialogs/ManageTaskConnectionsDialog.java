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
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ManageTaskConnectionsDialog extends ApplicationDialog {
    @FXML private ListView precedingTasksListView;
    @FXML private ListView followingTasksListView;
    @FXML private TextArea infoTextArea;
    @FXML private Button removeConnectionButton;
    private ScenarioController controller;
    private WorkflowTask task;
    private ObservableList<WorkflowTask> precedingTasksList;
    private ObservableList<WorkflowTask> followingTasksList;
    
    public ManageTaskConnectionsDialog(ScenarioController controller, WorkflowTask task) {
        super("/fxml/dialogs/ManageTaskConnectionsDialog.fxml");
        
        this.controller = controller;
        this.task = task;        
              
        this.setTitle("Zarządzanie połączeniami " + task.getNameWithIndexWithSameNameInScenario());
        
        precedingTasksList = FXCollections.observableArrayList();
        precedingTasksListView.setItems(precedingTasksList);
        precedingTasksListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<WorkflowTask>() {
                public void changed(ObservableValue<? extends WorkflowTask> ov, WorkflowTask old_val, WorkflowTask new_val) {
                    listSelectionChange(precedingTasksListView, followingTasksListView); 
            }
        });
        for(int precedingTaskIndex : task.getPrecedingTasksArray())
        {
            precedingTasksList.add(controller.getTask(precedingTaskIndex));
        }
        
        followingTasksList = FXCollections.observableArrayList();
        followingTasksListView.setItems(followingTasksList);
        followingTasksListView.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<WorkflowTask>() {
                @Override
                public void changed(ObservableValue<? extends WorkflowTask> ov, WorkflowTask old_val, WorkflowTask new_val) {
                    listSelectionChange(followingTasksListView, precedingTasksListView); 
            }
        });
        for(int followingTaskIndex : task.getFollowingTasksArray())
        {
            followingTasksList.add(controller.getTask(followingTaskIndex));
        }
        
        removeConnectionButton.setDisable(true);
    }
    
    private void listSelectionChange(ListView changedSelectionListView, ListView otherListView)
    {
        WorkflowTask chosenTask = (WorkflowTask)changedSelectionListView.getSelectionModel().getSelectedItem();
        if(chosenTask != null)
        {
            otherListView.getSelectionModel().clearSelection();
            infoTextArea.setText(chosenTask.info());
            removeConnectionButton.setDisable(false);
        }
        else
        {
            if(otherListView.getSelectionModel().getSelectedItem() == null)
            {
                infoTextArea.setText("");
                removeConnectionButton.setDisable(true);
            }
        }
    }
    
    private boolean removeConnectionDialog(int taskIndex1, int taskIndex2)
    {
        WorkflowTask task1 = controller.getTask(taskIndex1);
        WorkflowTask task2 = controller.getTask(taskIndex2);
        Alert alert = Commons.createAlert(AlertType.CONFIRMATION, 
                "Usunięcie połączenia " + task1.getNameWithIndexWithSameNameInScenario() 
                        + " z " + task2.getNameWithIndexWithSameNameInScenario(),
                "Czy rzeczywiscie chcesz usunąć połączenie między zadaniami " + task1.getNameWithIndexWithSameNameInScenario() 
                        + " i " + task2.getNameWithIndexWithSameNameInScenario() + " ze scenariusza?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {        
            controller.removeTaskConnection(taskIndex1, taskIndex2);
            return true;
        }
        return false;
    }
    
    @FXML
    private void removeConnectionButtonAction(ActionEvent event) {
        int chosenPrecedingTaskIndex = precedingTasksListView.getSelectionModel().getSelectedIndex();
        int chosenFollowingTaskIndex = followingTasksListView.getSelectionModel().getSelectedIndex();
        if((chosenPrecedingTaskIndex != -1)
                && (removeConnectionDialog(task.getPrecedingTasksArray()[chosenPrecedingTaskIndex], task.getIndexInScenario()))) {
            
            precedingTasksList.remove(chosenPrecedingTaskIndex);           
        }
        else if((chosenFollowingTaskIndex != -1) 
                && (removeConnectionDialog(task.getIndexInScenario(), task.getFollowingTasksArray()[chosenFollowingTaskIndex]))) {
            followingTasksList.remove(chosenFollowingTaskIndex);
        }
    }
}
