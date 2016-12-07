/**
 * Kodo Kojo - Software factory done right
 * Copyright © 2016 Kodo Kojo (infos@kodokojo.io)
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
package io.kodokojo.test.bdd.stage;

import io.kodokojo.commons.model.User;

public class UserInfo {

    private final String username;

    private final String identifier;

    private final String entityId;

    private final String password;

    private final String email;

    public UserInfo(String username, String identifier, String entityId, String password, String email) {
        this.username = username;
        this.entityId = entityId;
        this.identifier = identifier;
        this.password = password;
        this.email = email;
    }

    public UserInfo(User user) {
        this(user.getUsername(), user.getIdentifier(), user.getEntityIdentifier(), user.getPassword(), user.getEmail());
    }

    public String getUsername() {
        return username;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}
