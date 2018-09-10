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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class EditCategoryHandler extends CloudServicesRequestHandler {
    private static final int SUCH_CATEGORY_ALREADY_EXISTS = 3;
    
    public EditCategoryHandler(HikariDataSource dataSource) {
         super(dataSource);
    }

    @Override
    protected void executeHandler(JSONObject categoryJSONObject, Connection con) throws SQLException {
        if(categoryJSONObject.containsKey(Commons.OLD_NAME_JSON_KEY)) {
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM kategorie WHERE wlasciciel=? AND nazwa=?");
            pstmt.setString(1, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
            pstmt.setString(2, (String)categoryJSONObject.get(Commons.NEW_NAME_JSON_KEY));
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                resultsForEachThread.put(Thread.currentThread().getId(), SUCH_CATEGORY_ALREADY_EXISTS);
            } else {                            
                con.setAutoCommit(false);
                Commons.insertCategoryIntoDB(con, categoryJSONObject, Commons.NEW_NAME_JSON_KEY);
                pstmt = con.prepareStatement("UPDATE kategorie_uzytkownicy SET nazwa_kategorii=? WHERE wlasciciel_kategorii=? AND nazwa_kategorii=? AND uzytkownik!=?");
                pstmt.setString(1, (String)categoryJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                pstmt.setString(2, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(3, (String)categoryJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                pstmt.setString(4, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.executeUpdate(); 
                pstmt = con.prepareStatement("UPDATE zadania SET nazwa_kategorii=? WHERE wlasciciel_kategorii=? AND nazwa_kategorii=?");
                pstmt.setString(1, (String)categoryJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                pstmt.setString(2, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(3, (String)categoryJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                pstmt.executeUpdate();
                pstmt = con.prepareStatement("UPDATE kategorie_zasoby SET nazwa_kategorii=? WHERE wlasciciel_kategorii=? AND nazwa_kategorii=?");
                pstmt.setString(1, (String)categoryJSONObject.get(Commons.NEW_NAME_JSON_KEY));
                pstmt.setString(2, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(3, (String)categoryJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                pstmt.executeUpdate();
                pstmt = con.prepareStatement("DELETE FROM kategorie WHERE wlasciciel=? AND nazwa=?");
                pstmt.setString(1, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
                pstmt.setString(2, (String)categoryJSONObject.get(Commons.OLD_NAME_JSON_KEY));
                pstmt.executeUpdate();
                con.commit();
            }
        } else {
            con.setAutoCommit(true);
            PreparedStatement pstmt = con.prepareStatement("UPDATE kategorie SET opis=?,parametry_url=?,parametry_pliki_wejsciowe=? WHERE wlasciciel=? AND nazwa=?");
            if(categoryJSONObject.containsKey(Commons.DESCRIPTION_JSON_KEY))
                pstmt.setString(1, (String)categoryJSONObject.get(Commons.DESCRIPTION_JSON_KEY));
            else
                pstmt.setString(1, null);
            if(categoryJSONObject.containsKey(Commons.URL_PARAMETERS_JSON_KEY))
                pstmt.setString(2, ((JSONArray)categoryJSONObject.get(Commons.URL_PARAMETERS_JSON_KEY)).toJSONString());
            else
                pstmt.setString(2, null);
            if(categoryJSONObject.containsKey(Commons.INPUT_FILES_PARAMETERS_JSON_KEY))
                pstmt.setString(3, ((JSONArray)categoryJSONObject.get(Commons.INPUT_FILES_PARAMETERS_JSON_KEY)).toJSONString());
            else
                pstmt.setString(3, null);
            pstmt.setString(4, (String)categoryJSONObject.get(Commons.LOGIN_JSON_KEY));
            pstmt.setString(5, (String)categoryJSONObject.get(Commons.NAME_JSON_KEY));
            pstmt.executeUpdate();
        }
    }
}
