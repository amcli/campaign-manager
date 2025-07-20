package com.dnd.campaignmanager;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    @GetMapping("/")
    public String home() {
        return "DnD Campaign Manager is running! Visit /hello for the test endpoint";
    }
    @GetMapping("/hello")
    public String hello(){
        return "DnD Backend :DD";
    }

}
