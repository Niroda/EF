
import java.sql.SQLException;
import models.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample
 * @author Ali
 */
public class Program {

    public static void main(String args[]) {
        Db db = new Db();
        /**
         * Adding Feature
         */
        // creating two persons
        Person p1 = new Person();
        p1.setName("Ali");
        p1.setIsTeacher(true);
        Person p2 = new Person();
        p2.setName("Someone");
        p2.setIsTeacher(false);
        // creating two courses
        Course c1 = new Course();
        c1.setName("C#");
        Course c2 = new Course();
        c2.setName("Java");
        // creating two locations
        Location l1 = new Location();
        l1.setName("Växjö");
        Location l2 = new Location();
        l2.setName("Stockholm");
        
        try {
             // adding people to the database
            db.getPerson().add(p1);
            db.getPerson().add(p2);
            // adding courses to the database
            db.getCourses().add(c1);
            db.getCourses().add(c2);
            // adding locations to the database
            db.getLocation().add(l1);
            db.getLocation().add(l2);
        } catch (SQLException ex) {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
        }
        /**
         * Updating Feature
         */
        Person ali = db.getPerson().first(x -> x.getName() == "Ali");
        ali.setLocations(db.getLocation().where(x -> x.getName().startsWith("sto")));
        Course c = db.getCourses().first(x -> x.getName().contains("#"));
        c.setTeacher(ali);
        // saving changes
        db.getPerson().update(ali);
        db.getCourses().update(c);
        
        /**
         * Reading Feature
         */
        
        List<Person> people = db.getPerson().getItems(); // fetching all rows in `persons` table
        // printing info
        for(Person p : people)
            System.out.println("ID: " + p.getId() + ", Name: " + p.getName());
        // fetching all rows in `courses` table with teacher assigned to each course, 
        // condition is if teacher id is greater than 0 (JUST FOR TESTING!) I have only two rows in persons :)
        // testing complex where statement :)
        // one to many relationship ..
        List<Course> courses = db.getCourses()
                                .include(x -> x.getTeacher())
                                .where(x -> x.getTeacher().getId() > 0);
        // printing info
        for(Course course : courses) {
            System.out.println("ID: " + course.getId() + ", Name: " + course.getName());
            if(course.getTeacher() != null)
                System.out.println("\tTeacher ID: " + course.getTeacher().getId() 
                        + ", Teacher name: " + course.getTeacher().getName());
        }
        // fetching all rows in `locations` table with persons 
        // many to many relationship ..
        List<Location> locations = db.getLocation()
                                    .include(x -> x.getPersons())
                                    .getItems();
        // printing info
        for(Location location : locations) {
            System.out.println("ID: " + location.getId() + ", Name: " + location.getName());
            if(location.getPersons() != null)
                for(Person person : location.getPersons())
                    System.out.println("\tPerson ID: " + person.getId() + ", Person name: " + person.getName()
                                     + ", Person is teacher: " + person.getIsTeacher());
        }
        
        /**
         * Deleting feature
         */
        // will add a person to delete it, for testing :)
        Person tempPerson = new Person();
        tempPerson.setName("Temp");
        tempPerson.setIsTeacher(true);
        try {
            db.getPerson().add(tempPerson);
        } catch (SQLException ex) {
            Logger.getLogger(Program.class.getName()).log(Level.SEVERE, null, ex);
        }
        // delete it
        db.getPerson().remove(x -> x.getName().equals("Temp"));
    }
}
