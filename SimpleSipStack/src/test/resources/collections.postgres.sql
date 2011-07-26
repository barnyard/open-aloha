create table StateInfo (
	object_id VARCHAR(128) not null primary key,
	object_type VARCHAR(32) not null,
	object_version VARCHAR(32) not null,
	last_use_time BIGINT not null,
	is_dead INT not null,
    force_housekeep INT not null,
	object_value BYTEA
);

create table callinfo (
  callId varchar(64) not null primary key,
  classname varchar(128),
  simpleSipBeanId varchar(32),
  createTime BIGINT not null,
  object_version VARCHAR(32) not null,
  last_use_time BIGINT not null,
  startTime BIGINT not null,
  endTime BIGINT not null,
  callState varchar(32) not null,
  firstDialogId varchar(64),
  secondDialogId varchar(64),
  firstCallLegConnectionState varchar(32) not null,
  secondCallLegConnectionState varchar(32) not null,
  mediaNegotiationState varchar(32) not null,
  mediaNegotiationMethod varchar(32),
  maxDurationInMinutes BIGINT,
  autoTerminate varchar(32),
  callTerminationCause varchar(32),
  callLegCausingTermination varchar(32),
  pendingCallReinvite_dialogId varchar(64),
  pendingCallReinvite_remoteContact varchar(32),
  pendingCallReinvite_offerInOkResponse boolean,
  pendingCallReinvite_autoTerminate boolean,
  pendingCallReinvite_mediaDescription OID null,
  pendingCallReinvite_applicationData varchar(128),
  housekeepingForced boolean
);

create table conferenceinfo (
  conferenceId varchar(64) not null primary key,
  simpleSipBeanId varchar(32),
  createTime BIGINT not null,
  object_version VARCHAR(32) not null,
  last_use_time BIGINT not null,
  startTime BIGINT not null,
  endTime BIGINT not null,
  mediaServerAddress varchar(32) not null,
  conferenceState varchar(32) not null,
  conferenceTerminationCause varchar(32),
  maxNumberOfParticipants BIGINT,
  maxDurationInMinutes BIGINT,
  housekeepingForced boolean,
  participants text
);