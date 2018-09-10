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
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class GetUsersWithoutCredentialsHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> usersForEachThread;
    
    public GetUsersWithoutCredentialsHandler(HikariDataSource dataSource) {
        super(dataSource);
        usersForEachThread = new ConcurrentHashMap();
    }
    
    @Override
    protected void executeHandler(JSONObject scenarioDescriptionElementJSONObject, Connection con) throws SQLException {
        JSONArray usersWithoutCredentialsJSONArray = new JSONArray();
        con.setAutoCommit(true);
        PreparedStatement pstmt = null;
        switch((String)scenarioDescriptionElementJSONObject.get(Commons.SCENARIO_DESCRIPTION_ELEMENT_TYPE_JSON_KEY)) {
            case Commons.SCENARIO_DESCRIPTION_ELEMENT_RESOURCE_JSON_KEY:
                String query = "SELECT * FROM uzytkownicy WHERE nazwa LIKE '" + scenarioDescriptionElementJSONObject.get(Commons.USERS_SEARCH_STRING)
                        + "%' AND NOT EXISTS (SELECT 1 FROM zasoby_uzytkownicy WHERE uzytkownik=uzytkownicy.nazwa "
                        + "AND wlasciciel_zasobu=? AND nazwa_zasobu=?)";
                pstmt = con.prepareStatement(query);
                break;
            case Commons.SCENARIO_DESCRIPTION_ELEMENT_TASK_JSON_KEY:
                query = "SELECT * FROM uzytkownicy WHERE nazwa LIKE '" + scenarioDescriptionElementJSONObject.get(Commons.USERS_SEARCH_STRING)
                        + "%' AND NOT EXISTS (SELECT 1 FROM zadania_uzytkownicy WHERE uzytkownik=uzytkownicy.nazwa "
                        + "AND wlasciciel_zadania=? AND nazwa_zadania=?)";
                pstmt = con.prepareStatement(query);
                break;
            case Commons.SCENARIO_DESCRIPTION_ELEMENT_CATEGORY_JSON_KEY:
                query = "SELECT * FROM uzytkownicy WHERE nazwa LIKE '" + scenarioDescriptionElementJSONObject.get(Commons.USERS_SEARCH_STRING)
                        + "%' AND NOT EXISTS (SELECT 1 FROM kategorie_uzytkownicy WHERE uzytkownik=uzytkownicy.nazwa "
                        + "AND wlasciciel_kategorii=? AND nazwa_kategorii=?)";
                pstmt = con.prepareStatement(query);
                break;
            case Commons.SCENARIO_DESCRIPTION_ELEMENT_SCENARIO_JSON_KEY:
                query = "SELECT * FROM uzytkownicy WHERE nazwa LIKE '" + scenarioDescriptionElementJSONObject.get(Commons.USERS_SEARCH_STRING)
                        + "%' AND NOT EXISTS (SELECT 1 FROM scenariusze_uzytkownicy WHERE uzytkownik=uzytkownicy.nazwa "
                        + "AND wlasciciel_scenariusza=? AND nazwa_scenariusza=?)";
                pstmt = con.prepareStatement(query);
                break;
        }
        pstmt.setString(1, (String)scenarioDescriptionElementJSONObject.get(Commons.LOGIN_JSON_KEY));
        pstmt.setString(2, (String)scenarioDescriptionElementJSONObject.get(Commons.NAME_JSON_KEY));
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()) {
            usersWithoutCredentialsJSONArray.add(rs.getString("nazwa"));
        }
        usersForEachThread.put(Thread.currentThread().getId(), usersWithoutCredentialsJSONArray);
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == Commons.OPERATION_SUCCESSFULL) {
            JSONArray usersJSONArray = usersForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(usersJSONArray.toJSONString());
        }
    }
}
