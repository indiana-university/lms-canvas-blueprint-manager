package edu.iu.uits.lms.blueprintmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintCourseModel implements Serializable  {

    private String courseId;
    private String courseName;
    private String termName;
    private boolean originallyChecked;
    private boolean currentlyChecked;

    public BlueprintCourseModel(String courseId, String courseName, List<String> associatedCourseIds) {
        this.courseId = courseId;
        this.courseName = courseName;
        if (associatedCourseIds.contains(courseId)) {
            this.originallyChecked = true;
            this.currentlyChecked = true;
        }
    }
}
