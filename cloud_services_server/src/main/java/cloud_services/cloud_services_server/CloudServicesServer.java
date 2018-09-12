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

import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

//server main class - creates connection pool, DB tables if they not exist and sets up server
public class CloudServicesServer {

    private static final String DATABASE_URL_PROPERTY = "database_url";
    private static final String DATABASE_USER_PROPERTY = "database_user";
    private static final String DATABASE_PASSWORD_PROPERTY = "database_password";
    private static final String SERVER_PORT_PROPERTY = "server_port";
    
    public static void main(String[] args) {
        try {
            Properties prop = new Properties();
            try(FileInputStream fis = new FileInputStream("config.properties")) {
                prop.load(fis);
            }
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(prop.getProperty(DATABASE_URL_PROPERTY));
            dataSource.setUsername(prop.getProperty(DATABASE_USER_PROPERTY));
            dataSource.setPassword(prop.getProperty(DATABASE_PASSWORD_PROPERTY));
            dataSource.addDataSourceProperty("transactionIsolation", Connection.TRANSACTION_READ_COMMITTED);
            dataSource.addDataSourceProperty("maximumPoolSize", 50);
            dataSource.addDataSourceProperty("isolateInternalQueries", true);
            try(Connection con = dataSource.getConnection();
                    Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS uzytkownicy("
                        + "nazwa TEXT PRIMARY KEY NOT NULL,"
                        + "haslo TEXT NOT NULL"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS kategorie("
                        + "wlasciciel TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "nazwa TEXT NOT NULL,"
                        + "opis TEXT,"
                        + "parametry_url TEXT,"
                        + "parametry_pliki_wejsciowe TEXT,"
                        + "PRIMARY KEY (wlasciciel,nazwa)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS kategorie_uzytkownicy("
                        + "uzytkownik TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "wlasciciel_kategorii TEXT NOT NULL,"
                        + "nazwa_kategorii TEXT NOT NULL,"
                        + "kolejnosc INTEGER NOT NULL,"
                        + "FOREIGN KEY (wlasciciel_kategorii,nazwa_kategorii) REFERENCES kategorie (wlasciciel,nazwa) ON DELETE CASCADE,"                   
                        + "PRIMARY KEY (uzytkownik,wlasciciel_kategorii,nazwa_kategorii)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS zadania("
                        + "wlasciciel TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "nazwa TEXT NOT NULL,"
                        + "wlasciciel_kategorii TEXT NOT NULL,"
                        + "nazwa_kategorii TEXT NOT NULL,"
                        + "rozmiar_danych_do_wyslania INTEGER NOT NULL,"
                        + "liczba_operacji INTEGER NOT NULL,"
                        + "opis TEXT,"
                        + "FOREIGN KEY (wlasciciel,wlasciciel_kategorii,nazwa_kategorii) REFERENCES kategorie_uzytkownicy (uzytkownik,wlasciciel_kategorii,nazwa_kategorii) ON DELETE CASCADE,"
                        + "PRIMARY KEY (wlasciciel,nazwa)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS zadania_uzytkownicy("
                        + "uzytkownik TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "wlasciciel_zadania TEXT NOT NULL,"
                        + "nazwa_zadania TEXT NOT NULL,"
                        + "kolejnosc INTEGER NOT NULL,"
                        + "FOREIGN KEY (wlasciciel_zadania,nazwa_zadania) REFERENCES zadania (wlasciciel,nazwa) ON DELETE CASCADE,"                   
                        + "PRIMARY KEY (uzytkownik,wlasciciel_zadania,nazwa_zadania)"
                        + ")");            
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS zasoby("
                        + "wlasciciel TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "nazwa TEXT NOT NULL,"
                        + "czas_operacji BIGINT NOT NULL,"
                        + "koszt_operacji INTEGER NOT NULL,"
                        + "czas_wysłania_danych BIGINT NOT NULL,"
                        + "liczba_jednoczesnych_zadan INTEGER NOT NULL,"
                        + "opis TEXT,"
                        + "PRIMARY KEY (wlasciciel,nazwa)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS kategorie_zasoby("
                        + "wlasciciel_zasobu TEXT NOT NULL,"
                        + "nazwa_zasobu TEXT NOT NULL,"
                        + "wlasciciel_kategorii TEXT NOT NULL,"
                        + "nazwa_kategorii TEXT NOT NULL,"
                        + "technologia_wykonania TEXT NOT NULL,"
                        + "kolejnosc INTEGER NOT NULL,"
                        + "FOREIGN KEY (wlasciciel_zasobu,nazwa_zasobu) REFERENCES zasoby (wlasciciel,nazwa) ON DELETE CASCADE,"
                        + "FOREIGN KEY (wlasciciel_zasobu,wlasciciel_kategorii,nazwa_kategorii) REFERENCES kategorie_uzytkownicy (uzytkownik,wlasciciel_kategorii,nazwa_kategorii) ON DELETE CASCADE,"                   
                        + "PRIMARY KEY (wlasciciel_zasobu,nazwa_zasobu,wlasciciel_kategorii,nazwa_kategorii)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS zasoby_uzytkownicy("
                        + "uzytkownik TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "wlasciciel_zasobu TEXT NOT NULL,"
                        + "nazwa_zasobu TEXT NOT NULL,"
                        + "kolejnosc INTEGER NOT NULL,"
                        + "FOREIGN KEY (wlasciciel_zasobu,nazwa_zasobu) REFERENCES zasoby (wlasciciel,nazwa) ON DELETE CASCADE,"                   
                        + "PRIMARY KEY (uzytkownik,wlasciciel_zasobu,nazwa_zasobu)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS scenariusze("
                        + "wlasciciel TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "nazwa TEXT NOT NULL,"
                        + "opis TEXT,"
                        + "obiekty_scenariusza TEXT NOT NULL,"
                        + "PRIMARY KEY (wlasciciel,nazwa)"
                        + ")");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS scenariusze_uzytkownicy("
                        + "uzytkownik TEXT NOT NULL REFERENCES uzytkownicy(nazwa),"
                        + "wlasciciel_scenariusza TEXT NOT NULL,"
                        + "nazwa_scenariusza TEXT NOT NULL,"
                        + "kolejnosc INTEGER NOT NULL,"
                        + "FOREIGN KEY (wlasciciel_scenariusza,nazwa_scenariusza) REFERENCES scenariusze (wlasciciel,nazwa) ON DELETE CASCADE,"                   
                        + "PRIMARY KEY (uzytkownik,wlasciciel_scenariusza,nazwa_scenariusza)"
                        + ")");
                InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1",Integer.parseInt(prop.getProperty(SERVER_PORT_PROPERTY)));
                HttpServer server = HttpServer.create(serverAddress, 0);
                server.createContext("/health",new HealthHandler());
                server.createContext("/register",new RegistrationHandler(dataSource));
                server.createContext("/login",new LoginHandler(dataSource));
                server.createContext("/category/add",new CreateCategoryHandler(dataSource));
                server.createContext("/category/edit",new EditCategoryHandler(dataSource));
                server.createContext("/category/delete",new DeleteCategoryHandler(dataSource));
                server.createContext("/category/get",new GetCategoriesHandler(dataSource));
                server.createContext("/task/add",new CreateTaskHandler(dataSource));
                server.createContext("/task/edit",new EditTaskHandler(dataSource));
                server.createContext("/task/delete",new DeleteTaskHandler(dataSource));
                server.createContext("/task/get",new GetTasksHandler(dataSource));            
                server.createContext("/resource/add",new CreateResourceHandler(dataSource));
                server.createContext("/resource/edit",new EditResourceHandler(dataSource));
                server.createContext("/resource/delete",new DeleteResourceHandler(dataSource));
                server.createContext("/resource/get",new GetResourcesHandler(dataSource));
                server.createContext("/scenario/add",new CreateScenarioHandler(dataSource));
                server.createContext("/scenario/edit",new EditScenarioHandler(dataSource));
                server.createContext("/scenario/delete",new DeleteScenarioHandler(dataSource));
                server.createContext("/scenario/get",new GetScenariosHandler(dataSource));
                server.createContext("/users/get/with_credentials",new GetUsersWithCredentialsHandler(dataSource));
                server.createContext("/users/get/without_credentials",new GetUsersWithoutCredentialsHandler(dataSource));
                server.createContext("/users/set",new SetUsersCredentialsHandler(dataSource));
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                System.out.println("Server is listening on port " + prop.getProperty(SERVER_PORT_PROPERTY));
            } catch (SQLException ex) {
                Logger.getLogger(CloudServicesServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
