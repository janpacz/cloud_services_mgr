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

package cloud_services.cloud_services.working_window.menu_bars;

import cloud_services.cloud_services.MainApp;
import cloud_services.cloud_services.working_window.ManageCategoriesController;
import cloud_services.cloud_services.working_window.ManageResourcesController;
import cloud_services.cloud_services.working_window.ManageScenariosController;
import cloud_services.cloud_services.working_window.ManageTasksController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;

public class UserLoggedInMenuBarController implements Initializable {
    @FXML private MenuBar menuBar;
    @FXML private Menu userNameMenu;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userNameMenu.setText("Zalogowany jako: " + MainApp.getCurrentUser());
    }
    
    @FXML
    private void logoutMenuAction(ActionEvent event) {
        MainApp.setCurrentUser("");
        MainApp.setCurrentPassword("");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoggingScene.fxml"));
            menuBar.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(UserLoggedInMenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void manageCategoriesMenuAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ManageScenarioDescriptionElementsScene.fxml"));
            fxmlLoader.setController(new ManageCategoriesController());
            Parent root = fxmlLoader.load();
            menuBar.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(UserLoggedInMenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void manageTasksMenuAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ManageScenarioDescriptionElementsScene.fxml"));
            fxmlLoader.setController(new ManageTasksController());
            Parent root = fxmlLoader.load();
            menuBar.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(UserLoggedInMenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void manageResourcesMenuAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ManageScenarioDescriptionElementsScene.fxml"));
            fxmlLoader.setController(new ManageResourcesController());
            Parent root = fxmlLoader.load();
            menuBar.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(UserLoggedInMenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void manageScenariosMenuAction(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ManageScenarioDescriptionElementsScene.fxml"));
            fxmlLoader.setController(new ManageScenariosController());
            Parent root = fxmlLoader.load();
            menuBar.getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(UserLoggedInMenuBarController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
