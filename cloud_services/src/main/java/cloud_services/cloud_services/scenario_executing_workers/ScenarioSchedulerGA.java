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

import cloud_services.cloud_services.scenario_description_objects.GAMapping;
import cloud_services.cloud_services.scenario_description_objects.GATaskResourceAssingment;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

//object that schedules scenario according to mapping chosen previously by genetic algorithm
public class ScenarioSchedulerGA extends ScenarioScheduler {
    private GAMapping mapping; // task resources mapping chosen previously by genetic algorithm

    public ScenarioSchedulerGA(
            ExecuteScenarioDialog dialog, 
            int tasksAmount, 
            Semaphore readyNotScheduledOrFailedTasksWaiting, 
            LinkedBlockingQueue<WorkflowTask> readyNotScheduledTasks, 
            LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks, 
            GAMapping mapping) {
        super(dialog, tasksAmount, readyNotScheduledOrFailedTasksWaiting, readyNotScheduledTasks, resourcesToExecuteOnOrFailedTasks);
        this.mapping = mapping;
    }

    @Override
    protected void setTaskAndResourceToSchedule() throws InterruptedException {
        taskToSchedule = readyNotScheduledTasks.take();
        GATaskResourceAssingment assignment = mapping.getAssignment(mapping.getIndexOfAssignmentForTask(taskToSchedule.getIndexInScenario()));                    
        resourceToSchedule = dialog.getResource(assignment.getResourceIndex());
    }
}
