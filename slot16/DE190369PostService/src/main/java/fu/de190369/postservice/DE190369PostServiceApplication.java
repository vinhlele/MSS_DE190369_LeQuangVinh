package fu.de190369.postservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190369PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DE190369PostServiceApplication.class, args);
    }
}
