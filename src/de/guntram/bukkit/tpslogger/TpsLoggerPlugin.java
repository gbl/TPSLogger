package de.guntram.bukkit.tpslogger;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TpsLoggerPlugin extends JavaPlugin {

    static final int millisPerInterval=10000;
    
    private Logger logger;
    File outputFile;
    boolean failureReported;
    FloatingAverage avg1, avg5, avg15;
    long currentTimeInterval;
    int  tickCount;
    boolean firstPartialTick;
    
    @Override
    public void onEnable() {
        logger=getLogger();
        saveDefaultConfig();
        outputFile=new File(getDataFolder(), "currenttps.txt");
        failureReported=false;
        
        avg1=new FloatingAverage(6);
        avg5=new FloatingAverage(5*6);
        avg15=new FloatingAverage(15*6);

        currentTimeInterval=0;
        new BukkitRunnable(){
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(this, 100l, 1l);
    }
    
    @Override
    public void onDisable() {
        try (FileWriter f = new FileWriter(outputFile)) {
            f.write("0.0\n0.0\n0.0\n");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "TPSLogger can't reset log file", ex);
        }
    }


    public void tick() {

        long thisTimeInterval=System.currentTimeMillis()/millisPerInterval;
        if (currentTimeInterval==0) {
            currentTimeInterval=thisTimeInterval;
            firstPartialTick=true;
        }
        if (thisTimeInterval > currentTimeInterval) {
            if (firstPartialTick) {
                tickCount=1;
                firstPartialTick=false;
                currentTimeInterval=thisTimeInterval;
                logger.log(Level.INFO, "TPSLogger starts logging now");
                return;
            }
            avg1.append(tickCount);
            avg5.append(tickCount);
            avg15.append(tickCount);
            currentTimeInterval=thisTimeInterval;
            tickCount=0;
            // Put writing the file into a new thread, because we don't want to
            // wait for the close() operation (which may wait for sync) on the main
            // thread.
            new Thread() {
                @Override
                public void run() {
                    try (FileWriter f = new FileWriter(outputFile)) {
                        f.write(""+avg1.getAverage()/(millisPerInterval / 1000)+"\n"
                                  +avg5.getAverage()/(millisPerInterval / 1000)+"\n"
                                  +avg15.getAverage()/(millisPerInterval / 1000)+"\n");
                        failureReported=false;
                    } catch (IOException ex) {
                        if (!failureReported) {
                            logger.log(Level.SEVERE, "TPSLogger can't write log file", ex);
                            failureReported=true;
                        }
                    }
                }
            }.start();
        }
        tickCount++;
    }
}
