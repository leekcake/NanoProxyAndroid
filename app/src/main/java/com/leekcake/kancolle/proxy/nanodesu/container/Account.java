package com.leekcake.kancolle.proxy.nanodesu.container;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by fkdlx on 2015-12-06.
 */
public class Account {
    public final String tokenID;

    public Account(String tokenID) {
        this.tokenID = tokenID;
    }

    public String nickName = "알 수 없음";

    public final ArrayList<Fleet> fleets = new ArrayList<>();

    public final void parseDeck(final JSONArray decks) {
        fleets.clear();
        for (final Object o : decks) {
            final JSONObject fleet = (JSONObject) o;
            final Fleet oFleet = new Fleet();

            //System.out.println("Detected fleet: " + fleet.get("api_name"));
            oFleet.name = (String) fleet.get("api_name");
            final JSONArray mission = (JSONArray) fleet.get("api_mission");
            if ((long) mission.get(0) == 1) {
                //final long mn = (long) mission.get(1);
                //final long finishAt = (long) mission.get(2);
                //System.out.println("Performing Mission " + mn + " finish at " + finishAt);
                oFleet.isPerformingMission = true;
                oFleet.Mission_Number = (long) mission.get(1);
                oFleet.Mission_finishAt = (long) mission.get(2);
            } //Fleet defaults is not performing mission.
            fleets.add(oFleet);
        }
    }

    public final void parsePort(final JSONObject port) {
        final JSONArray array = (JSONArray) ((JSONObject) port.get("api_data")).get("api_deck_port");
        parseDeck(array);
    }

    public final List<Fleet> getFleets() {
        ArrayList<Fleet> result = new ArrayList<>();

        for(Fleet fleet : fleets) {
            result.add( fleet.clone() );
        }

        return result;
    }

    @Override
    public String toString() {
        return nickName;
    }
}
