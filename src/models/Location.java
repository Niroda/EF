/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import java.util.List;

/**
 *
 * @author Ali
 */
public class Location {
    private int id;

    private boolean idIsDirty;
    private boolean getIdIsDirty() {
        return this.idIsDirty;
    }
    private String name;

    private boolean nameIsDirty;
    private boolean getNameIsDirty() {
        return this.nameIsDirty;
    }
    private List<Person> persons;

    private boolean personsIsDirty;
    private boolean getPersonsIsDirty() {
        return this.personsIsDirty;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.idIsDirty = true;
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.nameIsDirty = true;
        this.name = name;
    }

    /**
     * @return the persons
     */
    public List<Person> getPersons() {
        return persons;
    }

    /**
     * @param persons the persons to set
     */
    public void setPersons(List<Person> persons) {
        this.personsIsDirty = true;
        this.persons = persons;
    }
}
