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

package cloud_services.cloud_services.scenario_description_objects;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class WorkflowTaskCategory extends ScenarioDescriptionElement {
    private ArrayList<String> urlParameters;
    private LinkedHashMap<String, String> inputFilesInfos;
    
    public static final String INPUT_FILE_TYPE_SINGLE = "Single"; //input file parameter - single file
    public static final String INPUT_FILE_TYPE_MULTIPLE = "Multiple";//input file parameter - list of files
    
    public WorkflowTaskCategory(String owner, String name, ArrayList<String> urlParameters, 
            LinkedHashMap<String, String> inputFilesInfos) {
        super(owner, name);
        this.urlParameters = urlParameters;
        this.inputFilesInfos = inputFilesInfos;
    }
    
    public WorkflowTaskCategory(String owner, String name, ArrayList<String> urlParameters, 
            LinkedHashMap<String, String> inputFilesInfos, String description) {
        this(owner, name, urlParameters, inputFilesInfos);
        this.description = description;        
    }

    public ArrayList<String> getUrlParameters() {
        return urlParameters;
    }

    public LinkedHashMap<String, String> getInputFilesInfos() {
        return inputFilesInfos;
    }
    
    @Override
    public String info() {
        String info = "Właściciel: " + owner + "\nNazwa: " + name;
        if(description != null) {
            info += "\nOpis:\n" + description;
        }
        if(!urlParameters.isEmpty()) {
            info += "\nParametry w adresie url:";
            for(String urlParameter : urlParameters) {
                info += "\n" + urlParameter;
            }
        }
        if(!inputFilesInfos.isEmpty()) {
            info += "\nParametry - pliki wejściowe:";
            for(String inputFileInfo : inputFilesInfos.keySet()) {
                info += "\n" + inputFileInfo;
                if(inputFilesInfos.get(inputFileInfo).equals(INPUT_FILE_TYPE_SINGLE)) 
                    info += " - pojedynczy plik";
                else
                    info += " - lista plików";
            }
        }
        return info;
    }

    @Override
    public String toString() {
        return getNameAndOwner();        
    }
    
}
