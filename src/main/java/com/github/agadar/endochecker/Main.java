package com.github.agadar.endochecker;

import com.github.agadar.nsapi.NSAPI;
import com.github.agadar.nsapi.domain.nation.Nation;
import com.github.agadar.nsapi.domain.region.Region;
import com.github.agadar.nsapi.enums.shard.NationShard;
import com.github.agadar.nsapi.enums.shard.RegionShard;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry for the CLI version of the endorsements checker.
 * 
 * @author Agadar (https://github.com/Agadar/)
 */
public class Main {

    /**
     * Prompts the user with the supplied question, which should expect a
     * binary answer (yes or no), and returns whether the user answered yes.
     * @param scanIn The scanner used for reading the user's answer.
     * @param question The binary question to prompt the user.
     * @return True if the user answered 'y' or 'yes', otherwise false.
     */
    private static boolean PromptYesOrNo(Scanner scanIn, String question) {
        System.out.print(question + " (y/n?): ");
        final String reply = scanIn.nextLine().toLowerCase();
        return (reply != null && ("y".equals(reply) || "yes".equals(reply)));
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NSAPI.setUserAgent("Agadar's Endorsements Checker "
                + "(https://github.com/Agadar/Endorsements-Checker)");
        final Scanner scanIn = new Scanner(System.in);
        boolean exit = false;
        
        while (!exit) {
            // Retrieve region name and nation name from user.
            System.out.println("\n#### GIVE INPUT ####");
            System.out.print("Region: ");
            final String regionName = scanIn.nextLine().toLowerCase();
            System.out.print("Nation: ");
            final String nationToCheckName = scanIn.nextLine().toLowerCase();
            System.out.println("");
            
            // Retrieve nations list of specified region.
            final Region region = NSAPI.region(regionName)
                    .shards(RegionShard.NationNames).execute();
            if (region == null) {
                System.err.println("Region does not exist!");
                continue;
            }     
            final List<String> waNations = new ArrayList<>();
            List<String> endorsedBy = null;
            
            // Retrieve each nation's WA status.
            for (String nationName : region.NationNames) {
                Nation nation = NSAPI.nation(nationName).shards(
                        NationShard.WorldAssemblyStatus).execute();
                
                // If the nation exists and is a WA member...
                if (nation != null && (nation.WorldAssemblyStatus.equals("WA Member") ||
                        nation.WorldAssemblyStatus.equals("WA Delegate"))) {
                    
                    // If this is not the specified nation, just add it to the nations list.
                    if (!nationName.equals(nationToCheckName)) {
                        waNations.add(nationName);
                    }
                    // Else, retrieve its endorsements by list.
                    else {
                        System.out.println("dicks");
                        nation = NSAPI.nation(nationName).shards(NationShard.EndorsedBy).execute();
                        if (nation == null) {
                            System.err.println("Specified nation was deleted while retrieving its data!");
                            continue;
                        }
                        endorsedBy = nation.EndorsedBy;
                    }
                }
            }
            
            // Ensure the specified nation was found before continuing.
            if (endorsedBy == null) {
                System.err.println("Specified nation was not found or is not a WA member!");
                continue;
            }
            
            // Print info.
            System.out.println("\n#### RESULTS ####");
            System.out.println("# of WA members in region: " + waNations.size());
            System.out.println("# of nations endorsing specified nation: " + endorsedBy.size());
            
            // Filter nations that already vouched for the specified nations from the
            // nations list.
            waNations.removeAll(endorsedBy);
            
            // Print info.
            System.out.println("# of WA members in region not already endorsing "
                    + "specified nation: " + waNations.size());
            System.out.println();
            System.out.println("List:");
            
            // Sort list.
            Collections.sort(waNations);
            
            // Print resulting list.
            if (waNations.size() > 0) {
                String listStr = waNations.get(0);
                
                for (int i = 1; i < waNations.size(); i++) {
                    listStr += ", " + waNations.get(i);
                }
                System.out.println(listStr);
                System.out.println();
            }
                    
            // If requested, open browser tab for each nation.
            if (PromptYesOrNo(scanIn, "Open browser tabs for each nation?")) {
                // Make sure opening browser is supported by the system.
                final Desktop desktop = Desktop.isDesktopSupported() ? 
                        Desktop.getDesktop() : null;
                
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        // For each nation, open a browser tab.
                        for (String nation : waNations) {
                            desktop.browse(new URI("https://"
                                    + "www.nationstates.net/nation=" + nation));
                            // Required sleep, otherwise we get an error.
                            Thread.sleep(1000);
                        }
                    } catch (URISyntaxException | IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.err.println("Opening browser tabs not supported on "
                            + "your Operating System!");
                }
            }
        }
    }
    
}
