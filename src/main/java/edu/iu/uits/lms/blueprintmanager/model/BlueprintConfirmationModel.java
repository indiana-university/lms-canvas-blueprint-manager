package edu.iu.uits.lms.blueprintmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlueprintConfirmationModel implements Serializable  {

    private String blueprintCourseId;
    private List<CourseConfirmation> finalCourses = new ArrayList<>();
    private List<CourseConfirmation> addedCourses = new ArrayList<>();
    private List<CourseConfirmation> removedCourses = new ArrayList<>();
    private boolean publishAfterSync;

    public void addFinalCourse(CourseConfirmation cc) {
        finalCourses.add(cc);
    }

    public void addAddedCourse(CourseConfirmation cc) {
        addedCourses.add(cc);
    }

    public void addRemovedCourse(CourseConfirmation cc) {
        removedCourses.add(cc);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseConfirmation {
        private String courseId;
        private String courseName;
        private String termName;

        public CourseConfirmation(BlueprintCourseModel bcm) {
            this.courseId = bcm.getCourseId();
            this.courseName = bcm.getCourseName();
            this.termName = bcm.getTermName();
        }
    }
}
