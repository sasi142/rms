package controllers.exceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.exceptions.ApplicationException;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceAlreadyExistsException;
import core.exceptions.ResourceNotFoundException;
import core.exceptions.UnAuthorizedException;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

@Component
public class HttpExceptionHandler {
	final static Logger logger = LoggerFactory.getLogger(HttpExceptionHandler.class);
	public Result handleException(Throwable ex, Integer status) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		logger.error("exception is ", ex);
		if (ex instanceof ApplicationException) {
			ApplicationException e = (ApplicationException) ex;
			node.put("code", e.getCode().getId());
			node.put("text", e.getMessage());
			logger.error("detailed exception is ", e.getEx());
		} else {
			logger.error("exception is ", ex);
		}
		return Results.status(status, node.toString());
	}


	public Integer getStatus(Throwable ex) {
		int status = 500;
		if (ex instanceof ApplicationException) {
			if (ex instanceof BadRequestException) {
				status = 400;
			} else if (ex instanceof UnAuthorizedException) {
				status = 401;
			} else if (ex instanceof ForbiddenException) {
				status = 403;
			} else if (ex instanceof ResourceNotFoundException) {
				status = 404;
			} else if (ex instanceof ResourceAlreadyExistsException) {
				status = 409;
			} else if (ex instanceof InternalServerErrorException) {
				status = 500;
			} else {
				status = 500;
			}
		}
		else {
			status = 500;
		}
		logger.error("status: ", status);
		return status;
	}
}
