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
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

public abstract class CloudServicesRequestHandler implements HttpHandler {
    private HikariDataSource dataSource;
    protected ConcurrentHashMap<Long, Integer> resultsForEachThread;
    
    public CloudServicesRequestHandler(HikariDataSource dataSource) {
        this.dataSource=dataSource;
        resultsForEachThread = new ConcurrentHashMap();
    }
    
    @Override
    public void handle(HttpExchange exchange) {
        try {
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equalsIgnoreCase("POST")) {
                InputStream is = exchange.getRequestBody();
                JSONObject [] tab=new JSONObject[1];
                try (Connection con = dataSource.getConnection()) {
                    int result = Commons.login(con, is, tab);                
                    JSONObject incomingJSONObject=tab[0];
                    resultsForEachThread.put(Thread.currentThread().getId(), result);
                    if(result==Commons.OPERATION_SUCCESSFULL) {
                        try {
                            executeHandler(incomingJSONObject, con);
                        } catch (SQLException ex) {
                            Logger.getLogger(CloudServicesRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                            resultsForEachThread.put(Thread.currentThread().getId(), Commons.SERVER_ERROR);
                            try {
                                if(!con.getAutoCommit())
                                    con.rollback();                    
                            } catch(SQLException excep) {
                                Logger.getLogger(CloudServicesRequestHandler.class.getName()).log(Level.SEVERE, null, excep);
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(CloudServicesRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
                    resultsForEachThread.put(Thread.currentThread().getId(), Commons.SERVER_ERROR);
                }
                exchange.sendResponseHeaders(200, 0);
                try (DataOutputStream dos = new DataOutputStream(exchange.getResponseBody())) {
                    dos.writeChar(resultsForEachThread.get(Thread.currentThread().getId()));
                    sendResponse(dos);

                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesRequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        resultsForEachThread.remove(Thread.currentThread().getId());
    }
    
    protected abstract void executeHandler(JSONObject incomingJSONObject, Connection con) throws SQLException;
    
    protected void sendResponse(DataOutputStream dos) throws IOException { }
}
