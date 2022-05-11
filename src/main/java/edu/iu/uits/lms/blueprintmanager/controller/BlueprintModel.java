package edu.iu.uits.lms.blueprintmanager.controller;

import edu.iu.uits.lms.canvas.helpers.BlueprintHelper;
import edu.iu.uits.lms.canvas.model.BlueprintRestriction;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class BlueprintModel {

    private String courseId;
    private boolean enableBlueprint;
    private RADIO_OPTION radioOption;
    private BlueprintRestriction blueprintRestrictions;
    private Map<String, BlueprintRestriction> blueprintRestrictionsByObjectType = new HashMap<>();

    private boolean copySettings;
    private boolean sendNotifications;

    private static final Map<String, List<String>> objectMap;

    static {
        Map<String, List<String>> aMap = new LinkedHashMap<>();
        aMap.put(BlueprintHelper.CONTENT_TYPE.assignment.name(), Arrays.asList(BlueprintHelper.CONTENT_SETTINGS.content.name(),
              BlueprintHelper.CONTENT_SETTINGS.points.name(),
              BlueprintHelper.CONTENT_SETTINGS.due_dates.getKeyText(),
              BlueprintHelper.CONTENT_SETTINGS.availability_dates.getKeyText()));
        aMap.put(BlueprintHelper.CONTENT_TYPE.discussion_topic.name(), Arrays.asList(BlueprintHelper.CONTENT_SETTINGS.content.name(),
              BlueprintHelper.CONTENT_SETTINGS.points.name(),
              BlueprintHelper.CONTENT_SETTINGS.due_dates.getKeyText(),
              BlueprintHelper.CONTENT_SETTINGS.availability_dates.getKeyText()));
        aMap.put(BlueprintHelper.CONTENT_TYPE.wiki_page.name(), Collections.singletonList(BlueprintHelper.CONTENT_SETTINGS.content.name()));
        aMap.put(BlueprintHelper.CONTENT_TYPE.attachment.name(), Collections.singletonList(BlueprintHelper.CONTENT_SETTINGS.content.name()));
        aMap.put(BlueprintHelper.CONTENT_TYPE.quiz.name(), Arrays.asList(BlueprintHelper.CONTENT_SETTINGS.content.name(),
              BlueprintHelper.CONTENT_SETTINGS.points.name(),
              BlueprintHelper.CONTENT_SETTINGS.due_dates.getKeyText(),
              BlueprintHelper.CONTENT_SETTINGS.availability_dates.getKeyText()));
        objectMap = Collections.unmodifiableMap(aMap);
    }

    public Map getObjectMap() {
        return objectMap;
    }

    public enum RADIO_OPTION {
        restrictions, restrictionsByType
    }
}
