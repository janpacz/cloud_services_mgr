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

public class SoapTaskExecutionTechnology extends TaskExecutionTechnology{
    private String methodName;
    private String namespacePrefix;
    private String namespaceUri;
    
    public static final String SOAP_TECHNOLOGY_NAME = "SOAP";

    public SoapTaskExecutionTechnology(String url, 
            LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder, 
            String methodName, String namespacePrefix, String namespaceUri) {
        super(url, urlParametersNamesAndOrder);
        this.methodName = methodName;
        this.namespacePrefix = namespacePrefix;
        this.namespaceUri = namespaceUri;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getNamespacePrefix() {
        return namespacePrefix;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }
    
    @Override
    public String getTechnologyName() {
        return SOAP_TECHNOLOGY_NAME;
    }
    
    @Override
    public String info() {
        String info = super.info();
        info += "\nTechnologia: SOAP";
        info += "\nNazwa metody: " + methodName;        
        info += "\nPrefix przestrzeni nazw: " + namespacePrefix;
        info += "\nUri przestrzeni nazw: " + namespaceUri;
        return info;
    }
}
