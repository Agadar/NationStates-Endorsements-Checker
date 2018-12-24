package com.github.agadar.endochecker;

import java.util.Collection;
import java.util.stream.Collectors;

import com.github.agadar.nationstates.domain.nation.Nation;
import com.github.agadar.nationstates.domain.region.Region;
import com.github.agadar.nationstates.enumerator.WorldAssemblyStatus;
import com.github.agadar.nationstates.exception.NationStatesAPIException;

/**
 * The main component dictating the application flow.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
public class EndorsementsChecker {

    private static final String PRINT_URLS_FOR_NATIONS_NOT_ENDORSING_BACK = "Print URLs to each nation already endorsed, but that is not endorsing back?";
    private static final String OPEN_TABS_FOR_NATIONS_NOT_ENDORSING_BACK = "Open browser tab for each nation already endorsed, but that is not endorsing back?";
    private static final String PRINT_URLS_FOR_UNENDORSED_NATIONS = "Print URLs to each nation not yet endorsed?";
    private static final String OPEN_TABS_FOR_UNENDORSED_NATIONS = "Open browser tab for each nation not yet endorsed?";

    private final DesktopAccess desktopAccess;
    private final OutputWriter outputWriter;
    private final InputReader inputReader;
    private final NationStatesRepository repository;

    private String nationInQuestionName;
    private Nation nationInQuestion;
    private Region regionOfNationInQuestion;
    private int progressCounter = 1;
    private EndorsementsCheckResult checkResult;

    public EndorsementsChecker(DesktopAccess desktopAccess, OutputWriter outputWriter, InputReader inputReader,
            NationStatesRepository repository) {
        this.desktopAccess = desktopAccess;
        this.outputWriter = outputWriter;
        this.inputReader = inputReader;
        this.repository = repository;
    }

    public void start() {

        if (!desktopAccess.isBrowserSupported()) {
            outputWriter.printBrowserNotSupportedWarning();
        }

        nationInQuestionName = inputReader.askUserForNationName();
        try {
            nationInQuestion = repository.retrieveNationInQuestionFromApi(nationInQuestionName);
        } catch (NationStatesAPIException ex) {
            outputWriter.exitWithErrorMsg(ex.getMessage());
            return;
        }

        if (!isInWorldAssembly(nationInQuestion.getWorldAssemblyStatus())) {
            outputWriter.exitWithNotInWorldAssemblyError(nationInQuestionName);
            return;
        }

        regionOfNationInQuestion = repository.retrieveRegionFromApi(nationInQuestion.getRegionName());
        outputWriter.printNationAndRegionInfo(nationInQuestion.getRegionName(),
                regionOfNationInQuestion.getNationNames());

        checkResult = new EndorsementsCheckResult(nationInQuestionName);
        processNations();
        outputWriter.printProgressCompletionInfo(checkResult);

        promptOpenNationsQuestions(checkResult.getNationsNotEndorsedByNationInQuestion(),
                OPEN_TABS_FOR_UNENDORSED_NATIONS, PRINT_URLS_FOR_UNENDORSED_NATIONS);
        promptOpenNationsQuestions(checkResult.getNationsNotEndorsingNationInQuestionBack(),
                OPEN_TABS_FOR_NATIONS_NOT_ENDORSING_BACK, PRINT_URLS_FOR_NATIONS_NOT_ENDORSING_BACK);
    }

    private void processNations() {
        regionOfNationInQuestion.getNationNames().stream().filter(name -> !name.equals(nationInQuestionName))
                .forEach(name -> processNation(name));
    }

    private void processNation(String name) {
        Nation nation = repository.retrieveNationFromApi(name);

        if (isInWorldAssembly(nation.getWorldAssemblyStatus())) {
            checkResult.getNationsInWorldAssembly().add(name);

            if (!nation.getEndorsedBy().contains(nationInQuestionName)) {
                checkResult.getNationsNotEndorsedByNationInQuestion().add(name);

            } else if (!nationInQuestion.getEndorsedBy().contains(name)) {
                checkResult.getNationsNotEndorsingNationInQuestionBack().add(name);
            }
        }
        outputWriter.printNonSpammyProgress(regionOfNationInQuestion.getNationNames().size(), progressCounter);
        progressCounter++;
    }

    private void promptOpenNationsQuestions(Collection<String> nations, String browserQuestion,
            String printUrlsQuestion) {

        if (desktopAccess.isBrowserSupported() && inputReader.promptBinaryQuestion(browserQuestion)) {
            nations.forEach(nation -> {
                try {
                    desktopAccess.openUrlInBrowser(createUrlForNation(nation));
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else if (inputReader.promptBinaryQuestion(printUrlsQuestion)) {
            var nationUrls = nations.stream().map(this::createUrlForNation).collect(Collectors.toList());
            outputWriter.printLinesEndingWithNewline(nationUrls);
        }
    }

    private String createUrlForNation(String nationName) {
        return "https://www.nationstates.net/nation=" + nationName;
    }

    private boolean isInWorldAssembly(WorldAssemblyStatus worldAssemblyStatus) {
        return worldAssemblyStatus != null && (worldAssemblyStatus == WorldAssemblyStatus.MEMBER
                || worldAssemblyStatus == WorldAssemblyStatus.DELEGATE);
    }
}
