package edu.iu.uits.lms.blueprintmanager.controller;

/*-
 * #%L
 * blueprint-manager
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
