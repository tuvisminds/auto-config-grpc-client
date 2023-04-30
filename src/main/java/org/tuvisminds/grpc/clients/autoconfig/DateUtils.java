package org.tuvisminds.grpc.clients.autoconfig;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {
    public String getTime(String timeformat, String daystoAdd) {
        int addValue;
        if (daystoAdd.replace(daystoAdd.substring(0, 1), "").equals("")) {
            addValue = 0;
        } else {
            addValue = Integer.parseInt(daystoAdd.replace(daystoAdd.substring(0, 1), ""));
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(timeformat);
        calendar.add(Calendar.DAY_OF_MONTH, addValue);
        String date = dateFormat.format(calendar.getTime());
        return (date.substring(0, 10) + "T" + date.substring(11));
    }
}