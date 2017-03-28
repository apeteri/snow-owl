/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.api.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import com.b2international.snowowl.snomed.api.rest.domain.ChangeRequest;
import com.b2international.snowowl.snomed.api.rest.domain.RestApiError;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedRelationshipRestInput;
import com.b2international.snowowl.snomed.api.rest.domain.SnomedRelationshipRestUpdate;
import com.b2international.snowowl.snomed.api.rest.util.DeferredResults;
import com.b2international.snowowl.snomed.api.rest.util.Responses;
import com.b2international.snowowl.snomed.core.domain.ISnomedRelationship;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * @since 1.0
 */
@Api("Relationships")
@RestController
@RequestMapping(
		produces={ AbstractRestService.SO_MEDIA_TYPE })
public class SnomedRelationshipRestService extends AbstractSnomedRestService {

	@ApiOperation(
			value="Create Relationship", 
			notes="Creates a new Relationship directly on a version branch.")
	@ApiResponses({
		@ApiResponse(code = 201, message = "Created"),
		@ApiResponse(code = 404, message = "Branch not found", response = RestApiError.class)
	})
	@RequestMapping(
			value="/{path:**}/relationships", 
			method=RequestMethod.POST, 
			consumes={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Void> create(
			@ApiParam(value="The branch path")
			@PathVariable("path") 
			final String branchPath,
			
			@ApiParam(value="Relationship parameters")
			@RequestBody 
			final ChangeRequest<SnomedRelationshipRestInput> body,
			
			final Principal principal) {

		final String commitComment = body.getCommitComment();
		final String createdRelationshipId = body
				.getChange()
				.toRequestBuilder()
				.build(principal.getName(), branchPath, commitComment)
				.executeSync(bus, 120L * 1000L)
				.getResultAs(String.class);
				
		return Responses.created(getRelationshipLocation(branchPath, createdRelationshipId)).build();
	}

	@ApiOperation(
			value="Retrieve Relationship properties", 
			notes="Returns all properties of the specified Relationship, including the associated refinability value.",
			response=Void.class)
	@ApiResponses({
		@ApiResponse(code = 200, message = "OK"),
		@ApiResponse(code = 404, message = "Branch or Relationship not found", response = RestApiError.class)
	})
	@RequestMapping(value="/{path:**}/relationships/{relationshipId}", method=RequestMethod.GET)
	public DeferredResult<ISnomedRelationship> read(
			@ApiParam(value="The branch path")
			@PathVariable("path") 
			final String branchPath,
			
			@ApiParam(value="The Relationship identifier")
			@PathVariable("relationshipId") 
			final String relationshipId) {

		return DeferredResults.wrap(
				SnomedRequests
					.prepareGetRelationship()
					.setComponentId(relationshipId)
					.build(branchPath)
					.execute(bus));
	}

	@ApiOperation(
			value="Update Relationship",
			notes="Updates properties of the specified Relationship.")
	@ApiResponses({
		@ApiResponse(code = 204, message = "Update successful"),
		@ApiResponse(code = 404, message = "Branch or Relationship not found", response = RestApiError.class)
	})
	@RequestMapping(
			value="/{path:**}/relationships/{relationshipId}/updates", 
			method=RequestMethod.POST,
			consumes={ AbstractRestService.SO_MEDIA_TYPE, MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void update(
			@ApiParam(value="The branch path")
			@PathVariable("path") 
			final String branchPath,
			
			@ApiParam(value="The Relationship identifier")
			@PathVariable("relationshipId") 
			final String relationshipId,
			
			@ApiParam(value="Update Relationship parameters")
			@RequestBody 
			final ChangeRequest<SnomedRelationshipRestUpdate> body,
			
			final Principal principal) {

		final String userId = principal.getName();
		final String commitComment = body.getCommitComment();
		final SnomedRelationshipRestUpdate update = body.getChange();

		SnomedRequests
			.prepareUpdateRelationship(relationshipId)
			.setActive(update.isActive())
			.setModuleId(update.getModuleId())
			.setCharacteristicType(update.getCharacteristicType())
			.setGroup(update.getGroup())
			.setUnionGroup(update.getUnionGroup())
			.setModifier(update.getModifier())
			.build(userId, branchPath, commitComment)
			.executeSync(bus, 120L * 1000L);
	}

	@ApiOperation(
			value="Delete Relationship",
			notes="Permanently removes the specified unreleased Relationship and related components.<p>If the Relationship "
					+ "has already been released, it can not be removed and a <code>409</code> "
					+ "status will be returned.")
	@ApiResponses({
		@ApiResponse(code = 204, message = "Delete successful"),
		@ApiResponse(code = 404, message = "Branch or Relationship not found", response = RestApiError.class),
		@ApiResponse(code = 409, message = "Relationship cannot be deleted", response = RestApiError.class)
	})
	@RequestMapping(value="/{path:**}/relationships/{relationshipId}", method=RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@ApiParam(value="The branch path")
			@PathVariable("path") 
			final String branchPath,
			
			@ApiParam(value="The Relationship identifier")
			@PathVariable("relationshipId") 
			final String relationshipId,
			
			final Principal principal) {

		SnomedRequests
			.prepareDeleteRelationship()
			.setComponentId(relationshipId)
			.build(principal.getName(), branchPath, String.format("Deleted Relationship '%s' from store.", relationshipId))
			.executeSync(bus, 120L * 1000L);
	}

	private URI getRelationshipLocation(final String branchPath, final String relationshipId) {
		return linkTo(SnomedRelationshipRestService.class).slash(branchPath).slash("relationships").slash(relationshipId).toUri();
	}
}