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

import java.text.NumberFormat;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

class NoGroupingSpinner extends JSpinner {

	public static class NoGroupingNumberEditor extends NumberEditor {

		public NoGroupingNumberEditor(JSpinner spinner, SpinnerModel model) {
			super(spinner);
			JFormattedTextField ftf = (JFormattedTextField) this
					.getComponent(0);
			NumberFormat fmt = NumberFormat.getIntegerInstance();
			fmt.setGroupingUsed(false);
			ftf.setFormatterFactory(new DefaultFormatterFactory(
					new NumberFormatter(fmt)));
			revalidate();
		}

	}

	public NoGroupingSpinner(SpinnerModel spinnerModel) {
		super(spinnerModel);
	}

	protected JComponent createEditor(SpinnerModel model) {
		if (model instanceof SpinnerNumberModel)
			return new NoGroupingNumberEditor(this, model);

		return super.createEditor(model);
	}

}
