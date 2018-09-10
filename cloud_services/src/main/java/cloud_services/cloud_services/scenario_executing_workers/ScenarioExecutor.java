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

import cloud_services.cloud_services.working_window.dialogs.ExecuteScenarioDialog;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScenarioExecutor implements Runnable {
    private ExecuteScenarioDialog dialog;
    private int tasksAmount;
    private ExecutorService executors;
    private LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks;
    private long time;
    
    public ScenarioExecutor(ExecuteScenarioDialog dialog, int tasksAmount, LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks) {
        this.dialog = dialog;
        this.tasksAmount = tasksAmount;
        this.resourcesToExecuteOnOrFailedTasks = resourcesToExecuteOnOrFailedTasks;
    }
    
    public long getTime() {
        return time;
    }
    
    @Override
    public void run() {
        time = 0;
        executors = Executors.newCachedThreadPool();
        ArrayList<TaskExecutor> taskExecutors = new ArrayList<>();
        long start = System.currentTimeMillis();        
        try {
            for(int i=0; i<tasksAmount; i++) {
                Integer resourceIndex = resourcesToExecuteOnOrFailedTasks.take();                
                if(resourceIndex != -1) {
                    TaskExecutor taskExecutor = new TaskExecutor(dialog, resourceIndex);
                    taskExecutors.add(taskExecutor);
                    executors.execute(taskExecutor);   
                }
            }
            executors.shutdown();
            executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException ex) {
            executors.shutdownNow();
            for(TaskExecutor taskExecutor : taskExecutors) {
                taskExecutor.breakExecution();
            }
            try {
                executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);                
            } catch (InterruptedException ex1) {
                Logger.getLogger(ScenarioExecutor.class.getName()).log(Level.SEVERE, null, ex1);
            }
            Thread.currentThread().interrupt();
        }
        long stop = System.currentTimeMillis();
        time = stop - start;
        if(!Thread.currentThread().isInterrupted())
            dialog.finishExecution();
    }
}
