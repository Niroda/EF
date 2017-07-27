/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

/**
 *
 * @author Ali
 */
public class Course {

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
     * @return the Teacher
     */
    public Person getTeacher() {
        return teacher;
    }

    /**
     * @param teacher the Teacher to set
     */
    public void setTeacher(Person teacher) {
        this.teacherIsDirty = true;
        this.teacher = teacher;
    }
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
    private Person teacher;

    private boolean teacherIsDirty;
    private boolean getTeacherIsDirty() {
        return this.teacherIsDirty;
    }
}
