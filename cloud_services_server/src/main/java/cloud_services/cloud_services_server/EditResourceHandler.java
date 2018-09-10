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

public class EditResourceHandler extends CloudServicesRequestHandler {
    private ConcurrentHashMap<Long, JSONArray> notExistingCategoriesForEachThread;
            
    private static final int SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST = 3;
    private static final int SUCH_RESOURCE_ALREADY_EXISTS = 4;
    
    public EditResourceHandler(HikariDataSource dataSource) {
        super(dataSource);
        notExistingCategoriesForEachThread = new ConcurrentHashMap();
    }

    @Override
    protected void executeHandler(JSONObject resourceJSONObject, Connection con) throws SQLException {
       JSONArray notExistingCategoriesArray = new JSONArray();
       boolean suchCategoryOrCredentialsDoNotExist = false;
        if(resourceJSONObject.containsKey(Commons.CATEGORIES_JSON_KEY)) {
            for(Object obj : (JSONArray)resourceJSONObject.get(Commons.CATEGORIES_JSON_KEY)) {
                JSONObject categoryJSONObject = (JSONObject) obj;
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM kategorie_uzytkownicy WHERE uzytkownik=? AND wlasciciel_kategorii=? AND nazwa_kategorii=?");
                pstmt.setString(1, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(2, (String)categoryJSONObject.get(Commons.CATEGORY_OWNER_JSON_KEY));
                pstmt.setString(3, (String)categoryJSONObject.get(Commons.CATEGORY_NAME_JSON_KEY));
                ResultSet rs = pstmt.executeQuery();
                if(!rs.next()) {
                    resultsForEachThread.put(Thread.currentThread().getId(), SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST);
                    suchCategoryOrCredentialsDoNotExist = true;
                    notExistingCategoriesArray.add(categoryJSONObject);
                }
            }
        }
        if(suchCategoryOrCredentialsDoNotExist) {
            notExistingCategoriesForEachThread.put(Thread.currentThread().getId(), notExistingCategoriesArray);
        } else {
            if(resourceJSONObject.containsKey(Commons.OLD_NAME_JSON_KEY)) {
                con.setAutoCommit(true);
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM zasoby WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(2, (String)resourceJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()) {
                    resultsForEachThread.put(Thread.currentThread().getId(), SUCH_RESOURCE_ALREADY_EXISTS);
                } else {
                    con.setAutoCommit(false);
                    Commons.insertResourceIntoDB(con,resourceJSONObject,Commons.NEW_NAME_JSON_KEY);
                    pstmt = con.prepareStatement("UPDATE zasoby_uzytkownicy SET nazwa_zasobu=? WHERE wlasciciel_zasobu=? AND nazwa_zasobu=? AND uzytkownik!=?");
                    pstmt.setString(1, (String)resourceJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                    pstmt.setString(2, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.setString(3, (String)resourceJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                    pstmt.setString(4, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.executeUpdate();
                    pstmt = con.prepareStatement("DELETE FROM zasoby WHERE wlasciciel=? AND nazwa=?");
                    pstmt.setString(1, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.setString(2, (String)resourceJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                    pstmt.executeUpdate();
                    con.commit();
                }
            } else {
                con.setAutoCommit(false);
                PreparedStatement pstmt = con.prepareStatement(
                        "UPDATE zasoby SET czas_operacji=?,koszt_operacji=?,czas_wysłania_danych=?,"                                     
                                + "liczba_jednoczesnych_zadan=?,opis=? WHERE wlasciciel=? AND nazwa=?");
                pstmt.setLong(1, (Long)resourceJSONObject.get(Commons.OPERATION_TIME_JSON_KEY));                            
                pstmt.setInt(2, ((Long)resourceJSONObject.get(Commons.OPERATION_COST_JSON_KEY)).intValue());
                pstmt.setLong(3, (Long)resourceJSONObject.get(Commons.DATA_SEND_TIME_JSON_KEY));
                pstmt.setInt(4, ((Long)resourceJSONObject.get(Commons.TASKS_AT_ONCE_AMOUNT_JSON_KEY)).intValue());
                if(resourceJSONObject.containsKey(Commons.DESCRIPTION_JSON_KEY))
                    pstmt.setString(5, (String)resourceJSONObject.get(Commons.DESCRIPTION_JSON_KEY));
                else
                    pstmt.setString(5, null);                            
                pstmt.setString(6, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(7, (String)resourceJSONObject.get(Commons.NAME_JSON_KEY));
                pstmt.executeUpdate();
                pstmt = con.prepareStatement("DELETE FROM kategorie_zasoby WHERE wlasciciel_zasobu=? AND nazwa_zasobu=?");
                pstmt.setString(1, (String)resourceJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(2, (String)resourceJSONObject.get(Commons.NAME_JSON_KEY));
                pstmt.executeUpdate();
                Commons.insertCategoryResourceConnections(con, resourceJSONObject, Commons.NAME_JSON_KEY);
                con.commit();
            }
        }
    }
    
    @Override
    protected void sendResponse(DataOutputStream dos) throws IOException {
        if(resultsForEachThread.get(Thread.currentThread().getId()) == SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST) {
            JSONArray notExistingCategoriesArray = notExistingCategoriesForEachThread.remove(Thread.currentThread().getId());
            dos.writeChars(notExistingCategoriesArray.toJSONString()); 
        }  
    }
}
