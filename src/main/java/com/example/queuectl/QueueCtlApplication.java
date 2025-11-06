package com.example.queuectl;

import com.example.queuectl.cli.RootCommand;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class QueueCtlApplication {
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(QueueCtlApplication.class)
				.bannerMode(Banner.Mode.OFF)
				.web(WebApplicationType.NONE)
				.run(args);
		// Picocli is bootstrapped via RootCommand @Component
		int exit = ctx.getBean(RootCommand.class).execute(args);
		ctx.close();
		System.exit(exit);
	}
}
