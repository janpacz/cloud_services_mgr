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

import cloud_services.cloud_services.scenario_description_objects.ResourceUrlParameter;
import cloud_services.cloud_services.scenario_description_objects.RestTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.Scenario;
import cloud_services.cloud_services.scenario_description_objects.ScenarioDescriptionElement;
import cloud_services.cloud_services.scenario_description_objects.SoapTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.TaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.ManageCategoriesController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Commons {
    public static final String LOGIN_JSON_KEY = "login";
    public static final String PASSWORD_JSON_KEY = "password";
    public static final String NAME_JSON_KEY = "name";
    public static final String OLD_NAME_JSON_KEY = "older_name";
    public static final String NEW_NAME_JSON_KEY = "new_name";
    public static final String DESCRIPTION_JSON_KEY = "description";
    public static final String URL_PARAMETERS_JSON_KEY = "urlParameters";
    public static final String INPUT_FILES_PARAMETERS_JSON_KEY = "inputFilesParametersArray";
    public static final String INP_F_PARAMETER_NAME_JSON_KEY = "inputFileParameterName";
    public static final String INP_F_PARAMETER_TYPE_JSON_KEY = "inputFileParameterType";
    
    public static final String CATEGORY_OWNER_JSON_KEY = "category_owner";
    public static final String CATEGORY_NAME_JSON_KEY = "category_name";
    public static final String CATEGORY_JSON_KEY = "category";
    public static final String DATA_TO_SEND_SIZE_JSON_KEY = "data_size";
    public static final String OPERATIONS_AMOUNT_JSON_KEY = "operations_amount";
    
    public static final String OPERATION_TIME_JSON_KEY = "operation_time";
    public static final String OPERATION_COST_JSON_KEY = "operation_cost";
    public static final String DATA_SEND_TIME_JSON_KEY = "data_send_time";
    public static final String TASKS_AT_ONCE_AMOUNT_JSON_KEY = "tasks_at_once_amount";
    public static final String CATEGORIES_JSON_KEY = "categories";
    public static final String CATEGORY_TECHNOLOGY_JSON_KEY = "category_technology";
    public static final String TECHNOLOGY_URL_JSON_KEY = "technology_url";
    public static final String TECHNOLOGY_URL_PARAMETERS_JSON_KEY = "technology_query_parameters";
    public static final String TECHNOLOGY_URL_PARAMETER_NAME = "technology_query_parameter_name";
    public static final String TECHNOLOGY_URL_PARAMETER_TYPE = "technology_query_parameter_type";
    public static final String TECHNOLOGY_URL_PARAMETER_VALUE = "technology_query_parameter_value";
    public static final String TECHNOLOGY_TYPE_JSON_KEY = "technology_type";
    public static final String REST_TECHNOLOGY_TYPE = "rest";
    public static final String REST_EXPECTED_RESPONSE_CODE = "expected_response_code";
    public static final String SOAP_TECHNOLOGY_TYPE = "soap";
    public static final String SOAP_METHOD_NAME = "method_name";
    public static final String SOAP_NAMESPACE_PREFIX = "namespace_prefix";
    public static final String SOAP_NAMESPACE_URI = "namespace_uri";
    
    public static final String ORDER_JSON_KEY = "order";
            
    public static final String USER_JSON_KEY = "user";
    public static final String SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY = "scenario_description_element_type";
    public static final String SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY = "scenario_description_element_category";
    public static final String SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY = "scenario_description_element_task";
    public static final String SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY = "scenario_description_element_resource";
    public static final String SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY = "scenario_description_element_scenario";
    public static final String USERS_HAVING_CREDENTIALS_JSON_KEY = "users_having_credentials";
    public static final String USERS_RECEIVING_CREDENTIALS_JSON_KEY = "users_receiving_credentials";
    public static final String USERS_WITHOUT_CREDENTIALS_JSON_KEY = "users_without_credentials";
    public static final String USERS_SEARCH_STRING = "users_search_string";
    
    public static final String WORKFLOW_OBJECT_OWNER_JSON_KEY = "workflow_object_owner";
    public static final String WORKFLOW_OBJECT_NAME_JSON_KEY = "workflow_object_name";
    public static final String POSITION_X_JSON_KEY = "position_x";
    public static final String POSITION_Y_JSON_KEY = "position_y";
    public static final String PRECEDING_TASKS_JSON_KEY = "preceding_tasks";
    public static final String FOLLOWING_TASKS_JSON_KEY = "following_tasks";
    public static final String URL_PARAMETER_NAME_JSON_KEY = "url_parameter_name";
    public static final String URL_PARAMETER_VALUE_JSON_KEY = "url_parameter_value";
    public static final String INP_F_PARAMETER_VALUE_JSON_KEY = "input_file_parameter_value";
    public static final String TASKS_JSON_KEY = "tasks";
    public static final String RESOURCES_JSON_KEY = "resources";
    public static final String WORKFLOW_OBJECTS_JSON_KEY = "workflow_objects";
    
    // result codes of server responses
    public static final int OPERATION_SUCCESSFULL = 0;
    public static final int USER_DOES_NOT_EXIST = 1;
    public static final int WRONG_PASSWORD = 2;
    public static final int SERVER_ERROR = -1;
    
    //sorting orders
    public static final String SORTING_NAME_ASCENDING = "Według nazwy - rosnąco";
    public static final String SORTING_NAME_DESCENDING = "Według nazwy - malejąco";
    
    //comparator used for elements which order is stored in DB 
    public static Comparator<JSONObject> orderJSONKeyComparator = new Comparator<JSONObject>() {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            if((Long)o1.get(ORDER_JSON_KEY) > (Long)o2.get(ORDER_JSON_KEY))
                return 1;
            return -1;
        }
    };
    
    //sorting elements by name
    public static void sortElements(String sorting, List<? extends ScenarioDescriptionElement> elementsList) {
        Comparator<ScenarioDescriptionElement> comparator;
        if(sorting.equals(SORTING_NAME_ASCENDING) || sorting.equals(SORTING_NAME_DESCENDING)) {
            comparator = new Comparator<ScenarioDescriptionElement>() {
                @Override
                public int compare(ScenarioDescriptionElement left, ScenarioDescriptionElement right) {
                    return left.getName().compareToIgnoreCase(right.getName());
                }
            };
            if(sorting.equals(SORTING_NAME_ASCENDING)) {
                Collections.sort(elementsList, comparator);                        
            } else {
                Collections.sort(elementsList, comparator.reversed());
            }
        }
    }
    
    public static JSONObject createLoginJSONObject() {
        JSONObject loginJSONObject = new JSONObject();
        loginJSONObject.put(LOGIN_JSON_KEY, MainApp.getCurrentUser());
        loginJSONObject.put(PASSWORD_JSON_KEY, MainApp.getCurrentPassword());
        return loginJSONObject;
    }
    
    public static String postRequestToServer(String urlString, JSONObject objectToSend) throws IOException {
        URL serverUrl = new URL(MainApp.getServerUrl() + urlString);
        HttpURLConnection connection;
        connection = (HttpURLConnection) serverUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
            dos.writeChars(objectToSend.toJSONString());
        }
        String resultString = "";
        if (connection.getResponseCode() != 200) {
            resultString += (char)SERVER_ERROR;
        } else {
            try (DataInputStream dis = new DataInputStream(connection.getInputStream())) {
                try {
                    while(true) {                    
                        resultString+=dis.readChar();                    
                    }
                } catch (EOFException ex) {}
            }
        }
        return resultString;
    }
    
    public static String postRequestToServer(String urlString, JSONObject objectToSend, Label communicateLabel) throws IOException {
        communicateLabel.setText("");
        return postRequestToServer(urlString, objectToSend);        
    }
    
    public static Alert createAlert(AlertType alertType, String title, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentText);
        alert.getDialogPane().setMinWidth(title.length() * 10);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }
    
    private static WorkflowTaskCategory getCategoryFromJSON(JSONObject categoryJSONObject) {
        WorkflowTaskCategory category;
        String categoryOwner = (String) categoryJSONObject.get(LOGIN_JSON_KEY);
        String categoryName = (String) categoryJSONObject.get(NAME_JSON_KEY);
        String categoryDescription;
        if(categoryJSONObject.containsKey(DESCRIPTION_JSON_KEY)) 
            categoryDescription = (String) categoryJSONObject.get(DESCRIPTION_JSON_KEY);
        else
            categoryDescription = null;
        ArrayList<String> urlParameters = new ArrayList<>();
        if(categoryJSONObject.containsKey(URL_PARAMETERS_JSON_KEY)) {
            for(Object obj1 : (JSONArray)categoryJSONObject.get(URL_PARAMETERS_JSON_KEY))
                urlParameters.add((String)obj1);
        }
        LinkedHashMap<String, String> inputFilesInfos = new LinkedHashMap<>();
        if(categoryJSONObject.containsKey(INPUT_FILES_PARAMETERS_JSON_KEY)) {
            for(Object obj1 : (JSONArray)categoryJSONObject.get(INPUT_FILES_PARAMETERS_JSON_KEY)) {
                JSONObject inputFileJSONObject = (JSONObject) obj1;
                if(inputFileJSONObject.get(INP_F_PARAMETER_TYPE_JSON_KEY).equals(ManageCategoriesController.SINGLE_INPUT_FILE))
                    inputFilesInfos.put((String)inputFileJSONObject.get(INP_F_PARAMETER_NAME_JSON_KEY), WorkflowTaskCategory.INPUT_FILE_TYPE_SINGLE);
                else
                    inputFilesInfos.put((String)inputFileJSONObject.get(INP_F_PARAMETER_NAME_JSON_KEY), WorkflowTaskCategory.INPUT_FILE_TYPE_MULTIPLE);
            }
        }
        if(categoryDescription == null) 
            category = new WorkflowTaskCategory(categoryOwner,categoryName,urlParameters,inputFilesInfos);
        else
            category = new WorkflowTaskCategory(categoryOwner,categoryName,urlParameters,inputFilesInfos,categoryDescription);
        
        return category;
    }
    
    public static ArrayList<WorkflowTaskCategory> getCategoriesFromServer(Label communicateLabel) {
        ArrayList<WorkflowTaskCategory> categoriesArrayList = new ArrayList<>();
        JSONObject loginJSONObject = createLoginJSONObject();        
        try {
            String resultString = postRequestToServer("/category/get", loginJSONObject);
            int operationResult = (int)resultString.charAt(0);
            if(operationResult == OPERATION_SUCCESSFULL) { 
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray categoriesJSONArray = (JSONArray)parser.parse(resultString);
                for(Object obj : categoriesJSONArray) {
                    categoriesArrayList.add(getCategoryFromJSON((JSONObject) obj));
                }
            } else {
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        } catch (IOException | ParseException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
        return categoriesArrayList;
    }
    
    public static ArrayList<WorkflowTask> getTasksFromServer() {
        return getTasksFromServer(null);
    }
    
    public static ArrayList<WorkflowTask> getTasksFromServer(Label communicateLabel) {
        ArrayList<WorkflowTask> tasksArrayList = new ArrayList<>();
        JSONObject loginJSONObject = createLoginJSONObject();
        try {
            String resultString = postRequestToServer("/task/get", loginJSONObject);
            int operationResult = (int)resultString.charAt(0);
            if(operationResult == OPERATION_SUCCESSFULL) {
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray tasksJSONArray = (JSONArray)parser.parse(resultString);
                for(Object obj : tasksJSONArray) {
                    JSONObject taskJSONObject = (JSONObject) obj;
                    String taskOwner = (String) taskJSONObject.get(LOGIN_JSON_KEY);
                    String taskName = (String) taskJSONObject.get(NAME_JSON_KEY);
                    Integer taskDataToSendSize = ((Long) taskJSONObject.get(DATA_TO_SEND_SIZE_JSON_KEY)).intValue();
                    Integer taskOperationsAmount = ((Long) taskJSONObject.get(OPERATIONS_AMOUNT_JSON_KEY)).intValue();
                    String taskDescription;
                    if(taskJSONObject.containsKey(DESCRIPTION_JSON_KEY)) 
                        taskDescription = (String) taskJSONObject.get(DESCRIPTION_JSON_KEY);
                    else
                        taskDescription = null;
                    WorkflowTaskCategory category = getCategoryFromJSON((JSONObject) taskJSONObject.get(CATEGORY_JSON_KEY));
                    if(taskDescription == null)
                            tasksArrayList.add(new WorkflowTask(taskOwner, taskName, category, taskDataToSendSize, taskOperationsAmount));
                        else
                            tasksArrayList.add(new WorkflowTask(taskOwner, taskName, category, taskDataToSendSize, taskOperationsAmount, taskDescription));                                        
                } 
            } else {
                if(communicateLabel == null) {
                    Alert alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                    alert.showAndWait();
                } else
                    communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }   
        } catch (IOException | ParseException ex) {
            if(communicateLabel == null) {
                Alert alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                alert.showAndWait();
            } else
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
        return tasksArrayList;
    }
    
    public static ArrayList<WorkflowResource> getResourcesFromServer() {
        return getResourcesFromServer(null);
    }
    
    public static ArrayList<WorkflowResource> getResourcesFromServer(Label communicateLabel) {
        ArrayList<WorkflowResource> resourcesArrayList = new ArrayList<>();
        JSONObject loginJSONObject = createLoginJSONObject();
        try {
            String resultString = postRequestToServer("/resource/get", loginJSONObject);
            int operationResult = (int)resultString.charAt(0); 
            if(operationResult == OPERATION_SUCCESSFULL) {
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray resourcesJSONArray = (JSONArray)parser.parse(resultString);                        
                for(Object obj : resourcesJSONArray) {
                    JSONObject resourceJSONObject = (JSONObject) obj;
                    String resourceOwner = (String) resourceJSONObject.get(Commons.LOGIN_JSON_KEY);
                    String resourceName = (String) resourceJSONObject.get(Commons.NAME_JSON_KEY);
                    Long resourceOperationTime = (Long) resourceJSONObject.get(Commons.OPERATION_TIME_JSON_KEY);
                    Integer resourceOperationCost = ((Long) resourceJSONObject.get(Commons.OPERATION_COST_JSON_KEY)).intValue();
                    Long resourceDataSendTime = (Long) resourceJSONObject.get(Commons.DATA_SEND_TIME_JSON_KEY);
                    Integer resourceTasksCapacity = ((Long) resourceJSONObject.get(Commons.TASKS_AT_ONCE_AMOUNT_JSON_KEY)).intValue();
                    String resourceDescription;
                    if(resourceJSONObject.containsKey(Commons.DESCRIPTION_JSON_KEY)) 
                        resourceDescription = (String) resourceJSONObject.get(Commons.DESCRIPTION_JSON_KEY);
                    else
                        resourceDescription = null;
                    LinkedHashMap<String, TaskExecutionTechnology> categoriesTechnologies = new LinkedHashMap<>();
                    if(resourceJSONObject.containsKey(Commons.CATEGORIES_JSON_KEY)) {                                
                        JSONArray categoriesJSONArray = (JSONArray) resourceJSONObject.get(Commons.CATEGORIES_JSON_KEY);
                        Collections.sort(categoriesJSONArray, Commons.orderJSONKeyComparator);
                        for(Object obj1 : categoriesJSONArray) {
                            JSONObject categoryJSONObject = (JSONObject) obj1;
                            String categoryNameAndOwner = categoryJSONObject.get(Commons.CATEGORY_NAME_JSON_KEY) + "(" + categoryJSONObject.get(Commons.CATEGORY_OWNER_JSON_KEY) + ")";
                            JSONObject categoryTechnologyJSONObject = (JSONObject) categoryJSONObject.get(Commons.CATEGORY_TECHNOLOGY_JSON_KEY);
                            String technologyUrl = (String) categoryTechnologyJSONObject.get(Commons.TECHNOLOGY_URL_JSON_KEY);
                            JSONArray urlParametersJSONArray = (JSONArray) categoryTechnologyJSONObject.get(Commons.TECHNOLOGY_URL_PARAMETERS_JSON_KEY);
                            LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder = new LinkedHashMap<>();
                            for(Object obj2 : urlParametersJSONArray) {
                                JSONObject urlParameterJSONObject = (JSONObject) obj2;
                                String urlParameterName = (String) urlParameterJSONObject.get(Commons.TECHNOLOGY_URL_PARAMETER_NAME);
                                String urlParameterType = (String) urlParameterJSONObject.get(Commons.TECHNOLOGY_URL_PARAMETER_TYPE);
                                String urlParameterValue = (String) urlParameterJSONObject.get(Commons.TECHNOLOGY_URL_PARAMETER_VALUE);
                                urlParametersNamesAndOrder.put(urlParameterName, new ResourceUrlParameter(urlParameterType,urlParameterValue));
                            }
                            String technologyType = (String) categoryTechnologyJSONObject.get(Commons.TECHNOLOGY_TYPE_JSON_KEY);
                            TaskExecutionTechnology technology;
                            if(technologyType.equals(Commons.REST_TECHNOLOGY_TYPE)) {
                                Integer expectedResponseCode = ((Long) categoryTechnologyJSONObject.get(Commons.REST_EXPECTED_RESPONSE_CODE)).intValue();
                                technology = new RestTaskExecutionTechnology(technologyUrl, urlParametersNamesAndOrder, expectedResponseCode);
                            } else {
                                String methodName = (String) categoryTechnologyJSONObject.get(Commons.SOAP_METHOD_NAME);
                                String namespacePrefix = (String) categoryTechnologyJSONObject.get(Commons.SOAP_NAMESPACE_PREFIX);
                                String namespaceUri = (String) categoryTechnologyJSONObject.get(Commons.SOAP_NAMESPACE_URI);
                                technology = new SoapTaskExecutionTechnology(technologyUrl, urlParametersNamesAndOrder, methodName, namespacePrefix, namespaceUri);
                            }
                            categoriesTechnologies.put(categoryNameAndOwner, technology);
                        }                                
                    }
                    if(resourceDescription == null) 
                        resourcesArrayList.add(new WorkflowResource(resourceOwner,resourceName,resourceOperationTime,resourceOperationCost,
                                resourceDataSendTime,resourceTasksCapacity,categoriesTechnologies));
                    else
                        resourcesArrayList.add(new WorkflowResource(resourceOwner,resourceName,resourceOperationTime,resourceOperationCost,
                               resourceDataSendTime,resourceTasksCapacity,categoriesTechnologies,resourceDescription));
                }
            } else {
                if(communicateLabel == null) {
                    Alert alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                    alert.showAndWait();
                } else
                    communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        } catch (IOException | ParseException ex) {
            if(communicateLabel == null) {
                Alert alert = Commons.createAlert(AlertType.ERROR, "", "Błąd połączenia lub po stronie serwera");
                alert.showAndWait();
            } else
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }  
        return resourcesArrayList;
    }
    
    public static ArrayList<Scenario> getScenariosFromServer(Label communicateLabel) {
        ArrayList<Scenario> scenariosArrayList = new ArrayList<>();
        JSONObject loginJSONObject = createLoginJSONObject();        
        try {
            String resultString = postRequestToServer("/scenario/get", loginJSONObject);
            int operationResult = (int)resultString.charAt(0);
            if(operationResult == OPERATION_SUCCESSFULL) { 
                resultString = resultString.substring(1);
                JSONParser parser = new JSONParser();
                JSONArray scenariosJSONArray = (JSONArray)parser.parse(resultString);
                for(Object obj : scenariosJSONArray) {
                    JSONObject scenarioJSONObject = (JSONObject) obj;
                    String scenarioOwner = (String) scenarioJSONObject.get(LOGIN_JSON_KEY);
                    String scenarioName = (String) scenarioJSONObject.get(NAME_JSON_KEY);
                    JSONObject workflowObjectsJSONObject= (JSONObject) scenarioJSONObject.get(WORKFLOW_OBJECTS_JSON_KEY);
                    String scenarioDescription;
                    if(scenarioJSONObject.containsKey(DESCRIPTION_JSON_KEY)) 
                        scenarioDescription = (String) scenarioJSONObject.get(DESCRIPTION_JSON_KEY);
                    else
                        scenarioDescription = null;
                    if(scenarioDescription == null)
                            scenariosArrayList.add(new Scenario(scenarioOwner, scenarioName, workflowObjectsJSONObject));
                        else
                            scenariosArrayList.add(new Scenario(scenarioOwner, scenarioName, workflowObjectsJSONObject, scenarioDescription));    
                }
            } else {
                communicateLabel.setText("Błąd połączenia lub po stronie serwera");
            }
        } catch (IOException | ParseException ex) {
            communicateLabel.setText("Błąd połączenia lub po stronie serwera");
        }
        return scenariosArrayList;
    }
    
    //gets category's index in list given category's name and order
    public static int getCategoryIndex(ArrayList<WorkflowTaskCategory> categoriesList, String nameAndOwner) {
        for(int i=0; i<categoriesList.size(); i++) {
            if(categoriesList.get(i).getNameAndOwner().equals(nameAndOwner))
                return i;
        }
        return -1;
    }
}
