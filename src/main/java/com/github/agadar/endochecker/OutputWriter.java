package com.github.agadar.endochecker;

import java.util.Collection;

/**
 * Assists in printing output to the user.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
public class OutputWriter {

    private final static int MILLISECS_BETWEEN_PROGRESS_PRINTS = 2000;

    private long lastTimestamp = 0;

    public void printNationAndRegionInfo(String regionName, Collection<String> nationNames) {
        System.out.println("Number of nations in region '" + regionName + "': " + nationNames.size());
        System.out.println("Estimated duration: ~" + (int) ((float) nationNames.size() * 0.6010f) + " seconds"
                + System.lineSeparator());
        System.out.println("Retrieving nations data from NationStates API...");
    }

    public void printBrowserNotSupportedWarning() {
        System.out.println("Warning: Opening browser tabs is not supported on your Operating System."
                + " This program will not be able to open browser tabs for you.");
    }

    public void exitWithErrorMsg(String errorMsg) {
        System.err.println(errorMsg);
        System.exit(1);
    }

    public void exitWithNotInWorldAssemblyError(String nationInQuestionName) {
        exitWithErrorMsg("The nation '" + nationInQuestionName + "' is not a World Assembly member!");
    }

    public void printProgressCompletionInfo(EndorsementsCheckResult result) {
        System.out.println("...Finished retrieving data." + System.lineSeparator());
        System.out.println("WA members in the region: " + result.getNationsInWorldAssembly().size());
        System.out.println("...that are endorsed by '" + result.getNationInQuestionName() + "',"
                + " but are not endorsing back: " + result.getNationsNotEndorsingNationInQuestionBack().size());
        System.out.println("...that are NOT endorsed by '" + result.getNationInQuestionName() + "': "
                + result.getNationsNotEndorsedByNationInQuestion().size() + System.lineSeparator());
    }

    public void printNonSpammyProgress(int numberOfNations, int progress) {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastTimestamp > MILLISECS_BETWEEN_PROGRESS_PRINTS) {
            System.out.println("Processed " + progress + "/" + numberOfNations + " nations data...");
            lastTimestamp = currentTimestamp;
        }
    }

    public void printLinesEndingWithNewline(Collection<String> lines) {
        lines.stream().forEach(System.out::println);
        System.out.println();
    }
}
