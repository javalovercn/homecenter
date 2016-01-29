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

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

class MonthComboBoxModel extends AbstractListModel implements
		ComboBoxModel {

	public static final String PROPERTY_NAME_LOCALE = "locale";

	public static final String PROPERTY_NAME_DATE = "date";

	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
			this);

	private Calendar calendar;

	private Locale locale;

	private TimeZone zone;

	public MonthComboBoxModel(Date date, Locale locale, TimeZone zone) {
		super();
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

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		Locale old = this.locale;
		this.locale = locale;
		createLocaleAndZoneSensitive();
		changeSupport.firePropertyChange(PROPERTY_NAME_LOCALE, old, locale);
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public Date getDate() {
		return calendar.getTime();
	}

	public void setDate(Date date) {
		Date old = getDate();
		calendar.setTime(date);
		changeSupport.firePropertyChange(PROPERTY_NAME_DATE, old, date);
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public void setSelectedItem(Object anItem) {
		Date aDate = (Date) anItem;
		setDate(aDate);
	}

	public Object getSelectedItem() {
		return calendar.getTime();
	}

	public int getSize() {
		return calendar.getActualMaximum(Calendar.MONTH) + 1;
	}

	public Object getElementAt(int index) {
		Calendar c = Calendar.getInstance(locale);
		c.setTime(calendar.getTime());

		c.set(Calendar.MONTH, 0);
		for (int i = 0; i < index; i++)
			c.add(Calendar.MONTH, 1);

		return c.getTime();
	}

	public TimeZone getZone() {
		return zone;
	}

	public void setZone(TimeZone zone) {
		this.zone = zone;
		createLocaleAndZoneSensitive();
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
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

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

}
