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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zaxxer.hikari.HikariDataSource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class RegistrationHandler implements HttpHandler{
    private HikariDataSource dataSource;
    
    private static final int SUCH_USER_ALREADY_EXISTS = 1;
    
    public RegistrationHandler(HikariDataSource dataSource) {
        this.dataSource=dataSource;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equalsIgnoreCase("POST")) {  
            int response;
            try(Connection con = dataSource.getConnection()) {
                JSONObject registerJSONObject = Commons.retrieveJSONObjectFromClient(exchange.getRequestBody());
                con.setAutoCommit(true);
                PreparedStatement pstmt = con.prepareStatement("SELECT * FROM uzytkownicy WHERE nazwa=?");
                pstmt.setString(1, (String)registerJSONObject.get(Commons.LOGIN_JSON_KEY));
                ResultSet rs = pstmt.executeQuery();
                if(rs.next()) {
                    response = SUCH_USER_ALREADY_EXISTS;
                }
                else {
                    con.setAutoCommit(true);
                    pstmt = con.prepareStatement("INSERT INTO uzytkownicy(nazwa,haslo) Values(?,?)");
                    pstmt.setString(1, (String)registerJSONObject.get(Commons.LOGIN_JSON_KEY));
                    pstmt.setString(2, (String)registerJSONObject.get(Commons.PASSWORD_JSON_KEY));
                    pstmt.executeUpdate();
                    response = Commons.OPERATION_SUCCESSFULL;
                }
            } catch (SQLException | ParseException ex) {
                Logger.getLogger(RegistrationHandler.class.getName()).log(Level.SEVERE, null, ex);
                response = Commons.SERVER_ERROR;
            }
            exchange.sendResponseHeaders(200, 0);
            try (DataOutputStream dos = new DataOutputStream(exchange.getResponseBody())) {
                dos.writeChar(response);
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
    
}
