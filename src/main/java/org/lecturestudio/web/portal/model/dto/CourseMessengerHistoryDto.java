package org.lecturestudio.web.portal.model.dto;

import java.util.List;
import org.lecturestudio.web.api.message.WebMessage;

import groovy.transform.builder.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CourseMessengerHistoryDto {

    private List<WebMessage> messengerHistory;
    
}
