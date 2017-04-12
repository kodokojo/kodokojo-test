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
package io.kodokojo.test.bdd.stage;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Injector;
import com.tngtech.jgiven.CurrentStep;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.attachment.Attachment;
import io.kodokojo.commons.RSAUtils;
import io.kodokojo.commons.dto.BrickConfigDto;
import io.kodokojo.commons.dto.ProjectConfigurationCreationDto;
import io.kodokojo.commons.dto.StackConfigDto;
import io.kodokojo.commons.event.Event;
import io.kodokojo.commons.event.EventBuilder;
import io.kodokojo.commons.event.payload.UserCreationReply;
import io.kodokojo.commons.event.payload.UserCreationRequest;
import io.kodokojo.commons.model.*;
import io.kodokojo.commons.model.Stack;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.commons.service.repository.Repository;
import io.kodokojo.test.MicroServiceTesterMock;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ApplicationWhen<SELF extends ApplicationWhen<?>> extends Stage<SELF> {

    @ExpectedScenarioState
    String restEntryPointHost;

    @ExpectedScenarioState
    int restEntryPointPort;

    @ProvidedScenarioState
    String newUserId;

    @ProvidedScenarioState
    String projectConfigurationId;

    @ExpectedScenarioState
    Map<String, UserInfo> currentUsers;

    @ExpectedScenarioState
    String currentUserLogin;

    @ExpectedScenarioState
    CurrentStep currentStep;

    @ExpectedScenarioState
    HttpUserSupport httpUserSupport;

    @ExpectedScenarioState
    MicroServiceTesterMock microServiceTesterMock;

    @ExpectedScenarioState
    Injector injector;

    @ExpectedScenarioState
    boolean waitingList;

    public SELF retrive_a_new_id() {

        microServiceTesterMock.addCallback((event, eventBuilderFactory, eventBus) -> {
            if (event.getEventType().equals(Event.USER_IDENTIFIER_CREATION_REQUEST)) {
                MessageDigest messageDigest = null;
                try {
                    messageDigest = MessageDigest.getInstance("SHA-1");
                    Repository repository = injector.getInstance(Repository.class);
                    String newId = repository.generateId();
                    EventBuilder eventBuilder = eventBuilderFactory.create();
                    eventBuilder.setCorrelationId(event.getCorrelationId());
                    eventBuilder.setEventType(Event.USER_IDENTIFIER_CREATION_REPLY);
                    eventBuilder.setJsonPayload(newId);
                    eventBus.reply(event, eventBuilder.build());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        });

        OkHttpClient httpClient = new OkHttpClient();
        RequestBody emptyBody = RequestBody.create(null, new byte[0]);
        String baseUrl = getBaseUrl();
        Request request = new Request.Builder().post(emptyBody).url(baseUrl + "/api/v1/user").build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            if (response.code() != 200) {
                fail("Invalid HTTP code status " + response.code() + " expected 200");
            }
            newUserId = response.body().string();

        } catch (IOException e) {
            fail(e.getMessage(), e);
        } finally {
            if (response != null) {
                IOUtils.closeQuietly(response.body());
            }
        }
        return self();
    }

    public SELF create_user_with_email_$(@Quoted String email) {
        return create_user(email, true);
    }

    public SELF create_user_with_email_$_which_must_fail(@Quoted String email) {
        return create_user(email, false);
    }

    private SELF create_user(String email, boolean success) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("email must be defined.");
        }
        if (success && StringUtils.isBlank(newUserId)) {
            retrive_a_new_id();
        }
        //  TODO Refacto: Use HttpUserSupport instead
        microServiceTesterMock.addCallback((event, eventBuilderFactory, eventBus) -> {
            if (event.getEventType().equals(Event.USER_CREATION_REQUEST)) {
                UserCreationRequest creationRequest = event.getPayload(UserCreationRequest.class);
                EventBuilder eventBuilder = eventBuilderFactory.create();
                eventBuilder.setEventType(Event.USER_CREATION_REPLY);
                eventBuilder.setCorrelationId(event.getCorrelationId());
                if (waitingList) {
                    UserCreationReply userCreationReply = new UserCreationReply(creationRequest.getId(), null, creationRequest.getEmail(), true, false);
                    eventBuilder.setPayload(userCreationReply);
                } else {
                    try {
                        Repository repository = injector.getInstance(Repository.class);
                        boolean expectedNewUser = repository.identifierExpectedNewUser(creationRequest.getId());
                        if (expectedNewUser) {
                            boolean entityNotExist = false;
                            String entity = creationRequest.getOrganisationId();
                            if (StringUtils.isBlank(creationRequest.getOrganisationId())) {
                                entity = repository.addOrganisation(new Organisation(creationRequest.getEmail()));
                                entityNotExist = true;
                            }

                            KeyPair keyPair = RSAUtils.generateRsaKeyPair();
                            User user = new UserBuilder()
                                    .setIdentifier(creationRequest.getId())
                                    .setEmail(creationRequest.getEmail())
                                    .setEntityIdentifier(entity)
                                    .setFirstName(creationRequest.getUsername())
                                    .setLastName(creationRequest.getUsername())
                                    .setUsername(creationRequest.getUsername())
                                    .setPassword("1234")
                                    .setSshPublicKey(RSAUtils.encodePublicKey((RSAPublicKey) keyPair.getPublic(), creationRequest.getEmail()))
                                    .build();
                            repository.addUser(user);
                            if (entityNotExist) {
                                repository.addAdminToOrganisation(creationRequest.getId(), entity);
                            } else {
                                repository.addUserToOrganisation(creationRequest.getId(), entity);
                            }
                            UserCreationReply userCreationReply = new UserCreationReply(creationRequest.getId(), keyPair, creationRequest.getEmail(), false, true);

                            eventBuilder.setPayload(userCreationReply);
                        } else {
                            UserCreationReply userCreationReply = new UserCreationReply(creationRequest.getId(), null, creationRequest.getEmail(), false, false);
                            eventBuilder.setPayload(userCreationReply);
                        }
                    } catch (NoSuchAlgorithmException e) {
                        fail(e.getMessage());
                    }
                }
                eventBus.reply(event, eventBuilder.build());
            }
        });

        OkHttpClient httpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), ("{\"email\": \"" + email + "\" }").getBytes());
        String baseUrl = getBaseUrl();

        Request.Builder builder = new Request.Builder().url(baseUrl + "/api/v1/user" + (newUserId != null ? "/" + newUserId : ""));
        if (isNotBlank(currentUserLogin)) {
            UserInfo currentUser = currentUsers.get(currentUserLogin);
            if (currentUser != null) {
                builder = HttpUserSupport.addBasicAuthentification(currentUser, builder);
                body = RequestBody.create(MediaType.parse("application/json"), ("{\"email\": \"" + email + "\", \"organisationId\": \"" + currentUser.getEntityIds().iterator().next() + "\"}").getBytes());
            }
        }
        Request request = builder.post(body).build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            if (success) {
                switch (response.code()) {
                    case 201:
                        JsonParser parser = new JsonParser();
                        String bodyResponse = response.body().string();
                        System.out.println(bodyResponse);
                        JsonObject json = (JsonObject) parser.parse(bodyResponse);
                        String currentUsername = json.getAsJsonPrimitive("username").getAsString();
                        String currentUserPassword = json.getAsJsonPrimitive("password").getAsString();
                        String currentUserEmail = json.getAsJsonPrimitive("email").getAsString();
                        String currentUserIdentifier = json.getAsJsonPrimitive("identifier").getAsString();
                        JsonArray entityIdentifiers = json.getAsJsonArray("organisationIdentifiers");
                        String currentUserEntityIdentifier = entityIdentifiers.get(0).getAsString();
                        currentUsers.put(currentUsername, new UserInfo(currentUsername, currentUserIdentifier, currentUserEntityIdentifier, currentUserPassword, currentUserEmail));
                        if (isBlank(currentUserLogin)) {
                            currentUserLogin = currentUsername;
                        }
                        Attachment privateKey = Attachment.plainText(bodyResponse).withTitle(currentUsername + " response");
                        currentStep.addAttachment(privateKey);
                        break;
                    case 202:

                        break;
                    default:
                        fail("Unexpected return code " + response.code());
                        break;
                }


            } else {
                assertThat(response.code()).isNotEqualTo(201);
            }
            response.body().close();
        } catch (IOException e) {
            if (success) {
                fail(e.getMessage(), e);
            }
        } finally {
            if (response != null) {
                IOUtils.closeQuietly(response.body());
            }
        }
        return self();
    }

    public SELF create_a_new_default_project_configuration_with_name_$(String projectName) {
        return createProjectConfiguration(projectName, null);
    }

    public SELF create_a_small_custom_project_configuration_with_name_$_and_only_brick_type_$(String projectName, String brickName) {

        BrickConfiguration brickConfiguration = new BrickConfiguration("test", BrickType.MONITORING, "1.0.1", Collections.singleton(new PortDefinition(80)));
        BrickConfigDto brickConfigDto = new BrickConfigDto(brickName, brickConfiguration.getType().name(), brickConfiguration.getVersion());
        StackConfigDto stackConfigDto = new StackConfigDto("build-A", StackType.BUILD.name(), Collections.singletonList(brickConfigDto));
        return createProjectConfiguration(projectName, stackConfigDto);
    }

    public SELF createProjectConfiguration(String projectName, StackConfigDto stackConfigDto) {
        if (isBlank(projectName)) {
            throw new IllegalArgumentException("projectName must be defined.");
        }

        microServiceTesterMock.addCallback((event, eventBuilderFactory, eventBus) -> {
            if (Event.PROJECTCONFIG_CREATION_REQUEST.equals(event.getEventType())) {
                ProjectConfigurationCreationDto dto = event.getPayload(ProjectConfigurationCreationDto.class);
                Repository repository = injector.getInstance(Repository.class);
                ProjectConfigurationBuilder projectConfigurationBuilder = new ProjectConfigurationBuilder();
                projectConfigurationBuilder.setName(dto.getName());
                List<User> users = new ArrayList<>();
                users.add(repository.getUserByIdentifier(dto.getOwnerIdentifier()));
                if (dto.getUserIdentifiers() != null) {
                    dto.getUserIdentifiers().stream().map(repository::getUserByIdentifier).collect(Collectors.toList());
                }
                projectConfigurationBuilder.setAdmins(users);
                projectConfigurationBuilder.setUsers(users);
                String entityIdentifier = dto.getOrganisationIdentifier();
                if (StringUtils.isBlank(entityIdentifier)) {
                    User user = repository.getUserByIdentifier(dto.getOwnerIdentifier());
                    entityIdentifier = user.getOrganisationIds().iterator().next();
                }
                projectConfigurationBuilder.setEntityIdentifier(entityIdentifier);
                if (dto.getStackConfigs() == null) {
                    StackConfigurationBuilder stackConfigurationBuilder = new StackConfigurationBuilder();
                    stackConfigurationBuilder.setType(StackType.BUILD);
                    stackConfigurationBuilder.setName("Build-A");
                    Set<BrickConfiguration> bricks = new HashSet<>();
                    HashSet<PortDefinition> portDefinitions = new HashSet<>();
                    portDefinitions.add(new PortDefinition(80));
                    bricks.add(new BrickConfiguration("test", BrickType.SCM, "1.0.0", portDefinitions));
                    stackConfigurationBuilder.setBrickConfigurations(bricks);
                    projectConfigurationBuilder.setStackConfigurations(Collections.singleton(stackConfigurationBuilder.build()));
                } else {
                    projectConfigurationBuilder.setStackConfigurations(dto.getStackConfigs().stream().map(stackDto -> {
                        StackConfigurationBuilder stackConfigurationBuilder = new StackConfigurationBuilder();
                        stackConfigurationBuilder.setType(StackType.valueOf(stackConfigDto.getType()));
                        stackConfigurationBuilder.setName(stackConfigDto.getName());
                        HashSet<PortDefinition> portDefinitions = new HashSet<>();
                        portDefinitions.add(new PortDefinition(80));
                        stackConfigDto.getBrickConfigs().stream().map(brickConfigDto -> new BrickConfiguration(brickConfigDto.getName(), BrickType.valueOf(brickConfigDto.getType()), brickConfigDto.getVersion(), portDefinitions));
                        return stackConfigurationBuilder.build();
                    }).collect(Collectors.toSet()));
                }
                KeyPair keyPair = null;
                try {
                    keyPair = RSAUtils.generateRsaKeyPair();
                } catch (NoSuchAlgorithmException e) {
                    fail(e.getMessage());
                }
                UserService userService = new UserService("8976", "userService", "userService", "userService", (RSAPrivateKey) keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());
                repository.addUserService(userService);
                projectConfigurationBuilder.setUserService(userService);
                String projectConfiguration = repository.addProjectConfiguration(projectConfigurationBuilder.build());
                EventBuilder eventBuilder = eventBuilderFactory.create();
                eventBuilder.setEventType(Event.PROJECTCONFIG_CREATION_REPLY);
                eventBuilder.setJsonPayload(projectConfiguration);
                eventBus.reply(event, eventBuilder.build());
            }
        });

        //  Mock behavior
        BootstrapStackData boostrapData = new BootstrapStackData(projectName, 10022);
        //Mockito.when(projectManager.bootstrapStack(projectName, "build-A", StackType.BUILD)).thenReturn(boostrapData);
        KeyPair keyPair = null;
        try {
            keyPair = RSAUtils.generateRsaKeyPair();
        } catch (NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
        Set<Stack> stacks = new HashSet<>();
        stacks.add(new Stack("build-A", StackType.BUILD, new HashSet<BrickStateEvent>()));
        Project project = new Project("1234567890", projectName, new Date(), stacks);


        projectConfigurationId = httpUserSupport.createProjectConfiguration(projectName, stackConfigDto, currentUsers.get(currentUserLogin));

        String projectConfiguration = getProjectConfiguration(currentUserLogin, projectConfigurationId);
        currentStep.addAttachment(Attachment.plainText(projectConfiguration).withTitle("Project configuration for " + projectName).withFileName("projectconfiguration_" + projectName + ".json"));

        return self();
    }

    private String getProjectConfiguration(String username, String projectConfigurationId) {
        UserInfo userInfo = currentUsers.get(username);
        Request.Builder builder = new Request.Builder().get().url(getApiBaseUrl() + "/projectconfig/" + projectConfigurationId);
        Request request = HttpUserSupport.addBasicAuthentification(userInfo, builder).build();
        Response response = null;
        try {
            OkHttpClient httpClient = new OkHttpClient();
            response = httpClient.newCall(request).execute();
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            return body;
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (response != null) {
                IOUtils.closeQuietly(response.body());
            }
        }
        return null;

    }

    public SELF add_user_$_to_project_configuration(@Quoted String username) {
        UserInfo currentUser = currentUsers.get(currentUserLogin);
        UserInfo userToAdd = currentUsers.get(username);

        httpUserSupport.addUserToProjectConfiguration(projectConfigurationId, currentUser, userToAdd);

        return self();
    }

    public SELF remove_user_$_to_project_configuration(String usernameToDelete) {
        UserInfo currentUser = currentUsers.get(currentUserLogin);
        UserInfo userToAdd = currentUsers.get(usernameToDelete);
        httpUserSupport.removeUserToProjectConfiguration(projectConfigurationId, currentUser, userToAdd);

        return self();
    }

    public SELF update_user_$_with_password_$(@Quoted String username, @Quoted String password, boolean updateSSH) {
        UserInfo currentUser = currentUsers.get(currentUserLogin);
        UserInfo userToChange = currentUsers.get(username);
        UserInfo userChanged = new UserInfo(userToChange.getUsername(), userToChange.getIdentifier(), userToChange.getEntityIds(), password, userToChange.getEmail());
        httpUserSupport.updateUser(currentUser, userChanged);
        return self();
    }

    private String getBaseUrl() {
        return "http://" + restEntryPointHost + ":" + restEntryPointPort;
    }

    private String getApiBaseUrl() {
        return getBaseUrl() + "/api/v1";
    }

}
