package io.tronbot.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.cloud.netflix.feign.FeignClient
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.Resources
import org.springframework.integration.annotation.Gateway
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.Poller
import org.springframework.integration.core.MessageSource
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand

import groovy.util.logging.Log4j

@IntegrationComponentScan
@EnableBinding(Source.class)
@EnableDiscoveryClient
@EnableZuulProxy
@EnableFeignClients
@EnableCircuitBreaker
@SpringBootApplication
class DataConciergeGatewayApplication {

	static void main(String[] args) {
		SpringApplication.run DataConciergeGatewayApplication, args
	}

	//	@Bean
	//	@InboundChannelAdapter(value = Source.OUTPUT, poller = @Poller(fixedDelay = '10000', maxMessagesPerPoll = '1'))
	//	public MessageSource<TimeInfo> timerMessageSource() {
	//		return MessageBuilder.withPayload(new TimeInfo(new Date().getTime()+'','Label')).build()
	//	}
}
//class TimeInfo{
//	private String time
//	private String label
//
//	public TimeInfo(String time, String label) {
//		super()
//		this.time = time
//		this.label = label
//	}
//
//	public String getTime() {
//		return time
//	}
//
//	public String getLabel() {
//		return label
//	}
//}


class Reservation {
	private String reservationName
	public String getReservationName() {
		return reservationName
	}
}

@FeignClient('data-concierge-service')
interface ReservationReader{
	@RequestMapping(method = RequestMethod.GET, value = '/reservations')
	Resources<Reservation> read()
	@RequestMapping(method = RequestMethod.GET, value = '/message')
	String message()
}

@MessagingGateway
interface ReservationWriter{
	@Gateway(requestChannel = 'output')
	void write(String reservationName)
}

@RestController
@RequestMapping('/reservations')
@Log4j
@RefreshScope
class ReservationApiGateway{

	private final ReservationReader reservationReader
	private final ReservationWriter reservationWriter

	@Autowired
	public ReservationApiGateway(ReservationReader reservationReader, ReservationWriter reservationWriter) {
		super()
		this.reservationReader = reservationReader
		this.reservationWriter = reservationWriter
	}

	public Collection<String> fallback(){
		return []
	}

	@HystrixCommand(fallbackMethod = 'fallback')
	@GetMapping('/names')
	public Collection<String> names(){
		return this.reservationReader
				.read().getContent().collect{it.getReservationName()}
	}

	@PostMapping
	public void write(@RequestBody Reservation reservation){
		this.reservationWriter.write(reservation.getReservationName())
	}
}

//interface ReservationChannels{
//	@Output
//	MessageChannel output()//orders, customers
//}