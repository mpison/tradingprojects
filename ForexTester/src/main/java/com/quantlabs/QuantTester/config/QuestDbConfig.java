package com.quantlabs.QuantTester.config;

//import io.questdb.client.Sender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.questdb.client.Sender;

@Configuration
public class QuestDbConfig {

	@Value("${questdb.host:127.0.0.1}")
	private String host;

	@Value("${questdb.port:8812}")
	private int port;
	
	@Value("${questdb.username:admin}")
	private String username;

	@Value("${questdb.password:quest}")
	private String password;

	@Bean
    public Sender questDbSender() {

    	 Sender sender = Sender.builder(Sender.Transport.HTTP)
    	  .address(host)
    	  .autoFlushRows(5000)
    	  .retryTimeoutMillis(10000)
    	  .port(port)
    	  .httpUsernamePassword(username, password)
    	  .build();
    	  /*.build()) {
    	      sender.table(tableName).column("value", 42).atNow();
    	      sender.flush();
    	  }*/
    	  //LineSenderBuilder builder = Sender.builder();
          //return builder.address(host).port(port).build();
    	 
    	 return sender;
    }
}