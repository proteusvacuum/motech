package org.motechproject.openmrs.ws.impl;

import org.apache.commons.lang.Validate;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.mrs.EventKeys;
import org.motechproject.mrs.domain.MRSAttribute;
import org.motechproject.mrs.domain.MRSPerson;
import org.motechproject.mrs.exception.MRSException;
import org.motechproject.mrs.helper.EventHelper;
import org.motechproject.openmrs.ws.HttpException;
import org.motechproject.openmrs.ws.resource.PersonResource;
import org.motechproject.openmrs.ws.resource.model.Attribute;
import org.motechproject.openmrs.ws.resource.model.Attribute.AttributeType;
import org.motechproject.openmrs.ws.resource.model.AttributeTypeListResult;
import org.motechproject.openmrs.ws.resource.model.Concept;
import org.motechproject.openmrs.ws.resource.model.Person;
import org.motechproject.openmrs.ws.resource.model.Person.PreferredAddress;
import org.motechproject.openmrs.ws.resource.model.Person.PreferredName;
import org.motechproject.openmrs.ws.util.ConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MRSPersonAdapterImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(MRSPersonAdapterImpl.class);

    private final Map<String, String> attributeTypeUuidCache = new HashMap<String, String>();

    private final PersonResource personResource;
    private final EventRelay eventRelay;

    @Autowired
    public MRSPersonAdapterImpl(PersonResource personResource, EventRelay eventRelay) {
        this.personResource = personResource;
        this.eventRelay = eventRelay;
    }

    public MRSPerson getPerson(String uuid) {
        Person person = null;
        try {
            person = personResource.getPersonById(uuid);
        } catch (HttpException e) {
            LOGGER.error("Failed to retrieve person with uuid: " + uuid);
            throw new MRSException(e);
        }
        return ConverterUtils.convertToMrsPerson(person);
    }

    MRSPerson savePerson(MRSPerson person) {
        Validate.notNull(person, "Person canont be null");
        Person converted = ConverterUtils.convertToPerson(person, true);
        Person saved = null;
        try {
            saved = personResource.createPerson(converted);
        } catch (HttpException e) {
            LOGGER.error("Failed to create person for: " + person.getFirstName() + " " + person.getMiddleName() + " " + person.getLastName());
            throw new MRSException(e);
        }

        person.setPersonId(saved.getUuid());

        saveAttributesForPerson(person);
        eventRelay.sendEventMessage(new MotechEvent(EventKeys.CREATED_NEW_PERSON_SUBJECT, EventHelper.personParameters(person)));
        return person;
    }

    void saveAttributesForPerson(MRSPerson person) {
        for (MRSAttribute attribute : person.getAttributes()) {
            Attribute attr = new Attribute();
            attr.setValue(attribute.getValue());
            attr.setAttributeType(getAttributeTypeUuid(attribute.getName()));

            try {
                personResource.createPersonAttribute(person.getPersonId(), attr);
            } catch (HttpException e) {
                LOGGER.warn("Unable to add attribute to person with id: " + person.getPersonId());
            }
        }
    }

    private AttributeType getAttributeTypeUuid(String name) {
        if (!attributeTypeUuidCache.containsKey(name)) {
            AttributeTypeListResult result = null;
            try {
                result = personResource.queryPersonAttributeTypeByName(name);
            } catch (HttpException e) {
                LOGGER.error("HTTP request failed to get attribute type uuid for attribute with name: " + name);
                throw new MRSException(e);
            }

            if (result.getResults().size() == 0) {
                LOGGER.error("No attribute found with name: " + name);
                throw new MRSException("No attribute with name: " + name + " found in OpenMRS");
            } else if (result.getResults().size() > 1) {
                LOGGER.warn("Found more than 1 attribute with name: " + name);
            }
            attributeTypeUuidCache.put(name, result.getResults().get(0).getUuid());
        }

        AttributeType type = new AttributeType();
        type.setUuid(attributeTypeUuidCache.get(name));

        return type;
    }

    void deleteAllAttributes(MRSPerson person) {
        Person saved = null;
        try {
            saved = personResource.getPersonById(person.getPersonId());
        } catch (HttpException e) {
            LOGGER.error("Failed to retrieve person when deleting attributes with uuid: " + person.getPersonId());
            throw new MRSException(e);
        }

        List<Attribute> attributes = saved.getAttributes();
        for (Attribute attr : attributes) {
            try {
                personResource.deleteAttribute(attr);
            } catch (HttpException e) {
                LOGGER.warn("Failed to delete attribute with name: " + attr.getName());
            }
        }
    }

    void updatePerson(MRSPerson person) {
        Person converted = ConverterUtils.convertToPerson(person, false);
        try {
            // Must update the name and address separately when updating a
            // person.
            Person saved = personResource.getPersonById(person.getPersonId());

            PreferredName name = saved.getPreferredName();
            name.setGivenName(person.getFirstName());
            name.setMiddleName(person.getMiddleName());
            name.setFamilyName(person.getLastName());
            personResource.updatePersonName(saved.getUuid(), name);

            PreferredAddress addr = saved.getPreferredAddress();
            if (addr == null) {
                addr = new PreferredAddress();
            }

            addr.setAddress1(person.getAddress());
            personResource.updatePersonAddress(saved.getUuid(), addr);

            converted.setUuid(saved.getUuid());
            personResource.updatePerson(converted);
            eventRelay.sendEventMessage(new MotechEvent(EventKeys.UPDATED_PERSON_SUBJECT, EventHelper.personParameters(person)));
        } catch (HttpException e) {
            LOGGER.error("Failed to update a person in OpenMRS with id: " + person.getPersonId());
            throw new MRSException(e);
        }
    }

    void savePersonCauseOfDeath(String patientId, Date dateOfDeath, String conceptName) {
        Person person = new Person();
        person.setUuid(patientId);
        person.setDead(true);
        person.setDeathDate(dateOfDeath);

        Concept concept = new Concept();
        concept.setDisplay(conceptName);
        person.setCauseOfDeath(concept);

        try {
            personResource.updatePerson(person);
        } catch (HttpException e) {
            LOGGER.error("Failed to save cause of death observation for patient id: " + patientId);
            throw new MRSException(e);
        }
    }
}