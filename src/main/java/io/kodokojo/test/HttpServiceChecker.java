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

import com.github.dockerjava.api.command.CreateContainerResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;

public class HttpServiceChecker implements DockerTestApplicationBuilder.ServiceChecker {

    private final int exposedPort;

    private final String location;

    private final int timeout;

    public HttpServiceChecker(int exposedPort, int timeout, String location) {
        if (isBlank(location)) {
            throw new IllegalArgumentException("location must be defined.");
        }
        this.exposedPort = exposedPort;
        this.timeout = timeout;
        this.location = location;
    }

    public HttpServiceChecker(int exposedPort) {
        this(exposedPort, 10000, "/");
    }

    @Override
    public void checkServiceIsRunning(DockerTestSupport dockerTestSupport, CreateContainerResponse createContainerResponse) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().get().url("http://" + dockerTestSupport.getServerIp() + ":" + exposedPort + location).build();
        Response response = null;
        boolean ready = false;

        long timeout = System.currentTimeMillis() + this.timeout;
        do {
            try {
                response = client.newCall(request).execute();
                ready = response.code() >= 200 && response.code() < 400;
            } catch (IOException e) {
                //  Waiting ...
            } finally {
                if (response != null) {
                    IOUtils.closeQuietly(response.body());
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } while (!ready && System.currentTimeMillis() < timeout && !Thread.currentThread().isInterrupted());
    }
}
