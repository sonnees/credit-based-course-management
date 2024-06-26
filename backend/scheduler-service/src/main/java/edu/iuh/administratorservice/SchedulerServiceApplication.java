package edu.iuh.administratorservice;

import edu.iuh.administratorservice.dto.RegistrationSearch3FieldDTO;
import edu.iuh.administratorservice.repository.RegistrationFormRepository;
import edu.iuh.administratorservice.serialization.ExcelFileHandle;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SpringBootApplication
@AllArgsConstructor
public class SchedulerServiceApplication {
    RegistrationFormRepository registrationFormRepository;
    ExcelFileHandle excelFileHandle;
    public static void main(String[] args) {
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {


            }
        };
    }

}
