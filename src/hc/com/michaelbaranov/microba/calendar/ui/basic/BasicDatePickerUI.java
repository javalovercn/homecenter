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

import hc.com.michaelbaranov.microba.calendar.CalendarPane;
import hc.com.michaelbaranov.microba.calendar.CalendarResources;
import hc.com.michaelbaranov.microba.calendar.DatePicker;
import hc.com.michaelbaranov.microba.calendar.HolidayPolicy;
import hc.com.michaelbaranov.microba.calendar.VetoPolicy;
import hc.com.michaelbaranov.microba.calendar.resource.Resource;
import hc.com.michaelbaranov.microba.calendar.ui.CalendarPaneUI;
import hc.com.michaelbaranov.microba.calendar.ui.DatePickerUI;
import hc.com.michaelbaranov.microba.common.CommitEvent;
import hc.com.michaelbaranov.microba.common.CommitListener;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;


public class BasicDatePickerUI extends DatePickerUI implements
		PropertyChangeListener {

	protected static final String POPUP_KEY = "##BasicVetoDatePickerUI.popup##";

	protected DatePicker peer;

	protected CalendarPane calendarPane;

	protected JButton button;

	protected JPopupMenu popup;

	protected JFormattedTextField field;

	protected ComponentListener componentListener;

	public static ComponentUI createUI(JComponent c) {
		return new BasicDatePickerUI();
	}

	public void installUI(JComponent c) {
		peer = (DatePicker) c;
		installComponents();
		istallListeners();
		installKeyboardActions();
	}

	public void uninstallUI(JComponent c) {
		uninstallKeyboardActions();
		uninstallListeners();
		uninstallComponents();
		peer = null;
	}

	protected void installKeyboardActions() {
		InputMap input = peer
				.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK),
				POPUP_KEY);
		input.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), POPUP_KEY);

		peer.getActionMap().put(POPUP_KEY, new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				showPopup(true);
			}
		});

	}

	protected void uninstallKeyboardActions() {
		InputMap input = peer
				.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		input
				.remove(KeyStroke.getKeyStroke(KeyEvent.VK_C,
						InputEvent.ALT_MASK));
		input.remove(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));

		peer.getActionMap().remove(POPUP_KEY);
	}

	protected void istallListeners() {
		peer.addPropertyChangeListener(this);
	}

	protected void uninstallListeners() {
		peer.removePropertyChangeListener(this);
	}

	protected void uninstallComponents() {
		button.removeActionListener(componentListener);
		field.removePropertyChangeListener(componentListener);

		calendarPane.removePropertyChangeListener(componentListener);
		calendarPane.removeCommitListener(componentListener);
		calendarPane.removeActionListener(componentListener);

		peer.remove(field);
		peer.remove(button);
		popup = null;
		calendarPane = null;
		button = null;
		field = null;

	}

	protected void installComponents() {
		field = new JFormattedTextField(createFormatterFactory());
		field.setValue(peer.getDate());
		field.setFocusLostBehavior(peer.getFocusLostBehavior());
		field.setEditable(peer.isFieldEditable());
		field.setToolTipText(peer.getToolTipText());
		// button
		button = new JButton();
		button.setFocusable(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setToolTipText(peer.getToolTipText());

		setSimpeLook(false);
		// calendar
		calendarPane = new CalendarPane(peer.getStyle());
		calendarPane.setShowTodayButton(peer.isShowTodayButton());
		calendarPane.setFocusLostBehavior(JFormattedTextField.REVERT);
		calendarPane.setFocusCycleRoot(true);
		calendarPane.setBorder(BorderFactory.createEmptyBorder(1, 3, 0, 3));
		calendarPane.setStripTime(false);
		calendarPane.setLocale(peer.getLocale());
		calendarPane.setZone(peer.getZone());
		calendarPane.setFocusable(peer.isDropdownFocusable());
		calendarPane.setColorOverrideMap(peer.getColorOverrideMap());
		// popup
		popup = new JPopupMenu();
		popup.setLayout(new BorderLayout());
		popup.add(calendarPane, BorderLayout.CENTER);
		popup.setLightWeightPopupEnabled(true);
		// add
		peer.setLayout(new BorderLayout());

		switch (peer.getPickerStyle()) {
		case DatePicker.PICKER_STYLE_FIELD_AND_BUTTON:
			peer.add(field, BorderLayout.CENTER);
			peer.add(button, BorderLayout.EAST);
			break;
		case DatePicker.PICKER_STYLE_BUTTON:
			peer.add(button, BorderLayout.EAST);
			break;
		}

		peer.revalidate();
		peer.repaint();

		componentListener = new ComponentListener();

		button.addActionListener(componentListener);
		field.addPropertyChangeListener(componentListener);

		calendarPane.addPropertyChangeListener(componentListener);
		calendarPane.addCommitListener(componentListener);
		calendarPane.addActionListener(componentListener);

		peerDateChanged(peer.getDate());
	}

	public void setSimpeLook(boolean b) {
		if (b) {
			field.setBorder(BorderFactory.createEmptyBorder());
			button.setText("...");
			button.setIcon(null);
		} else {
			field.setBorder(new JTextField().getBorder());
			button.setText("");
			button.setIcon(new ImageIcon(Resource.class
					.getResource("picker-16.png")));
		}

	}

	public void showPopup(boolean visible) {
		if (visible) {

			// try to apply date to calendar pane popup, but not cause commit
			if (peer.isKeepTime())
				try {
					AbstractFormatter formatter = field.getFormatter();
					Date value = (Date) formatter
							.stringToValue(field.getText());
					calendarPane
							.removePropertyChangeListener(componentListener);
					calendarPane.setDate(value);
					calendarPane.addPropertyChangeListener(componentListener);

				} catch (ParseException e) {
					// ignore
				} catch (PropertyVetoException e) {
					// can not happen
				}

			popup.show(peer, 0, peer.getHeight());
			calendarPane.requestFocus(false);
		} else {
			popup.setVisible(false);
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (JComponent.TOOL_TIP_TEXT_KEY.equals(evt.getPropertyName())) {
			field.setToolTipText((String) evt.getNewValue());
			button.setToolTipText((String) evt.getNewValue());
		} else if (evt.getPropertyName().equals(DatePicker.PROPERTY_NAME_DATE)) {
			Date newValue = (Date) evt.getNewValue();
			peerDateChanged(newValue);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_FIELD_EDITABLE)) {
			field.setEditable(peer.isFieldEditable());
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_FOCUS_LOST_BEHAVIOR)) {
			field.setFocusLostBehavior(peer.getFocusLostBehavior());
		} else if (evt.getPropertyName()
				.equals(DatePicker.PROPERTY_NAME_LOCALE)) {
			field.setFormatterFactory(createFormatterFactory());
			calendarPane.setLocale(peer.getLocale());
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_DATE_FORMAT)) {
			field.setFormatterFactory(createFormatterFactory());
		} else if (evt.getPropertyName().equals(DatePicker.PROPERTY_NAME_ZONE)) {
			field.setFormatterFactory(createFormatterFactory());
			calendarPane.setZone((TimeZone) evt.getNewValue());
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_SHOW_TODAY_BTN)) {
			boolean value = ((Boolean) evt.getNewValue()).booleanValue();
			calendarPane.setShowTodayButton(value);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_SHOW_NONE_BTN)) {
			boolean value = ((Boolean) evt.getNewValue()).booleanValue();
			calendarPane.setShowNoneButton(value);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_SHOW_NUMBER_WEEK)) {
			boolean value = ((Boolean) evt.getNewValue()).booleanValue();
			calendarPane.setShowNumberOfWeek(value);
		} else if (evt.getPropertyName().equals(DatePicker.PROPERTY_NAME_STYLE)) {
			int value = ((Integer) evt.getNewValue()).intValue();
			calendarPane.setStyle(value);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_VETO_POLICY)) {
			calendarPane.setVetoPolicy((VetoPolicy) evt.getNewValue());
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_HOLIDAY_POLICY)) {
			calendarPane.setHolidayPolicy((HolidayPolicy) evt.getNewValue());
		} else if (evt.getPropertyName().equals("focusable")) {
			boolean value = ((Boolean) evt.getNewValue()).booleanValue();
			field.setFocusable(value);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_RESOURCES)) {
			CalendarResources resources = (CalendarResources) evt.getNewValue();
			calendarPane.setResources(resources);
		} else if (evt.getPropertyName().equals("enabled"/*
														 * DatePicker.PROPERTY_NAME_ENABLED
														 */)) {
			boolean value = ((Boolean) evt.getNewValue()).booleanValue();
			field.setEnabled(value);
			button.setEnabled(value);
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_PICKER_STYLE)) {
			peer.updateUI();
		} else if (evt.getPropertyName().equals(
				DatePicker.PROPERTY_NAME_DROPDOWN_FOCUSABLE)) {
			calendarPane.setFocusable(peer.isDropdownFocusable());
		}
	}

	private void peerDateChanged(Date newValue) {
		try {
			calendarPane.removePropertyChangeListener(componentListener);
			calendarPane.setDate(newValue);
			calendarPane.addPropertyChangeListener(componentListener);
		} catch (PropertyVetoException e) {
			// Ignore. CalendarPane has no VetoModel here.
		}
		field.removePropertyChangeListener(componentListener);
		field.setValue(newValue);
		field.addPropertyChangeListener(componentListener);
	}

	private DefaultFormatterFactory createFormatterFactory() {
		return new DefaultFormatterFactory(new DateFormatter(peer
				.getDateFormat()));
	}

	protected class ComponentListener implements ActionListener,
			PropertyChangeListener, CommitListener {
		public void actionPerformed(ActionEvent e) {

			if (e.getSource() != calendarPane)
				showPopup(true);
			else
				showPopup(false);

		}

		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() == calendarPane) {
				if (CalendarPane.PROPERTY_NAME_DATE.equals(evt
						.getPropertyName())) {
					showPopup(false);

					Date fieldValue = null;
					try {
						AbstractFormatter formatter = field.getFormatter();
						fieldValue = (Date) formatter.stringToValue(field
								.getText());

					} catch (ParseException e) {
						fieldValue = (Date) field.getValue();
					}

					if (fieldValue != null || evt.getNewValue() != null) {

						if (peer.isKeepTime() && fieldValue != null
								&& evt.getNewValue() != null) {

							Calendar fieldCal = Calendar.getInstance(peer
									.getZone(), peer.getLocale());
							fieldCal.setTime(fieldValue);

							Calendar valueCal = Calendar.getInstance(peer
									.getZone(), peer.getLocale());
							valueCal.setTime((Date) evt.getNewValue());

							// era
							fieldCal.set(Calendar.ERA, valueCal
									.get(Calendar.ERA));
							// year
							fieldCal.set(Calendar.YEAR, valueCal
									.get(Calendar.YEAR));
							// month
							fieldCal.set(Calendar.MONTH, valueCal
									.get(Calendar.MONTH));
							// date
							fieldCal.set(Calendar.DAY_OF_MONTH, valueCal
									.get(Calendar.DAY_OF_MONTH));
							field.setValue(fieldCal.getTime());
						} else
							field.setValue((Date) evt.getNewValue());

					}
				}
			}
			if (evt.getSource() == field) {
				if ("value".equals(evt.getPropertyName())) {
					Date value = (Date) field.getValue();

					try {
						peer.setDate(value);
					} catch (PropertyVetoException e) {
						field.setValue(peer.getDate());
					}
				}
			}

		}

		public void commit(CommitEvent action) {
			showPopup(false);
			if (field.getValue() != null || calendarPane.getDate() != null)
				field.setValue(calendarPane.getDate());
		}

		public void revert(CommitEvent action) {
			showPopup(false);

		}
	}

	public void commit() throws PropertyVetoException, ParseException {
		field.commitEdit();
	}

	public void revert() {
		peerDateChanged(peer.getDate());
	}

	public void observeMonth(int year, int month) {
		CalendarPaneUI ui = (CalendarPaneUI) calendarPane.getUI();
		ui.observeMonth(year, month);
	}

}
