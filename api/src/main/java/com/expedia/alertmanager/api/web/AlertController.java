package com.expedia.alertmanager.api.web;

import com.expedia.alertmanager.model.Alert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertController {

    @RequestMapping(value = "/alerts", method = RequestMethod.POST)
    public ResponseEntity receiveAlerts(@RequestBody List<Alert> alerts) {

        //TODO - store alerts in kafka
        return new ResponseEntity(HttpStatus.OK);
    }

}
