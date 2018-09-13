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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLongArray;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class WorkflowResource extends WorkflowObject {
    private long timePerOperation;
    private int costPerOperation;
    private long timePerDataToSendUnit;
    private LinkedBlockingQueue<WorkflowTask> tasksToExecute; //tasks to be executed on this resource
    private AtomicLongArray availabilityTimes; //times when each core will be available
    private Semaphore capacityGuard; //checks whether number of tasks which are executed does not exceed maximum number allowed
    private LinkedHashMap<String, TaskExecutionTechnology> categoriesNamesAndTechnologies; // categories available

    public WorkflowResource(String owner, String name, 
            long timePerOperation, int costPerOperation, long timePerDataToSendUnit, 
            int tasksCapacity, LinkedHashMap<String, TaskExecutionTechnology> categoriesNamesAndTechnologies) {
        
        super(owner, name);
        this.timePerOperation = timePerOperation;
        this.costPerOperation = costPerOperation;
        this.timePerDataToSendUnit = timePerDataToSendUnit;
        this.tasksToExecute = new LinkedBlockingQueue<>();
        availabilityTimes = new AtomicLongArray(tasksCapacity);
        this.capacityGuard = new Semaphore(tasksCapacity);
        this.categoriesNamesAndTechnologies = categoriesNamesAndTechnologies;
    }
    
    public WorkflowResource(String owner, String name, 
            long timePerOperation, int costPerOperation, long timePerDataToSendUnit, 
            int tasksCapacity, LinkedHashMap<String, TaskExecutionTechnology> categoriesTechnologies, String description) {
        
        this(owner, name, timePerOperation, costPerOperation, timePerDataToSendUnit, tasksCapacity, categoriesTechnologies);
        this.description = description;
    }
    
    public WorkflowResource(WorkflowResource wr, int indexInScenario) {
        
        this(wr.owner, wr.name, 
                wr.timePerOperation, wr.costPerOperation, wr.timePerDataToSendUnit, 
                wr.availabilityTimes.length(), wr.categoriesNamesAndTechnologies, wr.description
        );
        this.indexInScenario = indexInScenario;
        Text text = new Text(getNameAndOwner());
        Font font = Font.font("Arial", 15);
        text.setFont(font);
        sizeOnSchemeX = (int) text.getLayoutBounds().getWidth();
    }

    public long getTimePerOperation() {
        return timePerOperation;
    }

    public int getCostPerOperation() {
        return costPerOperation;
    }

    public long getTimePerDataToSendUnit() {
        return timePerDataToSendUnit;
    }
    
    public int getTasksCapacity() {
        return availabilityTimes.length();
    }

    public LinkedHashMap<String, TaskExecutionTechnology> getCategoriesNamesAndTechnologies() {
        return categoriesNamesAndTechnologies;
    }
    
    @Override
    public String info() {
        String info = "Właściciel: " + owner + 
                "\nNazwa: " + name +
                "\nCzas wykonania operacji: " + timePerOperation + 
                "\nKoszt wykonania operacji: "+ costPerOperation +
                "\nCzas wysyłania danych do zasobu: " + timePerDataToSendUnit +
                "\nLiczba jednocześnie wykonywanych zadań: " + availabilityTimes.length();
        if(!categoriesNamesAndTechnologies.isEmpty()) {
            info += "\nKategorie:";
            for(String categoryName : categoriesNamesAndTechnologies.keySet()) {
                info += "\n" + categoryName + ": " + categoriesNamesAndTechnologies.get(categoryName).getTechnologyName();
                info += " " +categoriesNamesAndTechnologies.get(categoryName).getUrl();
            }
        }
        if(description != null && !description.equals(""))
            info += "\nOpis:" + description;
        return info;
    }
    
    @Override
    public String toString() {
        return getNameAndOwner();        
    }
    
    @Override
    public void draw(GraphicsContext gc) {
         super.draw(gc, Color.BLACK, getNameAndOwner());
    }
    
    public boolean acceptsCategory(WorkflowTaskCategory category) {
        return categoriesNamesAndTechnologies.containsKey(category.getNameAndOwner());
    }
    
    public TaskExecutionTechnology getCategoryTechnology(String categoryNameAndOwner) {
        return categoriesNamesAndTechnologies.get(categoryNameAndOwner);
    }
    
    public void addTaskToExecute(WorkflowTask task) {
        tasksToExecute.add(task);
    }
    
    public WorkflowTask getTaskToExecute() throws InterruptedException {
       return tasksToExecute.take();
    }
    
    public void clearTasks() {
        tasksToExecute.clear();
        capacityGuard.drainPermits();
        capacityGuard.release(availabilityTimes.length());
    }
    
    //method for task to request to be executed when in will not exceed maximum number of tasks executed at once 
    public void getExecutePermission() throws InterruptedException {
        capacityGuard.acquire();        
    }
    
    //method used by task after its execution - another task execution may start
    public void releaseExecutePermission() {
        capacityGuard.release();
    }
    
    //expected execution time of specified task on this resource
    public long getTaskExecutionTime(WorkflowTask task) {
        return timePerOperation * task.getOperationsAmount() + timePerDataToSendUnit * task.getDataToSendSize();
    }
    
    //expected execution cost of specified task on this resource
    public int getTaskExecutionCost(WorkflowTask task) {
        return costPerOperation * task.getOperationsAmount();
    }
    
    //finds core which would be available at the earliest
    private int getEarliestAvailabilityTimeIndex() {
        long minAvailabilityTime = availabilityTimes.get(0);
        int minAvailabilityTimeIndex = 0;
        for(int i = 1; i < availabilityTimes.length(); i++) {
            long avilabilityTime = availabilityTimes.get(i);
            if(minAvailabilityTime > avilabilityTime) {
                minAvailabilityTime = avilabilityTime;
                minAvailabilityTimeIndex = i;
            }
        }
        return minAvailabilityTimeIndex;
    }
    
    //gets time when execution of task not yet added to queue would be possible
    //finds core where this time is earliest and returns this time
    //or returns current time when resource is iddle at the moment of method execution
    public long getAvailabilityTime() {
        long earliestAvailabilityTime = availabilityTimes.get(getEarliestAvailabilityTimeIndex());
        if(earliestAvailabilityTime < System.currentTimeMillis())
            return System.currentTimeMillis();
        else
            return earliestAvailabilityTime;
    }
    
    //when adding task to resource - updates availability time on core where task will be executed
    public void increaseAvailabilityTime(WorkflowTask task) {
        if(getAvailabilityTime() <= System.currentTimeMillis())
            availabilityTimes.set(getEarliestAvailabilityTimeIndex(), System.currentTimeMillis() + getTaskExecutionTime(task));
        else
            availabilityTimes.addAndGet(getEarliestAvailabilityTimeIndex(), getTaskExecutionTime(task));
    }
    
    public void setAvailabilityTimeAtCurrentMoment() {
        for(int i = 1; i < availabilityTimes.length(); i++) {
            availabilityTimes.set(i, System.currentTimeMillis());
        }
    }
}
