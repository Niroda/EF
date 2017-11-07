# NOTE!
Do **NOT** use this code in a real project. I created this messy code just to practice/learn Java!

# EF library
This java library allows us to use **type safety** to deal with the databse using **Lambda expressions** instead of using **SQL QUERIES**
It supports relationships as well, one to many and many to many

# Example:
```
List<User> users = db.getUsers().where(u -> u.getName().contains("a"));
```
# How it works
It uses [JaQue](https://github.com/TrigerSoft/jaque) to parse **Lambda expressions**, and I created helpers to convert that lambda into **SQL Query**

# Limitations
  - Supports Lists only, doesn't support arrays or any another collections
  - Doesn't support BYTE, DECIMEL and CHAR
  - Attached migration tool is very primitive, it's used to :
    * Enable migration and adds private field and getter to each field in models with name "IsDirty" to determine if that field got changed. You have to use enable only once!
    * Add migration to create a copy of models as SQL file, if there is any table in the database, this command will make sure that the model matchs the table by ALTERING that table. You have to use this option each time you add or remove fields from your models!
    Good to know that this option doesn't provide private field with getter to determine if the field got changed or not so you have to added manually (will fix this later).
    * Update option is used to update the database by executing generated SQL file
  - It supports MySql only for the moment, maybe others later :)
  - I've definitely forgotten something else, I'll added when I remember it :D

# Installation: 
After you download the library, first of all you have to create a DB configuration file and place it in the same path as your models. You must derive from **DbContext** class as following bellow:
```
public class Db extends DbContext {
```
You need a constructor to pass parameters to **DbContext** :
```
public Db() {
    super(SqlType.MySQLDB, "jdbc:mysql://localhost/dbName", "dbUser", "dbPass");
}
```
***SqlType*** is **ENUM** you can use it from package **encapsulationofef.helpers** which is included in the library :)
If ***dbName*** doesn't exist, the library will create it :)
Next step is to add fields of type **DbSet<Model>**, where **DbSet<Model>** is generic class included in the labrary as well. Replace ***Model*** with model name as following:
```
    private DbSet<Course> courses;
    private DbSet<Person> persons;
    private DbSet<Location> locations;
```
Next step is to create **Getter** for each field as following:
```
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
```
**DONE!**

# Using Migration tool
Place ***Migration.jar*** tool wherever you want and execute it using this command:
```sh
java -jar Migration.jar # to get usage message
java -jar Migration.jar  -[OPTION] [PARAM]
```
Use ***enable*** option to prepare models, ***add*** to create SQL file and ***update*** to execute that file :)

***Migration.jar*** tool is included in the default package in the sample.
It includes MySql connector.

# To do later:
- Adding support for all collection types
- I'm pretty sure there is a lot of bugs :D
- Developing Migration tool to use Compiler API instead of reading classes as text!