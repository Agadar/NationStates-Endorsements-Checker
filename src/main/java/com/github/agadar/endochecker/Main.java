package com.github.agadar.endochecker;

import com.github.agadar.nsapi.NSAPI;
import com.github.agadar.nsapi.domain.nation.Nation;
import com.github.agadar.nsapi.domain.region.Region;
import com.github.agadar.nsapi.enums.shard.NationShard;
import com.github.agadar.nsapi.enums.shard.RegionShard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry for the CLI version of the endorsements checker.
 * 
 * @author Agadar (https://github.com/Agadar/)
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NSAPI.setUserAgent("Agadar's Endorsements Checker (Testing)");
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
            System.out.println("List:");
            
            // Sort list.
            Collections.sort(waNations);
            
            // Print resulting list.
            waNations.stream().forEach((nationName) -> {
                System.out.println("@" + nationName);
            });
        }
    }
    
}
