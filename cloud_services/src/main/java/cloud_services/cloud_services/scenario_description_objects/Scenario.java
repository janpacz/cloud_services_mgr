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

import org.json.simple.JSONObject;

public class Scenario extends ScenarioDescriptionElement {
    // all elements of workflow in JSON structure which can be sent to server to store in DB
    private JSONObject workflowObjectsJSONObject;
    
    public Scenario(String owner, String name, JSONObject workflowObjectsJSONObject) {
        super(owner, name);
        this.workflowObjectsJSONObject = workflowObjectsJSONObject;
    }
    
    public Scenario(String owner, String name, JSONObject workflowObjectsJSONObject, String description) {
        this(owner, name, workflowObjectsJSONObject);
        this.description = description;
    }

    public JSONObject getWorkflowObjectsJSONObject() {
        return workflowObjectsJSONObject;
    }
    
    @Override
    public String info() {
        String info = "Właściciel: " + owner + "\nNazwa: " + name;
        if(description != null) {
            info += "\nOpis:\n" + description;
        }
        return info;
    }
    
    @Override
    public String toString() {
        return getNameAndOwner();        
    }    
}
