package org.coner.core.domain.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Registration extends DomainEntity {

    private String id;
    private Person person;
    private Car car;
    private Event event;
    private HandicapGroup handicapGroup;
    private CompetitionGroup competitionGroup;
    private String number;
    private boolean checkedIn;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public HandicapGroup getHandicapGroup() {
        return handicapGroup;
    }

    public void setHandicapGroup(HandicapGroup handicapGroup) {
        this.handicapGroup = handicapGroup;
    }

    public CompetitionGroup getCompetitionGroup() {
        return competitionGroup;
    }

    public void setCompetitionGroup(CompetitionGroup competitionGroup) {
        this.competitionGroup = competitionGroup;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
