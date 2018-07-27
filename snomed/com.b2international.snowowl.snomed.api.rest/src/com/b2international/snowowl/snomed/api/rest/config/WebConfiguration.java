/*
 * Copyright 2018 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.api.rest.config;

import java.util.List;
import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.b2international.commons.options.Metadata;
import com.b2international.commons.options.MetadataHolder;
import com.b2international.commons.options.MetadataHolderMixin;
import com.b2international.commons.options.MetadataMixin;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.domain.CollectionResource;
import com.b2international.snowowl.datastore.review.BranchState;
import com.b2international.snowowl.datastore.review.Review;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserComponent;
import com.b2international.snowowl.snomed.api.rest.AntPathWildcardMatcher;
import com.b2international.snowowl.snomed.api.rest.domain.BranchMixin;
import com.b2international.snowowl.snomed.api.rest.domain.BranchStateMixin;
import com.b2international.snowowl.snomed.api.rest.domain.CollectionResourceMixin;
import com.b2international.snowowl.snomed.api.rest.domain.ISnomedComponentMixin;
import com.b2international.snowowl.snomed.api.rest.domain.ReviewMixin;
import com.b2international.snowowl.snomed.api.rest.util.CsvMessageConverter;
import com.b2international.snowowl.snomed.core.domain.SnomedComponent;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

/**
 * @since 7.0
 */
@Configuration
@EnableWebMvc
public class WebConfiguration extends WebMvcConfigurerAdapter {

	@Bean
	public ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new GuavaModule());
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		final ISO8601DateFormat df = new ISO8601DateFormat();
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		objectMapper.setDateFormat(df);
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.addMixIn(CollectionResource.class, CollectionResourceMixin.class);
		objectMapper.addMixIn(SnomedComponent.class, ISnomedComponentMixin.class);
		objectMapper.addMixIn(ISnomedBrowserComponent.class, ISnomedComponentMixin.class);
		objectMapper.addMixIn(Branch.class, BranchMixin.class);
		objectMapper.addMixIn(Metadata.class, MetadataMixin.class);
		objectMapper.addMixIn(MetadataHolder.class, MetadataHolderMixin.class);
		objectMapper.addMixIn(Review.class, ReviewMixin.class);
		objectMapper.addMixIn(BranchState.class, BranchStateMixin.class);
		return objectMapper;
	}
	
	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Override
	public void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
		final StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);
		converters.add(stringConverter);

		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new CsvMessageConverter());

		final MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
		jacksonConverter.setObjectMapper(objectMapper());
		converters.add(jacksonConverter);
	}

	@Override
	public void configurePathMatch(final PathMatchConfigurer configurer) {
		configurer.setUseRegisteredSuffixPatternMatch(true);
		configurer.setPathMatcher(new AntPathWildcardMatcher());
	}

	@Override
	public void configureDefaultServletHandling(final DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}
}