package dk.superawesome.factorio.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DurationFormatter {

    public static final List<Long> TIMES = Arrays.asList(
            TimeUnit.DAYS.toMillis(365),
            TimeUnit.DAYS.toMillis(30),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.SECONDS.toMillis(1) );
    public static final List<String> TIMES_WORDS = Arrays.asList("year","month","day","hour","minute","second");

    public static String toDuration(long duration) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < TIMES.size(); i++) {
            long current = TIMES.get(i);
            long temp = duration / current;
            if (temp > 0) {
                res.append(temp)
                        .append(" ")
                        .append(TIMES_WORDS.get(i))
                        .append(temp != 1 ? "s" : "");
                break;
            }
        }

        return res.toString().isEmpty() ? "Nu" : res.toString();
    }
}
