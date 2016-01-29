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
package hc.com.michaelbaranov.microba.calendar;

import hc.com.michaelbaranov.microba.calendar.ui.DatePickerUI;

import java.beans.PropertyVetoException;
import java.util.Date;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreeCellEditor;


/**
 * This class in a concrete implementation of {@link TableCellEditor} and
 * {@link TreeCellEditor} interfaces. Uses {@link DatePicker} control as en
 * editor. Subclass to extend functionality. *
 * <p>
 * Note: you probably will want to set the property of the {@link DatePicker}
 * {@value DatePicker#PROPERTY_NAME_DROPDOWN_FOCUSABLE} to <code>false</code>
 * before using it to construct {@link DatePickerCellEditor}.
 * 
 * @see DefaultCellEditor
 * 
 * @author Michael Baranov
 * 
 */
public class DatePickerCellEditor extends DefaultCellEditor {

	/**
	 * Constructor.
	 * <p>
	 * Note: you probably will want to set the property of the
	 * {@link DatePicker} {@value DatePicker#PROPERTY_NAME_DROPDOWN_FOCUSABLE}
	 * to <code>false</code> before using it to construct
	 * {@link DatePickerCellEditor}.
	 * 
	 * @param datePicker
	 *            the editor component
	 */
	public DatePickerCellEditor(final DatePicker datePicker) {
		// trick: supply a dummy JCheckBox
		super(new JCheckBox());
		// get back the dummy JCheckBox
		JCheckBox checkBox = (JCheckBox) this.editorComponent;
		// remove listeners installed by super()
		checkBox.removeActionListener(this.delegate);
		// replace editor component with own
		this.editorComponent = datePicker;

		// set simple look
		((DatePickerUI) datePicker.getUI()).setSimpeLook(true);

		// replace delegate with own
		this.delegate = new EditorDelegate() {
			public void setValue(Object value) {
				try {
					((DatePicker) editorComponent).setDate((Date) value);
				} catch (PropertyVetoException e) {
				}
			}

			public Object getCellEditorValue() {
				return ((DatePicker) editorComponent).getDate();
			}

			public void cancelCellEditing() {
				((DatePicker) editorComponent).commitOrRevert();
				super.cancelCellEditing();
			}

			public boolean stopCellEditing() {
				((DatePicker) editorComponent).commitOrRevert();
				return super.stopCellEditing();
			}

		};
		// install listeners
		datePicker.addActionListener(delegate);
		// do not set it to 1
		setClickCountToStart(2);
	}

}