/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

/**
 *
 * @author gbl
 */
public class WorldCoordinate {
    private int world;
    private int x, y, z;
    
    public WorldCoordinate(String world, int x, int y, int z) {
        this.world=WorldRegistry.worldIndex(world);
        this.x=x;
        this.y=y;
        this.z=z;
    }
    
    public String getWorld() {
        return WorldRegistry.worldName(world);
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getWorldIndex() { return world; }
    
    @Override
    public String toString() {
        return WorldRegistry.worldName(world) + " - "+x+"/"+y+"/"+z;
    }
}
