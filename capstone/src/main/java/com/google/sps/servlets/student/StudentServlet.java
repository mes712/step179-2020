package com.google.sps.servlets;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns a student's profile content */
@WebServlet("/student-data")
public class StudentServlet extends HttpServlet {
  private static String TIMEZONE_PST = "PST";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get student object based on the logged in email
    UserService userService = UserServiceFactory.getUserService();
    String userEmail = userService.getCurrentUser().getEmail();
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity currentStudent = getStudent(userEmail, datastore);

    // Get club list from entity and convert to an ImmutableList
    // Initally empty in case there is no club list
    ImmutableList<String> clubs = ImmutableList.of();
    if (currentStudent.getProperty(Constants.PROPERTY_CLUBS) != null) {
      clubs =
          ImmutableList.copyOf(
              (ArrayList<String>) currentStudent.getProperty(Constants.PROPERTY_CLUBS));
    }

    // Create Student object based on stored information
    Student student =
        new Student(
            currentStudent.getProperty(Constants.PROPERTY_NAME).toString(),
            Integer.parseInt(currentStudent.getProperty(Constants.PROPERTY_GRADYEAR).toString()),
            currentStudent.getProperty(Constants.PROPERTY_MAJOR).toString(),
            currentStudent.getProperty(Constants.PROPERTY_EMAIL).toString(),
            clubs);

    ImmutableList<String> announcements = getAllAnnouncements(student.getClubList(), datastore);
    StudentInfo allInfo = new StudentInfo(student, announcements);
    String studentJson = convertToJsonUsingGson(allInfo);

    response.setContentType("application/json;");
    response.getWriter().println(studentJson);
  }

  private static String convertToJsonUsingGson(StudentInfo info) {
    Gson gson = new Gson();
    String json = gson.toJson(info);
    return json;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: All commented steps below
    // 1. Remove club from logged in student's club list if requested
    // 2. Update student information with edited content

    // Get student object based on the logged in email
    UserService userService = UserServiceFactory.getUserService();
    String userEmail = userService.getCurrentUser().getEmail();
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity student = getStudent(userEmail, datastore);

    String clubToJoin = request.getParameter(Constants.JOIN_CLUB_PROP);
    if (clubToJoin != null && !clubToJoin.isEmpty()) {
      // Add member to club's member list and update Datastore
      Entity club = retrieveClub(clubToJoin, datastore, response);
      if (club == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      addItemToEntity(club, datastore, userEmail, Constants.MEMBER_PROP);

      // Add new club to student's club list and update Datastore
      addItemToEntity(student, datastore, clubToJoin, Constants.PROPERTY_CLUBS);
      response.sendRedirect("/about-us.html?name=" + club.getProperty(Constants.PROPERTY_NAME));
    } else {
      response.sendRedirect("profile.html");
    }
  }

  private Entity getStudent(String userEmail, DatastoreService datastore) {
    // Get the user's information from Datastore
    Query query = new Query(userEmail);
    PreparedQuery results = datastore.prepare(query);
    ImmutableList<Entity> students = ImmutableList.copyOf(results.asIterable());

    // Add user to Datastore if this is the first time they login
    if (students.isEmpty()) {
      Entity studentEntity = createStudentEntity(userEmail);
      datastore.put(studentEntity);
      results = datastore.prepare(query);
      students = ImmutableList.copyOf(results.asIterable());
    }
    // A user can only be logged in with one email address at a time
    Entity currentStudent = students.get(0);
    return currentStudent;
  }

  private Entity createStudentEntity(String userEmail) {
    Entity studentEntity = new Entity(userEmail);
    studentEntity.setProperty(Constants.PROPERTY_NAME, "First Last");
    studentEntity.setProperty(Constants.PROPERTY_EMAIL, userEmail);
    studentEntity.setProperty(Constants.PROPERTY_GRADYEAR, 0);
    studentEntity.setProperty(Constants.PROPERTY_MAJOR, "Enter your major here");
    studentEntity.setProperty(Constants.PROPERTY_CLUBS, ImmutableList.of());
    return studentEntity;
  }

  private ImmutableList<String> getAllAnnouncements(
      ImmutableList<String> clubNames, DatastoreService datastore) {
    ImmutableList<String> announcements =
        Streams.stream(clubNames)
            .flatMap(clubName -> getClubAnnouncements(clubName, datastore).stream())
            .collect(toImmutableList());
    return announcements;
  }

  private ImmutableList<String> getClubAnnouncements(String clubName, DatastoreService datastore) {
    // Get all announcements from given club name in reverse chronological order
    Query query =
        new Query(Constants.ANNOUNCEMENT_PROP)
            .setFilter(new FilterPredicate(Constants.CLUB_PROP, FilterOperator.EQUAL, clubName))
            .addSort(Constants.TIME_PROP, SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    // Stream through results and get formatted announcements
    ImmutableList<String> announcements =
        Streams.stream(results.asIterable())
            .map(StudentServlet::getAnnouncementAsString)
            .collect(toImmutableList());
    return announcements;
  }

  private static String getAnnouncementAsString(Entity announcement) {
    // Set calendar timezone and time
    TimeZone timePST = TimeZone.getTimeZone(TIMEZONE_PST);
    Calendar calendar = Calendar.getInstance(timePST);
    calendar.setTimeInMillis(
        Long.parseLong(announcement.getProperty(Constants.TIME_PROP).toString()));

    // Get formatted date and time
    DateFormat formatDate = new SimpleDateFormat("HH:mm MM-dd-yyyy");
    formatDate.setTimeZone(timePST);
    String time = formatDate.format(calendar.getTime());

    String fullAnnouncement =
        String.format(
            "%1$s from %2$s in %3$s sent at %4$s",
            announcement.getProperty(Constants.CONTENT_PROP),
            announcement.getProperty(Constants.AUTHOR_PROP),
            announcement.getProperty(Constants.CLUB_PROP),
            time);
    return fullAnnouncement;
  }

  private Entity retrieveClub(
      String clubName, DatastoreService datastore, HttpServletResponse response) {
    Query query =
        new Query("Club")
            .setFilter(
                new FilterPredicate(Constants.PROPERTY_NAME, FilterOperator.EQUAL, clubName));
    PreparedQuery results = datastore.prepare(query);
    ImmutableList<Entity> clubs = ImmutableList.copyOf(results.asIterable());
    return clubs.isEmpty() ? null : clubs.get(0);
  }

  private void addItemToEntity(
      Entity entity, DatastoreService datastore, String itemToAdd, String property) {
    // Create empty List if property does not exist yet
    List<String> generalList = new ArrayList<String>();
    if (entity.getProperty(property) != null) {
      generalList = ((ArrayList<String>) entity.getProperty(property));
    }
    if (!generalList.contains(itemToAdd)) {
      generalList.add(itemToAdd);
    }
    // Add updated entity to Datastore
    entity.setProperty(property, generalList);
    datastore.put(entity);
  }
}

class StudentInfo {
  private Student student;
  private ImmutableList<String> announcements;

  public StudentInfo(Student student, ImmutableList<String> announcements) {
    this.student = student;
    this.announcements = announcements;
  }
}
