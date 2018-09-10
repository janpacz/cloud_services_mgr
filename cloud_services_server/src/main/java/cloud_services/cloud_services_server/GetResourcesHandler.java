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

public class GetResourcesHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> resourcesForEachThread;    
    
    public GetResourcesHandler(HikariDataSource dataSource) {
        super(dataSource);
        resourcesForEachThread = new ConcurrentHashMap();
    } 
    
    private JSONObject getResourceFromDB(ResultSet rs, Connection con) throws SQLException {        
        JSONObject resourceJSONObject = new JSONObject();
        JSONParser parser = new JSONParser();
        try {
            resourceJSONObject.put(Commons.LOGIN_JSON_KEY, rs.getString("wlasciciel"));
            resourceJSONObject.put(Commons.NAME_JSON_KEY, rs.getString("nazwa"));
            resourceJSONObject.put(Commons.OPERATION_TIME_JSON_KEY, rs.getLong("czas_operacji"));
            resourceJSONObject.put(Commons.OPERATION_COST_JSON_KEY, rs.getInt("koszt_operacji"));
            resourceJSONObject.put(Commons.DATA_SEND_TIME_JSON_KEY, rs.getLong("czas_wysłania_danych"));
            resourceJSONObject.put(Commons.TASKS_AT_ONCE_AMOUNT_JSON_KEY, rs.getInt("liczba_jednoczesnych_zadan"));
            if(rs.getString("opis") != null)
                resourceJSONObject.put(Commons.DESCRIPTION_JSON_KEY, rs.getString("opis"));
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM kategorie_zasoby WHERE wlasciciel_zasobu=? AND nazwa_zasobu=?");
            pstmt.setString(1, rs.getString("wlasciciel"));
            pstmt.setString(2, rs.getString("nazwa"));
            ResultSet rs2 = pstmt.executeQuery();
            JSONArray categoriesJSONArray = new JSONArray();
            while(rs2.next()) {
                if(!resourceJSONObject.containsKey(Commons.CATEGORIES_JSON_KEY))
                    resourceJSONObject.put(Commons.CATEGORIES_JSON_KEY, categoriesJSONArray);
                JSONObject categoryJSONObject = new JSONObject();
                categoryJSONObject.put(Commons.CATEGORY_OWNER_JSON_KEY, rs2.getString("wlasciciel_kategorii"));
                categoryJSONObject.put(Commons.CATEGORY_NAME_JSON_KEY, rs2.getString("nazwa_kategorii"));
                categoryJSONObject.put(Commons.CATEGORY_TECHNOLOGY_JSON_KEY, parser.parse(rs2.getString("technologia_wykonania")));
                categoryJSONObject.put(Commons.ORDER_JSON_KEY, rs2.getInt("kolejnosc"));
                categoriesJSONArray.add(categoryJSONObject);
            }
        } catch(ParseException ex) {
            Logger.getLogger(GetResourcesHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return resourceJSONObject;
    }

    @Override
    protected void executeHandler(JSONObject loginJSONObject, Connection con) throws SQLException {
        JSONArray resourcesJSONArray = new JSONArray();
        con.setAutoCommit(true);
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM zasoby_uzytkownicy WHERE uzytkownik=?");
        pstmt.setString(1, (String)loginJSONObject.get(Commons.LOGIN_JSON_KEY));
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()) {
            pstmt = con.prepareStatement("SELECT * FROM zasoby WHERE wlasciciel=? AND nazwa=?");
            pstmt.setString(1, rs.getString("wlasciciel_zasobu"));
            pstmt.setString(2, rs.getString("nazwa_zasobu"));
            ResultSet rs2 = pstmt.executeQuery();
            if(rs2.next()) {
                JSONObject resourceJSONObject = getResourceFromDB(rs2, con);                                          
                resourcesJSONArray.add(resourceJSONObject);
            }
        }
        resourcesForEachThread.put(Thread.currentThread().getId(), resourcesJSONArray);
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == Commons.OPERATION_SUCCESSFULL) {
            JSONArray resourcesJSONArray = resourcesForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(resourcesJSONArray.toJSONString());
        }
    }
}
