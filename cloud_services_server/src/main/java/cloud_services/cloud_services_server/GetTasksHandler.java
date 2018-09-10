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
import org.json.simple.parser.ParseException;

public class GetTasksHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> tasksForEachThread;        
    
    public GetTasksHandler(HikariDataSource dataSource) {
        super(dataSource);
        tasksForEachThread = new ConcurrentHashMap(); 
    } 
    
    private JSONObject getTaskFromDB(ResultSet rs, Connection con) throws SQLException, ParseException {
        JSONObject taskJSONObject = new JSONObject();
        taskJSONObject.put(Commons.LOGIN_JSON_KEY, rs.getString("wlasciciel"));
        taskJSONObject.put(Commons.NAME_JSON_KEY, rs.getString("nazwa"));       
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM kategorie WHERE wlasciciel=? AND nazwa=?");
        pstmt.setString(1, rs.getString("wlasciciel_kategorii"));
        pstmt.setString(2, rs.getString("nazwa_kategorii"));
        ResultSet rs2 = pstmt.executeQuery();
        if(rs2.next()) {
            JSONObject categoryJSONObject = Commons.getCategoryFromDB(rs2);
            taskJSONObject.put(Commons.CATEGORY_JSON_KEY, categoryJSONObject);
        } else
            return null;
        taskJSONObject.put(Commons.DATA_TO_SEND_SIZE_JSON_KEY, rs.getInt("rozmiar_danych_do_wyslania"));
        taskJSONObject.put(Commons.OPERATIONS_AMOUNT_JSON_KEY, rs.getInt("liczba_operacji"));
        if(rs.getString("opis") != null)
            taskJSONObject.put(Commons.DESCRIPTION_JSON_KEY, rs.getString("opis"));
        return taskJSONObject;
    }

    @Override
    protected void executeHandler(JSONObject loginJSONObject, Connection con) throws SQLException {
        JSONArray tasksJSONArray = new JSONArray();
        try {
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM zadania_uzytkownicy WHERE uzytkownik=?");
            pstmt.setString(1, (String)loginJSONObject.get(Commons.LOGIN_JSON_KEY));
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {  
                pstmt = con.prepareStatement("SELECT * FROM zadania WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, rs.getString("wlasciciel_zadania"));
                pstmt.setString(2, rs.getString("nazwa_zadania"));
                ResultSet rs2 = pstmt.executeQuery();
                if(rs2.next()) {
                    JSONObject taskJSONObject = getTaskFromDB(rs2, con);
                    if(taskJSONObject != null)
                        tasksJSONArray.add(taskJSONObject);
                }
            }
        } catch(ParseException ex) {
            Logger.getLogger(GetTasksHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        tasksForEachThread.put(Thread.currentThread().getId(), tasksJSONArray);
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == Commons.OPERATION_SUCCESSFULL) {
            JSONArray tasksJSONArray = tasksForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(tasksJSONArray.toJSONString());
        }
    }
}
