package com.zeezaglobal.prescription;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class PrescriptionApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrescriptionApplication.class, args);
	}
	@PostConstruct
	public void init() {
		Security.addProvider(new BouncyCastleProvider());
	}
}
