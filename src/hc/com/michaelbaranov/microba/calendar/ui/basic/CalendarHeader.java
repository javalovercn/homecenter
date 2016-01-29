/*
 * Microba controls http://sourceforge.net/projects/microba/
 * Copyright (c) 2005-2006, Michael Baranov
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.  
 * 3. Neither the name of the MICROBA, MICHAELBARANOV.COM, MICHAEL BARANOV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY 
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package hc.com.michaelbaranov.microba.calendar.ui.basic;

import hc.com.michaelbaranov.microba.Microba;
import hc.com.michaelbaranov.microba.calendar.CalendarPane;
import hc.com.michaelbaranov.microba.calendar.HolidayPolicy;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;


class CalendarHeader extends JPanel {

	private Locale locale;

	private TimeZone zone;

	private Date date;

	private HolidayPolicy holidayPolicy;

	private Color backgroundColorActive;

	private Color backgroundColorInactive;

	private Color foregroundColorActive;

	private Color foregroundColorInactive;

	private Color foregroundColorWeekendEnabled;

	private Color foregroundColorWeekendDisabled;

	public CalendarHeader(CalendarPane peer, Date date, Locale locale,
			TimeZone zone, HolidayPolicy holidayPolicy) {
		super();

		backgroundColorActive = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_BACKGROUND_ENABLED, peer,
				UIManager.getColor("activeCaption"));
		backgroundColorInactive = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_BACKGROUND_DISABLED, peer,
				UIManager.getColor("inactiveCaption"));
		foregroundColorActive = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_FOREGROUND_ENABLED, peer,
				UIManager.getColor("controlText"));
		foregroundColorInactive = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_FOREGROUND_DISABLED, peer,
				UIManager.getColor("textInactiveText"));
		foregroundColorWeekendEnabled = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_FOREGROUND_WEEKEND_ENABLED,
				peer, Color.RED);
		foregroundColorWeekendDisabled = Microba.getOverridenColor(
				CalendarPane.COLOR_CALENDAR_HEADER_FOREGROUND_WEEKEND_DISABLED,
				peer, foregroundColorInactive);

		this.locale = locale;
		this.zone = zone;
		this.date = date;
		this.holidayPolicy = holidayPolicy;
		reflectData();
	}

	private void reflectData() {

		Calendar cal = Calendar.getInstance(zone, locale);
		cal.setTime(date == null ? new Date() : date);

		SimpleDateFormat fmt = new SimpleDateFormat("E", locale);
		fmt.setTimeZone(zone);

		int numDaysInWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK)
				- cal.getActualMinimum(Calendar.DAY_OF_WEEK) + 1;
		int firstDayOfWeek = cal.getFirstDayOfWeek();

		cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);

		removeAll();
		setLayout(new GridLayout(1, numDaysInWeek, 2, 2));

		setBackground(isEnabled() ? backgroundColorActive
				: backgroundColorInactive);

		for (int i = 0; i < numDaysInWeek; i++) {
			JLabel label = new JLabel();
			// TODO: add option to control limit length:
			label.setText(fmt.format(cal.getTime())/* .substring(0,1) */);
			label.setForeground(isEnabled() ? foregroundColorActive
					: foregroundColorInactive);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
			Font boldFont = label.getFont().deriveFont(Font.BOLD);
			label.setFont(boldFont);
			add(label);

			boolean isHolliday = false;
			if (holidayPolicy != null) {
				isHolliday = holidayPolicy.isWeekend(this, cal);
			}

			if (isHolliday)
				label.setForeground(isEnabled() ? foregroundColorWeekendEnabled
						: foregroundColorWeekendDisabled);

			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
		setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		revalidate();
		repaint();

	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		reflectData();
	}

	public void setDate(Date date) {
		this.date = date;
		reflectData();
	}

	public TimeZone getZone() {
		return zone;
	}

	public void setZone(TimeZone zone) {
		this.zone = zone;
		reflectData();
	}

	public void setHolidayPolicy(HolidayPolicy holidayPolicy) {
		this.holidayPolicy = holidayPolicy;
		reflectData();

	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		reflectData();
	}

}
