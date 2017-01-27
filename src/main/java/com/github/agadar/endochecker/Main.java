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
     * Prompts the user with the supplied open (non-binary) question, and
     * returns the user's answer.
     *
     * @param scanIn The scanner used for reading the user's answer.
     * @param question The question to prompt the user.
     * @return The user's answer.
     */
    private static String PromptQuestion(Scanner scanIn, String question) {
        System.out.print(question + ": ");
        return scanIn.nextLine();
    }

    /**
     * Prompts the user with the supplied question, which should expect a binary
     * answer (yes or no), and returns whether the user answered yes.
     *
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
     * Returns whether or not the nation to which the supplied WA status string
     * belongs to is a member of the WA.
     * @param waStatusString The WA status of a nation.
     * @return Whether or not the owning nation is a WA member.
     */
    private static boolean IsWAMember(String waStatusString) {
        return waStatusString != null && (waStatusString.equals("WA Member")
                || waStatusString.equals("WA Delegate"));
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NSAPI.setUserAgent("Agadar's Endorsements Checker "
                + "(https://github.com/Agadar/Endorsements-Checker)");
        final Scanner scanIn = new Scanner(System.in);

        // Retrieve region name and nation name from user.
        final String nationToCheck = PromptQuestion(scanIn,
                "Nation whose endorsements to check").toLowerCase()
                .replace(' ', '_');

        // Retrieve the nation's home region from the API. While we're at it,
        // also prematurely fetch the nation's endorsedby list and his
        // endorsements status.
        final Nation nationToCheckObj = NSAPI.nation(nationToCheck)
                .shards(NationShard.WorldAssemblyStatus, NationShard.RegionName, 
                        NationShard.EndorsedBy).execute();

        // Ensure the nation exists, aborting if it does not.
        if (nationToCheckObj == null) {
            System.err.println("The specified nation '" + nationToCheck + 
                    "' was not found!");
            return;
        }
        
        // Ensure the nation is actually a member of the WA, otherwise
        // there is no point in continuing.
        if (!IsWAMember(nationToCheckObj.WorldAssemblyStatus)) {
            System.err.println("The specified nation '" + nationToCheck +
                    "' is not a WA member!");
            return;
        }

        // Retrieve nations list of specified region.
        final Region region = NSAPI.region(nationToCheckObj.RegionName)
                .shards(RegionShard.NationNames).execute();
        
        // Ensure the region exists, aborting if it does not.
        if (region == null) {
            System.err.println("The region in which the specified nation resides"
                    + " was not found!");
            return;
        }
        
        // The list to which we'll be adding all nations in the region that
        // are members of the world assembly, except for the nation to check.
        final List<String> waNations = new ArrayList<>();
        // The list to which we'll be adding all nations in the region that
        // are members of the world assembly, except for the nation to check,
        // and which are not already endorsing the nation to check even though
        // the nation to check has already endorsed them.
        //
        // The user will be offered to send telegrams to these nations,
        // requesting them to endorse the nation to check. The user will
        // have to manually supply the necessary values for sending telegrams.
        final List<String> waNationsNotEndorsing = new ArrayList<>();
        // The list to which we'll be adding all nations in the region that
        // are members of the world assembly, except for the nation to check,
        // and which are not already being endorsed by the nation to check.
        //
        // These nations will be opened in the browser tabs so that the user
        // can endorse them manually (auto-endorsing is forbidden).
        final List<String> waNationsNotYetEndorsed = new ArrayList<>();

        // For each nation in the region...
        for (String curNation : region.NationNames) {
            // If this is the nation we're meant to check, just continue.
            if (curNation.equals(nationToCheck)) { continue; }
            
            // Retrieve nation's WA status and endorsedby list.
            final Nation curNationObj = NSAPI.nation(curNation).shards(
                    NationShard.WorldAssemblyStatus, NationShard.EndorsedBy)
                    .execute();

            // If the nation exists and is a WA member, then add it to the
            // world assembly members list.
            if (curNationObj != null && IsWAMember(curNationObj.WorldAssemblyStatus)) {
                waNations.add(curNation);
                
                // If the nation is not already being endorsed by the nation to
                // check, then add it to waNationsNotYetEndorsed.
                if (!curNationObj.EndorsedBy.contains(nationToCheck)) {
                    waNationsNotYetEndorsed.add(curNation);
                }               
                // Else, if the nation is not endorsing the nation to check, then add
                // it to waNationsNotEndorsing.
                else if (!nationToCheckObj.EndorsedBy.contains(curNation)) {
                    waNationsNotEndorsing.add(curNation);
                }
            }
        }

        // Print info.
        System.out.println("WA members in the region: " + waNations.size());
        System.out.println("...that are endorsed by the specified nation,"
                + " but are not endorsing back: " + waNationsNotEndorsing.size());
        System.out.println("...that are NOT yet endorsed by the specified nation: "
                + waNationsNotYetEndorsed.size());

        // If requested, open browser tab for each nation not being endorsed
        // by the nation to check.
        if (PromptYesOrNo(scanIn, "Open browser tab for each nation not "
                + "yet endorsed?")) {
            // Make sure opening browser is supported by the system.
            final Desktop desktop = Desktop.isDesktopSupported()
                    ? Desktop.getDesktop() : null;

            // If it is, open tabs for all nations that the user would like to endorse.
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    // For each nation, open a browser tab.
                    for (String curNation : waNationsNotYetEndorsed) {
                        desktop.browse(new URI("https://"
                                + "www.nationstates.net/nation=" + curNation));
                        // Required sleep, otherwise we get an error on some machines/browsers.
                        Thread.sleep(1000);
                    }
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
                }
            } else {
                // Else, print error and carry on.
                System.err.println("Opening browser tabs is not supported on "
                        + "your Operating System!");
            }
        }
        
        // If requested, send telegrams to each nation already endorsed but not
        // yet endorsing back.
    }
}
