/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import encapsulationofef.DbContext;
import encapsulationofef.DbSet;
import encapsulationofef.helpers.SqlType;

/**
 *
 * @author Ali
 */
public class Db extends DbContext {
    public Db() {
        super(SqlType.MySQLDB, "jdbc:mysql://localhost/dbName", "dbUser", "dbPass");
    }
    
    private DbSet<Course> courses;
    private DbSet<Person> persons;
    private DbSet<Location> locations;
    
    public DbSet<Course> getCourses() {
        this.courses = new DbSet(Course.class, this.getConnection());
        return this.courses;
    }
    public DbSet<Person> getPerson() {
        this.persons = new DbSet(Person.class, this.getConnection());
        return this.persons;
    }
    public DbSet<Location> getLocation() {
        this.locations = new DbSet(Location.class, this.getConnection());
        return this.locations;
    }
    
}
