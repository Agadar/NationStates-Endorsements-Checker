package com.github.agadar.endochecker;

import java.util.Scanner;

/**
 * Assists in asking questions to the user and reading their response.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
public class InputReader {

    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String askUserForNationName() {
        return promptOpenQuestion(scanner, "Nation whose endorsements to check").toLowerCase().replace(' ', '_');
    }

    public String promptOpenQuestion(Scanner answerReader, String question) {
        System.out.print(question + " : ");
        return answerReader.nextLine();
    }

    public boolean promptBinaryQuestion(String question) {
        System.out.print(question + " (y/n) : ");
        String reply = scanner.nextLine().toLowerCase();
        return (reply != null && ("y".equals(reply) || "yes".equals(reply)));
    }
}
