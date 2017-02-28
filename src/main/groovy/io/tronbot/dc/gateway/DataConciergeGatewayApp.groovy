package io.tronbot.dc.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.cloud.netflix.feign.FeignClient
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand

import groovy.util.logging.Log4j

@EnableDiscoveryClient
@EnableZuulProxy
@EnableFeignClients
@EnableCircuitBreaker
@SpringBootApplication
class DataConciergeGatewayApp {
	static void main(String[] args) {
		SpringApplication.run DataConciergeGatewayApp, args
	}
}

@FeignClient('data-concierge-service')
interface DataConciergeServiceClient{

	@GetMapping('/reconciliation/places')
	ResponseEntity places(@RequestParam('q') String keywords)

	@GetMapping('/reconciliation/hospitals')
	ResponseEntity hospitals(@RequestParam('q') String keywords)

	@GetMapping('/reconciliation/physicians')
	ResponseEntity physicians(
			@RequestParam(value='firstName') String firstName,
			@RequestParam(value='lastName') String lastName,
			@RequestParam(value='address') String address,
			@RequestParam(value='city') String city,
			@RequestParam(value='state') String state,
			@RequestParam(value='postalCode', required=false) String postalCode,
			@RequestParam(value='phoneNumber', required=false) String phoneNumber)

	@GetMapping('/reconciliation/npi/{id}')
	ResponseEntity npi(@PathVariable('id') String id)
}

@RestController
@Log4j
@RefreshScope
class DataConciergeGateway{

	private final DataConciergeServiceClient dataConciergeService

	@Autowired
	public DataConciergeGateway(DataConciergeServiceClient dataConciergeService) {
		this.dataConciergeService = dataConciergeService
	}

	@HystrixCommand(fallbackMethod = 'fallback')
	@GetMapping('/reconciliation/places')
	ResponseEntity places(@RequestParam('q') String keywords){
		return dataConciergeService.places(keywords)
	}

	@HystrixCommand(fallbackMethod = 'fallback')
	@GetMapping('/reconciliation/hospitals')
	ResponseEntity hospitals(@RequestParam('q') String keywords){
		return dataConciergeService.hospitals(keywords)
	}

	@HystrixCommand(fallbackMethod = 'fallback')
	@GetMapping('/reconciliation/physicians')
	ResponseEntity physicians(
			@RequestParam(value='firstName') String firstName,
			@RequestParam(value='lastName') String lastName,
			@RequestParam(value='address') String address,
			@RequestParam(value='city') String city,
			@RequestParam(value='state') String state,
			@RequestParam(value='postalCode', required=false) String postalCode,
			@RequestParam(value='phoneNumber', required=false) String phoneNumber){
		return dataConciergeService.physicians(firstName,lastName,address,city,state,postalCode,phoneNumber)
	}

	@HystrixCommand(fallbackMethod = 'fallback')
	@GetMapping('/reconciliation/npi/{id}')
	ResponseEntity npi(@PathVariable('id') String id){
		return dataConciergeService.npi(id)
	}

	private Collection<String> fallback(){
		return []
	}
}