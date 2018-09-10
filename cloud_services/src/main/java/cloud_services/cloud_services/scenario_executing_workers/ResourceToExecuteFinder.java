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

import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import java.util.concurrent.LinkedBlockingQueue;

public class ResourceToExecuteFinder implements Runnable {
    private ScenarioSchedulerHeuristic scheduler;
    private WorkflowTask task;
    private WorkflowResource[] resources;
    private int maxCost;
    private int currentScenarioCost;
    private LinkedBlockingQueue<WorkflowTask> maxCostExceedingTasks;
    
    public ResourceToExecuteFinder(ScenarioSchedulerHeuristic scheduler, WorkflowTask task, WorkflowResource[] resources,
            int maxCost, int currentScenarioCost, LinkedBlockingQueue<WorkflowTask> maxCostExceedingTasks) {
        this.scheduler = scheduler;
        this.task = task;
        this.resources = resources;
        this.maxCost = maxCost;
        this.currentScenarioCost = currentScenarioCost;
        this.maxCostExceedingTasks = maxCostExceedingTasks;
    }
    
    private boolean canTaskBeExecutedOnResource(WorkflowTask task, WorkflowResource resource) {
        
        if(maxCost > -1 && currentScenarioCost + resource.getTaskExecutionCost(task) > maxCost)
            return false;
        return resource.acceptsCategory(task.getCategory());
    }
    
    @Override
    public void run() {
        
        int i=0;
        while(!canTaskBeExecutedOnResource(task, resources[i])) {
            i++;
            if(i == resources.length) {
                maxCostExceedingTasks.add(task);
                break;
            }
        }
        if(i < resources.length) {
            long minEarliestExecutionTime = resources[i].getAvailabilityTime() + resources[i].getTaskExecutionTime(task);
            int minEarliestExecutionTimeIndex = i;
            i++;
            while(i < resources.length) {

                if(canTaskBeExecutedOnResource(task, resources[i])) {

                    long earliestExecutionTime = resources[i].getAvailabilityTime() + resources[i].getTaskExecutionTime(task);
                    if(earliestExecutionTime < minEarliestExecutionTime) {
                        minEarliestExecutionTime = earliestExecutionTime;
                        minEarliestExecutionTimeIndex = i;
                    }
                }
                i++;
            }
            try {
                scheduler.findTaskAndResourcetoSchedule(task.getIndexInScenario(), resources[minEarliestExecutionTimeIndex].getIndexInScenario());
            } catch (InterruptedException ex) {}        
        }
    }
}
