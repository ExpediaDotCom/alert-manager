DROP DATABASE IF EXISTS alert_manager;

CREATE DATABASE alert_manager;

USE alert_manager;

create table subscription (
  id          int unsigned primary key not null auto_increment,
  type        char(50)				not null,
  endpoint    char(100)             not null,
  details	  json,
  enabled 	  bool                  not null default 1,
  name 		  varchar(100),
  description varchar(100),
  date_created  timestamp          	NULL         default CURRENT_TIMESTAMP
 );

create table subscription_metric_detector_mapping (
  id           int unsigned primary key not null auto_increment,
  metric_id    char(36),
  detector_id  char(36)        not null,
  subscription_id int unsigned not null,
  date_created timestamp                         default CURRENT_TIMESTAMP,
  constraint subscription_id_fk foreign key (subscription_id) references subscription (id)
);
