package com.rponce.Ticketify.controllers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rponce.Ticketify.models.dtos.SaveTicketDTO;
import com.rponce.Ticketify.models.dtos.ShowOrderDTO;
import com.rponce.Ticketify.models.entities.Order;
import com.rponce.Ticketify.models.entities.Ticket;
import com.rponce.Ticketify.models.entities.Tier;
import com.rponce.Ticketify.models.entities.User;
import com.rponce.Ticketify.services.OrderService;
import com.rponce.Ticketify.services.TicketService;
import com.rponce.Ticketify.services.TierService;
import com.rponce.Ticketify.services.UserService;
import com.rponce.Ticketify.utils.RequestErrorHandler;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/ticket")
@CrossOrigin("*")
public class TicketController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private TicketService ticketService;
	
	@Autowired
	private TierService tierService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private RequestErrorHandler errorHandler;
	
	//Metodo que realiza la compra de un ticket de una categoria
	@PostMapping("/save/{numberOfTickets}")
	private ResponseEntity<?> SaveTicket(@ModelAttribute @Valid SaveTicketDTO info, BindingResult validation, 
			@PathVariable(name = "numberOfTickets") int number){
		
		//Registering date
		Date date = new Date();
		
		//Setting data to send
		User userTicket = userService.findUserAuthenticated();
		Tier tierTicket = tierService.findOneById(info.getTierId());
		Float totalPrice = tierTicket.getPrice()*number;
		
		
		if(validation.hasErrors()) {
			return new ResponseEntity<>(errorHandler.mapErrors(validation.getFieldErrors()), 
					HttpStatus.BAD_REQUEST);
		}
		
//		Si no se encuentra un usuario o tier
		if(userTicket == null || tierTicket == null) {
			return new ResponseEntity<>("The user or tier does not exist",HttpStatus.BAD_REQUEST);
		}
		
		//validando que la cantidad de tickets no sobrepase la cantidad del tier
		if (number > tierTicket.getCapacity()) {
			return new ResponseEntity<>("The purchase exceeds the limits available in the tier",HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		try {
			//Create Order
			orderService.SaveOrder(totalPrice, userTicket, date);
			Order currOrder = orderService.findCurrentOrder(totalPrice, userTicket, date);
			
			for(int i = 0; i < number; i++) {
				ticketService.SaveTicket(userTicket, tierTicket, date, currOrder, false);
			}
			
			//actualizo la cantidad disponible de la tier
			tierService.updateTierCapacity(tierTicket, number);
			
			ShowOrderDTO order = new ShowOrderDTO();
			order.setId(currOrder.getUuid().toString());
			order.setEventName(tierTicket.getEvent().getTitle());
			order.setTier(tierTicket.getTier());
			order.setSubtotal(totalPrice);
			order.setTotalTickets(number);
			order.setTotal(totalPrice);
			
			return new ResponseEntity<>(order, HttpStatus.OK);
			
		}catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	@PostMapping("/finish")
	private ResponseEntity<?> FinishTicketBuy(String OrderId) {
		
		UUID uuid = UUID.fromString(OrderId);
		
		Order order = orderService.findOrderById(uuid);
		List<Ticket> tickets = ticketService.getTicketsByOrder(order);
		
		try {
			ticketService.ActivateTicket(tickets);
			return new ResponseEntity<>(HttpStatus.OK);
		}catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		
	}
	
	@PostMapping("/abort")
	private ResponseEntity<?> AbortTicketBuy(String OrderId) {
		
		UUID uuid = UUID.fromString(OrderId);
		Order orderToDelete = orderService.findOrderById(uuid);
		
		try {
			List<Ticket> tickets = ticketService.getTicketsByOrder(orderToDelete);
			for(Ticket t : tickets) {
				ticketService.DeleteTicket(t);
			}
			orderService.DeleteOrder(orderToDelete);
			return new ResponseEntity<>(HttpStatus.OK);
		}catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	@GetMapping("/all")
	private ResponseEntity<?> GetAllTickets() {
		if(ticketService.getAllTickets() == null) {
			return new ResponseEntity<>(
					HttpStatus.NOT_FOUND
					);
		}
		
		return new ResponseEntity<>(ticketService.getAllTickets(), HttpStatus.OK);
	}
	
	@GetMapping("/ticketid/{id}")
	private ResponseEntity<?> GetTicketById(@PathVariable(name = "id") String id){
		
		UUID uuid = UUID.fromString(id);
		
		if(ticketService.getTicketByID(uuid) == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(ticketService.getTicketByID(uuid), HttpStatus.OK);
	}
	
	@GetMapping("/userid/{userid}")
	private ResponseEntity<?> getTicketsByUser(@PathVariable(name = "userid") String id){
		
		User user = userService.FindOneUserById(id);
		
		if(user == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if(ticketService.getTicketsByUser(user) == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(ticketService.getTicketsByUser(user), HttpStatus.OK);
		
	}
	
	@GetMapping("/tierid/{tierid}")
	private ResponseEntity<?> getTicketsByTier(@PathVariable(name = "tierid") String id){
		
		Tier tier = tierService.findOneById(id);
		
		if(tier == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if(ticketService.getTicketsByTier(tier) == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(ticketService.getTicketsByTier(tier), HttpStatus.OK);
		
	}

}
