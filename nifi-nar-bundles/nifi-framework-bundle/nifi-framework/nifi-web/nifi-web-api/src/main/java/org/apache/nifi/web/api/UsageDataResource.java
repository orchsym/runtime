/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.web.api;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.stream.io.StreamUtils;
import org.apache.nifi.util.VersionHelper;
import org.apache.nifi.web.NiFiServiceFacade;
import org.apache.nifi.web.api.entity.StatsCountersEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orchsym.udc.manager.UsageDataManager;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.minidev.json.JSONObject;

/**
 * @author GU Guoqiang
 *
 */
@Path(UsageDataResource.PATH)
@Api(value = StatsResource.PATH, description = "Endpoint for collecting the usage data of platform.")
public class UsageDataResource extends ApplicationResource implements ICodeMessages {
    public static final String PATH = "/udc";

    private static final Logger logger = LoggerFactory.getLogger(UsageDataResource.class);

    private NiFiServiceFacade serviceFacade;

    public UsageDataResource() {
        super();
    }

    public void setServiceFacade(NiFiServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the summaries", //
            response = StatsCountersEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getUsageData() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        final JSONObject result = UsageDataManager.get().collect(null);

        return this.generateOkResponse(result).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    @ApiOperation(value = "Gets the list of udc files", //
            response = StatsCountersEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getUsageFiles() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        try {
            UsageDataManager.get().saveToRepository();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return noCache(Response.status(Status.INTERNAL_SERVER_ERROR)).entity(e).build();
        }

        final List<String> dateList = UsageDataManager.get().getDateOfCollectorFiles();

        return this.generateOkResponse(dateList).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    @Path("/download")
    @ApiOperation(value = "download usage data", //
            response = StatsCountersEntity.class //
    )
    @ApiResponses(value = { //
            @ApiResponse(code = 400, message = CODE_MESSAGE_400), //
            @ApiResponse(code = 401, message = CODE_MESSAGE_401), //
            @ApiResponse(code = 403, message = CODE_MESSAGE_403), //
            @ApiResponse(code = 409, message = CODE_MESSAGE_409) //
    })
    public Response getDownloadData() {
        if (isReplicateRequest()) {
            return replicate(HttpMethod.GET);
        }

        try {
            final File zipFile = File.createTempFile("udc", "repo");
            try {
                final List<File> udcFiles = UsageDataManager.get().getCollectorFiles();
                zip(zipFile, udcFiles);

                String fileName = "Orchsym_UDC_" + VersionHelper.INSTANCE.getOrchsymVersion() + ".zip";

                return responseFile(zipFile, fileName, StandardCharsets.UTF_8);
            } finally {
                FileUtils.deleteQuietly(zipFile);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return noCache(Response.status(Status.INTERNAL_SERVER_ERROR)).entity(e).build();
        }
    }

    private void zip(File zipFile, final List<File> filesToArchive) throws IOException, ArchiveException {

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile));
                ZipArchiveOutputStream o = (ZipArchiveOutputStream) new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, out)) {

            for (File f : filesToArchive) {
                ArchiveEntry entry = o.createArchiveEntry(f, f.getName());
                o.putArchiveEntry(entry);
                if (f.isFile()) {
                    try (InputStream i = Files.newInputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                    }
                }
                o.closeArchiveEntry();
            }

            o.finish();
        }
    }

    private Response responseFile(final File file, String fileName, Charset encoding) throws IOException {
        final byte[] byteArray = FileUtils.readFileToByteArray(file);

        // generate a streaming response
        final StreamingOutput response = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                output.write(byteArray);
                output.flush();
            }
        };

        if (null == fileName) {
            fileName = file.getName();
        }
        try {
            fileName = URLEncoder.encode(fileName, encoding.name());
        } catch (UnsupportedEncodingException e) {
            //
        }

        return generateOkResponse(response).type(MediaType.APPLICATION_OCTET_STREAM) //
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename* = " + encoding.name() + "''" + fileName).build();
    }
}
