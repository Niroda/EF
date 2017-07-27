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
public class Person {
    private int id;

    private boolean idIsDirty;
    private boolean getIdIsDirty() {
        return this.idIsDirty;
    }
    private boolean isTeacher;

    private boolean isTeacherIsDirty;
    private boolean getIsTeacherIsDirty() {
        return this.isTeacherIsDirty;
    }
    private String name;

    private boolean nameIsDirty;
    private boolean getNameIsDirty() {
        return this.nameIsDirty;
    }
    private List<Course> courses;

    private boolean coursesIsDirty;
    private boolean getCoursesIsDirty() {
        return this.coursesIsDirty;
    }
    private List<Location> locations;

    private boolean locationsIsDirty;
    private boolean getLocationsIsDirty() {
        return this.locationsIsDirty;
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
     * @return the courses
     */
    public List<Course> getCourses() {
        return courses;
    }

    /**
     * @param courses the courses to set
     */
    public void setCourses(List<Course> courses) {
        this.coursesIsDirty = true;
        this.courses = courses;
    }

    /**
     * @return the locations
     */
    public List<Location> getLocations() {
        return locations;
    }

    /**
     * @param locations the locations to set
     */
    public void setLocations(List<Location> locations) {
        this.locationsIsDirty = true;
        this.locations = locations;
    }

    /**
     * @return the isTeacher
     */
    public boolean getIsTeacher() {
        return isTeacher;
    }

    /**
     * @param isTeacher the isTeacher to set
     */
    public void setIsTeacher(boolean isTeacher) {
        this.isTeacher = isTeacher;
    }
}
