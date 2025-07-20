package com.dnd.campaignmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.dnd.campaignmanager")

public class CampaignManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampaignManagerApplication.class, args);

	}

}
