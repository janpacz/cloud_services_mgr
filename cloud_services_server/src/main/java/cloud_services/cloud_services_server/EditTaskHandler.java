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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;

public class EditTaskHandler extends CloudServicesRequestHandler {
    private static final int SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST = 3;
    private static final int SUCH_TASK_ALREADY_EXISTS = 4;
    
    public EditTaskHandler(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void executeHandler(JSONObject taskJSONObject, Connection con) throws SQLException {
        con.setAutoCommit(true);
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM kategorie_uzytkownicy WHERE uzytkownik=? AND wlasciciel_kategorii=? AND nazwa_kategorii=?");
        pstmt.setString(1, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
        pstmt.setString(2, (String)taskJSONObject.get(Commons.CATEGORY_OWNER_JSON_KEY));
        pstmt.setString(3, (String)taskJSONObject.get(Commons.CATEGORY_NAME_JSON_KEY));
        ResultSet rs = pstmt.executeQuery();
        if(!rs.next()) {
            resultsForEachThread.put(Thread.currentThread().getId(), SUCH_CATEGORY_OR_CREDENTIALS_DO_NOT_EXIST);
        } else {
            if(taskJSONObject.containsKey(Commons.OLD_NAME_JSON_KEY)) {                            
                pstmt = con.prepareStatement("SELECT * FROM zadania WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(2, (String)taskJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                rs = pstmt.executeQuery();
                if(rs.next()) {
                   resultsForEachThread.put(Thread.currentThread().getId(), SUCH_TASK_ALREADY_EXISTS);
                } else {
                    con.setAutoCommit(false);
                    Commons.insertTaskIntoDB(con, taskJSONObject, Commons.NEW_NAME_JSON_KEY);
                    pstmt = con.prepareStatement("UPDATE zadania_uzytkownicy SET nazwa_zadania=? WHERE wlasciciel_zadania=? AND nazwa_zadania=? AND uzytkownik!=?");
                    pstmt.setString(1, (String)taskJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                    pstmt.setString(2, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.setString(3, (String)taskJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                    pstmt.setString(4, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.executeUpdate();                                
                    pstmt = con.prepareStatement("DELETE FROM zadania WHERE wlasciciel=? AND nazwa=?");
                    pstmt.setString(1, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.setString(2, (String)taskJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                    pstmt.executeUpdate();
                    con.commit();
                }
            } else {
                con.setAutoCommit(true);
                pstmt = con.prepareStatement("UPDATE zadania SET wlasciciel_kategorii=?,nazwa_kategorii=?,"
                        + "rozmiar_danych_do_wyslania=?,liczba_operacji=?,opis=? WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, (String)taskJSONObject.get(Commons.CATEGORY_OWNER_JSON_KEY));
                pstmt.setString(2, (String)taskJSONObject.get(Commons.CATEGORY_NAME_JSON_KEY));
                pstmt.setInt(3, ((Long)taskJSONObject.get(Commons.DATA_TO_SEND_SIZE_JSON_KEY)).intValue());
                pstmt.setInt(4, ((Long)taskJSONObject.get(Commons.OPERATIONS_AMOUNT_JSON_KEY)).intValue());
                if(taskJSONObject.containsKey(Commons.DESCRIPTION_JSON_KEY))
                    pstmt.setString(5, (String)taskJSONObject.get(Commons.DESCRIPTION_JSON_KEY));
                else
                    pstmt.setString(5, null);                                  
                pstmt.setString(6, (String)taskJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(7, (String)taskJSONObject.get(Commons.NAME_JSON_KEY));
                pstmt.executeUpdate();  
            }
        }
    }
}
