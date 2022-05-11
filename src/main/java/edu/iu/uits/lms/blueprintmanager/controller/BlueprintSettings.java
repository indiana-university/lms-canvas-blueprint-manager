package edu.iu.uits.lms.blueprintmanager.controller;

import edu.iu.uits.lms.canvas.model.Course;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class BlueprintSettings {

    private Course course;

    private boolean hasAssociations;

    @Accessors(fluent = true)
    private boolean hasEnrollments;

    private boolean alreadyAssociated;

    private BlueprintModel blueprintModel;
}
