package edu.iu.uits.lms.blueprintmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class BlueprintTermModel implements Serializable  {

    @NonNull
    private String termId;

    @NonNull
    private String termName;

    @NonNull
    private List<BlueprintCourseModel> courses;

    private boolean visible;
}
