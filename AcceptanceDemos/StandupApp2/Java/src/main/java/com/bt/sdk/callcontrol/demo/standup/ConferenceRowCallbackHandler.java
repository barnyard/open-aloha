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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceActiveEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ConferenceEndedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantConnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantDisconnectedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantFailedEvent;
import com.bt.sdk.callcontrol.sip.media.conference.event.ParticipantTerminatedEvent;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceBean;
import com.bt.sdk.callcontrol.sip.media.convedia.conference.ConferenceListener;

public class ConferenceRowCallbackHandler implements RowCallbackHandler, ConferenceListener {
	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy:MM:dd");
	private final static String SELECT = "select b.telno from conferences a, participants b, conferences_participants c where a.name = ? and a.id = c.conference_id and b.id = c.participant_id";
	private Log log = LogFactory.getLog(this.getClass());
	private JdbcTemplate jdbcTemplate;
	private Map<String, Future<?>> conferenceFutures;// = new Hashtable<String, Future<?>>();
	private Map<String, String> conferences;// = new Hashtable<String, String>();
	private ScheduledExecutorService scheduledExecutorService;
	private ConferenceBean conferenceBean;
	private long maxConferenceDurationInMinutes = 30;
	private String pstnGatewayIpAddress = "10.238.67.22";
	
	public void processRow(ResultSet arg0) throws SQLException {
		String conferenceName = arg0.getString("name");
		log.debug(conferenceName);
		
		Calendar conferenceTime = Calendar.getInstance();
		conferenceTime.set(Calendar.HOUR_OF_DAY, arg0.getTime("time").getHours());
		conferenceTime.set(Calendar.MINUTE, arg0.getTime("time").getMinutes());
		conferenceTime.set(Calendar.SECOND, 0);
		conferenceTime.set(Calendar.MILLISECOND, 0);
		String date = DATE_FORMAT.format(Calendar.getInstance().getTime());
		final String key = String.format("%s:%02d%02d:%s", conferenceName, conferenceTime.get(Calendar.HOUR_OF_DAY), conferenceTime.get(Calendar.MINUTE), date);
		log.debug(key);
		if (conferenceFutures.containsKey(key)) {
			log.warn(String.format("conference already scheduled: %s", key));
			return;
		}
		final List<String> participants = getParticipants(conferenceName);
		log.debug(participants);
		log.info(String.format("Starting conference \"%s\" in %d seconds", conferenceName, secondsBeforeStartTime(conferenceTime)));
		conferenceFutures.put(key, scheduledExecutorService.schedule(new Runnable(){
			public void run() {
				String conferenceId = conferenceBean.createConference(participants.size(), maxConferenceDurationInMinutes);
				conferences.put(conferenceId, key);
				log.debug(conferences);
				for (String participantTelno: participants) {
					String callLegId = conferenceBean.createParticipantCallLeg(conferenceId, URI.create(String.format("sip:%s@%s", participantTelno, pstnGatewayIpAddress)));
					conferenceBean.inviteParticipant(conferenceId, callLegId);
				}
			}
		}, secondsBeforeStartTime(conferenceTime), TimeUnit.SECONDS));
	}
	
	private long secondsBeforeStartTime(Calendar conferenceTime) {
		Calendar now = Calendar.getInstance();
		return (conferenceTime.getTimeInMillis() - now.getTimeInMillis()) / 1000;
	}

	private List<String> getParticipants(String conferenceName) {
		final List<String> result = new ArrayList<String>();
		Object[] args = new Object[] {conferenceName};
		RowCallbackHandler rowCallbackHandler = new RowCallbackHandler() {
			public void processRow(ResultSet arg0) throws SQLException {
				result.add(arg0.getString("telno"));
			}
		};
		this.jdbcTemplate.query(SELECT, args, rowCallbackHandler);
		return result;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}
	
	public void setConferenceBean(ConferenceBean conferenceBean) {
		this.conferenceBean = conferenceBean;
	}
	
	public void setPstnGatewayIpAddress(String pstnGatewayIpAddress) {
		this.pstnGatewayIpAddress = pstnGatewayIpAddress;
	}
	
	public void setConferenceFutures(Map<String, Future<?>> conferenceFutures) {
		this.conferenceFutures = conferenceFutures;
	}
	
	public void setConferences(Map<String, String> conferences) {
		this.conferences = conferences;
	}

	public void onConferenceActive(ConferenceActiveEvent arg0) {
		log.debug(String.format("%s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId()));
	}

	public void onConferenceEnded(ConferenceEndedEvent arg0) {
		log.debug(String.format("%s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId()));
		String conferenceId = arg0.getConferenceId();
		if (conferences.containsKey(conferenceId)) {
			String key = conferences.remove(conferenceId);
			conferenceFutures.remove(key);
		}
	}

	public void onParticipantConnected(ParticipantConnectedEvent arg0) {
		log.debug(String.format("%s: %s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId(), arg0.getDialogId()));
	}

	public void onParticipantDisconnected(ParticipantDisconnectedEvent arg0) {
		log.debug(String.format("%s: %s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId(), arg0.getDialogId()));
	}

	public void onParticipantFailed(ParticipantFailedEvent arg0) {
		log.warn(String.format("%s: %s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId(), arg0.getDialogId()));
	}

	public void onParticipantTerminated(ParticipantTerminatedEvent arg0) {
		log.debug(String.format("%s: %s", arg0.getClass().getSimpleName(), arg0.getConferenceId()));
	}
}
