/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.tpslogger;

/**
 *
 * @author gbl
 */
public class FloatingAverage {
    
    private double values[];
    private double currentSum;
    private int curPos;
    boolean full;
    
    public FloatingAverage(int nValues) {
        values=new double[nValues];
        currentSum=0;
        curPos=0;
        full=false;
    }
    
    public void append(double value) {
        currentSum = currentSum - values[curPos] + value;
        values[curPos++]=value;
        if (curPos==values.length) {
            full=true;
            curPos=0;
        }
    }
    
    public double getAverage() {
        return currentSum / (full ? values.length : curPos);
    }
}
