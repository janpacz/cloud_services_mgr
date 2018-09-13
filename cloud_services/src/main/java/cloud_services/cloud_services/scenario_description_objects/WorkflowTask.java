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

import cloud_services.cloud_services.working_window.ScenarioController;
import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class WorkflowTask extends WorkflowObject{
    private ScenarioController controller;
    private WorkflowTaskCategory category;
    private int dataToSendSize;
    private int operationsAmount;
    private int executionState;
    private ArrayList<Integer> precedingTasks; 
    private ArrayList<Integer> followingTasks;
    private int resourceIndex; //index in scenario of resource where task is or will be executed
    private LinkedHashMap<String, String> urlParameters; //parameters added to url address and their values
    private LinkedHashMap<String, Object> inputFilesInfos; //input file parameters and their values
    private int indexWithSameNameInScenario; //number of task within tasks with the same name
    private TreeMap<Integer, String> precedingTasksThatFailed;
    //statuses of task
    public static final int TASK_NOT_EXECUTED = 0;
    public static final int TASK_IN_EXECUTION = 1;
    public static final int TASK_EXECUTED = 2;
    public static final int TASK_FAILED = 3;
    
    public static final String RESULT_FILENAME_DIRECTORY = "ResultFiles";
    private static final String RESULT_FILENAME_PREFIX = "Result_";

    public WorkflowTask(String owner, String name, WorkflowTaskCategory category, int dataToSendSize, int operationsAmount) {
        super(owner, name);
        this.category = category;
        this.dataToSendSize = dataToSendSize;
        this.operationsAmount = operationsAmount;
        this.executionState = TASK_NOT_EXECUTED;
        precedingTasks = new ArrayList<>();
        followingTasks = new ArrayList<>();
        resourceIndex = -1;
        urlParameters = new LinkedHashMap<>();
        for(String urlParameter : category.getUrlParameters()) {
            urlParameters.put(urlParameter, "");
        }
        inputFilesInfos = new LinkedHashMap<>();
        for(String inputFileParameter : category.getInputFilesInfos().keySet()) {
            if(category.getInputFilesInfos().get(inputFileParameter).equals(WorkflowTaskCategory.INPUT_FILE_TYPE_SINGLE))
                inputFilesInfos.put(inputFileParameter, "");
            else
                inputFilesInfos.put(inputFileParameter, new ArrayList<>());
        }
        indexWithSameNameInScenario = -1;
        precedingTasksThatFailed = new TreeMap<>();
    }
    
    public WorkflowTask(String owner, String name, WorkflowTaskCategory category, int dataToSendSize, int operationsAmount, String description) {
        this(owner, name, category, dataToSendSize, operationsAmount);
        this.description = description;
    }
    
    public WorkflowTask(WorkflowTask wt, ScenarioController controller, int indexInScenario, int indexWithSameNameInScenario) {
        this(wt.owner, wt.name, wt.category, wt.dataToSendSize, wt.operationsAmount, wt.description);
        this.controller = controller;
        this.indexInScenario = indexInScenario;
        this.indexWithSameNameInScenario = indexWithSameNameInScenario;
        setSizeOnSchemeX();
    }
    
    private void setSizeOnSchemeX() {
        Text text = new Text(getNameWithIndexWithSameNameInScenario());
        Font font = Font.font("Arial", 15);
        text.setFont(font);
        sizeOnSchemeX = (int) text.getLayoutBounds().getWidth();
    }
    
    public WorkflowTaskCategory getCategory() {
        return category;
    }
    
    public int getDataToSendSize() {
        return dataToSendSize;
    }
    
    public int getOperationsAmount() {
        return operationsAmount;
    }
    
    public String getNameWithIndexWithSameNameInScenario() {
        return getNameAndOwner() + "#" + indexWithSameNameInScenario;
    }
    
    public int getInputFilesAmount() {
        return inputFilesInfos.size();
    }
    
    public LinkedHashMap<String, String> getUrlParametersCopy() {
        LinkedHashMap<String, String> urlParametersCopy = new LinkedHashMap<>();
        for(String urlParameter : urlParameters.keySet()) {
            urlParametersCopy.put(urlParameter, urlParameters.get(urlParameter));
        }
        return urlParametersCopy;
    }

    public void setUrlParameters(LinkedHashMap<String, String> urlParameters) {
        this.urlParameters = urlParameters;
    }

    public LinkedHashMap<String, Object> getInputFilesInfosCopy() {
        LinkedHashMap<String, Object> inputFilesInfosCopy = new LinkedHashMap<>();
        for(String inputFileParameter : inputFilesInfos.keySet()) {
            Object inputFileInfo = inputFilesInfos.get(inputFileParameter);
            if(inputFileInfo instanceof ArrayList) {
                inputFilesInfosCopy.put(inputFileParameter, new ArrayList<>((ArrayList)inputFileInfo));
            } else {
                inputFilesInfosCopy.put(inputFileParameter, inputFilesInfos.get(inputFileParameter));
            }
        }
        return inputFilesInfosCopy;
    }

    public void setInputFilesInfos(LinkedHashMap<String, Object> inputFilesInfos) {
        this.inputFilesInfos = inputFilesInfos;
    }
    
    public String getTaskOutputFileName() {
        return RESULT_FILENAME_DIRECTORY + "/" + RESULT_FILENAME_PREFIX + getNameWithIndexWithSameNameInScenario();
    }

    public int getExecutionState() {
        return executionState;
    }
    
    public void setExecutionState(int executionState) {
        this.executionState = executionState;
    }
    
    public int getLeftMiddlePointX() {
        return positionX;
    }
    
    public int getLeftMiddlePointY() {
        return positionY + sizeOnSchemeY / 2;
    }
    
    public int getRightMiddlePointX() {
        return positionX + sizeOnSchemeX;
    }
    
    public int getRightMiddlePointY() {
        return positionY + sizeOnSchemeY / 2;
    }
    
    public void addPrecedingTask(int precedingTaskIndex) {
        if(!precedingTasks.contains(precedingTaskIndex))
            precedingTasks.add(precedingTaskIndex);
    }
    
    public void removePrecedingTask(Integer removedPrecedingTaskIndex) {
        Integer removedTaskIndexInPrecedingTasksList = precedingTasks.indexOf(removedPrecedingTaskIndex);
        precedingTasks.remove(removedPrecedingTaskIndex);
        for(String inputFileParameter : inputFilesInfos.keySet()) {
            if(inputFilesInfos.get(inputFileParameter) instanceof Integer) {
                Integer inputFilePrecedingTaskIndex = (Integer)inputFilesInfos.get(inputFileParameter);
                if(inputFilePrecedingTaskIndex > removedTaskIndexInPrecedingTasksList) 
                    inputFilesInfos.put(inputFileParameter, inputFilePrecedingTaskIndex - 1);
                else if(inputFilePrecedingTaskIndex == removedTaskIndexInPrecedingTasksList) 
                    inputFilesInfos.put(inputFileParameter, "");
            } else if(inputFilesInfos.get(inputFileParameter) instanceof ArrayList) {
                ArrayList<Object> inputFilesInfosArray = (ArrayList) inputFilesInfos.get(inputFileParameter);
                while(inputFilesInfosArray.contains(removedTaskIndexInPrecedingTasksList)) {
                    inputFilesInfosArray.remove(removedTaskIndexInPrecedingTasksList);
                }
                for(int i=0; i<inputFilesInfosArray.size(); i++) {
                    Object inputFileInfo = inputFilesInfosArray.get(i);
                    if(inputFileInfo instanceof Integer && (Integer)inputFileInfo > removedTaskIndexInPrecedingTasksList) {                        
                        inputFilesInfosArray.set(i, (Integer)inputFileInfo - 1);
                    }
                }                                
            }
        }
    }
    
    public Integer[] getPrecedingTasksArray() {
        
        Integer[] array = new Integer[precedingTasks.size()];
        precedingTasks.toArray(array);
        return array;
    }
    
    public Integer getPrecedingTaskIndexInScenario(int precedingTaskIndexInPrecedingTasksList)
    {
        return precedingTasks.get(precedingTaskIndexInPrecedingTasksList);
    }
    
    public int getPrecedingTasksAmount()
    {
        return precedingTasks.size();
    }
    
    public void addFollowingTask(int followingTaskIndex) {
        if(!followingTasks.contains(followingTaskIndex))
            followingTasks.add(followingTaskIndex);
    }
    
    public void removeFollowingTask(Integer followingTaskIndex) {
        followingTasks.remove(followingTaskIndex);
    }
    
    public Integer[] getFollowingTasksArray() {
        
        Integer[] array = new Integer[followingTasks.size()];
        followingTasks.toArray(array);
        return array;
    }
    
    public void actualizationAfterAnotherTaskRemoval(WorkflowTask removedTask) {
        if(indexInScenario > removedTask.indexInScenario) {
            indexInScenario -= 1;            
        }
        for(int i=0; i<precedingTasks.size(); i++) {
            if(precedingTasks.get(i) > removedTask.indexInScenario)
                precedingTasks.set(i, precedingTasks.get(i) - 1);
        }
        for(int i=0; i< followingTasks.size(); i++) {
            if(followingTasks.get(i) > removedTask.indexInScenario)
                followingTasks.set(i, followingTasks.get(i) - 1);
        }
        if(name.equals(removedTask.name) && indexWithSameNameInScenario > removedTask.indexWithSameNameInScenario) {
            indexWithSameNameInScenario--;
        }
        for(int failedTaskIndex : precedingTasksThatFailed.keySet()) {
            if(failedTaskIndex >= removedTask.indexInScenario) {
                String failReason = precedingTasksThatFailed.remove(failedTaskIndex);
                if(failedTaskIndex == removedTask.indexInScenario && precedingTasksThatFailed.isEmpty()) {
                    executionState = TASK_NOT_EXECUTED;
                } else if(failedTaskIndex > removedTask.indexInScenario) {
                    precedingTasksThatFailed.put(failedTaskIndex - 1, failReason);
                }
            }
        }
        setSizeOnSchemeX();
    }
        
    public void setResourceIndex(int resourceIndex) {
        this.resourceIndex = resourceIndex;
    }
    
    public int getResourceIndex() {
        return resourceIndex;
    }
    
    public void addFailedTask(int indexOfPrecedingTaskThatFailed, String reasonOfFailing, ExecuteScenarioDialog dialog, boolean reduceCost) {
        precedingTasksThatFailed.put(indexOfPrecedingTaskThatFailed, reasonOfFailing);
        if(executionState != TASK_FAILED) {
            executionState = TASK_FAILED;
            if(indexOfPrecedingTaskThatFailed != indexInScenario)
                dialog.stopWaitingForFailedTask();
            else if(reduceCost)
                dialog.reduceCost(this);
        }
        for(int followingTaskIndex : followingTasks) {
            WorkflowTask followingTask = controller.getTask(followingTaskIndex);
            followingTask.addFailedTask(indexOfPrecedingTaskThatFailed, reasonOfFailing, dialog, false);
        }
    }

    public TreeMap<Integer, String> getPrecedingTasksThatFailed() {
        return precedingTasksThatFailed;
    }
    
    public void removeAllFailedTasks() {
        precedingTasksThatFailed.clear();
    }
    
    @Override
    public String info() {
        String info = "Właściciel: " + owner + 
                "\nNazwa: " + name +
                "\nKategoria: " + category +
                "\nRozmiar danych do wysłania: " + dataToSendSize + 
                "\nLiczba operacji: " + operationsAmount;
        if(description != null && !description.equals(""))
            info += "\nOpis:" + description;
        return info;
    }
    
    @Override
    public String toString() {
        if(indexWithSameNameInScenario == -1)
            return getNameAndOwner();
        else
            return getNameWithIndexWithSameNameInScenario();
    }
    
    //returns task color on scenario screen
    public Color getTaskColor() {
        switch(executionState)
        {
            case TASK_NOT_EXECUTED:
                return Color.BLACK;
            case TASK_IN_EXECUTION:
                return Color.ORANGE;
            case TASK_EXECUTED:
                return Color.GREEN;
            case TASK_FAILED:
                return Color.RED;
        }
        return null;
    }
    
    @Override
    public void draw(GraphicsContext gc) {
         super.draw(gc, getTaskColor(), getNameWithIndexWithSameNameInScenario());
    }
}
