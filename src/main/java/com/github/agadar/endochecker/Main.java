package com.github.agadar.endochecker;

import com.github.agadar.nationstates.NationStates;
import com.github.agadar.nationstates.domain.nation.Nation;
import com.github.agadar.nationstates.domain.region.Region;
import com.github.agadar.nationstates.enumerator.WorldAssemblyStatus;
import com.github.agadar.nationstates.shard.NationShard;
import com.github.agadar.nationstates.shard.RegionShard;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
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
     * Returns whether or not the nation to which the supplied WA status belongs
     * to is a member/delegate of the WA.
     *
     * @param worldAssemblyStatus The WA status of a nation.
     * @return Whether or not the owning nation is a WA member/delegate.
     */
    private static boolean IsWAMember(WorldAssemblyStatus worldAssemblyStatus) {
        return worldAssemblyStatus != null && (worldAssemblyStatus == WorldAssemblyStatus.MEMBER
                || worldAssemblyStatus == WorldAssemblyStatus.DELEGATE);
    }

    /**
     * Prints a message to System.err and exits the program with error code 1.
     *
     * @param errorMsg The error message to print.
     */
    private static void ExitWithError(String errorMsg) {
        System.err.println(errorMsg);
        System.exit(1);
    }

    /**
     * Gives the correct URL to the supplied nation's page.
     *
     * @param nationName
     * @return
     */
    private static String BuildNationUrl(String nationName) {
        return "https://www.nationstates.net/nation=" + nationName;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Turn off the default logging so we don't spam the CLI.
        LogManager.getLogManager().reset();

        // Make sure opening browser tabs is supported by the system. If not, print a warning.
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        final boolean browserSupport = desktop != null && desktop.isSupported(Desktop.Action.BROWSE);
        if (!browserSupport) {
            System.out.println("Warning: Opening browser tabs is not supported on your Operating System."
                    + " This program will not be able to open browser tabs for you.");
        }

        // Set user agent for API, instantiate scanner for user input.
        NationStates.setUserAgent("Agadar's Endorsements Checker "
                + "(https://github.com/Agadar/Endorsements-Checker)");
        final Scanner scanIn = new Scanner(System.in);

        // Retrieve region name and nation name from user.
        final String nationToCheck = PromptQuestion(scanIn,
                "Nation whose endorsements to check").toLowerCase()
                .replace(' ', '_');

        // Retrieve the nation's home region from the API. While we're at it,
        // also prematurely fetch the nation's endorsedby list and his
        // endorsements status.
        final Nation nationToCheckObj = NationStates.nation(nationToCheck)
                .shards(NationShard.WORLD_ASSEMBLY_STATUS, NationShard.REGION_NAME,
                        NationShard.ENDORSED_BY).execute();

        // Ensure the nation exists, aborting if it does not.
        if (nationToCheckObj == null) {
            ExitWithError("The nation '" + nationToCheck + "' was not found!");
        }

        // Ensure the nation is actually a member of the WA, otherwise
        // there is no point in continuing.
        if (!IsWAMember(nationToCheckObj.worldAssemblyStatus)) {
            ExitWithError("The nation '" + nationToCheck + "' is not a WA member!");
        }

        // Retrieve nations list of specified region.
        final Region region = NationStates.region(nationToCheckObj.regionName)
                .shards(RegionShard.NATION_NAMES).execute();

        // Ensure the region exists, aborting if it does not.
        if (region == null) {
            ExitWithError("The region in which the nation resides was not found!");
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

        // Print info.
        System.out.println("Number of nations in region '" + nationToCheckObj.regionName + "': " + region.nationNames.size());
        System.out.println("Estimated duration: ~" + (int) ((float) region.nationNames.size() * 0.6010f) + " seconds" + System.lineSeparator());
        System.out.println("Retrieving nations data from NationStates API...");

        // For each nation in the region...
        for (int progress = 0; progress < region.nationNames.size(); progress++) {
            final String curNation = region.nationNames.get(progress);

            // Skip this nation if it's the nation to check.
            if (curNation.equals(nationToCheck)) {
                continue;
            }

            // Retrieve nation's WA status and endorsedby list.
            final Nation curNationObj = NationStates.nation(curNation).shards(
                    NationShard.WORLD_ASSEMBLY_STATUS, NationShard.ENDORSED_BY)
                    .execute();

            // If the nation exists and is a WA member, then add it to the
            // world assembly members list.
            if (curNationObj != null && IsWAMember(curNationObj.worldAssemblyStatus)) {
                waNations.add(curNation);

                // If the nation is not already being endorsed by the nation to
                // check, then add it to waNationsNotYetEndorsed.
                if (!curNationObj.endorsedBy.contains(nationToCheck)) {
                    waNationsNotYetEndorsed.add(curNation);
                } // Else, if the nation is not endorsing the nation to check, then add
                // it to waNationsNotEndorsing.
                else if (!nationToCheckObj.endorsedBy.contains(curNation)) {
                    waNationsNotEndorsing.add(curNation);
                }
            }

            // Print progress, but don't spam it.
            if ((progress + 1) % 2 == 0) {
                System.out.println("Retrieved " + (progress + 1) + "/" + region.nationNames.size() + " nations data...");
            }
        }

        // Print info.
        System.out.println("...Finished retrieving data." + System.lineSeparator());
        System.out.println("WA members in the region: " + waNations.size());
        System.out.println("...that are endorsed by '" + nationToCheck + "',"
                + " but are not endorsing back: " + waNationsNotEndorsing.size());
        System.out.println("...that are NOT endorsed by '" + nationToCheck + "': "
                + waNationsNotYetEndorsed.size() + System.lineSeparator());

        // If requested, open browser tab for each nation not being endorsed
        // by the nation to check.
        if (browserSupport && PromptYesOrNo(scanIn, "Open browser tab for each nation not yet endorsed?")) {
            try {
                // For each nation, open a browser tab.
                for (String curNation : waNationsNotYetEndorsed) {
                    desktop.browse(new URI(BuildNationUrl(curNation)));
                    // Required sleep, otherwise we get an error on some machines/browsers.
                    Thread.sleep(1000);
                }
            } catch (URISyntaxException | IOException | InterruptedException e) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            }
        } else if (PromptYesOrNo(scanIn, "Print URLs to each nation not yet endorsed?")) {
            // If the user does not want to have browser tabs opened for all
            // nations to endorse, then offer to print URLS for them instead.
            waNationsNotYetEndorsed.stream().forEach((curNation) -> {
                System.out.println(BuildNationUrl(curNation));
            });
            System.out.println();
        }

        // If requested, open browser tab for each nation endorsed by the specified nation
        // but not endorsing back.
        if (browserSupport && PromptYesOrNo(scanIn, "Open browser tab for each nation already endorsed, but that is not endorsing back?")) {
            try {
                // For each nation, open a browser tab.
                for (String curNation : waNationsNotEndorsing) {
                    desktop.browse(new URI(BuildNationUrl(curNation)));
                    // Required sleep, otherwise we get an error on some machines/browsers.
                    Thread.sleep(1000);
                }
            } catch (URISyntaxException | IOException | InterruptedException e) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            }
        } else if (PromptYesOrNo(scanIn, "Print URLs to each nation already endorsed, but that is not endorsing back?")) {
            // If the user does not want to have browser tabs opened for all
            // nations to endorse, then offer to print URLS for them instead.
            waNationsNotEndorsing.stream().forEach((curNation) -> {
                System.out.println(BuildNationUrl(curNation));
            });
            System.out.println();
        }
    }
}
