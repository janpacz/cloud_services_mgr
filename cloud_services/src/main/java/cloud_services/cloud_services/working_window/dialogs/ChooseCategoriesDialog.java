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
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public abstract class ChooseCategoriesDialog extends ApplicationDialog {
    @FXML protected Label titleLabel;
    protected ObservableList<WorkflowTaskCategory> categoriesToChooseList;
    @FXML protected ListView categoriesToChooseListView;
    @FXML private TextArea categoryInfoTextArea;
    @FXML private Button saveCategoriesButton;
    @FXML private ComboBox sortingComboBox;
    protected String title;
    protected WorkflowObjectDialog dialog;
    
    protected ChooseCategoriesDialog(WorkflowObjectDialog dialog) {
        super("/fxml/dialogs/ChooseCategoriesDialog.fxml");
        this.dialog = dialog;
                
        categoriesToChooseList = FXCollections.observableArrayList();
        categoriesToChooseListView.setItems(categoriesToChooseList);
        categoriesToChooseListView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<WorkflowTaskCategory>() {
                    @Override
                    public void changed(ObservableValue<? extends WorkflowTaskCategory> ov, WorkflowTaskCategory old_val, WorkflowTaskCategory new_val) {                        
                        if(new_val == null) { 
                            saveCategoriesButton.setDisable(true);
                            categoryInfoTextArea.setText("");
                        } else {
                            saveCategoriesButton.setDisable(false);
                            categoryInfoTextArea.setText(new_val.info());
                        }

                    }
                }
        );
        
        saveCategoriesButton.setDisable(true);
        categoriesToChooseList.addAll(dialog.getNotChosenCategoriesList());
        
        sortingComboBox.setItems(FXCollections.observableArrayList(
                    Commons.SORTING_NAME_ASCENDING,
                    Commons.SORTING_NAME_DESCENDING
                ));
        sortingComboBox.setValue(Commons.SORTING_NAME_ASCENDING);
        sortingComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String old_val, String new_val) {
                Commons.sortElements(new_val, categoriesToChooseList);                
            }
        });
        
        Commons.sortElements((String)sortingComboBox.getValue(), categoriesToChooseList);
    }
    
    protected void setTitle() {
        this.setTitle(title);
        titleLabel.setText(title);
    }
    
    @FXML protected abstract void saveCategoriesButtonAction(ActionEvent event); 
}
