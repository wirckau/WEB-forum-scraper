package pckge.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateParser {
    private String name;

    public DateParser(String name)
    {
        this.name = name;
    }

    public Date getDate() throws ParseException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd. MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat dateFormatter =
                new SimpleDateFormat("dd. MMM yyyy, HH:mm", Locale.ENGLISH);
        this.name = this.name
                .replace("Yesterday",""+dtf.format(LocalDate.now().minusDays(1))+"")
                .replace("Today", ""+dtf.format(LocalDate.now())+"");
        return dateFormatter.parse(this.name);
    }

    public Date getConvActivityDate() throws ParseException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat dateFormatter =
                new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);
        this.name = this.name
                .replace("Yesterday",""+dtf.format(LocalDate.now().minusDays(1))+"")
                .replace("Today", ""+dtf.format(LocalDate.now())+"");
        this.name = this.name.substring(0, this.name.indexOf(":")+3).trim(); //: 24 Aug, 23:39 / user  to 24 Aug, 23:39
        LocalDate cal = LocalDate.of(2022, 1, 1);
        this.name = this.name.replace("2022",""); //rmv year(if possible) to add yr to all
        this.name = this.name.replace(","," "+cal.getYear()+",");
        // 18. Jul 2021, 20:04 to "19 Jul 2021, 20:04";
        return dateFormatter.parse(this.name);
    }
}