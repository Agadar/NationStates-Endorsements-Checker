package com.github.agadar.endochecker;

import com.github.agadar.nationstates.NationStates;
import com.github.agadar.nationstates.domain.nation.Nation;
import com.github.agadar.nationstates.domain.region.Region;
import com.github.agadar.nationstates.shard.NationShard;
import com.github.agadar.nationstates.shard.RegionShard;

/**
 * Our repository for retrieving nation-and region-data.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
public class NationStatesRepository {

    private final NationStates nationStates;

    public NationStatesRepository(NationStates nationStates) {
        this.nationStates = nationStates;
    }

    public Nation retrieveNationInQuestionFromApi(String nationInQuestionName) {
        return nationStates.getNation(nationInQuestionName)
                .shards(NationShard.WORLD_ASSEMBLY_STATUS, NationShard.REGION_NAME, NationShard.ENDORSED_BY).execute();
    }

    public Nation retrieveNationFromApi(String nationName) {
        return nationStates.getNation(nationName).shards(NationShard.WORLD_ASSEMBLY_STATUS, NationShard.ENDORSED_BY)
                .execute();
    }

    public Region retrieveRegionFromApi(String regionName) {
        return nationStates.getRegion(regionName).shards(RegionShard.NATION_NAMES).execute();
    }
}
