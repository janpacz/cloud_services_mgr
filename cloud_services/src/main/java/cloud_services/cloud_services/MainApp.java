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

package cloud_services.cloud_services;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    
    //current user of application and it's password
    private static String currentUser="";
    private static String currentPassword="";
    
    private static String serverUrl="";
    private static final String SERVER_URL_PROPERTY = "server_url";
    
    public static String getCurrentUser() {
        return currentUser;
    }
    
    public static void setCurrentUser(String currentUser) {
        MainApp.currentUser = currentUser;
    }
    
    public static String getCurrentPassword() {
        return currentPassword;
    }
    
    public static void setCurrentPassword(String currentPassword) {
        MainApp.currentPassword = currentPassword;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    @Override
    public void start(Stage stage) throws Exception {
        Properties prop = new Properties();
        try(FileInputStream fis = new FileInputStream("config.properties")) {
            prop.load(fis);
        }
        serverUrl = prop.getProperty(SERVER_URL_PROPERTY);
        
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/LoggingScene.fxml"));
        Scene scene = new Scene(root);
        
        stage.setTitle("Cloud Services");
        stage.setWidth(720);
        stage.setHeight(790);
        stage.setScene(scene);
        stage.show();        
        
        try {
            URL url = new URL(serverUrl + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                 System.out.println("Connection with server failed. Error code is " + conn.getResponseCode());
            }
            else
            {
                System.out.println("Connection with server established");
            }
        } catch (IOException ex) {
            System.out.println("Connection with server failed.");
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
