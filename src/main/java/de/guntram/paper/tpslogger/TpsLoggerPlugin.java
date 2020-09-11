package de.guntram.paper.tpslogger;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TpsLoggerPlugin extends JavaPlugin {

    static final int millisPerInterval=5000;
    
    private Logger logger;
    File outputFile, logFile;
    boolean tpsFailureReported, logFailureReported;
    FloatingAverage avg1, avg5, avg15;
    FloatingAverage mspt;
    long currentTimeInterval;
    int  tickCount;
    boolean firstPartialTick;
    DateTimeFormatter logFormat, logFilePrepend;
    
    @Override
    public void onEnable() {
        logger=getLogger();
        saveDefaultConfig();
        outputFile=new File(getDataFolder(), "currenttps.txt");
        tpsFailureReported=false;
        logFailureReported=false;
        
        avg1=new FloatingAverage(6);
        avg5=new FloatingAverage(5*6);
        avg15=new FloatingAverage(15*6);
        mspt=null;

        currentTimeInterval=0;
        new BukkitRunnable(){
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(this, 100l, 1l);

        logFormat = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm:ss")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());
        
        logFilePrepend = DateTimeFormatter
                .ofPattern("uuuu-MM-dd")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault());
        
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerTickEndEvent");
            mspt = new FloatingAverage(100);
            getServer().getPluginManager().registerEvents(new MSPTLogger(mspt), this);
        } catch (ClassNotFoundException ex) {
            logger.warning("Paper server not detected, will not log mspt");
        }
    }
    
    @Override
    public void onDisable() {
        try (FileWriter f = new FileWriter(outputFile)) {
            f.write("0.0\n0.0\n0.0\n");
            if (mspt != null) {
                f.write("MSPT:0.0\n");
            }
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
                        f.write(""+avg1.getAverage() / millisPerInterval * 1000+"\n"
                                  +avg5.getAverage() / millisPerInterval * 1000+"\n"
                                  +avg15.getAverage() / millisPerInterval * 1000+"\n");
                        if (mspt != null) {
                            f.write("MSPT:"+mspt.getAverage()+"\n");
                        }
                        tpsFailureReported=false;
                    } catch (IOException ex) {
                        if (!tpsFailureReported) {
                            logger.log(Level.SEVERE, "TPSLogger can't write TPS file", ex);
                            tpsFailureReported=true;
                        }
                    }
                }
            }.start();

            List<String> playerInfo = new ArrayList<>();
            for (Player player: getServer().getOnlinePlayers()) {
                Location loc = player.getLocation();
                String worldName = loc.getWorld().getName();
                String logEntry = String.format(" --- %-20.20s %-10.10s %5d %5d %5d", player.getName(), worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                playerInfo.add(logEntry);
            }
            new Thread() {
                @Override
                public void run() {
                    logFile = new File(getDataFolder(), "msptlog-"+logFilePrepend.format(Instant.now())+".log");
                    try (FileWriter f = new FileWriter(logFile, true)) {
                        Collections.sort(playerInfo);
                        double msptAverage = (mspt == null ? Double.NaN : mspt.getAverage());
                        f.write(logFormat.format(Instant.now())+String.format(" %7.2f %7.2f ",
                                avg1.getAverage() / millisPerInterval * 1000,
                                msptAverage));
                        for (String s: playerInfo) {
                            f.write(s);
                        }
                        f.write(System.lineSeparator());
                    } catch (IOException ex) {
                        if (!logFailureReported) {
                            logger.log(Level.SEVERE, "TPSLogger can't write TPS file", ex);
                            logFailureReported=true;
                        }
                    }
                    
                    for (File file: getDataFolder().listFiles()) {
                        if (file.getName().startsWith("msptlog")
                        &&  file.lastModified() < System.currentTimeMillis() - 86400 * 1000 * 7) {
                            file.delete();
                        }
                    }
                }
            }.start();
        }
        tickCount++;
    }
}
