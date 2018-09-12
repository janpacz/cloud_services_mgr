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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

//class scheduling scenario using heuristic algorithms
public class ScenarioSchedulerHeuristic extends ScenarioScheduler {
    private String algorithm;
    private ExecutorService executors;
    private int taskToScheduleIndex;
    private int resourceToScheduleIndex;   
    private long bestExecutionTime; //best execution time among all task/resource pairs
    private Semaphore findBestExecutionTimeAreaLock; 
    private int maxCost;
    private LinkedBlockingQueue<WorkflowTask> maxCostExceedingTasks;
    private ConcurrentHashMap<Integer, Integer> myopicResourceForEachTask;
    
    public ScenarioSchedulerHeuristic(
             ExecuteScenarioDialog dialog,               
             int tasksAmount,
             Semaphore readyNotScheduledOrFailedTasksWaiting, 
             LinkedBlockingQueue<WorkflowTask> readyNotScheduledTasks,
             LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks,
             int maxCost,
             String algorithm) {
        super(dialog, tasksAmount, readyNotScheduledOrFailedTasksWaiting, readyNotScheduledTasks, resourcesToExecuteOnOrFailedTasks);
        this.maxCost = maxCost;
        this.algorithm = algorithm;
        findBestExecutionTimeAreaLock = new Semaphore(1);
        maxCostExceedingTasks = new LinkedBlockingQueue<>(tasksAmount);
        myopicResourceForEachTask = new ConcurrentHashMap();
    }
    
    @Override
    protected void setTaskAndResourceToSchedule() throws InterruptedException {        
        switch(algorithm) {
            case ExecuteScenarioDialog.MYOPIC:
                myopicResourceForEachTask.clear();
                break;
            case ExecuteScenarioDialog.MIN_MIN:
                bestExecutionTime = Long.MAX_VALUE;
                break;
            case ExecuteScenarioDialog.MAX_MIN:
                bestExecutionTime = Long.MIN_VALUE;
                break;
        }
        executors = Executors.newCachedThreadPool();
        for(WorkflowTask wt : readyNotScheduledTasks) {
            executors.execute(new ResourceToExecuteFinder(this, wt, dialog.getResourcesArray(), maxCost, scenarioCost, maxCostExceedingTasks));
        }
        executors.shutdown();
        try {
            executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for(WorkflowTask maxCostExceedingTask : maxCostExceedingTasks) {
                readyNotScheduledTasks.remove(maxCostExceedingTask);
                maxCostExceedingTask.addFailedTask(maxCostExceedingTask.getIndexInScenario(),
                        "Zadanie nie może być wykonane, gdyż przekroczyło by to ustawiony koszt maksymalny " + maxCost,
                        dialog, 
                        false);
            }            
            if(readyNotScheduledTasks.isEmpty()) {
                taskToSchedule = null;
                resourceToSchedule = null;
                dialog.drawLaterSchemeCanvas();
            } else {
                //MYOPIC algorithm - random choise of task/resource pair
                if(algorithm.equals(ExecuteScenarioDialog.MYOPIC)) {                    
                    Random random = new Random();
                    int taskToScheduleIndexInMap = random.nextInt(myopicResourceForEachTask.size());
                    int i = 0;
                    for(Integer taskIndex : myopicResourceForEachTask.keySet()) {
                        if(i == taskToScheduleIndexInMap) {
                            taskToScheduleIndex = taskIndex;
                            resourceToScheduleIndex = myopicResourceForEachTask.get(taskIndex);
                            break;
                        }
                        i++;
                    } 
                }
                taskToSchedule = dialog.getTask(taskToScheduleIndex);
                resourceToSchedule = dialog.getResource(resourceToScheduleIndex);
                readyNotScheduledTasks.remove(taskToSchedule);
            }
        } catch (InterruptedException ex) {
            executors.shutdownNow();
            throw new InterruptedException();
        }
    }

    //method used by ResourceToExecuteFinder - for MIN_MIN and MAX_MIN algorithm chooses best task/resource pair
    public void findTaskAndResourcetoSchedule(int taskIndex, int resourceIndex) throws InterruptedException {
        WorkflowResource resource = dialog.getResource(resourceIndex);
        WorkflowTask task = dialog.getTask(taskIndex);
        long executionTime = resource.getAvailabilityTime() + resource.getTaskExecutionTime(task);        
        findBestExecutionTimeAreaLock.acquire();
        switch(algorithm) {
            case ExecuteScenarioDialog.MYOPIC:
                myopicResourceForEachTask.put(taskIndex, resourceIndex);
                break;
            case ExecuteScenarioDialog.MIN_MIN:
                if(bestExecutionTime > executionTime) {
                    bestExecutionTime = executionTime;
                    taskToScheduleIndex = taskIndex;
                    resourceToScheduleIndex = resourceIndex;
                }
                break;
            case ExecuteScenarioDialog.MAX_MIN:
                if(bestExecutionTime < executionTime) {
                   bestExecutionTime = executionTime;
                   taskToScheduleIndex = taskIndex;
                   resourceToScheduleIndex = resourceIndex;
                }
                break;
        }
        findBestExecutionTimeAreaLock.release();
    }    
}
