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
    public static final List<String> TIMES_WORDS = Arrays.asList("år", "måned", "dag", "time", "minut", "sekund");
    public static final List<String> TIMES_WORDS_PLURAL = Arrays.asList("år", "måneder", "dage", "timer", "minutter", "sekunder");

    public static String toDuration(long duration) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < TIMES.size(); i++) {
            long current = TIMES.get(i);
            long temp = duration / current;
            if (temp > 0) {
                res
                        .append(temp)
                        .append(" ")
                        .append(temp == 1 ? TIMES_WORDS.get(i) : TIMES_WORDS_PLURAL.get(i));
                break;
            }
        }

        return res.toString().isEmpty() ? "Nu" : res.toString();
    }
}
