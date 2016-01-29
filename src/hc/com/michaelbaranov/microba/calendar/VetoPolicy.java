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

import hc.com.michaelbaranov.microba.common.Policy;

import java.util.Calendar;


/**
 * This interface is used by {@link CalendarPane} and {@link DatePicker} to
 * provide means to restrict dates in a control.
 * 
 * @author Michael Baranov
 * 
 */
public interface VetoPolicy extends Policy {

	/**
	 * This method is used to check if a date is restricted. Restricted dates
	 * can not be selected by users in a control.
	 * 
	 * @param source
	 *            a control calling this method
	 * @param date
	 *            a date to check. Is never <code>null</code>
	 * @return <code>true</code> if given <code>date</code> is restricted
	 *         <code>false</code> otherwise
	 */
	public boolean isRestricted(Object source, Calendar date);

	/**
	 * This method is used to check if no-date (<code>null</code> date) is
	 * restricted. Restricted dates can not be selected by users in a control.
	 * 
	 * @param source
	 *            a control calling this method
	 * @return <code>false</code> to allow no-date, <code>true</code>
	 *         otherwise
	 */
	public boolean isRestrictNull(Object source);

}
