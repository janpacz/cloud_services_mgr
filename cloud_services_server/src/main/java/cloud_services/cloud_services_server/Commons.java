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

package cloud_services.cloud_services_server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    public static final String WORKFLOW_OBJECTS_JSON_KEY = "workflow_objects";
    
    public static final int OPERATION_SUCCESSFULL = 0;
    public static final int USER_DOES_NOT_EXIST = 1;
    public static final int WRONG_PASSWORD = 2;
    public static final int SERVER_ERROR = -1;
    
    public static JSONObject retrieveJSONObjectFromClient(InputStream is) throws IOException, ParseException {
        String JSONString = "";
        JSONParser parser = new JSONParser();
        DataInputStream dis = new DataInputStream(is);
        try {
            while(true) {
                JSONString+=dis.readChar();
            }
        } catch (EOFException ex) {}
        return (JSONObject)parser.parse(JSONString);
    }
    
    public static int login(Connection con, InputStream is, JSONObject[] loginJSONObject) {
        try {            
            loginJSONObject[0] = retrieveJSONObjectFromClient(is);
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM uzytkownicy WHERE nazwa=?");
            pstmt.setString(1, (String)loginJSONObject[0].get(LOGIN_JSON_KEY));
            ResultSet rs = pstmt.executeQuery();
            if(!rs.next()) {
                return USER_DOES_NOT_EXIST;
            }
            else {
                String passwordFromDB = rs.getString(2);
                if(passwordFromDB.equals((String)loginJSONObject[0].get(PASSWORD_JSON_KEY))) {
                    return OPERATION_SUCCESSFULL;
                }
                else {
                    return WRONG_PASSWORD;
                }
            }
        } catch (IOException | ParseException | SQLException ex) {
            Logger.getLogger(Commons.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return SERVER_ERROR;
    }
    
    public static JSONObject getCategoryFromDB(ResultSet rs) throws SQLException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject categoryJSONObject = new JSONObject();
        categoryJSONObject.put(LOGIN_JSON_KEY, rs.getString("wlasciciel"));
        categoryJSONObject.put(NAME_JSON_KEY, rs.getString("nazwa"));
        if(rs.getString("opis") != null)
            categoryJSONObject.put(DESCRIPTION_JSON_KEY, rs.getString("opis"));
        if(rs.getString("parametry_url") != null) {
            categoryJSONObject.put(URL_PARAMETERS_JSON_KEY,parser.parse(rs.getString("parametry_url")));
        }
        if(rs.getString("parametry_pliki_wejsciowe") != null) {
            categoryJSONObject.put(INPUT_FILES_PARAMETERS_JSON_KEY,parser.parse(rs.getString("parametry_pliki_wejsciowe")));
        }
        return categoryJSONObject;
    }
    
    public static void insertCategoryIntoDB(Connection con, JSONObject categoryJSONObject, String nameJSONKey) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO kategorie(wlasciciel,nazwa,opis,parametry_url,parametry_pliki_wejsciowe) Values(?,?,?,?,?)");
        pstmt.setString(1, (String)categoryJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)categoryJSONObject.get(nameJSONKey));
        if(categoryJSONObject.containsKey(DESCRIPTION_JSON_KEY))
            pstmt.setString(3, (String)categoryJSONObject.get(DESCRIPTION_JSON_KEY));
        else
            pstmt.setString(3, null);
        if(categoryJSONObject.containsKey(URL_PARAMETERS_JSON_KEY))
            pstmt.setString(4, ((JSONArray)categoryJSONObject.get(URL_PARAMETERS_JSON_KEY)).toJSONString());
        else
            pstmt.setString(4, null);
        if(categoryJSONObject.containsKey(INPUT_FILES_PARAMETERS_JSON_KEY))
            pstmt.setString(5, ((JSONArray)categoryJSONObject.get(INPUT_FILES_PARAMETERS_JSON_KEY)).toJSONString());
        else
            pstmt.setString(5, null);
        pstmt.executeUpdate();
        pstmt = con.prepareStatement("INSERT INTO kategorie_uzytkownicy(uzytkownik,wlasciciel_kategorii,nazwa_kategorii,kolejnosc) Values(?,?,?,0)");
        pstmt.setString(1, (String)categoryJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)categoryJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(3, (String)categoryJSONObject.get(nameJSONKey));
        pstmt.executeUpdate();
    }
    
    public static void insertTaskIntoDB(Connection con, JSONObject taskJSONObject, String nameJSONKey) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(                                 
                "INSERT INTO zadania(wlasciciel,nazwa,wlasciciel_kategorii,nazwa_kategorii,"
                        + "rozmiar_danych_do_wyslania,liczba_operacji,opis"
                        + ") Values(?,?,?,?,?,?,?)");
        pstmt.setString(1, (String)taskJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)taskJSONObject.get(nameJSONKey));
        pstmt.setString(3, (String)taskJSONObject.get(CATEGORY_OWNER_JSON_KEY));
        pstmt.setString(4, (String)taskJSONObject.get(CATEGORY_NAME_JSON_KEY));
        pstmt.setInt(5, ((Long)taskJSONObject.get(DATA_TO_SEND_SIZE_JSON_KEY)).intValue());
        pstmt.setInt(6, ((Long)taskJSONObject.get(OPERATIONS_AMOUNT_JSON_KEY)).intValue());
        if(taskJSONObject.containsKey(DESCRIPTION_JSON_KEY))
            pstmt.setString(7, (String)taskJSONObject.get(DESCRIPTION_JSON_KEY));
        else
            pstmt.setString(7, null);
        pstmt.executeUpdate();
        pstmt = con.prepareStatement("INSERT INTO zadania_uzytkownicy(uzytkownik,wlasciciel_zadania,nazwa_zadania,kolejnosc) Values(?,?,?,0)");
        pstmt.setString(1, (String)taskJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)taskJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(3, (String)taskJSONObject.get(nameJSONKey));
        pstmt.executeUpdate();
    }
    
    public static void insertCategoryResourceConnections(Connection con, JSONObject resourceJSONObject, String nameJSONKey) throws SQLException {
        int placeInOrder = 0;
        if(resourceJSONObject.containsKey(CATEGORIES_JSON_KEY)) {
            for(Object obj : (JSONArray)resourceJSONObject.get(CATEGORIES_JSON_KEY)) {
                JSONObject categoryJSONObject = (JSONObject) obj;
                PreparedStatement pstmt = con.prepareStatement(
                        "INSERT INTO kategorie_zasoby(wlasciciel_zasobu,nazwa_zasobu,wlasciciel_kategorii,"                                            
                                + "nazwa_kategorii,technologia_wykonania,kolejnosc"
                                + ") Values(?,?,?,?,?,?)");
                pstmt.setString(1, (String)resourceJSONObject.get(LOGIN_JSON_KEY));
                pstmt.setString(2, (String)resourceJSONObject.get(nameJSONKey));
                pstmt.setString(3, (String)categoryJSONObject.get(CATEGORY_OWNER_JSON_KEY));
                pstmt.setString(4, (String)categoryJSONObject.get(CATEGORY_NAME_JSON_KEY));
                pstmt.setString(5, ((JSONObject)categoryJSONObject.get(CATEGORY_TECHNOLOGY_JSON_KEY)).toJSONString());
                pstmt.setInt(6, placeInOrder);
                pstmt.executeUpdate();
                placeInOrder++;
            }
        }
    }
    
    public static void insertResourceIntoDB(Connection con, JSONObject resourceJSONObject, String nameJSONKey) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO zasoby(wlasciciel,nazwa,czas_operacji,koszt_operacji,"                                            
                        + "czas_wysłania_danych,liczba_jednoczesnych_zadan,opis"
                        + ") Values(?,?,?,?,?,?,?)");
        pstmt.setString(1, (String)resourceJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)resourceJSONObject.get(nameJSONKey));
        pstmt.setLong(3, (Long)resourceJSONObject.get(OPERATION_TIME_JSON_KEY));                            
        pstmt.setInt(4, ((Long)resourceJSONObject.get(OPERATION_COST_JSON_KEY)).intValue());
        pstmt.setLong(5, (Long)resourceJSONObject.get(DATA_SEND_TIME_JSON_KEY));
        pstmt.setInt(6, ((Long)resourceJSONObject.get(TASKS_AT_ONCE_AMOUNT_JSON_KEY)).intValue());
        if(resourceJSONObject.containsKey(DESCRIPTION_JSON_KEY))
            pstmt.setString(7, (String)resourceJSONObject.get(DESCRIPTION_JSON_KEY));
        else
            pstmt.setString(7, null);
        pstmt.executeUpdate();
        pstmt = con.prepareStatement("INSERT INTO zasoby_uzytkownicy(uzytkownik,wlasciciel_zasobu,nazwa_zasobu,kolejnosc) Values(?,?,?,0)");
        pstmt.setString(1, (String)resourceJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)resourceJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(3, (String)resourceJSONObject.get(nameJSONKey));
        pstmt.executeUpdate();
        insertCategoryResourceConnections(con, resourceJSONObject, nameJSONKey);        
    }    
    
    public static void insertScenarioIntoDB(Connection con, JSONObject scenarioJSONObject, String nameJSONKey) throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(                                 
               "INSERT INTO scenariusze(wlasciciel,nazwa,obiekty_scenariusza,opis"
                       + ") Values(?,?,?,?)");
        pstmt.setString(1, (String)scenarioJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)scenarioJSONObject.get(nameJSONKey));
        pstmt.setString(3, ((JSONObject)scenarioJSONObject.get(WORKFLOW_OBJECTS_JSON_KEY)).toJSONString());
        if(scenarioJSONObject.containsKey(DESCRIPTION_JSON_KEY))
            pstmt.setString(4, (String)scenarioJSONObject.get(DESCRIPTION_JSON_KEY));
        else
            pstmt.setString(4, null);
        pstmt.executeUpdate();
        pstmt = con.prepareStatement("INSERT INTO scenariusze_uzytkownicy(uzytkownik,wlasciciel_scenariusza,nazwa_scenariusza,kolejnosc) Values(?,?,?,0)");
        pstmt.setString(1, (String)scenarioJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(2, (String)scenarioJSONObject.get(LOGIN_JSON_KEY));
        pstmt.setString(3, (String)scenarioJSONObject.get(nameJSONKey));
        pstmt.executeUpdate();
    }
}
