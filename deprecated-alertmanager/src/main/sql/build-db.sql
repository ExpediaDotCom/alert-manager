DROP DATABASE IF EXISTS alert_manager;

CREATE DATABASE alert_manager;

USE alert_manager;

create table subscription (
  id          int unsigned primary key not null auto_increment,
  type        char(50)				not null,
  endpoint    char(100)             not null,
  owner 	  varchar(40),
  details	  json,
  enabled 	  bool                  not null default 1,
  name 		  varchar(100),
  description varchar(100),
  metric_id    char(36),
  detector_id  char(36)        not null,
  date_created  timestamp          	NULL         default CURRENT_TIMESTAMP,
  unique index (endpoint, metric_id, detector_id)
 );
