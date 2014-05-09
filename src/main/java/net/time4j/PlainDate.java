/*
 * -----------------------------------------------------------------------
 * Copyright © 2013 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (PlainDate.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j;

import net.time4j.base.GregorianDate;
import net.time4j.base.GregorianMath;
import net.time4j.base.MathUtils;
import net.time4j.base.TimeSource;
import net.time4j.base.UnixTime;
import net.time4j.engine.AttributeQuery;
import net.time4j.engine.CalendarEra;
import net.time4j.engine.CalendarSystem;
import net.time4j.engine.Calendrical;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoException;
import net.time4j.engine.ChronoMerger;
import net.time4j.engine.ElementRule;
import net.time4j.engine.EpochDays;
import net.time4j.engine.FormattableElement;
import net.time4j.engine.Normalizer;
import net.time4j.engine.StartOfDay;
import net.time4j.engine.TimeAxis;
import net.time4j.engine.TimeSpan;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarType;
import net.time4j.format.ChronoFormatter;
import net.time4j.format.ChronoPattern;
import net.time4j.format.DisplayMode;
import net.time4j.format.Leniency;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import net.time4j.tz.ZonalOffset;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * <p>Repr&auml;sentiert ein reines Kalenderdatum im ISO-8601-Standard. </p>
 *
 * <p>Der Wertebereich fasst auch negative Jahre bis zu {@code -999999999}.
 * Diese Jahre sind nicht historisch zu interpretieren, sondern vielmehr
 * als eine andere Z&auml;hlweise &auml;hnlich zur Z&auml;hlung von
 * Epochentagen. Die Schaltjahrregeln des gregorianischen Kalenders werden
 * proleptisch, also r&uuml;ckwirkend auch in die ferne Vergangenheit
 * angewandt (eine ahistorische mathematische Abstraktion). </p>
 *
 * <p>Registriert sind folgende als Konstanten deklarierte Elemente: </p>
 *
 * <ul>
 *  <li>{@link #CALENDAR_DATE}</li>
 *  <li>{@link #DAY_OF_MONTH}</li>
 *  <li>{@link #DAY_OF_QUARTER}</li>
 *  <li>{@link #DAY_OF_WEEK}</li>
 *  <li>{@link #DAY_OF_YEAR}</li>
 *  <li>{@link #MONTH_AS_NUMBER}</li>
 *  <li>{@link #MONTH_OF_YEAR}</li>
 *  <li>{@link #QUARTER_OF_YEAR}</li>
 *  <li>{@link #WEEKDAY_IN_MONTH}</li>
 *  <li>{@link #YEAR}</li>
 *  <li>{@link #YEAR_OF_WEEKDATE}</li>
 * </ul>
 *
 * <p>Dar&uuml;berhinaus sind alle Elemente der Klasse {@link Weekmodel}
 * und der Klasse {@link EpochDays} nutzbar. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
@CalendarType("iso8601")
public final class PlainDate
    extends Calendrical<IsoDateUnit, PlainDate>
    implements GregorianDate, Normalizer<CalendarUnit> {

    //~ Statische Felder/Initialisierungen --------------------------------

    /** Fr&uuml;hestm&ouml;gliches Datum [-999999999-01-01]. */
    static final PlainDate MIN =
        new PlainDate(GregorianMath.MIN_YEAR, 1, 1);

    /** Sp&auml;testm&ouml;gliches Datum [+999999999-12-31]. */
    static final PlainDate MAX =
        new PlainDate(GregorianMath.MAX_YEAR, 12, 31);

    /** Entspricht dem Jahr {@code -999999999}. */
    static final Integer MIN_YEAR =
        Integer.valueOf(GregorianMath.MIN_YEAR);

    /** Entspricht dem Jahr {@code +999999999}. */
    static final Integer MAX_YEAR =
        Integer.valueOf(GregorianMath.MAX_YEAR);

    private static final Integer STD_YEAR_LEN = Integer.valueOf(365);
    private static final Integer LEAP_YEAR_LEN = Integer.valueOf(366);
    private static final int[] DAY_OF_YEAR_PER_MONTH = new int[12];
    private static final int[] DAY_OF_LEAP_YEAR_PER_MONTH = new int[12];

    static {
        DAY_OF_YEAR_PER_MONTH[0] = 31;
        DAY_OF_YEAR_PER_MONTH[1] = 59;
        DAY_OF_YEAR_PER_MONTH[2] = 90;
        DAY_OF_YEAR_PER_MONTH[3] = 120;
        DAY_OF_YEAR_PER_MONTH[4] = 151;
        DAY_OF_YEAR_PER_MONTH[5] = 181;
        DAY_OF_YEAR_PER_MONTH[6] = 212;
        DAY_OF_YEAR_PER_MONTH[7] = 243;
        DAY_OF_YEAR_PER_MONTH[8] = 273;
        DAY_OF_YEAR_PER_MONTH[9] = 304;
        DAY_OF_YEAR_PER_MONTH[10] = 334;
        DAY_OF_YEAR_PER_MONTH[11] = 365;

        DAY_OF_LEAP_YEAR_PER_MONTH[0] = 31;
        DAY_OF_LEAP_YEAR_PER_MONTH[1] = 60;
        DAY_OF_LEAP_YEAR_PER_MONTH[2] = 91;
        DAY_OF_LEAP_YEAR_PER_MONTH[3] = 121;
        DAY_OF_LEAP_YEAR_PER_MONTH[4] = 152;
        DAY_OF_LEAP_YEAR_PER_MONTH[5] = 182;
        DAY_OF_LEAP_YEAR_PER_MONTH[6] = 213;
        DAY_OF_LEAP_YEAR_PER_MONTH[7] = 244;
        DAY_OF_LEAP_YEAR_PER_MONTH[8] = 274;
        DAY_OF_LEAP_YEAR_PER_MONTH[9] = 305;
        DAY_OF_LEAP_YEAR_PER_MONTH[10] = 335;
        DAY_OF_LEAP_YEAR_PER_MONTH[11] = 366;
    }

    /**
     * <p>Element mit dem Datum im Wertebereich {@code [-999999999-01-01]}
     * bis {@code [+999999999-12-31]}. </p>
     */
    public static final ChronoElement<PlainDate> CALENDAR_DATE =
        DateElement.INSTANCE;

    /**
     * <p>Element mit dem proleptischen ISO-Jahr ohne &Auml;ra-Bezug mit dem
     * Wertebereich {@code -999999999} bis {@code 999999999}. </p>
     *
     * <p>Beispiele: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.YEAR;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(YEAR)); // Ausgabe: 2012
     *
     *  date = date.with(YEAR, 2014);
     *  System.out.println(date); // Ausgabe: 2014-02-28
     *
     *  date = date.with(YEAR.incremented()); // n&auml;chstes Jahr
     *  System.out.println(date); // Ausgabe: 2015-02-28
     *
     *  date = date.with(YEAR.atCeiling()); // letzter Tag des Jahres
     *  System.out.println(date); // Ausgabe: 2015-12-31
     *
     *  date = date.with(YEAR.atFloor()); // erster Tag des Jahres
     *  System.out.println(date); // Ausgabe: 2015-01-01
     * </pre>
     *
     * <p>Der Begriff &quot;proleptisch&quot; bedeutet, da&szlig; die Regeln
     * des gregorianischen Kalenders und damit verbunden die Jahresz&auml;hlung
     * auch r&uuml;ckwirkend vor der Einf&uuml;hrung des Kalenders angewandt
     * werden. Insbesondere ist auch das Jahr {@code 0} zugelassen - nebst
     * negativen Jahreszahlen. F&uuml;r historische Jahreszahlen ist diese
     * mathematische Extrapolation nicht geeignet. </p>
     */
    @FormattableElement(format = "u")
    public static final AdjustableElement<Integer, DateOperator> YEAR =
        IntegerDateElement.create(
            "YEAR",
            IntegerDateElement.YEAR,
            GregorianMath.MIN_YEAR,
            GregorianMath.MAX_YEAR,
            'u');

    /**
     * <p>Definiert ein Element f&uuml;r das wochenbasierte Jahr in einem
     * ISO-Wochendatum. </p>
     *
     * <p>Das wochenbasierte Jahr stimmt in der Regel mit dem
     * Kalenderjahr &uuml;berein. Ausnahmen sind der Beginn und
     * das Ende des Kalenderjahres, weil die erste Woche des Jahres
     * erst nach Neujahr anfangen und die letzte Woche des Jahres
     * bereits vor Sylvester enden kann. Beispiele: </p>
     *
     * <ul><li>Sonntag, [1995-01-01] =&gt; [1994-W52-7]</li>
     * <li>Dienstag, [1996-31-12] =&gt; [1997-W01-2]</li></ul>
     *
     * <p>Notiz: Dieses Element hat eine spezielle Basiseinheit, die so
     * verwendet werden kann, da&szlig; nicht der Tag des Monats nach
     * einer Jahresaddition erhalten bleibt, sondern der Tag der Woche: </p>
     *
     * <pre>
     *  PlainDate date = PlainDate.of(2014, JANUARY, 2); // Donnerstag
     *  IsoDateUnit unit = CalendarUnit.weekBasedYears();
     *  System.out.println(date.plus(1, unit)); // Ausgabe: 2015-01-01
     * </pre>
     *
     * @see     CalendarUnit#weekBasedYears()
     */
    @FormattableElement(format = "Y")
    public static final
    AdjustableElement<Integer, DateOperator> YEAR_OF_WEEKDATE =
        YOWElement.INSTANCE;

    /**
     * <p>Element mit dem Quartal des Jahres (Wertebereich {@code Q1-Q4}). </p>
     */
    @FormattableElement(format = "Q", standalone="q")
    public static final NavigableElement<Quarter> QUARTER_OF_YEAR =
        new EnumElement<Quarter>(
            "QUARTER_OF_YEAR",
            Quarter.class,
            Quarter.Q1,
            Quarter.Q4,
            EnumElement.QUARTER_OF_YEAR,
            'Q');

    /**
     *  <p>Element mit dem Monat als Enum
     * (Wertebereich {@code JANUARY-DECEMBER}). </p>
     *
     * <p>Beispiele: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.MONTH_OF_YEAR;
     *  import static net.time4j.Month.*;
     *
     *  PlainDate date = PlainDate.of(2012, 2, 29);
     *  System.out.println(date.get(MONTH_OF_YEAR)); // Ausgabe: February
     *
     *  date = date.with(MONTH_OF_YEAR, APRIL);
     *  System.out.println(date); // Ausgabe: 2012-04-29
     *
     *  date = date.with(MONTH_OF_YEAR.incremented()); // n&auml;chster Monat
     *  System.out.println(date); // Ausgabe: 2012-05-29
     *
     *  date = date.with(MONTH_OF_YEAR.maximized()); // letzter Monat im Jahr
     *  System.out.println(date); // Ausgabe: 2012-12-29
     *
     *  date = date.with(MONTH_OF_YEAR.atCeiling()); // letzter Monatstag
     *  System.out.println(date); // Ausgabe: 2012-12-31
     *
     *  date = date.with(MONTH_OF_YEAR.atFloor()); // erster Tag im Monat
     *  System.out.println(date); // Ausgabe: 2012-12-01
     *
     *  date = date.with(MONTH_OF_YEAR.setToNext(JULY)); // zum Juli vorangehen
     *  System.out.println(date); // Ausgabe: 2013-07-01
     * </pre>
     */
    @FormattableElement(format = "M", standalone="L")
    public static final NavigableElement<Month> MONTH_OF_YEAR =
        new EnumElement<Month>(
            "MONTH_OF_YEAR",
            Month.class,
            Month.JANUARY,
            Month.DECEMBER,
            EnumElement.MONTH,
            'M');

    /**
     * <p>Element mit dem Monat in Nummernform (Wertebereich {@code 1-12}). </p>
     *
     * <p>Im allgemeinen empfiehlt sich wegen der Typsicherheit und sprachlichen
     * Klarheit die enum-Form, die zudem auch als Text formatierbar ist. </p>
     *
     * @see     #MONTH_OF_YEAR
     */
    public static final
    ProportionalElement<Integer, DateOperator, PlainDate> MONTH_AS_NUMBER =
        IntegerDateElement.create(
            "MONTH_AS_NUMBER",
            IntegerDateElement.MONTH,
            1,
            12,
            '\u0000');

    /**
     * <p>Element mit dem Tag des Monats
     * (Wertebereich {@code 1-28/29/30/31}). </p>
     */
    @FormattableElement(format = "d")
    public static final
    ProportionalElement<Integer, DateOperator, PlainDate> DAY_OF_MONTH =
        IntegerDateElement.create(
            "DAY_OF_MONTH",
            IntegerDateElement.DAY_OF_MONTH,
            1,
            31,
            'd');

    /**
     * <p>Element mit dem Tag der Woche
     * (Wertebereich {@code MONDAY-SUNDAY}). </p>
     *
     * <p>Eine lokalisierte Form ist mittels {@link Weekmodel#localDayOfWeek()}
     * verf&uuml;gbar. In den USA z.B. ist der Sonntag der erste Tag der
     * Woche, anders als hier definiert. Daher sollte in den USA vielmehr
     * der Ausdruck {@code Weekmodel.of(Locale.US).localDayOfWeek()}
     * verwendet werden. </p>
     */
    @FormattableElement(format = "E")
    public static final NavigableElement<Weekday> DAY_OF_WEEK =
        new EnumElement<Weekday>(
            "DAY_OF_WEEK",
            Weekday.class,
            Weekday.MONDAY,
            Weekday.SUNDAY,
            EnumElement.DAY_OF_WEEK,
            'E');

    /**
     * <p>Element mit dem Tag des Jahres (Wertebereich {@code 1-365/366}). </p>
     */
    @FormattableElement(format = "D")
    public static final
    ProportionalElement<Integer, DateOperator, PlainDate> DAY_OF_YEAR =
        IntegerDateElement.create(
            "DAY_OF_YEAR",
            IntegerDateElement.DAY_OF_YEAR,
            1,
            365,
            'D');

    /**
     * <p>Element mit dem Tag des Quartals
     * (Wertebereich {@code 1-90/91/92}). </p>
     */
    public static final
    ProportionalElement<Integer, DateOperator, PlainDate> DAY_OF_QUARTER =
        IntegerDateElement.create(
            "DAY_OF_QUARTER",
            IntegerDateElement.DAY_OF_QUARTER,
            1,
            92,
            '\u0000');

    /**
     * <p>Element mit dem x-ten Wochentag im Monat
     * (Wertebereich {@code 1-5}). </p>
     *
     * <p>Beispiel: </p>
     *
     * <pre>
     *  import static net.time4j.PlainDate.WEEKDAY_IN_MONTH;
     *  import static net.time4j.Weekday.*;
     *
     *  PlainDate date = PlainDate.of(2013, 3, 1); // 1. M&auml;rz 2013
     *  System.out.println(date.with(WEEKDAY_IN_MONTH.setToThird(WEDNESDAY)));
     *  // Ausgabe: 2013-03-20 (Mittwoch)
     * </pre>
     */
    @FormattableElement(format = "F")
    public static final OrdinalWeekdayElement WEEKDAY_IN_MONTH =
        WeekdayInMonthElement.INSTANCE;

    /**
     * <p>Element mit der &Auml;ra des proleptischen gregorianischen
     * Kalenders. </p>
     */
    @FormattableElement(format = "G")
    static final ChronoElement<SimpleEra> ERA =
        new EnumElement<SimpleEra>(
            "ERA",
            SimpleEra.class,
            SimpleEra.BC,
            SimpleEra.AD,
            EnumElement.ERA,
            'G');

    /**
     * <p>Element mit dem Jahr der &Auml;ra des proleptischen gregorianischen
     * Kalenders. </p>
     */
    @FormattableElement(format = "y")
    static final ChronoElement<Integer> YEAR_OF_ERA =
        IntegerDateElement.create(
            "YEAR_OF_ERA",
            IntegerDateElement.YEAR_OF_ERA,
            1,
            GregorianMath.MAX_YEAR,
            'y');

    // Dient der Serialisierungsunterstützung.
    private static final long serialVersionUID = -6698431452072325688L;

    private static final Map<String, Object> ELEMENTS;
    private static final CalendarSystem<PlainDate> TRANSFORMER;

    /** Zeitachse eines ISO-Kalenderdatums. */
    static final TimeAxis<IsoDateUnit, PlainDate> ENGINE;

    static {
        Map<String, Object> constants = new HashMap<String, Object>();
        fill(constants, CALENDAR_DATE);
        fill(constants, YEAR);
        fill(constants, YEAR_OF_WEEKDATE);
        fill(constants, QUARTER_OF_YEAR);
        fill(constants, MONTH_OF_YEAR);
        fill(constants, MONTH_AS_NUMBER);
        fill(constants, DAY_OF_MONTH);
        fill(constants, DAY_OF_WEEK);
        fill(constants, DAY_OF_YEAR);
        fill(constants, DAY_OF_QUARTER);
        fill(constants, WEEKDAY_IN_MONTH);
        fill(constants, ERA);
        fill(constants, YEAR_OF_ERA);
        ELEMENTS = Collections.unmodifiableMap(constants);

        TRANSFORMER = new Transformer();

        TimeAxis.Builder<IsoDateUnit, PlainDate> builder =
            TimeAxis.Builder.setUp(
                IsoDateUnit.class,
                PlainDate.class,
                new Merger(),
                TRANSFORMER)
            .appendElement(
                CALENDAR_DATE,
                new DateElementRule(),
                CalendarUnit.DAYS)
            .appendElement(
                YEAR,
                new IntegerElementRule(YEAR),
                CalendarUnit.YEARS)
            .appendElement(
                YEAR_OF_WEEKDATE,
                YOWElement.elementRule(PlainDate.class),
                YOWElement.YOWUnit.WEEK_BASED_YEARS)
            .appendElement(
                QUARTER_OF_YEAR,
                new EnumElementRule<Quarter>(QUARTER_OF_YEAR),
                CalendarUnit.QUARTERS)
            .appendElement(
                MONTH_OF_YEAR,
                new EnumElementRule<Month>(MONTH_OF_YEAR),
                CalendarUnit.MONTHS)
            .appendElement(
                MONTH_AS_NUMBER,
                new IntegerElementRule(MONTH_AS_NUMBER),
                CalendarUnit.MONTHS)
            .appendElement(
                DAY_OF_MONTH,
                new IntegerElementRule(DAY_OF_MONTH),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_WEEK,
                new EnumElementRule<Weekday>(DAY_OF_WEEK),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_YEAR,
                new IntegerElementRule(DAY_OF_YEAR),
                CalendarUnit.DAYS)
            .appendElement(
                DAY_OF_QUARTER,
                new IntegerElementRule(DAY_OF_QUARTER),
                CalendarUnit.DAYS)
            .appendElement(
                WEEKDAY_IN_MONTH,
                new WIMRule(WEEKDAY_IN_MONTH),
                CalendarUnit.WEEKS)
            // TODO: .appendExtension(new HistoryExtension())
            .appendExtension(new WeekExtension());
        registerUnits(builder);
        ENGINE = builder.build();
    }

    //~ Instanzvariablen --------------------------------------------------

    private transient final int year;
    private transient final byte month;
    private transient final byte dayOfMonth;

    //~ Konstruktoren -----------------------------------------------------

    private PlainDate(
        int year,
        int month,
        int dayOfMonth
    ) {
        super();

        GregorianMath.checkDate(year, month, dayOfMonth);

        this.year = year;
        this.month = (byte) month;
        this.dayOfMonth = (byte) dayOfMonth;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Erzeugt ein neues ISO-konformes Kalenderdatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (1-12)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, Month, int)
     * @see     #of(int, int)
     * @see     #of(int, int, Weekday)
     */
    public static PlainDate of(
        int year,
        int month,
        int dayOfMonth
    ) {

        // TODO: konfigurierbaren Cache einbauen?
        return new PlainDate(year, month, dayOfMonth);

    }

    /**
     * <p>Erzeugt ein neues ISO-konformes Kalenderdatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   month       gregorian month in range (January-December)
     * @param   dayOfMonth  day of month in range (1-31)
     * @return  new or cached calendar date instance
     * @throws  IllegalArgumentException if any argument is out of range
     * @see     #of(int, int, int)
     */
    public static PlainDate of(
        int year,
        Month month,
        int dayOfMonth
    ) {

        return PlainDate.of(year, month.getValue(), dayOfMonth);

    }

    /**
     * <p>Erzeugt ein neues ISO-konformes Ordinaldatum. </p>
     *
     * @param   year        proleptic iso year [(-999,999,999)-999,999,999]
     * @param   dayOfYear   day of year in the range (1-366)
     * @return  new or cached ordinal date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public static PlainDate of(
        int year,
        int dayOfYear
    ) {

        if (dayOfYear < 1) {
            throw new IllegalArgumentException(
                "Day of year out of range: " + dayOfYear);
        } else if (dayOfYear <= 31) {
            return PlainDate.of(year, 1, dayOfYear);
        }

        int[] table = (
            GregorianMath.isLeapYear(year)
            ? DAY_OF_LEAP_YEAR_PER_MONTH
            : DAY_OF_YEAR_PER_MONTH);

        for (int i = (dayOfYear > table[6] ? 7 : 1); i < 12; i++) {
            if (dayOfYear <= table[i]) {
                int dom = dayOfYear - table[i - 1];
                return PlainDate.of(year, i + 1, dom);
            }
        }

        throw new IllegalArgumentException(
            "Day of year out of range: " + dayOfYear);

    }

    /**
     * <p>Erzeugt ein neues ISO-konformes Wochendatum. </p>
     *
     * @param   yearOfWeekdate  week-based-year according to ISO-definition
     * @param   weekOfYear      week of year in the range (1-52/53)
     * @param   dayOfWeek       day of week in the range (MONDAY-SUNDAY)
     * @return  new or cached week date instance
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public static PlainDate of(
        int yearOfWeekdate,
        int weekOfYear,
        Weekday dayOfWeek
    ) {

        if (
            (weekOfYear < 1)
            || (weekOfYear > 53)
        ) {
            throw new IllegalArgumentException(
                "Week of year out of range: " + weekOfYear);
        }

        GregorianMath.checkDate(yearOfWeekdate, 1, 1);

        Weekday wdNewYear =
            Weekday.valueOf(GregorianMath.getDayOfWeek(yearOfWeekdate, 1, 1));
        int dow = wdNewYear.getValue();
        int doy = (
            ((dow <= 4) ? 2 - dow : 9 - dow)
            + (weekOfYear - 1) * 7
            + dayOfWeek.getValue() - 1
        );

        if (doy <= 0) {
            yearOfWeekdate--;
            doy += (GregorianMath.isLeapYear(yearOfWeekdate) ? 366 : 365);
        } else {
            int yearlen =
                (GregorianMath.isLeapYear(yearOfWeekdate) ? 366 : 365);
            if (doy > yearlen) {
                doy -= yearlen;
                yearOfWeekdate++;
            }
        }

        PlainDate result = PlainDate.of(yearOfWeekdate, doy);

        if (
            (weekOfYear == 53)
            && (result.getWeekOfYear() != 53)
        ) {
            throw new IllegalArgumentException(
                "Week of year out of range: " + weekOfYear);
        }

        return result;

    }

    /**
     * <p>Erzeugt ein Datum zur gegebenen Anzahl von Tagen seit einer
     * Epoche. </p>
     *
     * @return  found calendar date based on given epoch days
     * @throws  IllegalArgumentException if first argument is out of range
     */
    public static PlainDate of(
        long amount,
        EpochDays epoch
    ) {

        return TRANSFORMER.transform(EpochDays.UTC.transform(amount, epoch));

    }

    /**
     * <p>Allgemeine Konversionsmethode. </p>
     *
     * @param   date    ISO-date
     * @return  PlainDate
     */
    public static PlainDate from(GregorianDate date) {

        if (date instanceof PlainDate) {
            return (PlainDate) date;
        } else {
            return PlainDate.of(
                date.getYear(), date.getMonth(), date.getDayOfMonth());
        }

    }

    /**
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum zu Mitternacht
     * am Beginn des Tages. </p>
     *
     * @return  local timestamp as composition of this date and midnight
     * @see     #atTime(PlainTime)
     */
    public PlainTimestamp atStartOfDay() {

        return this.atTime(PlainTime.MIN);

    }

    /**
     * <p>Erzeugt einen globalen Zeitstempel mit diesem Datum zur fr&uuml;hesten
     * g&uuml;ltigen Uhrzeit in der angegebenen Zeitzone. </p>
     *
     * @param   tzid        timezone id
     * @return  global timestamp based on composition of this date and earliest
     *          wall time in given timezone
     * @see     #atTime(PlainTime)
     */
    public Moment atStartOfDay(TZID tzid) {

        // TODO: TransitionStrategy#laterOffset() benutzen?
        return this.atStartOfDay().inTimezone(tzid);

    }

    /**
     * <p>Erzeugt einen lokalen Zeitstempel mit diesem Datum und der
     * angegebenen Uhrzeit. </p>
     *
     * <p>Wenn {@link PlainTime#midnightAtEndOfDay() T24:00} angegeben wird,
     * dann wird der Zeitstempel automatisch so normalisiert, da&szlig; er auf
     * den n&auml;chsten Tag verweist. </p>
     *
     * @param   time    wall time
     * @return  local timestamp as composition of this date and given time
     */
    public PlainTimestamp atTime(PlainTime time) {

        return PlainTimestamp.of(this, time);

    }

    /**
     * <p>Entspricht {@code atTime(PlainTime.of(hour, minute))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public PlainTimestamp atTime(
        int hour,
        int minute
    ) {

        return this.atTime(PlainTime.of(hour, minute));

    }

    /**
     * <p>Entspricht {@code atTime(PlainTime.of(hour, minute, second))}. </p>
     *
     * @param   hour        hour of day in range (0-24)
     * @param   minute      minute of hour in range (0-59)
     * @param   second      second of hour in range (0-59)
     * @return  local timestamp as composition of this date and given time
     * @throws  IllegalArgumentException if any argument is out of range
     */
    public PlainTimestamp atTime(
        int hour,
        int minute,
        int second
    ) {

        return this.atTime(PlainTime.of(hour, minute, second));

    }

    @Override
    public int getYear() {

        return this.year;

    }

    @Override
    public int getMonth() {

        return this.month;

    }

    @Override
    public int getDayOfMonth() {

        return this.dayOfMonth;

    }

    /**
     * <p>Ermittelt die L&auml;nge des aktuellen Monats in Tagen. </p>
     *
     * @return  int im Bereich 28-31
     */
    public int lengthOfMonth() {

        return GregorianMath.getLengthOfMonth(this.year, this.month);

    }

    /**
     * <p>Ermittelt die L&auml;nge des aktuellen Jahres in Tagen. </p>
     *
     * @return  365 oder 366 wenn das aktuelle Jahr ein Schaltjahr ist
     */
    public int lengthOfYear() {

        return this.isLeapYear() ? 366 : 365;

    }

    /**
     * <p>Liegt dieses Datum in einem Schaltjahr? </p>
     *
     * @return  boolean
     */
    public boolean isLeapYear() {

        return GregorianMath.isLeapYear(this.year);

    }

    /**
     * <p>Liegt das Datum im angegebenen Land an einem Wochenende? </p>
     *
     * @param   country     country setting with two-letter ISO-3166-code
     * @return  {@code true} if in given country this date is on weekend
     *          else {@code false}
     * @see     Weekmodel#weekend()
     */
    public boolean isWeekend(Locale country) {

        return this.matches(Weekmodel.of(country).weekend());

    }

    /**
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Musters
     * in der Standard-Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @return  format object for formatting {@code PlainDate}-objects
     *          using system locale
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     ChronoFormatter#with(Locale)
     */
    public static ChronoFormatter<PlainDate> localFormatter(
        String formatPattern,
        ChronoPattern patternType
    ) {

        return ChronoFormatter
            .setUp(PlainDate.class, Locale.getDefault())
            .addPattern(formatPattern, patternType)
            .build();

    }

    /**
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Stils
     * in der Standard-Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. </p>
     *
     * @param   mode        formatting style
     * @return  format object for formatting {@code PlainDate}-objects
     *          using system locale
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     ChronoFormatter#with(Locale)
     */
    public static ChronoFormatter<PlainDate> localFormatter(DisplayMode mode) {

        int style = PatternType.getFormatStyle(mode);
        DateFormat df = DateFormat.getDateInstance(style);
        String pattern = PatternType.getFormatPattern(df);

        return ChronoFormatter
            .setUp(PlainDate.class, Locale.getDefault())
            .addPattern(pattern, PatternType.SIMPLE_DATE_FORMAT)
            .build();

    }

    /**
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Musters
     * in der angegebenen Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @param   locale          locale setting
     * @return  format object for formatting {@code PlainDate}-objects
     *          using given locale
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     #localFormatter(String,ChronoPattern)
     */
    public static ChronoFormatter<PlainDate> formatter(
        String formatPattern,
        ChronoPattern patternType,
        Locale locale
    ) {

        return ChronoFormatter
            .setUp(PlainDate.class, locale)
            .addPattern(formatPattern, patternType)
            .build();

    }

    /**
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Stils
     * und in der angegebenen Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. </p>
     *
     * @param   mode        formatting style
     * @param   locale      locale setting
     * @return  format object for formatting {@code PlainDate}-objects
     *          using given locale
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     #localFormatter(DisplayMode)
     */
    public static ChronoFormatter<PlainDate> formatter(
        DisplayMode mode,
        Locale locale
    ) {

        int style = PatternType.getFormatStyle(mode);
        DateFormat df = DateFormat.getDateInstance(style, locale);
        String pattern = PatternType.getFormatPattern(df);

        return ChronoFormatter
            .setUp(PlainDate.class, locale)
            .addPattern(pattern, PatternType.SIMPLE_DATE_FORMAT)
            .build();

    }

    /**
     * <p>Erzeugt eine kanonische Darstellung im Format
     * &quot;yyyy-MM-dd&quot;. </p>
     *
     * @return  canonical ISO-8601-formatted string
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(32);
        formatYear(sb, this.year);
        format2Digits(sb, this.month);
        format2Digits(sb, this.dayOfMonth);
        return sb.toString();

    }

    @Override
    public Duration<CalendarUnit> normalize(
        TimeSpan<? extends CalendarUnit> timespan) {

        return this.until(this.plus(timespan), Duration.inYearsMonthsDays());

    }

    @Override
    public TimeAxis<IsoDateUnit, PlainDate> getChronology() {

        return ENGINE;

    }

    @Override
    protected PlainDate getContext() {

        return this;

    }

    @Override
    protected int compareByTime(Calendrical<?, ?> date) {

        if (date instanceof PlainDate) { // Optimierung
            PlainDate d1 = this;
            PlainDate d2 = (PlainDate) date;

            int delta = d1.year - d2.year;
            if (delta == 0) {
                delta = d1.month - d2.month;
                if (delta == 0) {
                    delta = d1.dayOfMonth - d2.dayOfMonth;
                }
            }

            return delta;
        }

        return super.compareByTime(date); // basiert auf Epochentagen

    }

    /**
     * <p>Liefert die Tage seit der UTC-Epoche. </p>
     *
     * @return  count of days since UTC (1972-01-01)
     */
    long getDaysSinceUTC() {

        return TRANSFORMER.transform(this);

    }

    /**
     * <p>Wandelt die Tage seit der UTC-Epoche in ein Datum um. </p>
     *
     * @param   utcDays     count of days since UTC (1972-01-01)
     * @return  found calendar date
     */
    PlainDate withDaysSinceUTC(long utcDays) {

        return TRANSFORMER.transform(utcDays);

    }

    /**
     * <p>Erzeugt ein neues Datum passend zur angegebenen absoluten Zeit. </p>
     *
     * @param   ut      unix time
     * @param   offset  shift of local time relative to UTC
     * @return  new calendar date
     */
    static PlainDate from(
        UnixTime ut,
        ZonalOffset offset
    ) {

        long localSeconds = ut.getPosixTime() + offset.getIntegralAmount();
        int localNanos = ut.getNanosecond() + offset.getFractionalAmount();

        if (localNanos < 0) {
            localSeconds--;
        } else if (localNanos >= 1000000000) {
            localSeconds++;
        }

        long mjd =
            EpochDays.MODIFIED_JULIAN_DATE.transform(
                MathUtils.floorDivide(localSeconds, 86400),
                EpochDays.UNIX);
        long packedDate = GregorianMath.toPackedDate(mjd);

        return PlainDate.of(
            GregorianMath.readYear(packedDate),
            GregorianMath.readMonth(packedDate),
            GregorianMath.readDayOfMonth(packedDate)
        );

    }

    /**
     * <p>Dient der Serialisierungsunterst&uuml;tzung. </p>
     *
     * @param   elementName     name of element
     * @return  found element or {@code null}
     */
    // optional
    static Object lookupElement(String elementName) {

        return ELEMENTS.get(elementName);

    }

    /**
     * <p>Executes an addition of {@code amount} in calendar units to given
     * context. </p>
     *
     * @param   unit        calendar unit
     * @param   context     calendar date
     * @param   amount      amount to be added
     * @param   policy      overflow policy
     * @return  result of addition
     */
    static PlainDate doAdd(
        CalendarUnit unit,
        PlainDate context,
        long amount,
        OverflowPolicy policy
    ) {

        switch (unit) {
            case MILLENNIA:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 1000),
                    policy);
            case CENTURIES:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 100),
                    policy);
            case DECADES:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12 * 10),
                    policy);
            case YEARS:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 12),
                    policy);
            case QUARTERS:
                return doAdd(
                    CalendarUnit.MONTHS,
                    context,
                    MathUtils.safeMultiply(amount, 3),
                    policy);
            case MONTHS:
                long months =
                    MathUtils.safeAdd(context.getEpochMonths(), amount);
                return PlainDate.fromEpochMonths(
                    months,
                    context.dayOfMonth,
                    policy);
            case WEEKS:
                return doAdd(
                    CalendarUnit.DAYS,
                    context,
                    MathUtils.safeMultiply(amount, 7),
                    policy);
            case DAYS:
                PlainDate date = addDays(context, amount);
                if (policy == OverflowPolicy.END_OF_MONTH) {
                    return PlainDate.of(
                        date.year,
                        date.month,
                        GregorianMath.getLengthOfMonth(date.year, date.month)
                    );
                } else {
                    return date;
                }
            default:
                throw new UnsupportedOperationException(unit.name());
        }

    }

    /**
     * <p>Liefert die Epochenmonate relativ zu 1970. </p>
     *
     * @return  epoch months relative to 1970
     */
    long getEpochMonths() {

        return ((this.year - 1970) * 12L + this.month - 1);

    }

    /**
     * <p>Ermittelt den Tag des Jahres. </p>
     *
     * @return  int
     */
    int getDayOfYear() {

        switch (this.month) {
            case 1:
                return this.dayOfMonth;
            case 2:
                return 31 + this.dayOfMonth;
            default:
                return (
                    DAY_OF_YEAR_PER_MONTH[this.month - 2]
                    + this.dayOfMonth
                    + (GregorianMath.isLeapYear(this.year) ? 1 : 0));
        }

    }

    /**
     * <p>Bestimmt den Wochentag. </p>
     *
     * @return  day of week as enum
     */
    Weekday getDayOfWeek() {

        return Weekday.valueOf(
            GregorianMath.getDayOfWeek(
                this.year,
                this.month,
                this.dayOfMonth
            )
        );

    }

    /**
     * <p>Liefert die ISO-Kalenderwoche des Jahres. </p>
     *
     * <p>Als erste Kalenderwoche gilt die Woche, die mindestens vier Tage hat
     * und mit dem Montag anf&auml;ngt. Die Woche davor ist dann die letzte
     * Woche des vorherigen Jahres und kann noch in das aktuelle Jahr
     * hineinreichen. </p>
     *
     * @return  week of year in the range (1-53)
     * @see     Weekmodel#ISO
     */
    int getWeekOfYear() {

        return this.get(Weekmodel.ISO.weekOfYear()).intValue();

    }

    private int getDayOfQuarter() {

        switch (this.month) {
            case 1:
            case 4:
            case 7:
            case 10:
                return this.dayOfMonth;
            case 2:
            case 8:
            case 11:
                return 31 + this.dayOfMonth;
            case 3:
                return (
                    (GregorianMath.isLeapYear(this.year) ? 60 : 59)
                    + this.dayOfMonth);
            case 5:
                return 30 + this.dayOfMonth;
            case 6:
            case 12:
                return 61 + this.dayOfMonth;
            case 9:
                return 62 + this.dayOfMonth;
            default:
                throw new AssertionError("Unknown month: " + this.month);
        }

    }

    private PlainDate withYear(int year) {

        if (this.year == year) {
            return this;
        }

        int mlen = GregorianMath.getLengthOfMonth(year, this.month);

        return PlainDate.of(
            year,
            this.month,
            Math.min(mlen, this.dayOfMonth)
        );

    }

    private PlainDate withMonth(int month) {

        if (this.month == month) {
            return this;
        }

        int mlen = GregorianMath.getLengthOfMonth(this.year, month);

        return PlainDate.of(
            this.year,
            month,
            Math.min(mlen, this.dayOfMonth)
        );

    }

    private PlainDate withDayOfMonth(int dayOfMonth) {

        if (this.dayOfMonth == dayOfMonth) {
            return this;
        }

        return PlainDate.of(this.year, this.month, dayOfMonth);

    }

    private PlainDate withDayOfWeek(Weekday dayOfWeek) {

        Weekday old = this.getDayOfWeek();

        if (old == dayOfWeek) {
            return this;
        }

        return TRANSFORMER.transform(
            MathUtils.safeAdd(
                this.getDaysSinceUTC(),
                dayOfWeek.getValue() - old.getValue()
            )
        );

    }

    private PlainDate withDayOfYear(int dayOfYear) {

        if (this.getDayOfYear() == dayOfYear) {
            return this;
        }

        return PlainDate.of(this.year, dayOfYear);

    }

    private static PlainDate fromEpochMonths(
        long emonths,
        int dayOfMonth,
        OverflowPolicy policy
    ) {

        int year =
            MathUtils.safeCast(
                MathUtils.safeAdd(
                    MathUtils.floorDivide(emonths, 12),
                    1970
                )
            );
        int month = MathUtils.floorModulo(emonths, 12) + 1;
        int max = GregorianMath.getLengthOfMonth(year, month);
        int dom = dayOfMonth;

        if (dayOfMonth > max) {
            switch (policy) {
                case PREVIOUS_VALID_DATE:
                case END_OF_MONTH:
                    dom = max;
                    break;
                case NEXT_VALID_DATE:
                    return PlainDate.fromEpochMonths(
                        MathUtils.safeAdd(emonths, 1),
                        1,
                        policy);
                case CARRY_OVER:
                    return PlainDate.fromEpochMonths(
                        MathUtils.safeAdd(emonths, 1),
                        dayOfMonth - max,
                        policy);
                case UNLESS_INVALID:
                    StringBuilder sb = new StringBuilder(32);
                    sb.append("Day of month out of range: ");
                    formatYear(sb, year);
                    format2Digits(sb, month);
                    format2Digits(sb, dayOfMonth);
                    throw new ChronoException(sb.toString());
                default:
                    throw new UnsupportedOperationException(
                        "Not implemented: " + policy.toString());
            }
        } else if(
            (dayOfMonth < max)
            && (policy == OverflowPolicy.END_OF_MONTH)
        ) {
            dom = max;
        }

        return PlainDate.of(year, month, dom);

    }

    private static PlainDate addDays(
        PlainDate date,
        long amount
    ) {

        long dom = MathUtils.safeAdd(date.dayOfMonth, amount);

        if ((dom >= 1) && (dom <= 28)) {
            return PlainDate.of(date.year, date.month, (int) dom);
        }

        long doy = MathUtils.safeAdd(date.getDayOfYear(), amount);

        if ((doy >= 1) && (doy <= 365)) {
            return PlainDate.of(date.year, (int) doy);
        }

        long utcDays =
            MathUtils.safeAdd(date.getDaysSinceUTC(), amount);
        return TRANSFORMER.transform(utcDays);

    }

    private static void fill(
        Map<String, Object> map,
        ChronoElement<?> element
    ) {

        map.put(element.name(), element);

    }

    private static void formatYear(
        StringBuilder sb,
        int year
    ) {

        int value = year;

        if (value < 0) {
            sb.append('-');
            value = MathUtils.safeNegate(year);
        }

        if (value >= 10000) {
            if (year > 0) {
                sb.append('+');
            }
        } else if (value < 1000) {
            sb.append('0');
            if (value < 100) {
                sb.append('0');
                if (value < 10) {
                    sb.append('0');
                }
            }
        }

        sb.append(value);

    }

    private static void format2Digits(
        StringBuilder sb,
        int value
    ) {

        sb.append('-');

        if (value < 10) {
            sb.append('0');
        }

        sb.append(value);

    }

    private static
    void registerUnits(TimeAxis.Builder<IsoDateUnit, PlainDate> builder) {

        Set<CalendarUnit> monthly =
            EnumSet.range(CalendarUnit.MILLENNIA, CalendarUnit.MONTHS);
        Set<CalendarUnit> daily =
            EnumSet.range(CalendarUnit.WEEKS, CalendarUnit.DAYS);

        for (CalendarUnit unit : CalendarUnit.values()) {
            builder.appendUnit(
                unit,
                new CalendarUnit.Rule<PlainDate>(unit),
                unit.getLength(),
                (unit.compareTo(CalendarUnit.WEEKS) < 0) ? monthly : daily
            );
        }

    }

    /**
     * @serialData  Uses <a href="../../serialized-form.html#net.time4j.SPX">
     *              a dedicated serialization form</a> as proxy. The format
     *              is bit-compressed. The first byte contains in the four
     *              most significant bits the type-ID {@code 1}. The following
     *              bits 4-7 contain the month. The second byte contains
     *              at the bits 1-2 a year mark: 1 = year in the range
     *              1850-2100, 2 = four-digit-year, 3 = year number with more
     *              than four digits. The five least significant bits of second
     *              byte contain the day of month. Then the year will be written
     *              dependent on the year mark. Is the mark 1 then the year
     *              will be written as byte, if the mark is 2 then the year
     *              will be written as short else as int with four bytes.
     *
     * Schematic algorithm:
     *
     * <pre>
     *  int range;
     *
     *  if (year >= 1850 && year <= 2100) {
     *      range = 1;
     *  } else if (Math.abs(year) < 10000) {
     *      range = 2;
     *  } else {
     *      range = 3;
     *  }
     *
     *  int header = 1;
     *  header <<= 4;
     *  header |= month;
     *  out.writeByte(header);
     *
     *  int header2 = range;
     *  header2 <<= 5;
     *  header2 |= dayOfMonth;
     *  out.writeByte(header2);
     *
     *  if (range == 1) {
     *      out.writeByte(year - 1850 - 128);
     *  } else if (range == 2) {
     *      out.writeShort(year);
     *  } else {
     *      out.writeInt(year);
     *  }
     * </pre>
     */
    private Object writeReplace() throws ObjectStreamException {

        return new SPX(this, SPX.DATE_TYPE);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @throws      InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

    //~ Innere Klassen ----------------------------------------------------

    private static class Merger
        implements ChronoMerger<PlainDate> {

        //~ Methoden ------------------------------------------------------

        @Override
        public PlainDate createFrom(
            TimeSource<?> clock,
            AttributeQuery attributes
        ) {

            Timezone zone;

            if (attributes.contains(Attributes.TIMEZONE_ID)) {
                zone = Timezone.of(attributes.get(Attributes.TIMEZONE_ID));
            } else {
                zone = Timezone.ofSystem();
            }

            final UnixTime ut = clock.currentTime();
            return PlainDate.from(ut, zone.getOffset(ut));

        }

        @Override
        public PlainDate createFrom(
            ChronoEntity<?> entity,
            AttributeQuery attributes
        ) {

            if (entity.contains(CALENDAR_DATE)) {
                return entity.get(CALENDAR_DATE);
            }

            if (entity.contains(EpochDays.MODIFIED_JULIAN_DATE)) {
                Long mjd = entity.get(EpochDays.MODIFIED_JULIAN_DATE);
                long utcDays =
                    EpochDays.UTC.transform(
                        mjd.longValue(),
                        EpochDays.MODIFIED_JULIAN_DATE);
                return TRANSFORMER.transform(utcDays);
            }

            Integer year = null;

            if (entity.contains(YEAR)) {
                year = entity.get(YEAR);
            } else if (entity.contains(YEAR_OF_ERA)) {
                if (entity.contains(ERA)) {
                    int yearOfEra = entity.get(YEAR_OF_ERA).intValue();
                    switch (entity.get(ERA)) {
                        case AD:
                            year = Integer.valueOf(yearOfEra);
                            break;
                        case BC:
                            year = Integer.valueOf(1 - yearOfEra);
                            break;
                        default:
                            // no year;
                    }
                } else {
                    Leniency leniency =
                        attributes.get(Attributes.LENIENCY, Leniency.SMART);
                    if (!leniency.isStrict()) {
                        year = entity.get(YEAR_OF_ERA);
                    }
                }
            }

            if (year != null) {
                Month month = null;
                if (entity.contains(MONTH_OF_YEAR)) {
                    month = entity.get(MONTH_OF_YEAR);
                } else if (entity.contains(MONTH_AS_NUMBER)) {
                    Integer mNum = entity.get(MONTH_AS_NUMBER);
                    month = Month.valueOf(mNum.intValue());
                }

                if (
                    (month != null)
                    && entity.contains(DAY_OF_MONTH)
                ) {
                    Integer dayOfMonth = entity.get(DAY_OF_MONTH);

                    // Standardszenario
                    return PlainDate.of(
                        year.intValue(),
                        month,
                        dayOfMonth.intValue());

                }

                if (entity.contains(DAY_OF_YEAR)) {
                    Integer dayOfYear = entity.get(DAY_OF_YEAR);

                    // Ordinaldatum
                    return PlainDate.of(
                        year.intValue(),
                        dayOfYear.intValue());
                }

                if (
                    entity.contains(QUARTER_OF_YEAR)
                    && entity.contains(DAY_OF_QUARTER)
                ) {
                    Quarter q = entity.get(QUARTER_OF_YEAR);
                    Integer doq = entity.get(DAY_OF_QUARTER);

                    // Quartalsdatum
                    int doy = doq.intValue();
                    boolean leap = GregorianMath.isLeapYear(year.intValue());
                    if (q == Quarter.Q2) {
                        doy += ((leap ? 91 : 90));
                    } else if (q == Quarter.Q3) {
                        doy += ((leap ? 91 : 90) + 91);
                    } else if (q == Quarter.Q4) {
                        doy += ((leap ? 91 : 90) + 91 + 92);
                    }
                    return PlainDate.of(year.intValue(), doy);
                }
            }

            if (
                entity.contains(YEAR_OF_WEEKDATE)
                && entity.contains(Weekmodel.ISO.weekOfYear())
                && entity.contains(DAY_OF_WEEK)
            ) {
                Integer yearOfWeekdate =
                    entity.get(YEAR_OF_WEEKDATE);
                Integer weekOfYear =
                    entity.get(Weekmodel.ISO.weekOfYear());
                Weekday dayOfWeek =
                    entity.get(DAY_OF_WEEK);

                // Wochendatum
                return PlainDate.of(
                    yearOfWeekdate.intValue(),
                    weekOfYear.intValue(),
                    dayOfWeek);
            }

            return null;

        }

        @Override
        public ChronoEntity<?> preformat(
            PlainDate context,
            AttributeQuery attributes
        ) {

            return context;

        }

    }

    private static class Transformer
        implements CalendarSystem<PlainDate> {

        //~ Statische Felder/Initialisierungen ----------------------------

        private static final long MIN_LONG;
        private static final long MAX_LONG;

        static {
            MIN_LONG = doTransform(PlainDate.MIN);
            MAX_LONG = doTransform(PlainDate.MAX);
        }

        //~ Methoden ------------------------------------------------------

        @Override
        public PlainDate transform(long utcDays) {

            if (utcDays == MIN_LONG) {
                return PlainDate.MIN;
            } else if (utcDays == MAX_LONG) {
                return PlainDate.MAX;
            }

            long mjd =
                EpochDays.MODIFIED_JULIAN_DATE.transform(
                    utcDays,
                    EpochDays.UTC);
            long packedDate = GregorianMath.toPackedDate(mjd);

            return PlainDate.of(
                GregorianMath.readYear(packedDate),
                GregorianMath.readMonth(packedDate),
                GregorianMath.readDayOfMonth(packedDate)
            );

        }

        @Override
        public long transform(PlainDate date) {

            return doTransform(date);

        }

        private static long doTransform(PlainDate date) {

            return EpochDays.UTC.transform(
                GregorianMath.toMJD(date),
                EpochDays.MODIFIED_JULIAN_DATE
            );

        }

        @Override
        public long getMinimumSinceUTC() {

            return MIN_LONG;

        }

        @Override
        public long getMaximumSinceUTC() {

            return MAX_LONG;

        }

        @Override
        public StartOfDay getStartOfDay() {

            return StartOfDay.MIDNIGHT;

        }

        @Override
        public List<CalendarEra> getEras() {

            return Collections.emptyList();

        }

    }

    private static class DateElementRule
        implements ElementRule<PlainDate, PlainDate> {

        //~ Methoden ------------------------------------------------------

        @Override
        public PlainDate getValue(PlainDate context) {

            return context;

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            PlainDate value,
            boolean lenient
        ) {

            if (value == null) {
                throw new NullPointerException("Missing date value.");
            }

            return value;

        }

        @Override
        public boolean isValid(
            PlainDate context,
            PlainDate value
        ) {

            return (value != null);

        }

        @Override
        public PlainDate getMinimum(PlainDate context) {

            return PlainDate.MIN;

        }

        @Override
        public PlainDate getMaximum(PlainDate context) {

            return PlainDate.MAX;

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return null;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return null;

        }

    }

    private static class IntegerElementRule
        implements ElementRule<PlainDate, Integer> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<Integer> element;
        private final int index;

        //~ Konstruktoren -------------------------------------------------

        IntegerElementRule(ChronoElement<Integer> element) {
            super();

            this.element = element;

            if (element instanceof IntegerDateElement) {
                this.index = ((IntegerDateElement) element).getIndex();
            } else {
                this.index = -1;
            }

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Integer getValue(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return Integer.valueOf(context.year);
                case IntegerDateElement.MONTH:
                    return Integer.valueOf(context.month);
                case IntegerDateElement.DAY_OF_MONTH:
                    return Integer.valueOf(context.dayOfMonth);
                case IntegerDateElement.DAY_OF_YEAR:
                    return Integer.valueOf(context.getDayOfYear());
                case IntegerDateElement.DAY_OF_QUARTER:
                    return Integer.valueOf(context.getDayOfQuarter());
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            Integer value,
            boolean lenient
        ) {

            int v = value.intValue();

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return context.withYear(v);
                case IntegerDateElement.MONTH:
                    if (lenient) {
                        return context.plus(
                            (v - context.month),
                            CalendarUnit.MONTHS);
                    } else {
                        return context.withMonth(v);
                    }
                case IntegerDateElement.DAY_OF_MONTH:
                    if (lenient) {
                        return context.plus(
                            (v - context.dayOfMonth),
                            CalendarUnit.DAYS);
                    } else {
                        return context.withDayOfMonth(v);
                    }
                case IntegerDateElement.DAY_OF_YEAR:
                    if (lenient) {
                        return context.plus(
                            (v - context.getDayOfYear()),
                            CalendarUnit.DAYS);
                    } else {
                        return context.withDayOfYear(v);
                    }
                case IntegerDateElement.DAY_OF_QUARTER:
                    if (
                        lenient
                        || ((v >= 1) && (v <= getMaximumOfQuarterDay(context)))
                    ) {
                        return context.plus(
                            (v - context.getDayOfQuarter()),
                            CalendarUnit.DAYS);
                    } else {
                        throw new IllegalArgumentException(
                            "Out of range: " + value);
                    }
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public boolean isValid(
            PlainDate context,
            Integer value
        ) {

            if (value == null) {
                return false;
            }

            int v = value.intValue();

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return (
                        (v >= GregorianMath.MIN_YEAR)
                        && (v <= GregorianMath.MAX_YEAR)
                    );
                case IntegerDateElement.MONTH:
                    return ((v >= 1) && (v <= 12));
                case IntegerDateElement.DAY_OF_MONTH:
                    int mlen =
                        GregorianMath.getLengthOfMonth(
                            context.year,
                            context.month);
                    return ((v >= 1) && (v <= mlen));
                case IntegerDateElement.DAY_OF_YEAR:
                    boolean leapyear = GregorianMath.isLeapYear(context.year);
                    return ((v >= 1) && (v <= (leapyear ? 366 : 365)));
                case IntegerDateElement.DAY_OF_QUARTER:
                    int max = getMaximumOfQuarterDay(context);
                    return ((v >= 1) && (v <= max));
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public Integer getMinimum(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MIN_YEAR;
                case IntegerDateElement.MONTH:
                case IntegerDateElement.DAY_OF_MONTH:
                case IntegerDateElement.DAY_OF_YEAR:
                case IntegerDateElement.DAY_OF_QUARTER:
                    return Integer.valueOf(1);
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public Integer getMaximum(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MAX_YEAR;
                case IntegerDateElement.MONTH:
                    return Integer.valueOf(12);
                case IntegerDateElement.DAY_OF_MONTH:
                    return Integer.valueOf(
                        GregorianMath.getLengthOfMonth(
                            context.year,
                            context.month));
                case IntegerDateElement.DAY_OF_YEAR:
                    return (
                        GregorianMath.isLeapYear(context.year)
                        ? LEAP_YEAR_LEN
                        : STD_YEAR_LEN);
                case IntegerDateElement.DAY_OF_QUARTER:
                    return Integer.valueOf(getMaximumOfQuarterDay(context));
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return this.getChild(context);

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return this.getChild(context);

        }

        private ChronoElement<?> getChild(PlainDate context) {

            switch (this.index) {
                case IntegerDateElement.YEAR:
                    return MONTH_AS_NUMBER;
                case IntegerDateElement.MONTH:
                    return DAY_OF_MONTH;
                case IntegerDateElement.DAY_OF_MONTH:
                case IntegerDateElement.DAY_OF_YEAR:
                case IntegerDateElement.DAY_OF_QUARTER:
                    return null;
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        private static int getMaximumOfQuarterDay(PlainDate context) {

            int q = ((context.month - 1) / 3) + 1;

            if (q == 1) {
                return (GregorianMath.isLeapYear(context.year) ? 91 : 90);
            } else if (q == 2) {
                return 91;
            } else {
                return 92;
            }

        }

    }

    private static class EnumElementRule<V extends Enum<V>>
        implements ElementRule<PlainDate, V> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<V> element;
        private final int index;

        //~ Konstruktoren -------------------------------------------------

        EnumElementRule(ChronoElement<V> element) {
            super();

            this.element = element;

            if (element instanceof EnumElement) {
                this.index = ((EnumElement) element).getIndex();
            } else {
                this.index = -1;
            }

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public V getValue(PlainDate context) {

            Object ret;

            switch (this.index) {
                case EnumElement.MONTH:
                    ret = Month.valueOf(context.month);
                    break;
                case EnumElement.DAY_OF_WEEK:
                    ret = context.getDayOfWeek();
                    break;
                case EnumElement.QUARTER_OF_YEAR:
                    ret = Quarter.valueOf(((context.month - 1) / 3) + 1);
                    break;
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

            return this.element.getType().cast(ret);

        }

        @Override
        public V getMinimum(PlainDate context) {

            return this.element.getDefaultMinimum();

        }

        @Override
        public V getMaximum(PlainDate context) {

            return this.element.getDefaultMaximum();

        }

        @Override
        public boolean isValid(
            PlainDate context,
            V value
        ) {

            return (value != null);

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            V value,
            boolean lenient
        ) {

            switch (this.index) {
                case EnumElement.MONTH:
                    return context.withMonth(
                        Month.class.cast(value).getValue());
                case EnumElement.DAY_OF_WEEK:
                    return context.withDayOfWeek(Weekday.class.cast(value));
                case EnumElement.QUARTER_OF_YEAR:
                    int q1 = ((context.month - 1) / 3) + 1;
                    int q2 = Quarter.class.cast(value).getValue();
                    return context.plus((q2 - q1), CalendarUnit.QUARTERS);
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return this.getChild(context);

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return this.getChild(context);

        }

        private ChronoElement<?> getChild(PlainDate context) {

            switch (this.index) {
                case EnumElement.MONTH:
                    return DAY_OF_MONTH;
                case EnumElement.DAY_OF_WEEK:
                    return null;
                case EnumElement.QUARTER_OF_YEAR:
                    return DAY_OF_QUARTER;
                default:
                    throw new UnsupportedOperationException(
                        this.element.name());
            }

        }

    }

    private static class WIMRule
        implements ElementRule<PlainDate, Integer> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<Integer> element;

        //~ Konstruktoren -------------------------------------------------

        WIMRule(ChronoElement<Integer> element) {
            super();

            this.element = element;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Integer getValue(PlainDate context) {

            return Integer.valueOf(((context.dayOfMonth - 1) / 7) + 1);

        }

        @Override
        public Integer getMinimum(PlainDate context) {

            return Integer.valueOf(1);

        }

        @Override
        public Integer getMaximum(PlainDate context) {

            int y = context.year;
            int m = context.month;
            int d = context.dayOfMonth;
            int maxday = GregorianMath.getLengthOfMonth(y, m);
            int n = 0;

            while (d + (n + 1) * 7 <= maxday) {
                n++;
            }

            return Integer.valueOf(((d + n * 7 - 1) / 7) + 1);

        }

        @Override
        public boolean isValid(
            PlainDate context,
            Integer value
        ) {

            if (value == null) {
                return false;
            }

            int wim = value.intValue();
            int max = this.getMaximum(context).intValue();
            return ((wim >= 1) && (wim <= max));

        }

        @Override
        public PlainDate withValue(
            PlainDate context,
            Integer value,
            boolean lenient
        ) {

            int wim = value.intValue();
            int max = this.getMaximum(context).intValue();
            int old = ((context.dayOfMonth - 1) / 7) + 1;

            if (
                lenient
                || ((wim >= 1) && (wim <= max))
            ) {
                return context.plus(
                    wim - old,
                    CalendarUnit.WEEKS);
            } else {
                throw new IllegalArgumentException(
                    "Out of range: " + value);
            }

        }

        @Override
        public ChronoElement<?> getChildAtFloor(PlainDate context) {

            return null;

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(PlainDate context) {

            return null;

        }

    }

}
