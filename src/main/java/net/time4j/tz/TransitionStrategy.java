/*
 * -----------------------------------------------------------------------
 * Copyright © 2013 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (TransitionStrategy.java) is part of project Time4J.
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

package net.time4j.tz;

import net.time4j.base.GregorianDate;
import net.time4j.base.UnixTime;
import net.time4j.base.WallTime;


/**
 * <p>Dient der Aufl&ouml;sung von lokalen Zeitangaben zu einer UTC-Weltzeit,
 * wenn wegen L&uuml;cken oder &Uuml;berlappungen auf dem lokalen Zeitstrahl
 * Konflikte auftreten. </p>
 *
 * @author  Meno Hochschild
 * @see     net.time4j.ZonalOperator#STRICT_MODE
 */
public interface TransitionStrategy {

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Konvertiert eine lokale Zeitangabe in einen globalen Zeitstempel. </p>
     *
     * @param   localDate   local calendar date in given timezone
     * @param   localTime   local wall time in given timezone
     * @param   timezone    timezone data containing offset history
     * @return  global unix timestamp
     */
    UnixTime resolve(
        GregorianDate localDate,
        WallTime localTime,
        Timezone timezone
    );

}
