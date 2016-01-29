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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.SpinnerNumberModel;

class YearSpinnerModel extends SpinnerNumberModel {

	public static final String PROPERTY_NAME_LOCALE = "locale";

	public static final String PROPERTY_NAME_DATE = "date";

	public static final String PROPERTY_NAME_ZONE = "zone";

	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
			this);

	private Locale locale;

	private TimeZone zone;

	private Calendar calendar;

	public YearSpinnerModel(Date date, Locale locale, TimeZone zone) {
		this.locale = locale;
		this.zone = zone;
		createLocaleAndZoneSensitive();
		calendar.setTime(date);
	}

	private void createLocaleAndZoneSensitive() {
		if (calendar != null) {
			Date old = calendar.getTime();
			calendar = Calendar.getInstance(zone, locale);
			calendar.setTime(old);
		} else
			calendar = Calendar.getInstance(zone, locale);
	}

	public Object getValue() {
		return new Integer(calendar.get(Calendar.YEAR));
	}

	public void setValue(Object value) {
		Number newVal = (Number) value;
		Number oldVal = (Number) getValue();
		if (oldVal.longValue() != newVal.longValue()) {

			int diff = newVal.intValue() - oldVal.intValue();
			int sign = diff > 0 ? 1 : -1;
			if (diff < 0)
				diff = -diff;
			Date oldDate = calendar.getTime();

			for (int i = 0; i < diff; i++)
				calendar.add(Calendar.YEAR, sign);

			changeSupport.firePropertyChange(PROPERTY_NAME_DATE, oldDate,
					getDate());
			fireStateChanged();
		}
	}

	public Object getNextValue() {

		Integer currVal = (Integer) getValue();
		int newVal = currVal.intValue() + 1;

		if (newVal <= calendar.getActualMaximum(Calendar.YEAR))
			return new Integer(newVal);

		return currVal;
	}

	public Object getPreviousValue() {
		Integer currVal = (Integer) getValue();
		int newVal = currVal.intValue() - 1;

		if (newVal >= calendar.getActualMinimum(Calendar.YEAR))
			return new Integer(newVal);

		return currVal;
	}

	public Date getDate() {
		return calendar.getTime();
	}

	public void setDate(Date date) {
		Date old = calendar.getTime();
		if (!old.equals(date)) {
			calendar.setTime(date);
			changeSupport.firePropertyChange(PROPERTY_NAME_DATE, old, date);
			fireStateChanged();
		}
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		Locale old = this.locale;
		this.locale = locale;
		createLocaleAndZoneSensitive();
		changeSupport.firePropertyChange(PROPERTY_NAME_LOCALE, old, locale);
		fireStateChanged();
	}

	public TimeZone getZone() {
		return zone;
	}

	public void setZone(TimeZone zone) {
		TimeZone old = this.zone;
		this.zone = zone;
		createLocaleAndZoneSensitive();
		changeSupport.firePropertyChange(PROPERTY_NAME_LOCALE, old, locale);
		fireStateChanged();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return changeSupport.getPropertyChangeListeners();
	}

	public PropertyChangeListener[] getPropertyChangeListeners(
			String propertyName) {
		return changeSupport.getPropertyChangeListeners(propertyName);
	}

	public boolean hasListeners(String propertyName) {
		return changeSupport.hasListeners(propertyName);
	}

}
