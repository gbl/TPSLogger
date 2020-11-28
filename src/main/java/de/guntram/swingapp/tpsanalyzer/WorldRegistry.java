/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gbl
 */
public class WorldRegistry {
    
    static List<String> registry = new ArrayList<>();
    
    public static int worldIndex(String worldName) {
        int index = registry.indexOf(worldName);
        if (index >= 0) {
            return index;
        }
        registry.add(worldName);
        return registry.size()-1;
    }
    
    public static String worldName(int worldIndex) {
        return registry.get(worldIndex);
    }
}
