/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.paper.tpslogger;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author gbl
 */
public class MSPTLogger implements Listener {

    private final FloatingAverage collector;
    
    MSPTLogger(FloatingAverage collector) {
        this.collector=collector;
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent ev) {
        collector.append(ev.getTickDuration());
    }
}
