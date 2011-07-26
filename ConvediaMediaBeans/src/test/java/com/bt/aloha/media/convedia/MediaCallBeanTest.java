/*
 * Aloha Open Source SIP Application Server- https://trac.osmosoft.com/Aloha
 *
 * Copyright (c) 2008, British Telecommunications plc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

 	

 	
 	
 
/**
 * (c) British Telecommunications plc, 2007, All Rights Reserved
 */
package com.bt.aloha.media.convedia;

import org.easymock.EasyMock;
import org.junit.Test;

import com.bt.aloha.media.MediaCallBean;
import com.bt.aloha.media.MediaCallLegBean;


public class MediaCallBeanTest {
	/**
	 * Tests that when we set the media dialog bean that the media call bean is added to the list of listeners
	 */
	@Test
	public void addMediaCallBeanAsListenerWhenSettingMediaDialog() {
		// setup
		MediaCallBeanImpl mediaCallBean = new MediaCallBeanImpl();
		MediaCallLegBean dialogBean = EasyMock.createMock(MediaCallLegBean.class);
		dialogBean.addMediaCallLegListener(mediaCallBean);
		EasyMock.replay(dialogBean);
		
		// act
		mediaCallBean.setMediaCallLegBean(dialogBean);
		
		// assert
		EasyMock.verify(dialogBean);
	}
	
	/**
	 * tests that when we set the media dialog bean that's already been set we remove ourselves first
	 */
	@Test
	public void removeMediaCallBeanAsListenerWhenResettingMediaDialog() {
		// setup
		MediaCallBeanImpl mediaCallBean = new MediaCallBeanImpl();
		MediaCallLegBean dialogBean = EasyMock.createMock(MediaCallLegBean.class);
		dialogBean.addMediaCallLegListener(mediaCallBean);
		dialogBean.removeMediaCallLegListener(mediaCallBean);
		EasyMock.replay(dialogBean);
		MediaCallLegBean anotherDialogBean = EasyMock.createMock(MediaCallLegBean.class);
		anotherDialogBean.addMediaCallLegListener(mediaCallBean);
		EasyMock.replay(anotherDialogBean);
		mediaCallBean.setMediaCallLegBean(dialogBean);
		
		// act
		mediaCallBean.setMediaCallLegBean(anotherDialogBean);
		
		// assert
		EasyMock.verify(dialogBean);
		EasyMock.verify(anotherDialogBean);
	}
	
	// test that you cannot add a null listener
	@Test(expected=IllegalArgumentException.class)
	public void addNullListener() {
		// act
		MediaCallBean mediaCallBean = new MediaCallBeanImpl();
		mediaCallBean.addMediaCallListener(null);
		
		// assert - exception
	}
	
	// test that you cannot remove a null listener
	@Test(expected=IllegalArgumentException.class)
	public void removeNullListener() {
		// act
		MediaCallBean mediaCallBean = new MediaCallBeanImpl();
		mediaCallBean.removeMediaCallListener(null);
		
		// assert - exception
	}
}
