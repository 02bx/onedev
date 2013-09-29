package com.pmease.gitop.web.resource;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.codahale.dropwizard.jackson.Jackson;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pmease.gitop.web.SitePaths;

@Path("/file")
public class TestResource {

	private static final String SUCCESS_RESPONSE = "Successful";
	private static final String FAILED_RESPONSE = "Failed";

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@Context HttpServletRequest request) throws JsonProcessingException {
		String candidateName = null;
		File uploadDir = SitePaths.get().uploadsDir();

		Result result = new Result();
		
		// checks whether there is a file upload request or not
		if (ServletFileUpload.isMultipartContent(request)) {
			final FileItemFactory factory = new DiskFileItemFactory();
			final ServletFileUpload fileUpload = new ServletFileUpload(factory);
			
			try {
				/*
				 * parseRequest returns a list of FileItem but in old
				 * (pre-java5) style
				 */
				final List<FileItem> items = fileUpload.parseRequest(request);

				if (items != null) {
					final Iterator<FileItem> iter = items.iterator();
					while (iter.hasNext()) {
						final FileItem item = iter.next();
						final String itemName = item.getName();
						final String fieldName = item.getFieldName();
						final String fieldValue = item.getString();

						if (item.isFormField()) {
							candidateName = fieldValue;
							System.out.println("Field Name: " + fieldName
									+ ", Field Value: " + fieldValue);
							System.out.println("Candidate Name: "
									+ candidateName);
						} else {
							final File savedFile = new File(uploadDir, itemName);
							System.out.println("Saving the file: "
									+ savedFile.getName());
							item.write(savedFile);

							UploadFile f = new UploadFile();
							f.name = savedFile.getName();
							f.size = savedFile.length();
							result.files.add(f);
						}

					}
				}
			} catch (FileUploadException fue) {
				fue.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ObjectMapper mapper = Jackson.newObjectMapper();
		String str = mapper.writeValueAsString(result);
		
		System.out.println("Returned Response Status: " + str);
		return Response.ok().entity(str).build();
	}
	
	static class Result {
		@JsonProperty
		List<UploadFile> files = Lists.newArrayList();
	}
	
	static class UploadFile {
		@JsonProperty
		String name;
		@JsonProperty
		long size;
	}
}
