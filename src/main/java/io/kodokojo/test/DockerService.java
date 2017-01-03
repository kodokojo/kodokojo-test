/**
 * Kodo Kojo - ${project.description}
 * Copyright © 2017 Kodo Kojo (infos@kodokojo.io)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.kodokojo.test;

import io.kodokojo.commons.model.PortDefinition;
import io.kodokojo.commons.model.Service;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DockerService extends Service {

    private final String containerId;

    public DockerService(String containerId, String name, String host, PortDefinition portDefinition) {
        super(name, host, portDefinition);
        if (isBlank(containerId)) {
            throw new IllegalArgumentException("containerId must be defined.");
        }
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }
}
