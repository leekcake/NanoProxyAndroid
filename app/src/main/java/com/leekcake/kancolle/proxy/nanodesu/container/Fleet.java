package com.leekcake.kancolle.proxy.nanodesu.container;

/**
 * Created by fkdlx on 2015-12-06.
 */
public class Fleet implements Cloneable {
    public String name = "unknown";

    public boolean isPerformingMission = false;
    public long Mission_Number = -1;
    public long Mission_finishAt = -1;

    @Override
    public final Fleet clone() {
        Fleet result = new Fleet();

        result.name = this.name;
        result.isPerformingMission = this.isPerformingMission;
        result.Mission_Number = this.Mission_Number;
        result.Mission_finishAt = this.Mission_finishAt;

        return result;
    }

    public final String getLeftTime() {
        if(!isPerformingMission)
            return "휴식중";
        long leftTime = Mission_finishAt - System.currentTimeMillis();
        String sleftTime;
        if (leftTime <= 0) {
            sleftTime = "완료됨";
        } else {
            int second = (int) Math.floor(leftTime / 1000);
            int minute = (int) Math.floor(second / 60);
            int hour = (int) Math.floor(minute / 60);
            sleftTime = hour + ":" + (minute % 60) + ":" + (second % 60);
        }
        return sleftTime;
    }
}
