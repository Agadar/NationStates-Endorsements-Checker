package com.github.agadar.endochecker;

import java.util.Collection;
import java.util.HashSet;

import lombok.Getter;

/**
 * The results of an endorsements check.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
@Getter
public class EndorsementsCheckResult {

    private final String nationInQuestionName;
    private final Collection<String> nationsInWorldAssembly;
    private final Collection<String> nationsNotEndorsedByNationInQuestion;
    private final Collection<String> nationsNotEndorsingNationInQuestionBack;

    public EndorsementsCheckResult(String nationInQuestionName) {
        this.nationInQuestionName = nationInQuestionName;
        nationsInWorldAssembly = new HashSet<>();
        nationsNotEndorsedByNationInQuestion = new HashSet<>();
        nationsNotEndorsingNationInQuestionBack = new HashSet<>();
    }
}
