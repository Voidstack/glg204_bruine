package com.enosistudio.bruine.common;

import org.springframework.format.Formatter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;


/**
 * DateFormatter
 */
@Component
public class DateFormatter implements Formatter<LocalDate> {

    @Override
    @NonNull
    public String print(@NonNull LocalDate date, @NonNull Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
        return formatter.format(date);
    }

    @Override
    @NonNull
    public LocalDate parse(@NonNull String text, @NonNull Locale locale) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
        return LocalDate.from(formatter.parse(text));
    }


}