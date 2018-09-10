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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GetScenariosHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> scenariosForEachThread;    
    
    public GetScenariosHandler(HikariDataSource dataSource) {
        super(dataSource);
        scenariosForEachThread = new ConcurrentHashMap();
    }
    
    private JSONObject getScenarioFromDB(ResultSet rs) throws SQLException, ParseException {
        JSONParser parser = new JSONParser();
        
        JSONObject scenarioJSONObject = new JSONObject();
        scenarioJSONObject.put(Commons.LOGIN_JSON_KEY, rs.getString("wlasciciel"));
        scenarioJSONObject.put(Commons.NAME_JSON_KEY, rs.getString("nazwa"));
        scenarioJSONObject.put(Commons.WORKFLOW_OBJECTS_JSON_KEY, parser.parse(rs.getString("obiekty_scenariusza")));
        if(rs.getString("opis") != null)
            scenarioJSONObject.put(Commons.DESCRIPTION_JSON_KEY, rs.getString("opis"));
        return scenarioJSONObject;
    }

    @Override
    protected void executeHandler(JSONObject loginJSONObject, Connection con) throws SQLException {
        JSONArray scenariosJSONArray = new JSONArray();
        try {          
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM scenariusze_uzytkownicy WHERE uzytkownik=?");
            pstmt.setString(1, (String)loginJSONObject.get(Commons.LOGIN_JSON_KEY));
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                pstmt = con.prepareStatement("SELECT * FROM scenariusze WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, rs.getString("wlasciciel_scenariusza"));
                pstmt.setString(2, rs.getString("nazwa_scenariusza"));
                ResultSet rs2 = pstmt.executeQuery();
                if(rs2.next()) {
                    JSONObject scenarioJSONObject = getScenarioFromDB(rs2);
                    scenariosJSONArray.add(scenarioJSONObject);
                }
            }
        } catch(ParseException ex) {
            Logger.getLogger(GetScenariosHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        scenariosForEachThread.put(Thread.currentThread().getId(), scenariosJSONArray);
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == Commons.OPERATION_SUCCESSFULL) {
            JSONArray scenariosJSONArray = scenariosForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(scenariosJSONArray.toJSONString());
        }
    }
}
