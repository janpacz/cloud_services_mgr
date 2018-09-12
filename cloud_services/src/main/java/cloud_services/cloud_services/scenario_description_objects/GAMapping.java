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

import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.util.ArrayList;

// mapping of tasks to resources - member of genetic algorithm population
public class GAMapping {
    private long time; //expected time of execution of scenario using this mapping
    private int cost; //expected cost of execution of scenario using this mapping
    private ArrayList<GATaskResourceAssingment> order;
    private ExecuteScenarioDialog dialog;
    
    public GAMapping(ExecuteScenarioDialog dialog) {
        this.dialog = dialog;
        order = new ArrayList<>();
    }
    
    public GAMapping(GAMapping mapping) {
        this.dialog = mapping.dialog;
        this.order = new ArrayList<>();
        for(GATaskResourceAssingment assignment : mapping.order) {
            this.order.add(new GATaskResourceAssingment(assignment));
        }
        this.time = mapping.time;
        this.cost = mapping.cost;
    }
    
    public void addAssignment(GATaskResourceAssingment assignment) {
        order.add(assignment);
    }
    
    public void replaceAssignment(int assignmentIndex, GATaskResourceAssingment assignment) {
        order.set(assignmentIndex, assignment);
    }
    
    public int getAssignmentsAmount() {
        return order.size();
    }
    
    public GATaskResourceAssingment getAssignment(int assignmentIndex) {
        return order.get(assignmentIndex);
    }
    
    public int getIndexOfAssignmentForTask(int taskIndex) {
        for(int i=0; i<order.size(); i++) {
            if(order.get(i).getTaskIndex() == taskIndex)
                return i;
        }
        return -1;
    }
    
    public void calculateTimeAndCost() {
        cost = 0;
        time = 0L;
        ArrayList<Long> resourcesWorkTimes = new ArrayList<>();
        ArrayList<Long> tasksFinishTimes = new ArrayList<>();
        long precedingTasksFinishTime = 0L;
        for(int i=0; i<dialog.getResourcesAmount(); i++) {
            resourcesWorkTimes.add(0L);
        }
        for(int i=0; i<dialog.getTasksAmount(); i++) {
            tasksFinishTimes.add(0L);
        }
        for(GATaskResourceAssingment assignment : order) {
            WorkflowResource resource = dialog.getResource(assignment.getResourceIndex());
            WorkflowTask task = dialog.getTask(assignment.getTaskIndex()); 
            for(int precedingTaskIndex : task.getPrecedingTasksArray()) {
                if(precedingTasksFinishTime < tasksFinishTimes.get(precedingTaskIndex))
                    precedingTasksFinishTime = tasksFinishTimes.get(precedingTaskIndex);
            }
            long currentResourceWorkTime = resourcesWorkTimes.get(assignment.getResourceIndex());
            if(currentResourceWorkTime > precedingTasksFinishTime) {
                resourcesWorkTimes.set(assignment.getResourceIndex(), 
                        currentResourceWorkTime + resource.getTaskExecutionTime(task));
                tasksFinishTimes.set(assignment.getTaskIndex(), 
                        currentResourceWorkTime + resource.getTaskExecutionTime(task));
            } else {
                resourcesWorkTimes.set(assignment.getResourceIndex(), 
                        precedingTasksFinishTime + resource.getTaskExecutionTime(task));
                tasksFinishTimes.set(assignment.getTaskIndex(), 
                        precedingTasksFinishTime + resource.getTaskExecutionTime(task));
            }
            cost += resource.getTaskExecutionCost(task);
        }
        for(Long resourceWorkTime : resourcesWorkTimes) {
            if(time < resourceWorkTime)
                time = resourceWorkTime;
        }
    }

    public long getTime() {
        return time;
    }

    public int getCost() {
        return cost;
    }   
}
