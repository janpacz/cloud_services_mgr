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

package cloud_services.cloud_services.working_window.dialogs;

import cloud_services.cloud_services.scenario_description_objects.GAMapping;
import cloud_services.cloud_services.scenario_description_objects.GATaskResourceAssingment;
import cloud_services.cloud_services.scenario_executing_workers.ScenarioExecutor;
import cloud_services.cloud_services.scenario_executing_workers.ScenarioSchedulerHeuristic;
import cloud_services.cloud_services.scenario_description_objects.WorkflowResource;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTask;
import cloud_services.cloud_services.scenario_description_objects.WorkflowTaskCategory;
import cloud_services.cloud_services.scenario_executing_workers.ScenarioScheduler;
import cloud_services.cloud_services.scenario_executing_workers.ScenarioSchedulerGA;
import cloud_services.cloud_services.working_window.ScenarioController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;

//Dialog responsible for sceanrio execution
public class ExecuteScenarioDialog extends ApplicationDialog {
    @FXML private ComboBox algorithmComboBox;
    @FXML private TextField maxCostTextField;
    @FXML private CheckBox checkMaxCostCheckBox;
    @FXML private TextField populationSizeTextField;
    @FXML private TextField iterationsAmountTextField;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label scenarioCostLabel;
    @FXML private Button executeButton;
    @FXML private Button breakButton;
    @FXML private Button closeButton;
    private ScenarioController scenarioController;
    private ArrayList<WorkflowTask> scenarioTasksList;
    private ArrayList<WorkflowResource> scenarioResourcesList;
    private AtomicIntegerArray notExecutedPrecedingTasksAmounts; //amounts of not executed preceding tasks for each task
    private Semaphore readyNotScheduledOrFailedTasksWaiting;
    private LinkedBlockingQueue<WorkflowTask> readyNotScheduledTasks;
    private LinkedBlockingQueue<Integer> resourcesToExecuteOnOrFailedTasks;
    private ExecutorService executors;
    private ScenarioScheduler scenarioScheduler;
    private ScenarioExecutor scenarioExecutor;
    //fields useed in genetic algorithm
    private ArrayList<GAMapping> geneticAlgorithmPopulation;
    private GAMapping bestMapping;
    private ArrayList<Integer> readyNotScheduledTasksGA;
    private ArrayList<Integer> readyNotScheduledPrecedingTasksAmountsGA;
    
    public static final String MYOPIC = "Algorytm myopic- dowolna kolejność";
    public static final String MIN_MIN = "Najpierw najmniejszy czas trwania";
    public static final String MAX_MIN = "Najpierw największy czas trwania";
    public static final String GENETIC = "Algorytm genetyczny";    
    
    public ExecuteScenarioDialog(ScenarioController scenarioController) {
        super("/fxml/dialogs/ExecuteScenarioDialog.fxml");
        
        this.scenarioController = scenarioController;
                
        this.setTitle("Wykonaj scenariusz");
        
        scenarioTasksList = scenarioController.getTasksList();
        scenarioResourcesList= scenarioController.getResourcesList();
        notExecutedPrecedingTasksAmounts = new AtomicIntegerArray(scenarioTasksList.size());
        readyNotScheduledOrFailedTasksWaiting = new Semaphore(scenarioTasksList.size());
        readyNotScheduledTasks = new LinkedBlockingQueue<>(scenarioTasksList.size());
        resourcesToExecuteOnOrFailedTasks = new LinkedBlockingQueue<>(scenarioTasksList.size());
        algorithmComboBox.setItems(FXCollections.observableArrayList(
                    MYOPIC,
                    MIN_MIN,
                    MAX_MIN,
                    GENETIC
                ));
        algorithmComboBox.setValue(MYOPIC);
        GATextFieldsSetEnability();
        algorithmComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String old_val, String new_val) {
                GATextFieldsSetEnability();
            }
        });
        geneticAlgorithmPopulation = new ArrayList<>();
        readyNotScheduledTasksGA = new ArrayList<>();
        readyNotScheduledPrecedingTasksAmountsGA = new ArrayList<>();
        maxCostTextField.setDisable(true);
        checkMaxCostCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov, Boolean old_val, Boolean new_val) {
                    maxCostTextFieldSetEnability();
            }
        });
        
        breakButton.setDisable(true);
        
        this.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent we) {
                we.consume();
            }        
        });
    }
    
    private int getMaxCost() {
        try {
            if(checkMaxCostCheckBox.isSelected()) {
                int maxCost = Integer.parseUnsignedInt(maxCostTextField.getText().trim());
                int minPossibleCost = 0;
                for(WorkflowTask task : scenarioTasksList) {
                    int minTaskCost = Integer.MAX_VALUE;
                    for(WorkflowResource resource : scenarioResourcesList) {
                        if(resource.acceptsCategory(task.getCategory())) {
                            int taskCostOnResource = resource.getTaskExecutionCost(task);
                            if(taskCostOnResource < minTaskCost) {
                                minTaskCost = taskCostOnResource;
                            }
                        }
                    }
                    minPossibleCost += minTaskCost;
                }
                if(maxCost < minPossibleCost) {
                    scenarioCostLabel.setText("Podany koszt maksymalny jest mniejszy niż najmniejszy możliwy koszt, który wynosi " + minPossibleCost);
                    return -2;
                } else
                    return maxCost;               
            } 
        } catch (NumberFormatException ex) {
            scenarioCostLabel.setText("Koszt maksymalny musi być dodatnią liczbą całkowitą");
            return -2;
        }
        return -1;
    }
    
    private int getGAPopulationSize() {
        int GAPopulationSize = -1;
        try {
            if(algorithmComboBox.getValue().equals(GENETIC)) {
                GAPopulationSize = Integer.parseUnsignedInt(populationSizeTextField.getText().trim());
                if(GAPopulationSize != 0)
                    return GAPopulationSize; 
            } 
        } catch (NumberFormatException ex) {
            GAPopulationSize = 0;
        }
        if(GAPopulationSize == 0) {
            scenarioCostLabel.setText("Wielkość populacji musi być dodatnią liczbą całkowitą");
            return -2;
        }
        return -1;
    }
    
    private int getGAIterationsAmount() {
        int GAIterationsAmount = -1;
        try {
            if(algorithmComboBox.getValue().equals(GENETIC)) {
                GAIterationsAmount = Integer.parseUnsignedInt(iterationsAmountTextField.getText().trim());
                if(GAIterationsAmount != 0)
                    return GAIterationsAmount; 
            } 
        } catch (NumberFormatException ex) {
            GAIterationsAmount = 0;
        }
        if(GAIterationsAmount == 0) {
            scenarioCostLabel.setText("Ilość iteracji musi być dodatnią liczbą całkowitą");
            return -2;
        }
        return -1;
    }
    
    @FXML
    private void executeButtonAction(ActionEvent event) {
        elapsedTimeLabel.setText(" ");
        scenarioCostLabel.setText(" ");
        int GAIterationsAmount = getGAIterationsAmount();
        int GAPopulationSize = getGAPopulationSize();
        int maxCost = getMaxCost();
        if(GAIterationsAmount != -2 && GAPopulationSize != -2 && maxCost != -2) {                            
            disableComponents();                    
            readyNotScheduledOrFailedTasksWaiting.drainPermits();  
            readyNotScheduledTasks.clear();
            resourcesToExecuteOnOrFailedTasks.clear();
            for(int i=0; i<scenarioTasksList.size(); i++) {
                scenarioTasksList.get(i).setExecutionState(WorkflowTask.TASK_NOT_EXECUTED);
                scenarioTasksList.get(i).removeAllFailedTasks();
                notExecutedPrecedingTasksAmounts.set(i, scenarioTasksList.get(i).getPrecedingTasksAmount());
                if(scenarioTasksList.get(i).getPrecedingTasksAmount() == 0) {                    
                    readyNotScheduledTasks.add(scenarioTasksList.get(i));
                    readyNotScheduledOrFailedTasksWaiting.release();                    
                }
            }
            scenarioController.drawSchemeCanvas();
            if(algorithmComboBox.getValue().equals(GENETIC)) {
                ArrayList<Integer> availableResources = new ArrayList<>();
                Random generator = new Random();
                geneticAlgorithmPopulation.clear();
                for(int i=0; i<GAPopulationSize; i++) {
                    GAMapping mapping = new GAMapping(this);
                    readyNotScheduledPrecedingTasksAmountsGA.clear();
                    for(int j=0; j<scenarioTasksList.size(); j++) {
                        readyNotScheduledPrecedingTasksAmountsGA.add(scenarioTasksList.get(j).getPrecedingTasksAmount());
                        if(scenarioTasksList.get(j).getPrecedingTasksAmount() == 0) {
                            readyNotScheduledTasksGA.add(j);
                        }
                    }
                    for(int j=0; j<scenarioTasksList.size(); j++) {
                        int taskIndexInNotScheduledTaskList = generator.nextInt(readyNotScheduledTasksGA.size());
                        WorkflowTask task = scenarioTasksList.get(readyNotScheduledTasksGA.remove(taskIndexInNotScheduledTaskList));                        
                        availableResources.clear();
                        for(WorkflowResource resource : scenarioResourcesList) {
                            if(resource.acceptsCategory(task.getCategory())) {
                                availableResources.add(resource.getIndexInScenario());
                            }
                        }
                        int chosenResourceIndex = availableResources.get(generator.nextInt(availableResources.size()));
                        mapping.addAssignment(new GATaskResourceAssingment(
                                        task.getIndexInScenario(),
                                        chosenResourceIndex
                                )
                        );
                        for(int followingTaskIndex : task.getFollowingTasksArray()) {
                            readyNotScheduledPrecedingTasksAmountsGA.set(followingTaskIndex, 
                                    readyNotScheduledPrecedingTasksAmountsGA.get(followingTaskIndex) - 1);
                            if(readyNotScheduledPrecedingTasksAmountsGA.get(followingTaskIndex) == 0)
                                readyNotScheduledTasksGA.add(followingTaskIndex);
                        }
                    }
                    mapping.calculateTimeAndCost();
                    geneticAlgorithmPopulation.add(mapping);
                }
                bestMapping = new GAMapping(geneticAlgorithmPopulation.get(generator.nextInt(geneticAlgorithmPopulation.size())));
                for(int i=0; i<GAIterationsAmount; i++) {
                    //CROSSOVER
                    GAMapping parent1 = geneticAlgorithmPopulation.get(generator.nextInt(geneticAlgorithmPopulation.size()));
                    GAMapping parent2 = geneticAlgorithmPopulation.get(generator.nextInt(geneticAlgorithmPopulation.size()));
                    int crossoverStartPoint = generator.nextInt(parent1.getAssignmentsAmount());
                    int crossoverEndPoint = generator.nextInt(parent1.getAssignmentsAmount());
                    if(crossoverEndPoint != crossoverStartPoint) {
                        if(crossoverEndPoint > crossoverStartPoint) {
                            int temp = crossoverStartPoint;
                            crossoverStartPoint = crossoverEndPoint;
                            crossoverEndPoint = temp;
                        }
                        GAMapping offspring1 = new GAMapping(parent1);
                        GAMapping offspring2 = new GAMapping(parent2);
                        for(int j=crossoverStartPoint; j<crossoverEndPoint; j++) {
                            GATaskResourceAssingment assignment1 = parent1.getAssignment(j);
                            int assignment2Index = parent2.getIndexOfAssignmentForTask(assignment1.getTaskIndex());
                            GATaskResourceAssingment assignment2 = parent2.getAssignment(assignment2Index);
                            offspring1.replaceAssignment(j, assignment2);
                            offspring2.replaceAssignment(assignment2Index, assignment1);
                        }
                        offspring1.calculateTimeAndCost();
                        geneticAlgorithmPopulation.add(offspring1);
                        offspring2.calculateTimeAndCost();
                        geneticAlgorithmPopulation.add(offspring2);
                    }
                    for (int j=0; j<geneticAlgorithmPopulation.size(); j++) {
                        GAMapping mutatedMapping = new GAMapping(geneticAlgorithmPopulation.get(j));
                        //MUTATION
                        int assignmentIndex = generator.nextInt(mutatedMapping.getAssignmentsAmount());
                        GATaskResourceAssingment assignment = mutatedMapping.getAssignment(assignmentIndex);
                        availableResources.clear();
                        WorkflowTaskCategory category = scenarioTasksList.get(assignment.getTaskIndex()).getCategory();
                        for(WorkflowResource resource : scenarioResourcesList) {                            
                            if(resource.acceptsCategory(category)) {
                                availableResources.add(resource.getIndexInScenario());
                            }
                        }
                        int chosenResourceIndex = availableResources.get(generator.nextInt(availableResources.size()));
                        if(chosenResourceIndex != assignment.getResourceIndex()) {
                            assignment.setResourceIndex(chosenResourceIndex);
                            mutatedMapping.calculateTimeAndCost();
                            geneticAlgorithmPopulation.set(j, mutatedMapping);
                        }
                        GAMapping mapping = geneticAlgorithmPopulation.get(j);
                        if(mapping.getTime() <= bestMapping.getTime()) {
                            if(mapping.getTime() < bestMapping.getTime() || mapping.getCost() < bestMapping.getCost()) {
                                if(maxCost == -1 || mapping.getCost() <= maxCost)
                                    bestMapping = new GAMapping(mapping);
                            }
                        }                           
                    }
                    //ROULETTE WHEEL ELITISM
                    while(geneticAlgorithmPopulation.size() > GAPopulationSize) {
                        long timeSum = 0L;
                        for(GAMapping mapping : geneticAlgorithmPopulation) {
                            timeSum += mapping.getTime();
                        }
                        TreeMap<Double, Integer> fitnessValuesAndIndexes = new TreeMap(Collections.reverseOrder());
                        for(int j = 0; j<geneticAlgorithmPopulation.size(); j++) {
                            fitnessValuesAndIndexes.put(
                                    ((Long)geneticAlgorithmPopulation.get(j).getTime()).doubleValue() / timeSum, j
                            );
                        }
                        Double mappingToRemoveChoise = generator.nextDouble();
                        int mappingToRemoveIndex = 0;
                        Double currentFitnessValueSum = 0.0;
                        for(Double fitnessValue : fitnessValuesAndIndexes.keySet()) {
                            currentFitnessValueSum += fitnessValue;
                            mappingToRemoveIndex = fitnessValuesAndIndexes.get(fitnessValue);
                            if(currentFitnessValueSum > mappingToRemoveChoise)
                                break;  
                        }
                        geneticAlgorithmPopulation.remove(mappingToRemoveIndex);
                    }
                }
                if(maxCost != -1 && bestMapping.getCost() > maxCost) {
                    scenarioCostLabel.setText("Algorytm nie znalazł zaplanowania spełniającego limit kosztu");
                    scenarioScheduler = null;
                    enableComponents();
                } else {
                    scenarioScheduler = new ScenarioSchedulerGA(
                        this,
                        scenarioTasksList.size(),
                        readyNotScheduledOrFailedTasksWaiting,
                        readyNotScheduledTasks,
                        resourcesToExecuteOnOrFailedTasks,
                        bestMapping);
                }
            } else {
                scenarioScheduler = new ScenarioSchedulerHeuristic(
                    this,                    
                    scenarioTasksList.size(),
                    readyNotScheduledOrFailedTasksWaiting,
                    readyNotScheduledTasks,
                    resourcesToExecuteOnOrFailedTasks,
                    maxCost, 
                    (String)algorithmComboBox.getValue());
            }
            if(scenarioScheduler != null) {
                executors = Executors.newFixedThreadPool(2);            
                executors.execute(scenarioScheduler);
                scenarioExecutor = new ScenarioExecutor(
                        this, 
                        scenarioTasksList.size(), 
                        resourcesToExecuteOnOrFailedTasks);
                executors.execute(scenarioExecutor);
                breakButton.setDisable(false);   
            }
        }
    }

    @FXML
    private void breakButtonAction(ActionEvent event) {
       breakButton.setDisable(true);
       new Thread(new Runnable() {
            @Override
            public void run() {
                breakScenario(); 
            }
       }).start();       
    }    
    
    private void disableComponents() {
        algorithmComboBox.setDisable(true);
        executeButton.setDisable(true);
        closeButton.setDisable(true);
        checkMaxCostCheckBox.setDisable(true);
        maxCostTextField.setDisable(true);
        populationSizeTextField.setDisable(true);
        iterationsAmountTextField.setDisable(true);        
    }
    
    private void enableComponents() {
        algorithmComboBox.setDisable(false);
        executeButton.setDisable(false);
        closeButton.setDisable(false);
        checkMaxCostCheckBox.setDisable(false);
        maxCostTextFieldSetEnability();
        GATextFieldsSetEnability();
    }
    
    private void breakScenario() {
        try {
            executors.shutdownNow();            
            executors.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);               
            for(int i=0; i<scenarioTasksList.size(); i++) {
                WorkflowTask task = scenarioTasksList.get(i);
                if(task.getResourceIndex() != -1) {
                    WorkflowResource resource = scenarioResourcesList.get(task.getResourceIndex());
                    resource.setAvailabilityTimeAtCurrentMoment();
                    resource.clearTasks();
                    scenarioScheduler.reduceCost(resource.getTaskExecutionCost(task));                        
                    setTaskResourceIndex(i, -1);
                    setTaskExecutionState(task.getIndexInScenario(), WorkflowTask.TASK_NOT_EXECUTED);
                }
            }
            finishExecution();
        } catch (InterruptedException ex) {
            Logger.getLogger(ExecuteScenarioDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void maxCostTextFieldSetEnability() {
        if(checkMaxCostCheckBox.isSelected()) {
            maxCostTextField.setDisable(false);
        } else {
            maxCostTextField.setDisable(true);
        }
    }
    
    private void GATextFieldsSetEnability() {
        if(algorithmComboBox.getValue().equals(GENETIC)) {
            populationSizeTextField.setDisable(false);
            iterationsAmountTextField.setDisable(false);
        } else {
            populationSizeTextField.setDisable(true);
            iterationsAmountTextField.setDisable(true);
        }
    }
    
    public WorkflowResource[] getResourcesArray() {
        WorkflowResource[] array = new WorkflowResource[scenarioResourcesList.size()];
        scenarioResourcesList.toArray(array);
        return array;
    }
    
    public int getResourcesAmount() {
        return scenarioResourcesList.size();
    }
    
    public int getTasksAmount() {
        return scenarioTasksList.size();
    }
    
    public WorkflowTask getTask(int taskIndex) {
        return scenarioTasksList.get(taskIndex);
    }
    
    public WorkflowResource getResource(int resourceIndex) {
        return scenarioResourcesList.get(resourceIndex);
    }
    
    //after task execution this method is executed for each following task    
    public void tryRunFollowingTask(int followingTaskIndex) {
        if(notExecutedPrecedingTasksAmounts.decrementAndGet(followingTaskIndex) == 0) {
            readyNotScheduledTasks.add(scenarioTasksList.get(followingTaskIndex));
            readyNotScheduledOrFailedTasksWaiting.release();
        }
    }
    
    public void stopWaitingForFailedTask() {
        readyNotScheduledOrFailedTasksWaiting.release();
    }
    
    public void drawLaterSchemeCanvas() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {                
                scenarioController.drawSchemeCanvas();
            }
        });
    }
    
    public void setTaskExecutionState(int taskIndex, int taskExecutionState) {
        scenarioTasksList.get(taskIndex).setExecutionState(taskExecutionState);
        drawLaterSchemeCanvas();
    }
    
    public void setTaskResourceIndex(int taskIndex, int resourceIndex) {
        scenarioTasksList.get(taskIndex).setResourceIndex(resourceIndex);
        drawLaterSchemeCanvas();
    }
    
    //reduce cost by subtracting tasks already in execution after execution fail or break by user
    public void reduceCost(WorkflowTask task) {
        WorkflowResource resource = scenarioResourcesList.get(task.getResourceIndex());
        scenarioScheduler.reduceCost(resource.getTaskExecutionCost(task));
    }
    
    public void finishExecution() {  
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                elapsedTimeLabel.setText("Czas wykonania: " + (((double)(scenarioExecutor.getTime())) / 1000) + "s");
                scenarioCostLabel.setText("Koszt wykonania: " + scenarioScheduler.getCost());
            }
        });
        
        enableComponents();
        breakButton.setDisable(true);
    }
}
