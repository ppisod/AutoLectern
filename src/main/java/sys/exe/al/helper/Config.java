package sys.exe.al.helper;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.include.com.google.common.base.Charsets;
import sys.exe.al.ALAutoTrade;
import sys.exe.al.ALGoal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Config {

    private final Logger logger;

    public boolean ItemSync = false;
    public boolean BreakCooldown = false;
    public boolean LogTrade = false;
    public boolean PreBreak = true;
    public boolean PreserveTool = true;
    public boolean AutoRemove = false;
    public ALAutoTrade AutoTrade = ALAutoTrade.OFF;
    public ArrayList<ALGoal> Goals = new ArrayList<>();

    public File file;

    public Config (File fileToLoadFrom) {
        file = fileToLoadFrom;
        logger = LoggerFactory.getLogger("Auto Lectern");
        try {
            try (final var bufferedReader = Files.newReader(fileToLoadFrom, Charsets.UTF_8)) {
                final var splitter = Splitter.on('=').limit(2);
                final var lineIterator = bufferedReader.lines().iterator();
                while (lineIterator.hasNext()) {
                    final var line = lineIterator.next();
                    try {
                        final var eIt = splitter.split(line).iterator();
                        final var key = eIt.next();
                        final var value = eIt.next();
                        switch(key) {
                            case "breakCooldown" -> BreakCooldown = (value.equals("true"));
                            case "itemSync" -> ItemSync = (value.equals("true"));
                            case "preserveTool" -> PreserveTool = (value.equals("true"));
                            case "logTrade" -> LogTrade = (value.equals("true"));
                            case "preBreak" -> PreBreak = (value.equals("true"));
                            case "autoRemove" -> AutoRemove = (value.equals("true"));
                            case "autoTrade" -> AutoTrade = value.equals("ENCHANT") ? ALAutoTrade.ENCHANT : (value.equals("CHEAPEST") ? ALAutoTrade.CHEAPEST : ALAutoTrade.OFF);
                            case "goals" -> {
                                final var gIt = Splitter.on(';').split(value).iterator();
                                final var COMMA_SPLITTER = Splitter.on(',');
                                while (gIt.hasNext()) {
                                    final var goalData = gIt.next();
                                    if(goalData.isEmpty())
                                        continue;
                                    final var gdIt = COMMA_SPLITTER.split(goalData).iterator();
                                    Goals.add(new ALGoal(Identifier.of(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next())));
                                }
                            }
                        }
                    } catch (Exception innerException) {
                        logger.warn("Skipping bad option: {}", line);
                    }
                }
            }
        } catch (final Exception exception) {
            logger.error("Failed to load the config file!!");
        }
    }

    public void save () {
        logger.info("Saving config...");
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.write("itemSync=");
            pw.write(ItemSync ? "true\n" : "false\n");
            pw.write("breakCooldown=");
            pw.write(BreakCooldown ? "true\n" : "false\n");
            pw.write("logTrade=");
            pw.write(LogTrade ? "true\n" : "false\n");
            pw.write("preBreak=");
            pw.write(PreBreak ? "true\n" : "false\n");
            pw.write("preserveTool=");
            pw.write(PreserveTool ? "true\n" : "false\n");
            pw.write("autoRemove=");
            pw.write(AutoRemove ? "true\n" : "false\n");
            pw.write("autoTrade=");
            pw.write(AutoTrade.name());
            pw.write('\n');
            pw.write("goals=");
            for(final var goal : Goals) {
                pw.write(goal.enchant().toString());
                pw.write(',');
                pw.print(goal.lvlMin());
                pw.write(',');
                pw.print(goal.lvlMax());
                pw.write(',');
                pw.print(goal.priceMin());
                pw.write(',');
                pw.print(goal.priceMax());
                pw.write(';');
            }
        } catch (final Exception exception) {
            logger.error("Failed to save config.", exception);
            return;
        }
        logger.info("Config saved!");
    }

}
