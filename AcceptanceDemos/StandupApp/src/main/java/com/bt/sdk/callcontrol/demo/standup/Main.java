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
package com.bt.sdk.callcontrol.demo.standup;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.sdk.callcontrol.sip.callleg.InboundCallLegBean;

public class Main {
    private ClassPathXmlApplicationContext applicationContext;
    private StandupBean standupBean;

//	private String participant1 = "sip:piotr@radon199.nat.bt.com";
//	private String participant2 = "sip:02087261150@10.238.67.22";

//	private String participant1 = "sip:07918039480@10.238.67.22";
//	private String participant2 = "sip:07918039798@10.238.67.22";
//	private String participant3 = "sip:07918083525@10.238.67.22";
//	private String participant4 = "sip:01442208294@10.238.67.22";
//	private String participant5 = "sip:0013035477112@10.238.67.22";

    public static void main(String[] args) {
    	Main main = new Main();
    	List<URI> participants = main.init(args);
    	main.execute(participants);
    }

	public List<URI> init(String args[]) {
    	applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
    	standupBean = (StandupBean) applicationContext.getBean("standupBean");
    	((StandupBeanImpl)standupBean).setApplicationContext(applicationContext);
        InboundCallLegBean inboundCallLegBean = (InboundCallLegBean)applicationContext.getBean("inboundCallLegBean");
        inboundCallLegBean.addInboundCallLegListener((StandupBeanImpl)standupBean);

        List<URI> participants = new ArrayList<URI>();
        if (args[0] != null) {
        	String[] uris = args[0].split(",");
        	for (String uri : uris)
        		participants.add(URI.create(uri));
        }
    	return participants;
	}

	private void execute(List<URI> participants) {
		standupBean.runStandup(participants);
	}
}
