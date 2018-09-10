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

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public abstract class WorkflowObject extends ScenarioDescriptionElement {
    protected int indexInScenario;
    protected int positionX;
    protected int positionY;
    protected int sizeOnSchemeX;
    protected int sizeOnSchemeY;

    public WorkflowObject(String owner, String name) {
        super(owner, name);
        this.indexInScenario = -1;
        this.positionX = 0;
        this.positionY = 0;        
        sizeOnSchemeY = 50;
    }
    
    public int getIndexInScenario() {
        return indexInScenario;
    }
    
    public void setIndexInScenario(int indexInScenario) {
        this.indexInScenario = indexInScenario;
    }
    
    public int getSizeOnSchemeX() {
        return sizeOnSchemeX;
    }
    
    public int getSizeOnSchemeY() {
        return sizeOnSchemeY;
    }
    
    public int getPositionX() {
        return positionX;
    }
    
    public int getPositionY() {
        return positionY;
    }
    
    public int getUpMiddlePointX() {
        return positionX + sizeOnSchemeX / 2;
    }
    
    public int getUpMiddlePointY() {
        return positionY;
    }
    
    public int getDownMiddlePointX() {
        return positionX + sizeOnSchemeX / 2;
    }
    
    public int getDownMiddlePointY() {
        return positionY + sizeOnSchemeY;
    }
    
    public void setPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
    }
    
    protected void draw(GraphicsContext gc, Color color, String text) {
        gc.setFill(Color.WHITE);
        gc.setStroke(color);
        gc.fillRect(positionX, positionY, sizeOnSchemeX, sizeOnSchemeY);
        gc.strokeRect(positionX, positionY, sizeOnSchemeX, sizeOnSchemeY);
        Font font = Font.font("Arial", 13);
        gc.setFont(font);
        gc.strokeText(text, positionX + 5, positionY + 25);
    }    
    
    public abstract void draw(GraphicsContext gc);    
}
