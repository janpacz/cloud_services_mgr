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

package cloud_services.cloud_services.scenario_executing_workers;

import cloud_services.cloud_services.scenario_description_objects.ResourceUrlParameter;
import cloud_services.cloud_services.scenario_description_objects.RestTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.SoapTaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.TaskExecutionTechnology;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

//object that executes specified task on specified resource - prepares and sends web request and receives response
public class TaskExecutor implements Runnable {
    private ExecuteScenarioDialog dialog;
    private Integer resourceIndex;
    private HttpURLConnection httpConnection;
    private boolean executionBroken;
    
    public TaskExecutor(ExecuteScenarioDialog dialog, Integer resourceIndex) {
        this.dialog = dialog;
        this.resourceIndex = resourceIndex;
        httpConnection = null;
        executionBroken = false;
    }
    
    @Override
    public void run() {        
        boolean previousExecutionCorrect = true;
        WorkflowResource resource = dialog.getResource(resourceIndex);
        try {            
            resource.getExecutePermission(); //wait until resource is available
            WorkflowTask task = resource.getTaskToExecute();            
            dialog.setTaskExecutionState(task.getIndexInScenario(), WorkflowTask.TASK_IN_EXECUTION);
            WorkflowTaskCategory category = task.getCategory();
            TaskExecutionTechnology technology = resource.getCategoryTechnology(category.getNameAndOwner());
            String urlString = technology.getUrl();
            LinkedHashMap<String, String> urlParameters = task.getUrlParametersCopy();
            LinkedHashMap<String, ResourceUrlParameter> urlParametersNamesAndOrder = technology.getUrlParametersNamesAndOrder();
            if(!urlParametersNamesAndOrder.isEmpty()) {
                //url parameters addad to url addrsss
                urlString += "?";
                for(String urlParameter : urlParametersNamesAndOrder.keySet()) {
                    ResourceUrlParameter resourceUrlParameter = urlParametersNamesAndOrder.get(urlParameter);
                    if(resourceUrlParameter.getType().equals(ResourceUrlParameter.RESOURCE)) {
                        //url parameters expected by resource service
                        urlString += urlParameter + "=" 
                            + resourceUrlParameter.getValue() + "&";
                    } else {
                        //url parameters expected by category
                        urlString += urlParameter + "=" 
                            + urlParameters.get(resourceUrlParameter.getValue()) + "&";
                    }

                }
                urlString = urlString.substring(0, urlString.length() - 1);
            }
            try {
                final URL url = new URL(urlString);
                File outputFolder = new File(WorkflowTask.RESULT_FILENAME_DIRECTORY);
                if (!outputFolder.exists()) {
                    outputFolder.mkdir();
                }
                File outputFile = new File(task.getTaskOutputFileName());
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }                
                if(technology.getTechnologyName().equals(RestTaskExecutionTechnology.REST_TECHNOLOGY_NAME)) {
                    RestTaskExecutionTechnology restTechnology = (RestTaskExecutionTechnology) technology;
                    httpConnection = (HttpURLConnection) url.openConnection();
                    if(executionBroken) {
                        httpConnection.disconnect();
                    } else {
                        if(category.getInputFilesInfos().isEmpty()) {                            
                            try {
                                httpConnection.setRequestMethod("GET");
                                httpConnection.setDoOutput(true);
                                if (httpConnection.getResponseCode() != restTechnology.getExpectedResponseCode()) {
                                    previousExecutionCorrect = false;
                                    task.addFailedTask(task.getIndexInScenario(),
                                            "Błędny kod odpowiedzi serwera - oczekiwany: " 
                                                    + restTechnology.getExpectedResponseCode() 
                                                    + ", aktualny: " + httpConnection.getResponseCode(),
                                            dialog,
                                            true);
                                }
                            } catch (IOException ex) {
                                previousExecutionCorrect = false;
                                if(! executionBroken) {
                                    task.addFailedTask(task.getIndexInScenario(),
                                            "Błąd połączenia z serwerem",
                                            dialog,
                                            true);
                                }
                            }
                        } else {
                            //input files added
                            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                            LinkedHashMap<String, Object> inputFilesInfos = task.getInputFilesInfosCopy();
                            for(String inputFileParameter : inputFilesInfos.keySet()) {
                                if(executionBroken) {
                                    previousExecutionCorrect = false;
                                } else if(inputFilesInfos.get(inputFileParameter) instanceof ArrayList) {
                                    ArrayList<Object> inputFilesInfosArray = (ArrayList) inputFilesInfos.get(inputFileParameter);
                                    for(Object inputFileInfo : inputFilesInfosArray) {
                                        previousExecutionCorrect = addInputFileToEntityBuilder(inputFileInfo, inputFileParameter, builder, task);                            
                                        if(!previousExecutionCorrect)
                                            break;
                                    }
                                } else {
                                    previousExecutionCorrect = addInputFileToEntityBuilder(inputFilesInfos.get(inputFileParameter), inputFileParameter, builder, task);                            
                                }
                                if(!previousExecutionCorrect)
                                    break;
                            }
                            if(executionBroken) {
                                previousExecutionCorrect = false;
                            } else if(previousExecutionCorrect) {
                                try {
                                    HttpEntity entity = builder.build();
                                    httpConnection.setRequestMethod("POST");
                                    httpConnection.setDoOutput(true);
                                    httpConnection.addRequestProperty("Content-length", entity.getContentLength()+"");
                                    httpConnection.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

                                    try (OutputStream os = httpConnection.getOutputStream()) {
                                        entity.writeTo(os);
                                        if (httpConnection.getResponseCode() != restTechnology.getExpectedResponseCode()) {
                                            previousExecutionCorrect = false;
                                            task.addFailedTask(task.getIndexInScenario(),
                                                    "Błędny kod odpowiedzi serwera - oczekiwany: " 
                                                            + restTechnology.getExpectedResponseCode() 
                                                            + ", aktualny: " + httpConnection.getResponseCode(),
                                                    dialog,
                                                    true);
                                        }
                                    }
                                } catch (IOException ex) {
                                    previousExecutionCorrect = false;
                                    if(! executionBroken) {
                                        task.addFailedTask(task.getIndexInScenario(),
                                                "Błąd połączenia z serwerem",
                                                dialog,
                                                true);
                                    }
                                }                            
                            }
                        }
                        if(previousExecutionCorrect) {
                            //save output file
                            int readedByte;                
                            try (InputStream is = httpConnection.getInputStream();
                                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                                while((readedByte=is.read())!=-1) {
                                    fos.write(readedByte);
                                }
                            } catch (IOException ex) {
                                previousExecutionCorrect = false;
                                task.addFailedTask(task.getIndexInScenario(),
                                        "Błąd podczas zapisywania pliku wynikowego",
                                        dialog,
                                        true);
                            }
                        }
                    }
                } else if(technology.getTechnologyName().equals(SoapTaskExecutionTechnology.SOAP_TECHNOLOGY_NAME)) {
                    try {
                        final SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
                        SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
                        SOAPBody soapBody = soapEnvelope.getBody();
                        SoapTaskExecutionTechnology soapTechnology = (SoapTaskExecutionTechnology)technology;
                        SOAPBodyElement element = soapBody.addBodyElement(soapEnvelope.createName(soapTechnology.getMethodName(), soapTechnology.getNamespacePrefix(), soapTechnology.getNamespaceUri()));
                        LinkedHashMap<String, Object> inputFilesInfos = task.getInputFilesInfosCopy();
                        for(String inputFileParameter : inputFilesInfos.keySet()) {
                            //input files added
                            File inputFile;
                            if(executionBroken) {
                                previousExecutionCorrect = false;
                            } else if(inputFilesInfos.get(inputFileParameter) instanceof ArrayList) {
                                ArrayList<byte[]> inputFilesByteArrays = new ArrayList<>();
                                ArrayList<Object> inputFilesInfosArray = (ArrayList) inputFilesInfos.get(inputFileParameter);
                                for(Object inputFileInfo : inputFilesInfosArray) {
                                    inputFile = getInputFileFromInputFileInfo(inputFileInfo, inputFileParameter, task);
                                    if(inputFile != null) {
                                        byte[] inputFileBytes = new byte[(int) inputFile.length()];
                                        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile))) {
                                            bis.read(inputFileBytes);
                                        }
                                        element.addChildElement(inputFileParameter + "Name").addTextNode(inputFile.getName());
                                        inputFilesByteArrays.add(inputFileBytes);
                                    } else {
                                        previousExecutionCorrect = false;
                                        break;
                                    }
                                }
                                if(previousExecutionCorrect) {
                                    for(byte[] inputFileBytes : inputFilesByteArrays) {
                                        element.addChildElement(inputFileParameter + "Bytes").addTextNode(Base64.getEncoder().encodeToString(inputFileBytes));
                                    }
                                }
                            } else {
                                inputFile = getInputFileFromInputFileInfo(inputFilesInfos.get(inputFileParameter), inputFileParameter, task);
                                if(inputFile != null) {
                                    byte[] inputFileBytes = new byte[(int) inputFile.length()];
                                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(inputFile))) {
                                        bis.read(inputFileBytes);
                                    }
                                    element.addChildElement(inputFileParameter + "Name").addTextNode(inputFile.getName());
                                    element.addChildElement(inputFileParameter + "Bytes").addTextNode(Base64.getEncoder().encodeToString(inputFileBytes));
                                } else
                                    previousExecutionCorrect = false;
                            }
                            if(!previousExecutionCorrect)
                                break;
                        }
                        if(executionBroken) {
                            previousExecutionCorrect = false;
                        } else if(previousExecutionCorrect) {
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            Callable<SOAPBody> callable = new Callable<SOAPBody>() {
                                @Override
                                public SOAPBody call() throws SOAPException {
                                    SOAPConnection soapConnection = SOAPConnectionFactory.newInstance().createConnection();
                                    SOAPMessage response = soapConnection.call(soapMessage, url);
                                    return response.getSOAPBody();
                                }                               
                            };
                            Future<SOAPBody> future = executor.submit(callable);
                            SOAPBody responseSoapBody = null;
                            try {
                                responseSoapBody = future.get();
                            } catch (ExecutionException ex) {
                                previousExecutionCorrect = false;                                
                                task.addFailedTask(task.getIndexInScenario(),
                                        "Błąd połączenia z serwerem lub po stronie serwera",
                                        dialog,
                                        true);
                            }
                            if(previousExecutionCorrect) {
                                if(responseSoapBody == null) {
                                    previousExecutionCorrect = false;
                                    task.addFailedTask(task.getIndexInScenario(),
                                            "Błąd po stronie serwera",
                                            dialog,
                                            true);
                                } else if(responseSoapBody.hasFault()) {
                                    SOAPFault soapFault = responseSoapBody.getFault();
                                    previousExecutionCorrect = false;
                                    task.addFailedTask(task.getIndexInScenario(),
                                            "Błąd po stronie serwera: " + soapFault.getFaultString(),
                                            dialog,
                                            true);
                                } else {
                                    //save output file
                                    Iterator soapBodyIterator = responseSoapBody.getChildElements();
                                    SOAPBodyElement methodResponseElement = (SOAPBodyElement) soapBodyIterator.next();
                                    Iterator methodResponseIterator = methodResponseElement.getChildElements();
                                    SOAPBodyElement methodReturnValueElement = (SOAPBodyElement) methodResponseIterator.next();
                                    String methodReturnValue = methodReturnValueElement.getValue();
                                    try {
                                        byte[] methodReturnValueBytes = Base64.getDecoder().decode(methodReturnValue);
                                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                            fos.write(methodReturnValueBytes);
                                        } catch (IOException ex) {
                                            previousExecutionCorrect = false;
                                            task.addFailedTask(task.getIndexInScenario(),
                                                    "Błąd podczas zapisywania pliku wynikowego",
                                                    dialog,
                                                    true);
                                        }
                                    } catch (Exception ex) {
                                        previousExecutionCorrect = false;
                                        task.addFailedTask(task.getIndexInScenario(),
                                                "Błędna odpowiedź z serwera",
                                                dialog,
                                                true);
                                    }                                
                                }
                            }
                        }                    
                    } catch (SOAPException ex) {
                        previousExecutionCorrect = false;
                        task.addFailedTask(task.getIndexInScenario(),
                                "Błąd podczas wykonywania zadania",
                                dialog,
                                true);
                    }            
                } 
            } catch (IOException ex) {
                previousExecutionCorrect = false;
                task.addFailedTask(task.getIndexInScenario(),
                                    "Błąd podczas wykonywania zadania",
                                    dialog,
                                    true);
            }
            resource.releaseExecutePermission(); //let resource know that it can accept another task            
            if(executionBroken && task.getExecutionState() == WorkflowTask.TASK_IN_EXECUTION) { 
                //execution was successfull but broken by user
                dialog.reduceCost(task);
                dialog.setTaskExecutionState(task.getIndexInScenario(), WorkflowTask.TASK_NOT_EXECUTED);
            } else if(previousExecutionCorrect) {
                //execution successfull
                dialog.setTaskExecutionState(task.getIndexInScenario(), WorkflowTask.TASK_EXECUTED);
                for(int followingTaskIndex : task.getFollowingTasksArray()) {
                    dialog.tryRunFollowingTask(followingTaskIndex);
                }
            }
            dialog.setTaskResourceIndex(task.getIndexInScenario(), -1);            
        } catch (InterruptedException ex) {} 
    }
    
    private boolean addInputFileToEntityBuilder(Object inputFileInfo, String inputFileParameter, MultipartEntityBuilder builder, WorkflowTask task) {
        File inputFile = getInputFileFromInputFileInfo(inputFileInfo, inputFileParameter, task);
        if(inputFile != null) {
            FileBody fileBody = new FileBody(inputFile, ContentType.DEFAULT_BINARY);
            builder.addPart(inputFileParameter, fileBody);
            return true;
        } else {
            return false;
        }
    }
    
    //maps tasks's inputFileInfo object (String file path or Integer index of preceding task 
    //(which output file is taken) on preceding tasks list) to File
    private File getInputFileFromInputFileInfo(Object inputFileInfo, String inputFileParameter, WorkflowTask task) {
        File inputFile;
        if(inputFileInfo instanceof Integer) {
            int precedingTaskIndexInScenario = task.getPrecedingTaskIndexInScenario((Integer)inputFileInfo);
            WorkflowTask precedingTask = dialog.getTask(precedingTaskIndexInScenario);
            inputFile = new File(precedingTask.getTaskOutputFileName());
        } else {
            inputFile = new File((String)inputFileInfo);
        }
        if(!inputFile.canRead()) {
            task.addFailedTask(task.getIndexInScenario(), 
                    "Nie można odczytać pliku " + inputFile.getAbsolutePath() 
                            + "\ndla parametru wejściowego " + inputFileParameter,
                    dialog,
                    true);
            return null;
        }
        return inputFile;
    }
    
    public void breakExecution() {
        if(httpConnection != null) {      
            httpConnection.disconnect();       
        }
        executionBroken = true;
    }
}
