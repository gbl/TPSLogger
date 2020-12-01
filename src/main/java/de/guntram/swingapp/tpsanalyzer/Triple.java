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

// Don't want to use javafx.util.pair because server JRE's don't have javafx.

public class Triple<K,V,S> {
    K left;
    V right;
    S note;
    
    Triple(K k, V v, S s) {
        left = k;
        right = v;
        note = s;
    }
    
    K getLeft() {
        return left;
    }
    
    V getRight() {
        return right;
    }
    
    S getNote() {
        return note;
    }
}
