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

import com.zaxxer.hikari.HikariDataSource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SetUsersCredentialsHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> notExistingUsersForEachThread;    
    
    private static final int SUCH_USER_DOES_NOT_EXIST = 3;
    
    public SetUsersCredentialsHandler(HikariDataSource dataSource) {
        super(dataSource);
        notExistingUsersForEachThread = new ConcurrentHashMap();
    } 
    
    @Override
    protected void executeHandler(JSONObject scenarioDescriptionElementJSONObject, Connection con) throws SQLException {        
        JSONArray notExistingUsersArray = new JSONArray();
        boolean suchUserDoesNotExist = false;
        con.setAutoCommit(true);
        JSONArray usersHavingCredentialsJSONArray = (JSONArray) scenarioDescriptionElementJSONObject.get(Commons.USERS_HAVING_CREDENTIALS_JSON_KEY);
        JSONArray usersReceivingCredentialsJSONArray = (JSONArray) scenarioDescriptionElementJSONObject.get(Commons.USERS_RECEIVING_CREDENTIALS_JSON_KEY);
        JSONArray usersToRemoveJSONArray = (JSONArray) scenarioDescriptionElementJSONObject.get(Commons.USERS_WITHOUT_CREDENTIALS_JSON_KEY);        
        ArrayList<String> allUsersList = new ArrayList<>();
        for(Object obj : usersHavingCredentialsJSONArray) {
            JSONObject userJSONObject = (JSONObject) obj;
            allUsersList.add((String)userJSONObject.get(Commons.USER_JSON_KEY));
        }
        for(Object obj : usersReceivingCredentialsJSONArray) {
            JSONObject userJSONObject = (JSONObject) obj;
            allUsersList.add((String)userJSONObject.get(Commons.USER_JSON_KEY));
        }
        allUsersList.addAll(usersToRemoveJSONArray);
        for(String user : allUsersList) {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM uzytkownicy WHERE nazwa=?");
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            if(!rs.next()) {
                resultsForEachThread.put(Thread.currentThread().getId(), SUCH_USER_DOES_NOT_EXIST);
                suchUserDoesNotExist = true;
                notExistingUsersArray.add(user);
            }
        }
        if(suchUserDoesNotExist) {
            notExistingUsersForEachThread.put(Thread.currentThread().getId(), notExistingUsersArray);
        } else {
            con.setAutoCommit(false);
            PreparedStatement usersHavingCredentialsPstmt = null, usersReceivingCredentialsPstmt = null, usersToRemovePstmt = null;
            switch((String)scenarioDescriptionElementJSONObject.get(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY)) {
                case Commons.SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY:
                    usersHavingCredentialsPstmt = con.prepareStatement("UPDATE zasoby_uzytkownicy SET kolejnosc=? WHERE wlasciciel_zasobu=? AND nazwa_zasobu=? AND uzytkownik=?");
                    usersReceivingCredentialsPstmt = con.prepareStatement("INSERT INTO zasoby_uzytkownicy (wlasciciel_zasobu,nazwa_zasobu,uzytkownik,kolejnosc) Values(?,?,?,?)");
                    usersToRemovePstmt = con.prepareStatement("DELETE FROM zasoby_uzytkownicy WHERE wlasciciel_zasobu=? AND nazwa_zasobu=? AND uzytkownik=?");
                    break;
                case Commons.SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY:
                    usersHavingCredentialsPstmt = con.prepareStatement("UPDATE zadania_uzytkownicy SET kolejnosc=? WHERE wlasciciel_zadania=? AND nazwa_zadania=? AND uzytkownik=?");
                    usersReceivingCredentialsPstmt = con.prepareStatement("INSERT INTO zadania_uzytkownicy (wlasciciel_zadania,nazwa_zadania,uzytkownik,kolejnosc) Values(?,?,?,?)");
                    usersToRemovePstmt = con.prepareStatement("DELETE FROM zadania_uzytkownicy WHERE wlasciciel_zadania=? AND nazwa_zadania=? AND uzytkownik=?");
                    break;
                case Commons.SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY:
                    usersHavingCredentialsPstmt = con.prepareStatement("UPDATE kategorie_uzytkownicy SET kolejnosc=? WHERE wlasciciel_kategorii=? AND nazwa_kategorii=? AND uzytkownik=?");
                    usersReceivingCredentialsPstmt = con.prepareStatement("INSERT INTO kategorie_uzytkownicy (wlasciciel_kategorii,nazwa_kategorii,uzytkownik,kolejnosc) Values(?,?,?,?)");
                    usersToRemovePstmt = con.prepareStatement("DELETE FROM kategorie_uzytkownicy WHERE wlasciciel_kategorii=? AND nazwa_kategorii=? AND uzytkownik=?");
                    break;
                case Commons.SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY:
                    usersHavingCredentialsPstmt = con.prepareStatement("UPDATE scenariusze_uzytkownicy SET kolejnosc=? WHERE wlasciciel_scenariusza=? AND nazwa_scenariusza=? AND uzytkownik=?");
                    usersReceivingCredentialsPstmt = con.prepareStatement("INSERT INTO scenariusze_uzytkownicy (wlasciciel_scenariusza,nazwa_scenariusza,uzytkownik,kolejnosc) Values(?,?,?,?)");
                    usersToRemovePstmt = con.prepareStatement("DELETE FROM scenariusze_uzytkownicy WHERE wlasciciel_scenariusza=? AND nazwa_scenariusza=? AND uzytkownik=?");
                    break;
            }
            usersHavingCredentialsPstmt.setString(2, (String)scenarioDescriptionElementJSONObject.get(Commons.LOGIN_JSON_KEY));
            usersHavingCredentialsPstmt.setString(3, (String)scenarioDescriptionElementJSONObject.get(Commons.NAME_JSON_KEY));
            usersReceivingCredentialsPstmt.setString(1, (String)scenarioDescriptionElementJSONObject.get(Commons.LOGIN_JSON_KEY));
            usersReceivingCredentialsPstmt.setString(2, (String)scenarioDescriptionElementJSONObject.get(Commons.NAME_JSON_KEY));
            usersToRemovePstmt.setString(1, (String)scenarioDescriptionElementJSONObject.get(Commons.LOGIN_JSON_KEY));
            usersToRemovePstmt.setString(2, (String)scenarioDescriptionElementJSONObject.get(Commons.NAME_JSON_KEY));
            for(Object obj : usersHavingCredentialsJSONArray) {
                JSONObject userJSONObject = (JSONObject) obj;
                usersHavingCredentialsPstmt.setInt(1, ((Long)userJSONObject.get(Commons.ORDER_JSON_KEY)).intValue());
                usersHavingCredentialsPstmt.setString(4, (String)userJSONObject.get(Commons.USER_JSON_KEY));
                usersHavingCredentialsPstmt.executeUpdate();
            }
            for(Object obj : usersReceivingCredentialsJSONArray) {
                JSONObject userJSONObject = (JSONObject) obj;
                usersReceivingCredentialsPstmt.setString(3, (String)userJSONObject.get(Commons.USER_JSON_KEY));
                usersReceivingCredentialsPstmt.setInt(4, ((Long)userJSONObject.get(Commons.ORDER_JSON_KEY)).intValue());                
                usersReceivingCredentialsPstmt.executeUpdate();
            }
            for(Object obj : usersToRemoveJSONArray) {
                String user = (String) obj;
                usersToRemovePstmt.setString(3, user);          
                usersToRemovePstmt.executeUpdate();
            }
            con.commit();
        }
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == SUCH_USER_DOES_NOT_EXIST) {
            JSONArray notExistingUsersArray = notExistingUsersForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(notExistingUsersArray.toJSONString()); 
        }
    }  
}
