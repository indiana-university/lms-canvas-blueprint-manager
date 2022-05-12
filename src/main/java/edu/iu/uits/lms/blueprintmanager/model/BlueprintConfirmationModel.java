package edu.iu.uits.lms.blueprintmanager.model;

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
