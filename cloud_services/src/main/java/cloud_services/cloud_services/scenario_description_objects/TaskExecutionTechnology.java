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

import java.util.LinkedHashMap;

public abstract class TaskExecutionTechnology {
    private String url;
    private LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder;
    
    public TaskExecutionTechnology(String url, LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder) {
        this.url = url;
        this.urlParametersNamesAndOrder = urlParametersNamesAndOrder;
    }

    public String getUrl() {
        return url;
    }

    public LinkedHashMap<String, ResourceUrlParameter> getUrlParametersNamesAndOrder() {
        return urlParametersNamesAndOrder;
    }

    public abstract String getTechnologyName();   
    
    public String info() {
        String info = "Url: " + url;
        if(!urlParametersNamesAndOrder.isEmpty()) {
            info += "\nParametry w adresie url: ";
            for(String urlParameter : urlParametersNamesAndOrder.keySet()) {
                info += "\n" + urlParameter + ": ";
                ResourceUrlParameter resourceUrlParameter = urlParametersNamesAndOrder.get(urlParameter);
                if(resourceUrlParameter.getType().equals(ResourceUrlParameter.RESOURCE))
                    info += resourceUrlParameter.getValue();
                else
                    info += "Parametr kategorii: " + resourceUrlParameter.getValue();
            }            
        }
        return info;
    }
}
