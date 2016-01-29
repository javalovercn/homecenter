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
package hc.com.michaelbaranov.microba.common;

import hc.com.michaelbaranov.microba.Microba;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;


/**
 * Superclass for all Microba GUI components.
 * 
 * @author Michael Baranov
 * 
 */
public class MicrobaComponent extends JComponent {

	public static final String PROPERTY_NAME_COLOR_OVERRIDE_MAP = "colorOverrideMap";

	static {
		Microba.init();
	}

	protected Map colorOverrideMap;

	public ComponentUI getUI() {
		return ui;
	}

	/**
	 * Sets the UI delegate of this component to the corresponding UI delegate
	 * taken from UIManager.
	 * <p>
	 * This implementation has a workarount to fix the problem with non-standard
	 * class-loaders.
	 */
	public void updateUI() {
		UIManager.getDefaults().put(UIManager.get(this.getUIClassID()), null);
		ComponentUI delegate = UIManager.getUI(this);

		setUI(delegate);
		invalidate();
	}

	/**
	 * Returns per-instance (only for this instance) map of color overrides. May
	 * be <code>null</code>.
	 * <p>
	 * NOTE: returned map is unmodifiable. Use {@link #setColorOverrideMap(Map)}
	 * to change the map.
	 * 
	 * @return keys in the map are {@link String} constants, valuse are of type
	 *         {@link Color} or of type {@link String} (in this case,
	 *         {@link Color} values are obtained via
	 *         {@link UIManager#getColor(Object)})
	 */
	public Map getColorOverrideMap() {
		if (colorOverrideMap == null)
			return null;

		return Collections.unmodifiableMap(colorOverrideMap);
	}

	/**
	 * Sets per-instance (only for this instance) map of color overrides.
	 * 
	 * @param colorOverrideMap
	 *            keys in the map are {@link String} constants, valuse are of
	 *            type {@link Color} or of type {@link String} (in this case,
	 *            {@link Color} values are obtained via
	 *            {@link UIManager#getColor(Object)}). May be <code>null</code>.
	 */
	public void setColorOverrideMap(Map colorOverrideMap) {
		Object old = this.colorOverrideMap;
		this.colorOverrideMap = colorOverrideMap;
		firePropertyChange(PROPERTY_NAME_COLOR_OVERRIDE_MAP, old,
				colorOverrideMap);

		updateUI();
	}

}
