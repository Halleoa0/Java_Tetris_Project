package shop;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 이번 주 미션 3개를 정한다. 같은 주에는 누가 언제 열어도 같은 미션이 나온다. */
public final class WeeklyMissions {
    public static final int COUNT = 3;

    private WeeklyMissions() { }

    /** 예: 2026년 ISO 41주차 -> 202641 */
    public static int currentWeekId() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return today.get(IsoFields.WEEK_BASED_YEAR) * 100 + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    /** 주차 번호로 미션을 뽑는다. 같은 지표(줄 삭제, 콤보 ...)가 겹치지 않게 한다. */
    public static List<Mission> forWeek(int weekId) {
        List<Mission> pool = new ArrayList<>(List.of(Mission.values()));
        Collections.shuffle(pool, new Random(weekId * 7919L));
        List<Mission> picked = new ArrayList<>();
        for (Mission m : pool) {
            boolean dup = false;
            for (Mission p : picked) if (p.metric == m.metric) { dup = true; break; }
            if (!dup) picked.add(m);
            if (picked.size() == COUNT) break;
        }
        return picked;
    }

    public static List<Mission> current() { return forWeek(currentWeekId()); }
}
