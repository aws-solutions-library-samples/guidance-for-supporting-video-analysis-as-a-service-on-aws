package org.example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String... args) {
        logger.info("Application starts");

//        Handler handler = new Handler();
//        handler.sendRequest();

        IotHandler iotHandler = new IotHandler();
        iotHandler.sendRequest();
        logger.info("Application ends");
    }
}
