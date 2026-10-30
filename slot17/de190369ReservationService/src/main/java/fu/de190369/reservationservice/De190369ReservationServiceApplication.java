package fu.de190369.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class De190369ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(De190369ReservationServiceApplication.class, args);
    }
}
