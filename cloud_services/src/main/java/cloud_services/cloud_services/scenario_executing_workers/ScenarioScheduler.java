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
import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public abstract class ScenarioScheduler implements Runnable {
    protected ExecuteScenarioDialog dialog;
    protected int tasksAmount;
    protected Semaphore readyNotScheduledOrFailedTasksWaiting;
    protected LinkedBlockingQueue<WorkflowTask> readyNotScheduledTasks;
    protected LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks;    
    protected int scenarioCost;
    protected WorkflowTask taskToSchedule;
    protected WorkflowResource resourceToSchedule;

    public ScenarioScheduler(
            ExecuteScenarioDialog dialog, 
            int tasksAmount, 
            Semaphore readyNotScheduledOrFailedTasksWaiting, 
            LinkedBlockingQueue<WorkflowTask> readyNotScheduledTasks, 
            LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks) {
        this.dialog = dialog;
        this.tasksAmount = tasksAmount;
        this.readyNotScheduledOrFailedTasksWaiting = readyNotScheduledOrFailedTasksWaiting;
        this.readyNotScheduledTasks = readyNotScheduledTasks;
        this.resourcesToExecuteOnOrFailedTasks = resourcesToExecuteOnOrFailedTasks;        
    }
    
    @Override
    public void run() {
        scenarioCost = 0;
        try {
            for(int i=0; i<tasksAmount; i++) {
                readyNotScheduledOrFailedTasksWaiting.acquire();                
                if(!readyNotScheduledTasks.isEmpty()) {                    
                    setTaskAndResourceToSchedule();
                    if(taskToSchedule != null) {
                        dialog.setTaskResourceIndex(taskToSchedule.getIndexInScenario(), resourceToSchedule.getIndexInScenario());
                        resourceToSchedule.increaseAvailabilityTime(taskToSchedule);
                        scenarioCost += resourceToSchedule.getTaskExecutionCost(taskToSchedule);
                        resourceToSchedule.addTaskToExecute(taskToSchedule);
                        resourcesToExecuteOnOrFailedTasks.add(resourceToSchedule.getIndexInScenario());
                    } else
                        resourcesToExecuteOnOrFailedTasks.add(-1);
                } else {
                    resourcesToExecuteOnOrFailedTasks.add(-1);
                }
            }
        } catch (InterruptedException ex) {}
    }
    
    public int getCost() {
        return scenarioCost;
    }
    
    public void reduceCost(int amountOfReduce) {
        scenarioCost -= amountOfReduce;
    }
    
    protected abstract void setTaskAndResourceToSchedule() throws InterruptedException;
}
