package com.rponce.Ticketify.services;

import java.util.List;


import com.rponce.Ticketify.models.dtos.SaveTierDTO;
import com.rponce.Ticketify.models.entities.Event;
import com.rponce.Ticketify.models.entities.Tier;

public interface TierService {
	void saveTier(SaveTierDTO info, Event event) throws Exception;
	//servicio de actualizacion de capacidad de Tier
	void updateTierCapacity(Tier tier, int cantidad);
	//servicio para la actualizacion de los datos de la Tier
	void updateTier(Tier tier);
	Tier findOneById(String id);
	List<Tier> findAllTier();
	void deleteTier(String id) throws Exception;
	List<Tier> findByEvent(Event event);
	Tier findByTier(String tier);
}