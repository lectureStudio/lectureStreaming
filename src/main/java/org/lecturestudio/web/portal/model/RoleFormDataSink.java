package org.lecturestudio.web.portal.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleFormDataSink {

    private Role role;

    private List<PrivilegeFormDataSink> privilegeSinks;

}