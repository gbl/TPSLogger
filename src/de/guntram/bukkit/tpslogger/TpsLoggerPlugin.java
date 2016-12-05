package de.guntram.bukkit.tpslogger;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ess3.api.IEssentials;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class TpsLoggerPlugin extends JavaPlugin implements Runnable {

    private FileConfiguration config;
    private Logger logger;
    private int frequency;
    IEssentials essentials;
    File outputFile;
    boolean failureReported;
    
    static final int TICKSPERSEC=20;
    
    @Override
    public void onEnable() {
        logger=getLogger();
        saveDefaultConfig();
        config=getConfig();
        outputFile=new File(getDataFolder(), "currenttps.txt");
        frequency=config.getInt("frequency", 60*TICKSPERSEC);
        essentials = (IEssentials) getServer().getPluginManager().getPlugin("Essentials");
        failureReported=false;
        this.getServer().getScheduler().scheduleSyncRepeatingTask((Plugin)this, 
                       this, frequency, frequency);        
    }
    

    @Override
    public void run() {
        try {
            FileWriter f=new FileWriter(outputFile);
            f.write(""+essentials.getTimer().getAverageTPS()+"\n");
            f.close();
            failureReported=false;
        } catch (IOException ex) {
            if (!failureReported) {
                logger.log(Level.SEVERE, "TPSLogger can't write log file", ex);
            }
            failureReported=true;
        }
    }
}
