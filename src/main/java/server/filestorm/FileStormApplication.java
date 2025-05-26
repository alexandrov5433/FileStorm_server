package server.filestorm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FileStormApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileStormApplication.class, args);
	}

}
