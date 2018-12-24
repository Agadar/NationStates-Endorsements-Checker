package com.github.agadar.endochecker;

import java.util.Scanner;

import com.github.agadar.nationstates.DefaultNationStatesImpl;

/**
 * Entry-point for the endorsements checker CLI application.
 *
 * @author Agadar (https://github.com/Agadar/)
 */
public class Main {

    private static final String USERAGENT = "Agadar's Endorsements Checker (https://github.com/Agadar/Endorsements-Checker)";

    public static void main(String[] args) {

        var inputReader = new InputReader(new Scanner(System.in));
        var outputWriter = new OutputWriter();
        var repository = new NationStatesRepository(new DefaultNationStatesImpl(USERAGENT));
        var desktopAccess = new DesktopAccess();
        var endorsementsChecker = new EndorsementsChecker(desktopAccess, outputWriter, inputReader, repository);

        endorsementsChecker.start();
    }
}
