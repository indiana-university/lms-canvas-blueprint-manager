package edu.iu.uits.lms.blueprintmanager.model;

import canvas.client.generated.model.BlueprintAssociatedCourse;
import canvas.client.generated.model.CanvasTerm;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class BlueprintAssociationModel implements Serializable {

    private boolean initialized;
    private List<CanvasTerm> termOptions = new ArrayList<>();
    private List<BlueprintTermModel> termModels = new ArrayList<>();
    private List<BlueprintAssociatedCourse> associatedCourses;
    private boolean publishAfterSync;

    public void addTermModel(BlueprintTermModel model) {
        termModels.add(model);
    }

    public void mergeModels(BlueprintAssociationModel model) {
        this.termModels = model.getTermModels();
        List<BlueprintCourseModel> flatCourses = termModels.stream()
                .map(BlueprintTermModel::getCourses)
                .flatMap(Collection::stream)
                .filter(BlueprintCourseModel::isCurrentlyChecked)
                .collect(Collectors.toList());

        associatedCourses = flatCourses.stream().map(foo -> {
            BlueprintAssociatedCourse bac =  new BlueprintAssociatedCourse();
            bac.setId(foo.getCourseId());
            bac.setName(foo.getCourseName());
            bac.setTermName(foo.getTermName());
            return bac;
        }).collect(Collectors.toList());
    }

    public void addTermOption(CanvasTerm termOption) {
        termOptions.add(termOption);
    }

}
